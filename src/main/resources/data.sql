-- 1. 기존 데이터베이스 삭제
DROP DATABASE IF EXISTS youtube_summary;

-- 2. 새로 생성
CREATE DATABASE youtube_summary;
USE youtube_summary;

-- 3. 테이블 생성
CREATE TABLE `user` (
                        user_id INT AUTO_INCREMENT PRIMARY KEY,
                        username VARCHAR(100) UNIQUE NOT NULL,
                        email VARCHAR(255) UNIQUE NOT NULL,
                        password_hash VARCHAR(255) NOT NULL,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE Video (
                       video_id VARCHAR(255) PRIMARY KEY,
                       title VARCHAR(255) NOT NULL,
                       original_url VARCHAR(512) UNIQUE NOT NULL,
                       uploader_name VARCHAR(100),
                       original_language_code VARCHAR(20),
                       duration_seconds BIGINT NOT NULL
);

CREATE TABLE Tag (
                     tag_id INT AUTO_INCREMENT PRIMARY KEY,
                     tag_name VARCHAR(100) UNIQUE NOT NULL
);

CREATE TABLE audio_transcript (
                                  transcript_id INT AUTO_INCREMENT PRIMARY KEY,
                                  video_id VARCHAR(255) NOT NULL UNIQUE,
                                  transcript_text LONGTEXT NOT NULL,
                                  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                                  CONSTRAINT fk_audio_video FOREIGN KEY (video_id) REFERENCES Video(video_id) ON DELETE CASCADE
);

CREATE TABLE Summary (
                         summary_id INT AUTO_INCREMENT PRIMARY KEY,
                         user_id INT NOT NULL,
                         transcript_id INT NOT NULL,
                         summary_text TEXT NOT NULL,
                         language_code VARCHAR(10) NOT NULL,
                         created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                         summary_type VARCHAR(50),
                         FOREIGN KEY (user_id) REFERENCES `user`(user_id) ON DELETE CASCADE,
                         FOREIGN KEY (transcript_id) REFERENCES audio_transcript(transcript_id) ON DELETE CASCADE
);

CREATE TABLE UserLibrary (
                             user_library_id INT AUTO_INCREMENT PRIMARY KEY,
                             user_id INT NOT NULL,
                             summary_id INT NOT NULL,
                             saved_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                             user_notes TEXT,
                             last_viewed_at TIMESTAMP,
                             FOREIGN KEY (user_id) REFERENCES `user`(user_id) ON DELETE CASCADE,
                             FOREIGN KEY (summary_id) REFERENCES Summary(summary_id) ON DELETE CASCADE,
                             UNIQUE (user_id, summary_id)
);

CREATE TABLE Reminder (
                          reminder_id INT AUTO_INCREMENT PRIMARY KEY,
                          user_id INT NOT NULL,
                          user_library_id INT NOT NULL,
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
                          FOREIGN KEY (user_id) REFERENCES `user`(user_id) ON DELETE CASCADE,
                          FOREIGN KEY (user_library_id) REFERENCES UserLibrary(user_library_id) ON DELETE CASCADE
);

CREATE TABLE Quiz (
                      quiz_id INT AUTO_INCREMENT PRIMARY KEY,
                      summary_id INT NOT NULL,
                      title VARCHAR(255),
                      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                      FOREIGN KEY (summary_id) REFERENCES Summary(summary_id) ON DELETE CASCADE
);

CREATE TABLE Question (
                          question_id INT AUTO_INCREMENT PRIMARY KEY,
                          quiz_id INT NOT NULL,
                          question_text TEXT NOT NULL,
                          language_code VARCHAR(10) NOT NULL,
                          FOREIGN KEY (quiz_id) REFERENCES Quiz(quiz_id) ON DELETE CASCADE
);

CREATE TABLE AnswerOption (
                              answer_option_id INT AUTO_INCREMENT PRIMARY KEY,
                              question_id INT NOT NULL,
                              option_text TEXT NOT NULL,
                              is_correct BOOLEAN NOT NULL,
                              FOREIGN KEY (question_id) REFERENCES Question(question_id) ON DELETE CASCADE
);

CREATE TABLE UserLibraryTag (
                                user_library_id INT NOT NULL,
                                tag_id INT NOT NULL,
                                PRIMARY KEY (user_library_id, tag_id),
                                FOREIGN KEY (user_library_id) REFERENCES UserLibrary(user_library_id) ON DELETE CASCADE,
                                FOREIGN KEY (tag_id) REFERENCES Tag(tag_id) ON DELETE CASCADE
);

CREATE TABLE VideoRecommendation (
                                     recommendation_id INT AUTO_INCREMENT PRIMARY KEY,
                                     user_id INT NOT NULL,
                                     source_video_id VARCHAR(255),
                                     recommended_video_id VARCHAR(255) NOT NULL,
                                     recommendation_reason TEXT,
                                     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                                     is_clicked BOOLEAN DEFAULT FALSE NOT NULL,
                                     clicked_at TIMESTAMP,
                                     FOREIGN KEY (user_id) REFERENCES `user`(user_id) ON DELETE CASCADE,
                                     FOREIGN KEY (source_video_id) REFERENCES Video(video_id) ON DELETE SET NULL,
                                     FOREIGN KEY (recommended_video_id) REFERENCES Video(video_id) ON DELETE CASCADE
);

CREATE TABLE UserActivityLog (
                                 log_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                 user_id INT NOT NULL,
                                 activity_type VARCHAR(50) NOT NULL,
                                 target_entity_type VARCHAR(50),
                                 target_entity_id_str VARCHAR(255),
                                 target_entity_id_int BIGINT,
                                 created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                                 activity_detail TEXT,
                                 details JSON,
                                 FOREIGN KEY (user_id) REFERENCES `user`(user_id) ON DELETE CASCADE
);
