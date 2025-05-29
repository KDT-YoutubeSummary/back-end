-- 사용자 (User) 테이블
CREATE TABLE `user` (
                        user_id INT AUTO_INCREMENT PRIMARY KEY, -- SERIAL -> INT AUTO_INCREMENT
                        username VARCHAR(100) UNIQUE NOT NULL,
                        email VARCHAR(255) UNIQUE NOT NULL,
                        password_hash VARCHAR(255) NOT NULL,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- 비디오 (Video) 테이블
CREATE TABLE Video (
                       video_id VARCHAR(255) PRIMARY KEY,
                       title VARCHAR(255) NOT NULL,
                       original_url VARCHAR(2048) UNIQUE NOT NULL,
                       uploader_name VARCHAR(100),
                       original_language_code VARCHAR(20),
                       duration_seconds BIGINT NOT NULL
);

-- 태그 (Tag) 테이블
CREATE TABLE Tag (
                     tag_id INT AUTO_INCREMENT PRIMARY KEY, -- SERIAL -> INT AUTO_INCREMENT
                     tag_name VARCHAR(100) UNIQUE NOT NULL
);

-- 오디오 트랜스크립트 (AudioTranscript) 테이블
CREATE TABLE AudioTranscript (
                                 transcript_id INT AUTO_INCREMENT PRIMARY KEY, -- SERIAL -> INT AUTO_INCREMENT
                                 video_id VARCHAR(255) NOT NULL UNIQUE, -- 단일 트랜스크립트 가정 유지
                                 transcript_text TEXT NOT NULL,
                                 created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL, -- create_at -> created_at
                                 CONSTRAINT fk_audiotranscript_video FOREIGN KEY (video_id) REFERENCES Video(video_id) ON DELETE CASCADE
);

-- 요약 (Summary) 테이블
CREATE TABLE Summary (
                         summary_id INT AUTO_INCREMENT PRIMARY KEY, -- SERIAL -> INT AUTO_INCREMENT
                         user_id INT NOT NULL, -- BIGINT -> INT (User.user_id와 일관성)
                         transcript_id INT NOT NULL, -- BIGINT -> INT (AudioTranscript.transcript_id와 일관성)
                         summary_text TEXT NOT NULL,
                         language_code VARCHAR(10) NOT NULL,
                         created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL, -- create_at -> created_at
                         summary_type VARCHAR(50),
                         CONSTRAINT fk_summary_user FOREIGN KEY (user_id) REFERENCES `User`(user_id) ON DELETE CASCADE,
                         CONSTRAINT fk_summary_audiotranscript FOREIGN KEY (transcript_id) REFERENCES AudioTranscript(transcript_id) ON DELETE CASCADE
);

-- 사용자 라이브러리 (UserLibrary) 테이블
CREATE TABLE UserLibrary (
                             user_library_id INT AUTO_INCREMENT PRIMARY KEY, -- SERIAL -> INT AUTO_INCREMENT
                             user_id INT NOT NULL, -- BIGINT -> INT
                             summary_id INT NOT NULL, -- BIGINT -> INT
                             saved_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                             user_notes TEXT,
                             last_viewed_at TIMESTAMP,
                             CONSTRAINT fk_userlibrary_user FOREIGN KEY (user_id) REFERENCES `User`(user_id) ON DELETE CASCADE,
                             CONSTRAINT fk_userlibrary_summary FOREIGN KEY (summary_id) REFERENCES Summary(summary_id) ON DELETE CASCADE,
                             CONSTRAINT uq_userlibrary_user_summary UNIQUE (user_id, summary_id)
);

-- 리마인더 (Reminder) 테이블
CREATE TABLE Reminder (
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
                          CONSTRAINT fk_reminder_user FOREIGN KEY (user_id) REFERENCES `User`(user_id) ON DELETE CASCADE,
                          CONSTRAINT fk_reminder_user_library FOREIGN KEY (user_library_id) REFERENCES UserLibrary(user_library_id) ON DELETE CASCADE
);

-- 퀴즈 (Quiz) 테이블
CREATE TABLE Quiz (
                      quiz_id INT AUTO_INCREMENT PRIMARY KEY, -- SERIAL -> INT AUTO_INCREMENT
                      summary_id INT NOT NULL, -- UNIQUE 제약 조건 제거 (하나의 요약에 여러 퀴즈 가능성)
                      title VARCHAR(255),
                      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                      CONSTRAINT fk_quiz_summary FOREIGN KEY (summary_id) REFERENCES Summary(summary_id) ON DELETE CASCADE
);

-- 질문 (Question) 테이블
CREATE TABLE Question (
                          question_id INT AUTO_INCREMENT PRIMARY KEY, -- SERIAL -> INT AUTO_INCREMENT
                          quiz_id INT NOT NULL,
                          question_text TEXT NOT NULL,
                          language_code VARCHAR(10) NOT NULL, -- VARCHAR(255) -> VARCHAR(10) (언어 코드에 적합)
                          CONSTRAINT fk_question_quiz FOREIGN KEY (quiz_id) REFERENCES Quiz(quiz_id) ON DELETE CASCADE
);

-- 답변 선택지 (AnswerOption) 테이블
CREATE TABLE AnswerOption (
                              answer_option_id INT AUTO_INCREMENT PRIMARY KEY, -- SERIAL -> INT AUTO_INCREMENT
                              question_id INT NOT NULL,
                              option_text TEXT NOT NULL,
                              is_correct BOOLEAN NOT NULL,
                              CONSTRAINT fk_answer_question FOREIGN KEY (question_id) REFERENCES Question(question_id) ON DELETE CASCADE
);

-- 사용자 라이브러리 태그 (UserLibraryTag) 연결 테이블
CREATE TABLE UserLibraryTag (
                                user_library_id INT NOT NULL, -- BIGINT -> INT
                                tag_id INT NOT NULL, -- BIGINT -> INT
                                PRIMARY KEY (user_library_id, tag_id),
                                CONSTRAINT fk_userlibrarytag_userlibrary FOREIGN KEY (user_library_id) REFERENCES UserLibrary(user_library_id) ON DELETE CASCADE,
                                CONSTRAINT fk_userlibrarytag_tag FOREIGN KEY (tag_id) REFERENCES Tag(tag_id) ON DELETE CASCADE
);

-- 영상 추천 (VideoRecommendation) 테이블
CREATE TABLE VideoRecommendation (
                                     recommendation_id INT AUTO_INCREMENT PRIMARY KEY, -- SERIAL -> INT AUTO_INCREMENT
                                     user_id INT NOT NULL, -- BIGINT -> INT
                                     source_video_id VARCHAR(255),
                                     recommended_video_id VARCHAR(255) NOT NULL,
                                     recommendation_reason TEXT,
                                     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL, -- create_at -> created_at
                                     is_clicked BOOLEAN DEFAULT FALSE NOT NULL,
                                     clicked_at TIMESTAMP,
                                     CONSTRAINT fk_videorecommendation_user FOREIGN KEY (user_id) REFERENCES `User`(user_id) ON DELETE CASCADE,
                                     CONSTRAINT fk_videorecommendation_source_video FOREIGN KEY (source_video_id) REFERENCES Video(video_id) ON DELETE SET NULL,
                                     CONSTRAINT fk_videorecommendation_recommended_video FOREIGN KEY (recommended_video_id) REFERENCES Video(video_id) ON DELETE CASCADE
);

-- 사용자 활동 로그 (UserActivityLog) 테이블
CREATE TABLE UserActivityLog (
                                 log_id BIGINT AUTO_INCREMENT PRIMARY KEY, -- BIGSERIAL -> BIGINT AUTO_INCREMENT
                                 user_id INT NOT NULL, -- BIGINT -> INT
                                 activity_type VARCHAR(50) NOT NULL,
                                 target_entity_type VARCHAR(50),
                                 target_entity_id_str VARCHAR(255),
                                 target_entity_id_int BIGINT,
                                 created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL, -- create_at -> created_at
                                 activity_detail TEXT,
                                 details JSON, -- MySQL에서는 JSONB 대신 JSON
                                 CONSTRAINT fk_useractivitylog_user FOREIGN KEY (user_id) REFERENCES `User`(user_id) ON DELETE CASCADE
);