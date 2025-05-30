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

-- 1. 사용자
INSERT INTO `user` (username, email, password_hash, created_at)
VALUES ('testuser', 'test@example.com', '1234hashed', NOW());

-- 2. 비디오
INSERT INTO Video (
    video_id, youtube_id, title, original_url, uploader_name, thumbnail_url,
    view_count, published_at
)
VALUES (
           'vid_test', 'abc123test', 'AI 시대의 시작',
           'https://youtube.com/watch?v=abc123test', 'TechWorld',
           'https://img.youtube.com/vi/abc123test/0.jpg', 987654, '2024-01-01 00:00:00'
       );

-- 3. 오디오 트랜스크립트
INSERT INTO AudioTranscript (video_id, transcript_text, created_at)
VALUES ('vid_test', 'AI is transforming the world.', NOW());

-- 4. 요약
INSERT INTO Summary (
    user_id, transcript_id, summary_text, user_prompt,
    language_code, created_at, summary_type
)
VALUES (
           1, 1, '이 영상은 인공지능의 시대를 소개합니다.',
           '간단한 요약 부탁해', 'ko', NOW(), 'basic'
       );

-- 5. 태그
INSERT INTO Tag (tag_name)
VALUES ('AI'), ('기술'), ('트렌드');

-- 6. 라이브러리
INSERT INTO UserLibrary (
    user_id, summary_id, saved_at, user_notes, last_viewed_at
)
VALUES (
           1, 1, NOW(), '학습용 영상입니다', NOW()
       );

-- 7. 라이브러리-태그 연결
INSERT INTO UserLibraryTag (user_library_id, tag_id)
VALUES (1, 1), (1, 2), (1, 3);

-- 8. 퀴즈
INSERT INTO Quiz (summary_id, title, created_at)
VALUES (1, 'AI Quiz', NOW());

-- 9. 질문
INSERT INTO Question (quiz_id, question_text, language_code)
VALUES (1, 'AI의 정의는 무엇인가요?', 'ko');

-- 10. 선택지
INSERT INTO AnswerOption (question_id, option_text, is_correct)
VALUES (1, '인공지능', TRUE), (1, '사람', FALSE), (1, '나무', FALSE);

-- 11. 리마인더
INSERT INTO Reminder (
    user_id, user_library_id, reminder_type, frequency_interval,
    base_datetime_for_recurrence, next_notification_datetime,
    reminder_note, is_active, created_at
)
VALUES (
           1, 1, 'weekly', 1, NOW(), DATE_ADD(NOW(), INTERVAL 7 DAY),
           '다음 주에 다시 보기!', TRUE, NOW()
       );

-- 12. 추천
INSERT INTO VideoRecommendation (
    user_id, source_video_id, recommended_video_id,
    recommendation_reason, created_at, is_clicked
)
VALUES (
           1, 'vid_test', 'vid_test',
           '비슷한 AI 주제 영상입니다.', NOW(), FALSE
       );

-- 13. 사용자 로그
INSERT INTO UserActivityLog (
    user_id, activity_type, target_entity_type, target_entity_id_str,
    created_at, activity_detail, details
)
VALUES (
           1, 'summary_view', 'Summary', '1',
           NOW(), '사용자가 요약을 조회했습니다.',
           JSON_OBJECT('action', 'view', 'summary_id', 1)
       );

