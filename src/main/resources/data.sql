DROP TABLE IF EXISTS UserActivityLog;
DROP TABLE IF EXISTS VideoRecommendation;
DROP TABLE IF EXISTS Reminder;
DROP TABLE IF EXISTS AnswerOption;
DROP TABLE IF EXISTS Question;
DROP TABLE IF EXISTS Quiz;
DROP TABLE IF EXISTS UserLibraryTag;
DROP TABLE IF EXISTS UserLibrary;
DROP TABLE IF EXISTS Summary;
DROP TABLE IF EXISTS AudioTranscript;
DROP TABLE IF EXISTS Tag;
DROP TABLE IF EXISTS Video;
DROP TABLE IF EXISTS `user`;

-- 사용자 (User) 테이블
CREATE TABLE `user` (
                        user_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        username VARCHAR(100) UNIQUE NOT NULL,
                        email VARCHAR(255) UNIQUE NOT NULL,
                        password_hash VARCHAR(255) NOT NULL,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- 비디오 (Video) 테이블
CREATE TABLE Video (
                       video_id VARCHAR(255) PRIMARY KEY,
                       youtube_id VARCHAR(255) UNIQUE NOT NULL,
                       title VARCHAR(255) NOT NULL,
                       original_url VARCHAR(255) UNIQUE NOT NULL,
                       uploader_name VARCHAR(100),
                       thumbnail_url TEXT,
                       view_count BIGINT,
                       published_at DATETIME
);

-- 태그 (Tag) 테이블
CREATE TABLE Tag (
                     tag_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
                     tag_name VARCHAR(100) UNIQUE NOT NULL
);

-- 오디오 트랜스크립트 (AudioTranscript) 테이블
CREATE TABLE AudioTranscript (
                                 transcript_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                 video_id VARCHAR(255) NOT NULL UNIQUE,
                                 transcript_text TEXT NOT NULL,
                                 created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                                 CONSTRAINT fk_audiotranscript_video FOREIGN KEY (video_id) REFERENCES Video(video_id) ON DELETE CASCADE
);

-- 요약 (Summary) 테이블
CREATE TABLE Summary (
                         summary_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                         user_id BIGINT NOT NULL,
                         transcript_id BIGINT NOT NULL,
                         summary_text TEXT NOT NULL,
                         user_prompt TEXT,
                         language_code VARCHAR(10) NOT NULL,
                         created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                         summary_type VARCHAR(50),
                         CONSTRAINT fk_summary_user FOREIGN KEY (user_id) REFERENCES `User`(user_id) ON DELETE CASCADE,
                         CONSTRAINT fk_summary_audiotranscript FOREIGN KEY (transcript_id) REFERENCES AudioTranscript(transcript_id) ON DELETE CASCADE
);

-- 사용자 라이브러리 (UserLibrary) 테이블
CREATE TABLE UserLibrary (
                             user_library_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                             user_id BIGINT NOT NULL,
                             summary_id BIGINT NOT NULL,
                             saved_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                             user_notes TEXT,
                             last_viewed_at TIMESTAMP,
                             CONSTRAINT fk_userlibrary_user FOREIGN KEY (user_id) REFERENCES `User`(user_id) ON DELETE CASCADE,
                             CONSTRAINT fk_userlibrary_summary FOREIGN KEY (summary_id) REFERENCES Summary(summary_id) ON DELETE CASCADE,
                             CONSTRAINT uq_userlibrary_user_summary UNIQUE (user_id, summary_id)
);

-- 리마인더 (Reminder) 테이블
CREATE TABLE Reminder (
                          reminder_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                          user_id BIGINT NOT NULL,
                          user_library_id BIGINT NOT NULL,
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
                          CONSTRAINT fk_reminder_user FOREIGN KEY (user_id) REFERENCES `User`(user_id) ON DELETE CASCADE,
                          CONSTRAINT fk_reminder_user_library FOREIGN KEY (user_library_id) REFERENCES UserLibrary(user_library_id) ON DELETE CASCADE
);

-- 퀴즈 (Quiz) 테이블
CREATE TABLE Quiz (
                      quiz_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                      summary_id BIGINT NOT NULL,
                      title VARCHAR(255),
                      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                      CONSTRAINT fk_quiz_summary FOREIGN KEY (summary_id) REFERENCES Summary(summary_id) ON DELETE CASCADE
);

-- 질문 (Question) 테이블
CREATE TABLE Question (
                          question_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                          quiz_id BIGINT NOT NULL,
                          question_text TEXT NOT NULL,
                          language_code VARCHAR(10) NOT NULL,
                          CONSTRAINT fk_question_quiz FOREIGN KEY (quiz_id) REFERENCES Quiz(quiz_id) ON DELETE CASCADE
);

-- 선택지 (AnswerOption) 테이블
CREATE TABLE AnswerOption (
                              answer_option_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                              question_id BIGINT NOT NULL,
                              option_text TEXT NOT NULL,
                              is_correct BOOLEAN NOT NULL,
                              CONSTRAINT fk_answer_question FOREIGN KEY (question_id) REFERENCES Question(question_id) ON DELETE CASCADE
);

-- 사용자 라이브러리 태그 연결 테이블
CREATE TABLE UserLibraryTag (
                                user_library_id BIGINT NOT NULL,
                                tag_id BIGINT UNSIGNED NOT NULL,
                                PRIMARY KEY (user_library_id, tag_id),
                                CONSTRAINT fk_userlibrarytag_userlibrary FOREIGN KEY (user_library_id) REFERENCES UserLibrary(user_library_id) ON DELETE CASCADE,
                                CONSTRAINT fk_userlibrarytag_tag FOREIGN KEY (tag_id) REFERENCES Tag(tag_id) ON DELETE CASCADE
);

-- 영상 추천 (VideoRecommendation) 테이블
CREATE TABLE VideoRecommendation (
                                     recommendation_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                     user_id BIGINT NOT NULL,
                                     source_video_id VARCHAR(255),
                                     recommended_video_id VARCHAR(255) NOT NULL,
                                     recommendation_reason TEXT,
                                     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                                     is_clicked BOOLEAN DEFAULT FALSE NOT NULL,
                                     clicked_at TIMESTAMP,
                                     CONSTRAINT fk_videorecommendation_user FOREIGN KEY (user_id) REFERENCES `User`(user_id) ON DELETE CASCADE,
                                     CONSTRAINT fk_videorecommendation_source_video FOREIGN KEY (source_video_id) REFERENCES Video(video_id) ON DELETE SET NULL,
                                     CONSTRAINT fk_videorecommendation_recommended_video FOREIGN KEY (recommended_video_id) REFERENCES Video(video_id) ON DELETE CASCADE
);

-- 사용자 활동 로그 (UserActivityLog) 테이블
CREATE TABLE UserActivityLog (
                                 log_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                 user_id BIGINT NOT NULL,
                                 activity_type VARCHAR(50) NOT NULL,
                                 target_entity_type VARCHAR(50),
                                 target_entity_id_str VARCHAR(255),
                                 target_entity_id_int BIGINT,
                                 created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                                 activity_detail TEXT,
                                 details JSON,
                                 CONSTRAINT fk_useractivitylog_user FOREIGN KEY (user_id) REFERENCES `User`(user_id) ON DELETE CASCADE
);

-- 사용자 (User) 테이블
CREATE TABLE `user` (
                        user_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        username VARCHAR(100) UNIQUE NOT NULL,
                        email VARCHAR(255) UNIQUE NOT NULL,
                        password_hash VARCHAR(255) NOT NULL,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- 비디오 (Video) 테이블
CREATE TABLE Video (
                       video_id VARCHAR(255) PRIMARY KEY,
                       youtube_id VARCHAR(255) UNIQUE NOT NULL,
                       title VARCHAR(255) NOT NULL,
                       original_url VARCHAR(255) UNIQUE NOT NULL,
                       uploader_name VARCHAR(100),
                       thumbnail_url TEXT,
                       view_count BIGINT,
                       published_at DATETIME
);

-- 태그 (Tag) 테이블
CREATE TABLE Tag (
                     tag_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
                     tag_name VARCHAR(100) UNIQUE NOT NULL
);

-- 오디오 트랜스크립트 (AudioTranscript) 테이블
CREATE TABLE AudioTranscript (
                                 transcript_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                 video_id VARCHAR(255) NOT NULL UNIQUE,
                                 transcript_text TEXT NOT NULL,
                                 created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                                 CONSTRAINT fk_audiotranscript_video FOREIGN KEY (video_id) REFERENCES Video(video_id) ON DELETE CASCADE
);

-- 요약 (Summary) 테이블
CREATE TABLE Summary (
                         summary_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                         user_id BIGINT NOT NULL,
                         transcript_id BIGINT NOT NULL,
                         summary_text TEXT NOT NULL,
                         user_prompt TEXT,
                         language_code VARCHAR(10) NOT NULL,
                         created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                         summary_type VARCHAR(50),
                         CONSTRAINT fk_summary_user FOREIGN KEY (user_id) REFERENCES `User`(user_id) ON DELETE CASCADE,
                         CONSTRAINT fk_summary_audiotranscript FOREIGN KEY (transcript_id) REFERENCES AudioTranscript(transcript_id) ON DELETE CASCADE
);

-- 사용자 라이브러리 (UserLibrary) 테이블
CREATE TABLE UserLibrary (
                             user_library_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                             user_id BIGINT NOT NULL,
                             summary_id BIGINT NOT NULL,
                             saved_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                             user_notes TEXT,
                             last_viewed_at TIMESTAMP,
                             CONSTRAINT fk_userlibrary_user FOREIGN KEY (user_id) REFERENCES `User`(user_id) ON DELETE CASCADE,
                             CONSTRAINT fk_userlibrary_summary FOREIGN KEY (summary_id) REFERENCES Summary(summary_id) ON DELETE CASCADE,
                             CONSTRAINT uq_userlibrary_user_summary UNIQUE (user_id, summary_id)
);

-- 리마인더 (Reminder) 테이블
CREATE TABLE Reminder (
                          reminder_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                          user_id BIGINT NOT NULL,
                          user_library_id BIGINT NOT NULL,
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
                          CONSTRAINT fk_reminder_user FOREIGN KEY (user_id) REFERENCES `User`(user_id) ON DELETE CASCADE,
                          CONSTRAINT fk_reminder_user_library FOREIGN KEY (user_library_id) REFERENCES UserLibrary(user_library_id) ON DELETE CASCADE
);

-- 퀴즈 (Quiz) 테이블
CREATE TABLE Quiz (
                      quiz_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                      summary_id BIGINT NOT NULL,
                      title VARCHAR(255),
                      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                      CONSTRAINT fk_quiz_summary FOREIGN KEY (summary_id) REFERENCES Summary(summary_id) ON DELETE CASCADE
);

-- 질문 (Question) 테이블
CREATE TABLE Question (
                          question_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                          quiz_id BIGINT NOT NULL,
                          question_text TEXT NOT NULL,
                          language_code VARCHAR(10) NOT NULL,
                          CONSTRAINT fk_question_quiz FOREIGN KEY (quiz_id) REFERENCES Quiz(quiz_id) ON DELETE CASCADE
);

-- 선택지 (AnswerOption) 테이블
CREATE TABLE AnswerOption (
                              answer_option_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                              question_id BIGINT NOT NULL,
                              option_text TEXT NOT NULL,
                              is_correct BOOLEAN NOT NULL,
                              CONSTRAINT fk_answer_question FOREIGN KEY (question_id) REFERENCES Question(question_id) ON DELETE CASCADE
);

-- 사용자 라이브러리 태그 연결 테이블
CREATE TABLE UserLibraryTag (
                                user_library_id BIGINT NOT NULL,
                                tag_id BIGINT UNSIGNED NOT NULL,
                                PRIMARY KEY (user_library_id, tag_id),
                                CONSTRAINT fk_userlibrarytag_userlibrary FOREIGN KEY (user_library_id) REFERENCES UserLibrary(user_library_id) ON DELETE CASCADE,
                                CONSTRAINT fk_userlibrarytag_tag FOREIGN KEY (tag_id) REFERENCES Tag(tag_id) ON DELETE CASCADE
);

-- 영상 추천 (VideoRecommendation) 테이블
CREATE TABLE VideoRecommendation (
                                     recommendation_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                     user_id BIGINT NOT NULL,
                                     source_video_id VARCHAR(255),
                                     recommended_video_id VARCHAR(255) NOT NULL,
                                     recommendation_reason TEXT,
                                     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                                     is_clicked BOOLEAN DEFAULT FALSE NOT NULL,
                                     clicked_at TIMESTAMP,
                                     CONSTRAINT fk_videorecommendation_user FOREIGN KEY (user_id) REFERENCES `User`(user_id) ON DELETE CASCADE,
                                     CONSTRAINT fk_videorecommendation_source_video FOREIGN KEY (source_video_id) REFERENCES Video(video_id) ON DELETE SET NULL,
                                     CONSTRAINT fk_videorecommendation_recommended_video FOREIGN KEY (recommended_video_id) REFERENCES Video(video_id) ON DELETE CASCADE
);

-- 사용자 활동 로그 (UserActivityLog) 테이블
CREATE TABLE UserActivityLog (
                                 log_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                 user_id BIGINT NOT NULL,
                                 activity_type VARCHAR(50) NOT NULL,
                                 target_entity_type VARCHAR(50),
                                 target_entity_id_str VARCHAR(255),
                                 target_entity_id_int BIGINT,
                                 created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                                 activity_detail TEXT,
                                 details JSON,
                                 CONSTRAINT fk_useractivitylog_user FOREIGN KEY (user_id) REFERENCES `User`(user_id) ON DELETE CASCADE
);


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