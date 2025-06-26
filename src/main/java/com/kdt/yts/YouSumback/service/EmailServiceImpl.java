package com.kdt.yts.YouSumback.service;

import com.kdt.yts.YouSumback.model.entity.Reminder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j // SLF4J 로거를 'log'라는 필드로 자동 생성

public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender; // JavaMailSender 자동 주입

    @Value("${yousum.frontend.base-url}")
    private String yousumFrontendBaseUrl;

    @Value("${spring.mail.username}") // application.properties에서 발신자 이메일 주소를 주입받음
    private String fromEmail;

    @Override
    public void sendNotificationEmail(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom(fromEmail);   // 발신자 이메일 주소 설정
        message.setTo(to);            // 수신자 이메일 주소 설정
        message.setSubject(subject);  // 이메일 제목 설정
        message.setText(text);        // 이메일 본문 내용 설정

        try {
            mailSender.send(message); // 실제 이메일 발송 호출
            log.info("실제 이메일 발송 성공: 수신자 '{}', 제목 '{}'", to, subject);
        } catch (MailException e) {
            // 예외 처리
            log.error("실제 이메일 발송 실패: 수신자 '{}', 제목 '{}'. 오류: {}", to, subject, e.getMessage());
            throw new RuntimeException("이메일 발송 중 오류가 발생했습니다.", e);
        }
    }


    @Override
    public String createSummaryPageUrl(Reminder reminder) {

        Long summaryId = reminder.getSummaryArchive().getSummary().getId();

        return yousumFrontendBaseUrl + "/summary/" + summaryId;
    }
}