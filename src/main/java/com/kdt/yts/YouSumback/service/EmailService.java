package com.kdt.yts.YouSumback.service;

import com.kdt.yts.YouSumback.model.entity.Reminder;

// 이메일 발송 기능을 추상화하는 인터페이스
public interface EmailService {
    /**
     * 지정된 이메일 주소로 알림 이메일을 발송합니다.
     * @param to 받는 사람의 이메일 주소
     * @param subject 이메일 제목
     * @param text 이메일 내용 (본문)
     */
    void sendNotificationEmail(String to, String subject, String text);

    /**
     * 리마인더에 연결된 요약의 웹사이트 URL을 생성합니다.
     * 프론트엔드 URL 구조에 따라 적절한 ID를 사용하여 링크를 만듭니다.
     *
     * @param reminder 리마인더 엔티티 (여기서 요약 정보나 라이브러리 정보 획득)
     * @return 생성된 요약 페이지의 전체 URL
     */
    String createSummaryPageUrl(Reminder reminder); // URL 생성 메서드
}