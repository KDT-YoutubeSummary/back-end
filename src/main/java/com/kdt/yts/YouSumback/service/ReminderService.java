package com.kdt.yts.YouSumback.service;

import com.kdt.yts.YouSumback.model.dto.request.ReminderCreateRequestDTO;
import com.kdt.yts.YouSumback.model.dto.response.ReminderResponse;
import com.kdt.yts.YouSumback.model.dto.request.ReminderUpdateRequestDTO;
import com.kdt.yts.YouSumback.model.entity.Reminder;
import com.kdt.yts.YouSumback.model.entity.ReminderType;
import com.kdt.yts.YouSumback.model.entity.User;
import com.kdt.yts.YouSumback.model.entity.UserLibrary;
import com.kdt.yts.YouSumback.exception.ResourceNotFoundException;
import com.kdt.yts.YouSumback.repository.ReminderRepository;
import com.kdt.yts.YouSumback.repository.UserRepository;
import com.kdt.yts.YouSumback.repository.UserLibraryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek; // 요일 정보
import java.time.LocalDateTime; // 날짜와 시간 정보
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters; // 날짜 조정을 위한 유틸리티
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReminderService {

    private final ReminderRepository reminderRepository; // 의존성 주입
    private final UserRepository userRepository;
    private final UserLibraryRepository userLibraryRepository;
    private final EmailService emailService;

    // ---------------------- 리마인더 (C)생성, (R)조회, (U)수정, (D)삭제 ----------------------

    @Transactional
    public ReminderResponse createReminder(ReminderCreateRequestDTO request) {
        // 사용자 및 사용자 라이브러리 존재 여부 확인
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + request.getUserId()));
        UserLibrary userLibrary = userLibraryRepository.findById(request.getUserLibraryId())
                .orElseThrow(() -> new ResourceNotFoundException("UserLibrary not found with ID: " + request.getUserLibraryId()));

        Reminder reminder = new Reminder();
        reminder.setUser(user);
        reminder.setUserLibrary(userLibrary);
        reminder.setReminderType(request.getReminderType());
        reminder.setFrequencyInterval(request.getFrequencyInterval() != null ? request.getFrequencyInterval() : 1); // 기본값 1
        reminder.setDayOfWeek(request.getDayOfWeek());
        reminder.setDayOfMonth(request.getDayOfMonth());
        reminder.setBaseDatetimeForRecurrence(request.getBaseDatetimeForRecurrence());
        reminder.setReminderNote(request.getReminderNote());
        reminder.setIsActive(request.getIsActive() != null ? request.getIsActive() : true); // 기본값 true

        // 다음 알림 시간을 계산하여 설정합니다.
        reminder.setNextNotificationDatetime(calculateNextNotificationTime(request.getBaseDatetimeForRecurrence(), request.getReminderType(),
                reminder.getFrequencyInterval(), request.getDayOfWeek(), request.getDayOfMonth(), LocalDateTime.now()));

        Reminder savedReminder = reminderRepository.save(reminder); // 데이터베이스에 리마인더 저장
        log.info("Created reminder with ID: {}", savedReminder.getId());
        return new ReminderResponse(savedReminder); // 저장된 리마인더를 DTO로 변환하여 반환
    }

    @Transactional(readOnly = true) // 읽기 전용 트랜잭션으로 설정하여 성능을 최적화합니다.
    public ReminderResponse getReminderById(Long reminderId) {
        Reminder reminder = reminderRepository.findById(reminderId)
                .orElseThrow(() -> new ResourceNotFoundException("Reminder not found with ID: " + reminderId));
        return new ReminderResponse(reminder);
    }

    @Transactional(readOnly = true) // 읽기 전용 트랜잭션
    public List<ReminderResponse> getRemindersByUserId(Long userId) {
        List<Reminder> reminders = reminderRepository.findByUser_Id(userId);
        return reminders.stream()
                .map(ReminderResponse::new) // 각 Reminder 엔티티를 ReminderResponse DTO로 매핑
                .collect(Collectors.toList());
    }

    @Transactional // 수정 작업은 트랜잭션으로 묶습니다.
    public ReminderResponse updateReminder(Long reminderId, ReminderUpdateRequestDTO request) {
        Reminder reminder = reminderRepository.findById(reminderId)
                .orElseThrow(() -> new ResourceNotFoundException("Reminder not found with ID: " + reminderId));

        // 요청에 따라 필드를 업데이트합니다. (null이 아닌 경우에만 업데이트)
        if (request.getReminderType() != null) {
            reminder.setReminderType(request.getReminderType());
        }
        if (request.getFrequencyInterval() != null) {
            reminder.setFrequencyInterval(request.getFrequencyInterval());
        }
        if (request.getDayOfWeek() != null) {
            reminder.setDayOfWeek(request.getDayOfWeek());
        }
        if (request.getDayOfMonth() != null) {
            reminder.setDayOfMonth(request.getDayOfMonth());
        }
        if (request.getBaseDatetimeForRecurrence() != null) {
            reminder.setBaseDatetimeForRecurrence(request.getBaseDatetimeForRecurrence());
        }
        if (request.getReminderNote() != null) {
            reminder.setReminderNote(request.getReminderNote());
        }
        if (request.getIsActive() != null) {
            reminder.setIsActive(request.getIsActive());
        }

        // 리마인더 타입이나 기준 시간이 변경되면 다음 알림 시간을 재계산합니다.
        if (request.getReminderType() != null || request.getBaseDatetimeForRecurrence() != null ||
            request.getFrequencyInterval() != null || request.getDayOfWeek() != null || request.getDayOfMonth() != null) {
            reminder.setNextNotificationDatetime(calculateNextNotificationTime(
                    reminder.getBaseDatetimeForRecurrence(), reminder.getReminderType(),
                    reminder.getFrequencyInterval(), reminder.getDayOfWeek(), reminder.getDayOfMonth(), LocalDateTime.now()));
        }

        Reminder updatedReminder = reminderRepository.save(reminder); // 데이터베이스에 변경사항 저장
        log.info("Updated reminder with ID: {}", updatedReminder.getId());
        return new ReminderResponse(updatedReminder);
    }

    @Transactional
    public void deleteReminder(Long reminderId) {
        if (!reminderRepository.existsById(reminderId)) { // 존재하지 않으면 예외 발생
            throw new ResourceNotFoundException("Reminder not found with ID: " + reminderId);
        }
        reminderRepository.deleteById(reminderId); // ID를 이용하여 리마인더 삭제
        log.info("Deleted reminder with ID: {}", reminderId);
    }

    // ---------------------- 리마인더 알림 처리 스케줄링 ----------------------

    @Scheduled(cron = "0 * * * * *") // 매 분 0초에 이 메서드를 실행합니다. (예: 00:00:00, 00:01:00 등)
    @Transactional
    public void processScheduledReminders() {
        LocalDateTime now = LocalDateTime.now(); // 현재 시간
        log.info("리마인더 알림 검색중 。 。 。『 {} 』", now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))); // 시간 형식 포맷팅

        // 현재 시간보다 다음 알림 시간이 이전이거나 같은 활성화된 리마인더들을 조회합니다.
        List<Reminder> remindersToNotify = reminderRepository.findByIsActiveTrueAndNextNotificationDatetimeLessThanEqual(now);

        if (remindersToNotify.isEmpty()) {
            log.info("발송할 알림이 없습니다。『 {} 』", now.format(DateTimeFormatter.ofPattern("HH:mm")));
            return;
        }

        // 이메일 발송
        for (Reminder reminder : remindersToNotify) {
            String recipientEmail = reminder.getUser().getEmail(); // 사용자 이메일 주소 가져오기
            String summaryTitle = reminder.getUserLibrary().getSummary().getTranscript().getVideo().getTitle(); // 요약 영상 제목 가져오기
            String reminderNote = reminder.getReminderNote(); // 리마인더 메모 가져오기

            String subject = "[YouSum] 리마인더 알림: " + summaryTitle; // 이메일 제목 구성
            String emailContent = buildEmailContent(reminder); // 이메일 내용 구성


            log.info("Processing reminder ID: {} for User ID: {} (Email: {})",
                    reminder.getId(), reminder.getUser().getId(), recipientEmail);

            try {
                // EmailService를 통해 이메일 발송 시도
                emailService.sendNotificationEmail(recipientEmail, subject, emailContent);
                log.info("Reminder ID {} notification email sent to {}.", reminder.getId(), recipientEmail);
            } catch (Exception e) {
                log.error("Failed to send reminder email for ID {} to {}: {}", reminder.getId(), recipientEmail, e.getMessage());
                // 이메일 발송 실패 시에도 스케줄러가 멈추지 않도록 예외 처리
            }

            // 알림 발송 후, 다음 알림 시간을 계산하고 업데이트합니다.
            if (reminder.getReminderType() == ReminderType.ONE_TIME) {
                reminder.setIsActive(false); // ONE_TIME 리마인더는 한 번 발송 후 비활성화
                log.info("One-time reminder ID {} deactivated.", reminder.getId());
            } else {
                reminder.setNextNotificationDatetime(
                        calculateNextNotificationTime(reminder.getBaseDatetimeForRecurrence(), reminder.getReminderType(),
                                reminder.getFrequencyInterval(), reminder.getDayOfWeek(), reminder.getDayOfMonth(), now));
                log.info("Reminder ID {} next notification set to: {}", reminder.getId(), reminder.getNextNotificationDatetime());
            }
            reminder.setLastSentAt(now); // 마지막 알림 발송 시간 업데이트
            reminderRepository.save(reminder); // 변경사항 저장
        }
        log.info("Reminder processing finished.");
    }

    // ---------------------- 이메일 내용 구성 유틸리티 메서드 (신규 추가) ----------------------
    private String buildEmailContent(Reminder reminder) {
        StringBuilder content = new StringBuilder();
        content.append("안녕하세요, ").append(reminder.getUser().getUserName()).append("님!\n\n");
        content.append("설정하신 리마인더 알림이 도착했습니다.\n\n");
        content.append("영상 제목: ").append(reminder.getUserLibrary().getSummary().getTranscript().getVideo().getTitle()).append("\n");
        content.append("리마인더 메모: ").append(reminder.getReminderNote()).append("\n");
        content.append("알림 시간: ").append(reminder.getNextNotificationDatetime().format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 HH시 mm분"))).append("\n\n");
        content.append("지금 바로 YouSum에서 요약 내용을 확인해보세요!\n");
        // 실제 요약 페이지 URL 추가 (예: front-end URL)
        String summaryPageUrl = emailService.createSummaryPageUrl(reminder);
        content.append("요약 바로가기: ").append(summaryPageUrl).append("\n\n"); // 예시로 비디오 원본 URL 사용
        content.append("감사합니다.\n***YouSum***\n");
        return content.toString();
    }

    // ---------------------- 다음 알림 시간 계산 유틸리티 ----------------------

    /**
     * 리마인더 타입과 설정에 따라 다음 알림 시간을 계산합니다.
     *
     * @param baseDateTime 기준 날짜/시간
     * @param type 리마인더 타입 (ONE_TIME, DAILY, WEEKLY, MONTHLY)
     * @param interval 반복 간격 (예: 2일, 3주)
     * @param dayOfWeek 주중 요일 (1=월, 7=일)
     * @param dayOfMonth 월중 일자 (1-31)
     * @param now 현재 시간 (알림 시간이 이미 지난 경우를 대비)
     * @return 계산된 다음 알림 시간
     */
    private LocalDateTime calculateNextNotificationTime(LocalDateTime baseDateTime, ReminderType type,
                                                        Integer interval, Integer dayOfWeek, Integer dayOfMonth, LocalDateTime now) {
        LocalDateTime next = baseDateTime; // 초기값은 기준 날짜/시간

        if (type == null) {
            return next; // 타입이 없으면 기준 날짜 반환 (사실상 발생하지 않아야 함)
        }

        switch (type) {
            case ONE_TIME:
                // ONE_TIME은 기준 날짜/시간이 곧 다음 알림 시간입니다.
                // 이미 지난 시간이라면 현재 시간보다 나중인 가장 가까운 시간을 계산할 필요는 없음.
                // (이후 스케줄러에서 isActive = false로 처리)
                break;
            case DAILY:
                // 현재 시간보다 과거라면 현재 시간부터 interval마다 더합니다.
                while (next.isBefore(now) || next.isEqual(now)) {
                    next = next.plusDays(interval != null ? interval : 1);
                }
                break;
            case WEEKLY:
                // 주간 반복: 기준 날짜의 시간은 유지하고, 다음 주 또는 다다음 주의 특정 요일로 이동합니다.
                // interval이 1이면 다음 주, 2이면 다다음 주 등.
                LocalDateTime target = next.with(TemporalAdjusters.nextOrSame(DayOfWeek.of(dayOfWeek != null ? dayOfWeek : 1))); // 요일이 지정되지 않으면 월요일 기준
                if (target.isBefore(now)) { // 계산된 시간이 현재보다 이전이라면, interval만큼 더 주차를 이동
                    target = target.plusWeeks(interval != null ? interval : 1);
                }
                while (target.isBefore(now) || target.isEqual(now)) {
                    target = target.plusWeeks(interval != null ? interval : 1);
                }
                next = target;
                break;
            case MONTHLY:
                // 월간 반복: 기준 날짜의 시간은 유지하고, 다음 달 또는 다다음 달의 특정 일자로 이동합니다.
                // interval이 1이면 다음 달, 2이면 다다음 달 등.
                LocalDateTime targetDate = next.withDayOfMonth(dayOfMonth != null ? dayOfMonth : 1); // 일자가 지정되지 않으면 1일 기준
                if (targetDate.isBefore(now)) { // 계산된 시간이 현재보다 이전이라면, interval만큼 더 월을 이동
                    targetDate = targetDate.plusMonths(interval != null ? interval : 1);
                }
                while (targetDate.isBefore(now) || targetDate.isEqual(now)) {
                    targetDate = targetDate.plusMonths(interval != null ? interval : 1);
                }
                next = targetDate;
                break;
        }
        return next;
    }
}