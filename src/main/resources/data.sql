show databases;

USE yousum_db;

show tables;

select * from user;

-- 1. `User` 테이블 더미 데이터
INSERT INTO `User` (user_id, username, email, password_hash, created_at) VALUES
    (1, '사용자1', 'user1@example.com', 'hashed_password_for_user1', '2025-05-01 10:00:00');

-- 2. `Video` 테이블 더미 데이터
INSERT INTO Video (video_id, title, original_url, uploader_name, original_language_code, duration_seconds) VALUES
    ('dQw4w9WgXcQ', '세상을 바꾸는 기술 트렌드', 'https://www.youtube.com/watch?v=dQw4w9WgXcQ', '테크트렌드', 'ko', 1800);

-- 3. `AudioTranscript` 테이블 더미 데이터
-- 비디오 ID 'dQw4w9WgXcQ'에 대한 트랜스크립트
INSERT INTO AudioTranscript (transcript_id, video_id, transcript_text, created_at) VALUES
    (101, 'dQw4w9WgXcQ', '안녕하세요. 오늘은 인공지능과 머신러닝의 최신 동향에 대해 이야기해보겠습니다. 딥러닝 기술은...', '2025-05-10 11:30:00');

-- 4. `Summary` 테이블 더미 데이터
-- 사용자 ID 1, 트랜스크립트 ID 101에 대한 요약
INSERT INTO Summary (summary_id, user_id, transcript_id, summary_text, language_code, created_at, summary_type) VALUES
    (201, 1, 101, '이 영상은 인공지능, 머신러닝, 딥러닝의 최신 기술 트렌드를 다룹니다. 특히 AI의 발전이 사회에 미치는 영향에 대해 요약하고 있습니다.', 'ko', '2025-05-15 14:00:00', 'AI_GENERATED');

-- 5. `UserLibrary` 테이블 더미 데이터
-- 사용자 ID 1, 요약 ID 201을 라이브러리에 저장
INSERT INTO UserLibrary (user_library_id, user_id, summary_id, saved_at, user_notes, last_viewed_at) VALUES
    (301, 1, 201, '2025-05-20 09:00:00', '중요한 AI 기술 요약. 주기적으로 복습 필요.', NULL);

-- 6. `Tag` 테이블 더미 데이터
INSERT INTO Tag (tag_id, tag_name) VALUES
                                       (501, 'AI'),
                                       (502, '기술트렌드');

-- 7. `UserLibraryTag` 테이블 더미 데이터 (라이브러리 항목에 태그 연결)
INSERT INTO UserLibraryTag (user_library_id, tag_id) VALUES
                                                         (301, 501), -- user_library_id 301에 AI 태그 연결
                                                         (301, 502); -- user_library_id 301에 기술트렌드 태그 연결


-- 8. `Reminder` 테이블 더미 데이터
-- user_id 1, user_library_id 301에 대한 리마인더들
-- 현재 날짜: 2025-05-30
INSERT INTO Reminder (reminder_id, user_id, user_library_id, reminder_type, frequency_interval, day_of_week, day_of_month, base_datetime_for_recurrence, next_notification_datetime, reminder_note, is_active, created_at, last_sent_at) VALUES
-- 8.1. 일회성 리마인더 (오늘 오후 1시 30분 예정, 테스트 후 비활성화될 것임)
(401, 1, 301, 'ONE_TIME', 1, NULL, NULL, '2025-05-30 13:30:00', '2025-05-30 13:30:00', '오늘 오후 AI 요약 다시 보기', TRUE, '2025-05-30 11:00:00', NULL),
-- 8.2. 매일 반복 리마인더 (내일 오전 9시 예정, 2일마다)
(402, 1, 301, 'DAILY', 2, NULL, NULL, '2025-05-30 09:00:00', '2025-06-01 09:00:00', 'AI 요약 매일 복습', TRUE, '2025-05-29 15:00:00', NULL),
-- 8.3. 매주 반복 리마인더 (다음주 월요일 오후 2시 예정, 매주)
(403, 1, 301, 'WEEKLY', 1, 2, NULL, '2025-05-26 14:00:00', '2025-06-02 14:00:00', '주간 AI 기술 트렌드 복습 (월요일)', TRUE, '2025-05-20 10:00:00', NULL),
-- 8.4. 매월 반복 리마인더 (다음달 1일 오전 10시 예정, 매달)
(404, 1, 301, 'MONTHLY', 1, NULL, 1, '2025-05-01 10:00:00', '2025-06-01 10:00:00', '월간 AI 요약 점검 (매달 1일)', TRUE, '2025-04-25 11:00:00', NULL);


-- 9. `Quiz` 테이블 더미 데이터
INSERT INTO Quiz (quiz_id, summary_id, title, created_at) VALUES
    (601, 201, 'AI 기술 요약 퀴즈', '2025-05-18 10:00:00');

-- 10. `Question` 테이블 더미 데이터
INSERT INTO Question (question_id, quiz_id, question_text, language_code) VALUES
                                                                              (701, 601, '이 영상에서 주로 다루는 기술은 무엇인가요?', 'ko'),
                                                                              (702, 601, 'AI의 어떤 하위 분야에 대한 언급이 있었나요?', 'ko');

-- 11. `AnswerOption` 테이블 더미 데이터
INSERT INTO AnswerOption (answer_option_id, question_id, option_text, is_correct) VALUES
                                                                                      (801, 701, '블록체인', FALSE),
                                                                                      (802, 701, '인공지능', TRUE),
                                                                                      (803, 701, '클라우드 컴퓨팅', FALSE),
                                                                                      (804, 702, '기계 학습', TRUE),
                                                                                      (805, 702, '로봇 공학', FALSE),
                                                                                      (806, 702, '양자 컴퓨팅', FALSE);

-- 12. `UserActivityLog` 테이블 더미 데이터
INSERT INTO UserActivityLog (log_id, user_id, activity_type, target_entity_type, target_entity_id_str, target_entity_id_int, created_at, activity_detail, details) VALUES
                                                                                                                                                                       (901, 1, 'VIEW_SUMMARY', 'SUMMARY', NULL, 201, '2025-05-21 10:30:00', '사용자가 요약을 조회함', '{"device": "web", "ip": "192.168.1.1"}'),
                                                                                                                                                                       (902, 1, 'SET_REMINDER', 'REMINDER', NULL, 401, '2025-05-30 11:00:00', '사용자가 리마인더를 설정함', '{"reminder_type": "ONE_TIME"}');

-- 13. `VideoRecommendation` 테이블 더미 데이터
INSERT INTO VideoRecommendation (recommendation_id, user_id, source_video_id, recommended_video_id, recommendation_reason, created_at, is_clicked, clicked_at) VALUES
    (1001, 1, '', 'dQw4w9WgXcQ', 'AI 기술 트렌드 관련 영상 시청 후 추천', '2025-05-25 16:00:00', FALSE, NULL);
-- INSERT INTO Video (video_id, title, original_url, uploader_name, original_language_code, duration_seconds) VALUES
-- ('video_id_xyz', 'AI 미래 전망', 'https://www.youtube.com/watch?v=xyz', 'AI연구소', 'ko', 1500);

select * from user;

SELECT @@global.time_zone, @@session.time_zone;

SELECT user_id, username, email FROM `User` WHERE user_id = 1;
SELECT reminder_id, user_id, next_notification_datetime, is_active FROM Reminder WHERE reminder_id = 401;
UPDATE Reminder
SET next_notification_datetime = DATE_ADD(NOW(), INTERVAL 2 MINUTE), -- 현재 시간 (2025-06-03 20:07 KST)보다 넉넉하게 미래로 설정
    is_active = TRUE,
    last_sent_at = NULL
WHERE reminder_id = 401;

-- 1. 새로운 일회성 리마인더 삽입
-- (reminder_id는 AUTO_INCREMENT이므로 명시하지 않거나, 적절히 큰 값을 지정하여 기존 더미와 충돌하지 않게 합니다.)
INSERT INTO Reminder (
    user_id,
    user_library_id,
    reminder_type,
    frequency_interval,
    day_of_week,
    day_of_month,
    base_datetime_for_recurrence,
    next_notification_datetime,
    reminder_note,
    is_active,
    created_at,
    last_sent_at
) VALUES (
             1,                                   -- 사용자 ID (기존 더미 데이터 1번 사용자)
             301,                                 -- 사용자 라이브러리 ID (기존 더미 데이터 301번 라이브러리 항목)
             'ONE_TIME',                          -- 알림 타입: 일회성
             1,                                   -- 반복 간격: 일회성이므로 의미 없음 (기본값 1)
             NULL,                                -- 주간 반복 아님
             NULL,                                -- 월간 반복 아님
             '2025-06-03 22:30:00',               -- 기준 날짜/시간 (오늘 밤 10시 30분)
             '2025-06-03 22:30:00',               -- 다음 알림 예정 시간 (오늘 밤 10시 30분)
             '오늘 밤 10시 30분 일회성 테스트 알림', -- 리마인더 메모
             TRUE,                                -- 활성화 상태
             '2025-06-03 22:20:00',               -- 생성 일시 (현재 시간보다 이전)
             NULL                                 -- 마지막 알림 발송 시간 (아직 발송 안됨)
         );

SELECT * FROM Reminder ORDER BY reminder_id;

UPDATE Reminder
SET next_notification_datetime = '2025-06-03 22:37:59',
    is_active = TRUE,
    last_sent_at = NULL
WHERE reminder_id = 401;

UPDATE Reminder
SET next_notification_datetime = DATE_ADD(NOW(), INTERVAL 2 MINUTE), -- NOW() 함수 사용 + 1분 추가
    is_active = TRUE,
    last_sent_at = NULL
WHERE reminder_id = 401;

SELECT reminder_id, next_notification_datetime, is_active, last_sent_at FROM Reminder WHERE reminder_id = 401;