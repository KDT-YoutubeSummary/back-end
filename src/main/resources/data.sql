CREATE DATABASE IF NOT EXISTS yousum;
USE yousum;

-- 사용자 (User) 테이블
CREATE TABLE `user` (
                        user_id INT AUTO_INCREMENT PRIMARY KEY, -- SERIAL -> INT AUTO_INCREMENT
                        username VARCHAR(100) UNIQUE NOT NULL,
                        email VARCHAR(255) UNIQUE NOT NULL,
                        password_hash VARCHAR(255) NOT NULL,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- 비디오 (Video) 테이블
CREATE TABLE video (
                       video_id INT PRIMARY KEY,
                       youtube_id  VARCHAR(255) UNIQUE NOT NULL,      --  (추가) 유튜브 영상 ID
                       title VARCHAR(255) NOT NULL,                 -- 영상 제목
                       original_url VARCHAR(512) UNIQUE NOT NULL,  -- 영상 원본 링크
                       uploader_name VARCHAR(100)             ,      -- 채널명 (업로더)
                       thumbnail_url TEXT,                          --  (추가)  썸네일 이미지 URL
                       view_count BIGINT,                           --  (추가)  조회수
                       published_at DATETIME,                       -- (추가)  업로드 날짜
                       duration_seconds INT NOT NULL,               -- 영상 길이 (초 단위)
                       original_language_code VARCHAR(20) NOT NULL  -- 영상의 원본 언어 코드
);

-- 태그 (Tag) 테이블
CREATE TABLE tag (
                     tag_id INT AUTO_INCREMENT PRIMARY KEY, -- SERIAL -> INT AUTO_INCREMENT
                     tag_name VARCHAR(100) UNIQUE NOT NULL
);

-- 오디오 트랜스크립트 (AudioTranscript) 테이블
CREATE TABLE audio_transcript (
                     transcript_id INT AUTO_INCREMENT PRIMARY KEY, -- SERIAL -> INT AUTO_INCREMENT
                     video_id INT NOT NULL UNIQUE, -- 단일 트랜스크립트 가정 유지
                     transcript_text TEXT NOT NULL,
                     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL, -- create_at -> created_at
                     CONSTRAINT fk_audio_transcript_video FOREIGN KEY (video_id) REFERENCES video(video_id) ON DELETE CASCADE
);

-- 요약 (Summary) 테이블
CREATE TABLE summary (
                     summary_id INT AUTO_INCREMENT PRIMARY KEY, -- SERIAL -> INT AUTO_INCREMENT
                     user_id INT NOT NULL, -- BIGINT -> INT (User.user_id와 일관성)
                     transcript_id INT NOT NULL, -- BIGINT -> INT (AudioTranscript.transcript_id와 일관성)
                     summary_text TEXT NOT NULL,
                     user_prompt TEXT, -- (추가) 사용자 프롬프트 (사용 목적, 요청 문장 등 전체 포함 가능)
                     language_code VARCHAR(10) NOT NULL,
                     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL, -- create_at -> created_at
                     summary_type VARCHAR(50),
                     CONSTRAINT fk_summary_user FOREIGN KEY (user_id) REFERENCES `user`(user_id) ON DELETE CASCADE,
                     CONSTRAINT fk_summary_audio_transcript FOREIGN KEY (transcript_id) REFERENCES audio_transcript(transcript_id) ON DELETE CASCADE
);

-- 사용자 라이브러리 (UserLibrary) 테이블
CREATE TABLE user_library (
                     user_library_id INT AUTO_INCREMENT PRIMARY KEY, -- SERIAL -> INT AUTO_INCREMENT
                     user_id INT NOT NULL, -- BIGINT -> INT
                     summary_id INT NOT NULL, -- BIGINT -> INT
                     saved_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                     user_notes TEXT,
                     last_viewed_at TIMESTAMP,
                     CONSTRAINT fk_user_library_user FOREIGN KEY (user_id) REFERENCES `user`(user_id) ON DELETE CASCADE,
                     CONSTRAINT fk_user_library_summary FOREIGN KEY (summary_id) REFERENCES summary(summary_id) ON DELETE CASCADE,
                     CONSTRAINT uq_user_library_user_summary UNIQUE (user_id, summary_id)
);

-- 리마인더 (Reminder) 테이블
CREATE TABLE reminder (
                          reminder_id INT AUTO_INCREMENT PRIMARY KEY, -- SERIAL -> INT AUTO_INCREMENT
                          user_id INT NOT NULL, -- BIGINT -> INT
                          user_library_id INT NOT NULL, -- BIGINT -> INT
                          reminder_type VARCHAR(50) NOT NULL,
                          frequency_interval INT DEFAULT 1,
                          day_of_week INT,
                          day_of_month INT,
                          base_datetime_for_recurrence TIMESTAMP NOT NULL,
                          next_notification_datetime TIMESTAMP NOT NULL,
                          reminder_note TEXT,
                          is_active BOOLEAN DEFAULT TRUE NOT NULL,
                          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                          last_sent_at TIMESTAMP,
                          CONSTRAINT fk_reminder_user FOREIGN KEY (user_id) REFERENCES `user`(user_id) ON DELETE CASCADE,
                          CONSTRAINT fk_reminder_user_library FOREIGN KEY (user_library_id) REFERENCES user_library(user_library_id) ON DELETE CASCADE
);

-- 퀴즈 (Quiz) 테이블
CREATE TABLE quiz (
                      quiz_id INT AUTO_INCREMENT PRIMARY KEY, -- SERIAL -> INT AUTO_INCREMENT
                      summary_id INT NOT NULL, -- UNIQUE 제약 조건 제거 (하나의 요약에 여러 퀴즈 가능성)
                      title VARCHAR(255),
                      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                      CONSTRAINT fk_quiz_summary FOREIGN KEY (summary_id) REFERENCES summary(summary_id) ON DELETE CASCADE
);

-- 질문 (Question) 테이블
CREATE TABLE question (
                          question_id INT AUTO_INCREMENT PRIMARY KEY, -- SERIAL -> INT AUTO_INCREMENT
                          quiz_id INT NOT NULL,
                          question_text TEXT NOT NULL,
                          language_code VARCHAR(10) NOT NULL, -- VARCHAR(255) -> VARCHAR(10) (언어 코드에 적합)
                          CONSTRAINT fk_question_quiz FOREIGN KEY (quiz_id) REFERENCES quiz(quiz_id) ON DELETE CASCADE
);

-- 답변 선택지 (AnswerOption) 테이블
CREATE TABLE answer_option (
                              answer_option_id INT AUTO_INCREMENT PRIMARY KEY, -- SERIAL -> INT AUTO_INCREMENT
                              question_id INT NOT NULL,
                              option_text TEXT NOT NULL,
                              is_correct BOOLEAN NOT NULL,
                              transcript_id BIGINT NOT NULL,
                              summary_text TEXT NOT NULL,
                              summary_type VARCHAR(50),
                              created_at DATETIME NOT NULL,
                              CONSTRAINT fk_answer_question FOREIGN KEY (question_id) REFERENCES question(question_id) ON DELETE CASCADE
);

-- 사용자 라이브러리 태그 (UserLibraryTag) 연결 테이블
CREATE TABLE user_library_tag (
                                user_library_id INT NOT NULL, -- BIGINT -> INT
                                tag_id INT NOT NULL, -- BIGINT -> INT
                                PRIMARY KEY (user_library_id, tag_id),
                                CONSTRAINT fk_user_library_tag_user_library FOREIGN KEY (user_library_id) REFERENCES user_library(user_library_id) ON DELETE CASCADE,
                                CONSTRAINT fk_user_library_tag_tag FOREIGN KEY (tag_id) REFERENCES tag(tag_id) ON DELETE CASCADE
);

-- 영상 추천 (VideoRecommendation) 테이블
CREATE TABLE video_recommendation (
                                     recommendation_id INT AUTO_INCREMENT PRIMARY KEY, -- SERIAL -> INT AUTO_INCREMENT
                                     user_id INT NOT NULL, -- BIGINT -> INT
                                     source_video_id INT NULL ,
                                     recommended_video_id INT NOT NULL,
                                     recommendation_reason TEXT,
                                     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL, -- create_at -> created_at
                                     is_clicked BOOLEAN DEFAULT FALSE NOT NULL,
                                     clicked_at TIMESTAMP,
                                     CONSTRAINT fk_video_recommendation_user FOREIGN KEY (user_id) REFERENCES `user`(user_id) ON DELETE CASCADE,
                                     CONSTRAINT fk_video_recommendation_source_video FOREIGN KEY (source_video_id) REFERENCES video(video_id) ON DELETE SET NULL,
                                     CONSTRAINT fk_video_recommendation_recommended_video FOREIGN KEY (recommended_video_id) REFERENCES video(video_id) ON DELETE CASCADE
);

-- 사용자 활동 로그 (UserActivityLog) 테이블
CREATE TABLE user_activity_log (
                                 log_id BIGINT AUTO_INCREMENT PRIMARY KEY, -- BIGSERIAL -> BIGINT AUTO_INCREMENT
                                 user_id INT NOT NULL, -- BIGINT -> INT
                                 activity_type VARCHAR(50) NOT NULL,
                                 target_entity_type VARCHAR(50),
                                 target_entity_id_str VARCHAR(255),
                                 target_entity_id_int BIGINT,
                                 created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL, -- create_at -> created_at
                                 activity_detail TEXT,
                                 details JSON, -- MySQL에서는 JSONB 대신 JSON
                                 CONSTRAINT fk_user_activity_log_user FOREIGN KEY (user_id) REFERENCES `user`(user_id) ON DELETE CASCADE
);
# SHOW DATABASES;
#
# USE yousum;
#
# SHOW TABLES;
#
# SELECT * FROM user;
#
# -- 더미 데이터 삽입
-- ✅ 1. 사용자
INSERT INTO user (user_id, username, email, password_hash) VALUES
    (1, 'test_user', 'test@example.com', 'hashedpassword');

-- ✅ 2. 비디오 (원본 + 추천용)
INSERT INTO video (video_id, youtube_id, title, original_url, uploader_name, thumbnail_url, view_count, published_at, duration_seconds, original_language_code) VALUES
                                                                                                                                                                    (1, 'yt001', 'Test Video', 'https://youtu.be/yt001', 'Uploader A', 'https://img.url', 1000, '2025-06-05 06:33:59', 600, 'en'),
                                                                                                                                                                    (2, 'yt002', '추천 영상 A', 'https://youtu.be/yt002', 'Uploader B', 'https://img.url/yt002', 2500, '2025-06-05 06:34:23', 300, 'en'),
                                                                                                                                                                    (3, 'yt999', '강력 추천 영상', 'https://www.youtube.com/watch?v=dQw4w9WgXcQ', 'Uploader X', NULL, 9999, '2025-06-05 06:35:00', 200, 'ko');

-- ✅ 3. 트랜스크립트
INSERT INTO audio_transcript (transcript_id, video_id, transcript_text) VALUES
    (1, 1, 'This is a sample transcript.');

-- ✅ 4. 요약
INSERT INTO summary (summary_id, user_id, transcript_id, summary_text, user_prompt, language_code, summary_type) VALUES
                                                                                                                     (1, 1, 1, 'Summary text here.', 'Summarize this', 'en', 'basic'),
                                                                                                                     (201, 1, 1, 'AI 요약 내용', '간단히 요약해줘', 'ko', 'basic');

-- ✅ 5. 사용자 라이브러리
INSERT INTO user_library (user_library_id, user_id, summary_id, user_notes, last_viewed_at) VALUES
    (301, 1, 1, 'Sample note', '2025-06-05 06:33:59');

-- ✅ 6. 리마인더
INSERT INTO reminder (
    user_id, user_library_id, reminder_type, frequency_interval, day_of_week, day_of_month,
    base_datetime_for_recurrence, next_notification_datetime, reminder_note, is_active, created_at, last_sent_at
) VALUES (
             1, 301, 'ONE_TIME', 1, NULL, NULL,
             '2025-06-05 06:33:00', '2025-06-05 06:38:00',
             'Test reminder at 06:38', TRUE, '2025-06-05 06:33:59', NULL
         );

-- ✅ 7. 태그
INSERT INTO tag (tag_id, tag_name) VALUES
                                       (1, '교육'), (2, '기술');

-- ✅ 8. 사용자 라이브러리 태그 연결
INSERT INTO user_library_tag (user_library_id, tag_id) VALUES
                                                           (301, 1), (301, 2);

-- ✅ 9. 퀴즈
INSERT INTO quiz (quiz_id, summary_id, title) VALUES
    (601, 201, 'AI 기술 요약 퀴즈');

-- ✅ 10. 질문
INSERT INTO question (question_id, quiz_id, question_text, language_code) VALUES
                                                                              (701, 601, '이 영상에서 주로 다루는 기술은 무엇인가요?', 'ko'),
                                                                              (702, 601, 'AI의 어떤 하위 분야에 대한 언급이 있었나요?', 'ko');

-- ✅ 11. 선택지
INSERT INTO answer_option (
    answer_option_id, question_id, option_text, is_correct,
    transcript_id, summary_text, summary_type, created_at
) VALUES
      (801, 701, '블록체인', FALSE, 1001, 'AI는 데이터를 분석하여 미래를 예측할 수 있습니다.', 'SUMMARY', NOW()),
      (802, 701, '인공지능', TRUE, 1001, 'AI는 데이터를 분석하여 미래를 예측할 수 있습니다.', 'SUMMARY', NOW()),
      (803, 701, '클라우드 컴퓨팅', FALSE, 1001, 'AI는 데이터를 분석하여 미래를 예측할 수 있습니다.', 'SUMMARY', NOW()),
      (804, 702, '기계 학습', TRUE, 1002, '기계 학습은 AI의 하위 분야로, 패턴 인식을 기반으로 합니다.', 'SUMMARY', NOW()),
      (805, 702, '로봇 공학', FALSE, 1002, '기계 학습은 AI의 하위 분야로, 패턴 인식을 기반으로 합니다.', 'SUMMARY', NOW()),
      (806, 702, '양자 컴퓨팅', FALSE, 1002, '기계 학습은 AI의 하위 분야로, 패턴 인식을 기반으로 합니다.', 'SUMMARY', NOW());

-- ✅ 12. 사용자 활동 로그
INSERT INTO user_activity_log (log_id, user_id, activity_type, target_entity_type, target_entity_id_str, target_entity_id_int, activity_detail, details) VALUES
                                                                                                                                                             (1, 1, 'CREATE_SUMMARY', 'summary', NULL, 1, '사용자가 요약을 생성함', '{"summary_id": 1}'),
                                                                                                                                                             (2, 1, 'SET_REMINDER', 'reminder', NULL, 1, '사용자가 리마인더를 설정함', '{"reminder_id": 1}');

-- ✅ 13. 추천 영상
INSERT INTO video_recommendation (recommendation_id, user_id, source_video_id, recommended_video_id, recommendation_reason, created_at, is_clicked, clicked_at) VALUES
                                                                                                                                                                    (1, 1, 1, 2, '관련 주제 기반 추천', '2025-06-05 06:34:23', FALSE, NULL),
                                                                                                                                                                    (2, 1, NULL, 3, '핵심 주제 기반 추천', '2025-06-05 06:35:00', FALSE, NULL);


# -- 1. 새로운 일회성 리마인더 삽입
# -- (reminder_id는 AUTO_INCREMENT이므로 명시하지 않거나, 적절히 큰 값을 지정하여 기존 더미와 충돌하지 않게 합니다.)
# INSERT INTO Reminder (
#     user_id,
#     user_library_id,
#     reminder_type,
#     frequency_interval,
#     day_of_week,
#     day_of_month,
#     base_datetime_for_recurrence,
#     next_notification_datetime,
#     reminder_note,
#     is_active,
#     created_at,
#     last_sent_at
# ) VALUES (
#              1,                                   -- 사용자 ID (기존 더미 데이터 1번 사용자)
#              301,                                 -- 사용자 라이브러리 ID (기존 더미 데이터 301번 라이브러리 항목)
#              'ONE_TIME',                          -- 알림 타입: 일회성
#              1,                                   -- 반복 간격: 일회성이므로 의미 없음 (기본값 1)
#              NULL,                                -- 주간 반복 아님
#              NULL,                                -- 월간 반복 아님
#              '2025-06-03 22:30:00',               -- 기준 날짜/시간 (오늘 밤 10시 30분)
#              '2025-06-03 22:30:00',               -- 다음 알림 예정 시간 (오늘 밤 10시 30분)
#              '오늘 밤 10시 30분 일회성 테스트 알림', -- 리마인더 메모
#              TRUE,                                -- 활성화 상태
#              '2025-06-03 22:20:00',               -- 생성 일시 (현재 시간보다 이전)
#              NULL                                 -- 마지막 알림 발송 시간 (아직 발송 안됨)
#          );
#
# SELECT * FROM Reminder ORDER BY reminder_id;
#
# UPDATE Reminder
# SET next_notification_datetime = '2025-06-03 22:37:59',
#     is_active = TRUE,
#     last_sent_at = NULL
# WHERE reminder_id = 401;
#
# UPDATE Reminder
# SET next_notification_datetime = DATE_ADD(NOW(), INTERVAL 2 MINUTE), -- NOW() 함수 사용 + 1분 추가
#     is_active = TRUE,
#     last_sent_at = NULL
# WHERE reminder_id = 401;
#
# SELECT reminder_id, next_notification_datetime, is_active, last_sent_at FROM Reminder WHERE reminder_id = 401;