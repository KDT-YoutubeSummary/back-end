package com.kdt.yts.YouSumback.service;

import com.kdt.yts.YouSumback.model.entity.Reminder;

// 이메일 발송 기능을 추상화하는 인터페이스
public interface EmailService {
    // 지정된 이메일 주소로 알림 이메일을 발송

    void sendNotificationEmail(String to, String subject, String text);

    // 리마인더에 연결된 요약의 웹사이트 URL을 생성

    String createSummaryPageUrl(Reminder reminder); // URL 생성 메서드
}