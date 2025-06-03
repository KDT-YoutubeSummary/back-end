package com.YouSumback.service;

import com.YouSumback.model.entity.Reminder;
import com.YouSumback.model.entity.Summary;
import lombok.extern.slf4j.Slf4j; // 로깅을 위한 Lombok 어노테이션
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service; // 서비스 컴포넌트로 선언

@Service // 이 클래스가 Spring의 서비스 컴포넌트임을 나타냅니다.
@Slf4j // SLF4J 로거를 'log'라는 필드로 자동 생성합니다.
public class EmailServiceImpl implements EmailService {

    @Value("${yousum.frontend.base-url}")
    private String yousumFrontendBaseUrl;

    @Override
    public void sendNotificationEmail(String to, String subject, String text) {
        // --- 실제 이메일 발송 로직 대신, 콘솔 로그로 발송을 시뮬레이션합니다. ---
        // TODO: 실제 이메일 발송 구현 (예: JavaMailSender 사용, AWS SES 연동 등)
        log.info("--------------------------------------------------");
        log.info("이메일 발송 시뮬레이션:");
        log.info("수신자: {}", to);
        log.info("제목: {}", subject);
        log.info("내용: {}", text);
        log.info("--------------------------------------------------");
    }

    @Override
    public String createSummaryPageUrl(Reminder reminder) {

        Long summaryId = reminder.getUserLibrary().getSummary().getSummary_id();

        return yousumFrontendBaseUrl + "/summary/" + summaryId;
    }
}