-- 사용자 (User) 테이블
CREATE DATABASE youtube_summary;
USE youtube_summary;


CREATE TABLE `user` (
                        user_id INT AUTO_INCREMENT PRIMARY KEY,
                        username VARCHAR(100) UNIQUE NOT NULL,
                        email VARCHAR(255) UNIQUE NOT NULL,
                        password_hash VARCHAR(255) NOT NULL,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- 2. 영상 테이블
CREATE TABLE `video` (
                         video_id INT AUTO_INCREMENT PRIMARY KEY,
                         youtube_id VARCHAR(255) UNIQUE NOT NULL,
                         title VARCHAR(255) NOT NULL,
                         original_url VARCHAR(512) UNIQUE NOT NULL,
                         uploader_name VARCHAR(100),
                         thumbnail_url TEXT,
                         view_count BIGINT,
                         published_at DATETIME
);

-- 3. 오디오 트랜스크립트 테이블
CREATE TABLE `audiotranscript` (
                                   transcript_id INT AUTO_INCREMENT PRIMARY KEY,
                                   video_id INT NOT NULL,
                                   youtube_id VARCHAR(255) NOT NULL UNIQUE,
                                   transcript_text TEXT NOT NULL,
                                   created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                                   CONSTRAINT fk_audio_video FOREIGN KEY (video_id) REFERENCES video(video_id) ON DELETE CASCADE
);

-- 4. 요약 테이블
CREATE TABLE `summary` (
                           summary_id INT AUTO_INCREMENT PRIMARY KEY,
                           user_id INT NOT NULL,
                           transcript_id INT NOT NULL,
                           summary_text TEXT NOT NULL,
                           user_prompt TEXT,
                           language_code VARCHAR(10) NOT NULL,
                           created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                           summary_type VARCHAR(50),
                           CONSTRAINT fk_summary_user FOREIGN KEY (user_id) REFERENCES `user`(user_id) ON DELETE CASCADE,
                           CONSTRAINT fk_summary_transcript FOREIGN KEY (transcript_id) REFERENCES audiotranscript(transcript_id) ON DELETE CASCADE
);

-- 5. 사용자 라이브러리
CREATE TABLE `userlibrary` (
                               user_library_id INT AUTO_INCREMENT PRIMARY KEY,
                               user_id INT NOT NULL,
                               summary_id INT NOT NULL,
                               saved_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                               user_notes TEXT,
                               last_viewed_at TIMESTAMP,
                               CONSTRAINT fk_ul_user FOREIGN KEY (user_id) REFERENCES `user`(user_id) ON DELETE CASCADE,
                               CONSTRAINT fk_ul_summary FOREIGN KEY (summary_id) REFERENCES summary(summary_id) ON DELETE CASCADE,
                               CONSTRAINT uq_ul UNIQUE (user_id, summary_id)
);

-- 6. 리마인더
CREATE TABLE `reminder` (
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
                            CONSTRAINT fk_reminder_user FOREIGN KEY (user_id) REFERENCES `user`(user_id) ON DELETE CASCADE,
                            CONSTRAINT fk_reminder_ul FOREIGN KEY (user_library_id) REFERENCES userlibrary(user_library_id) ON DELETE CASCADE
);

-- 7. 퀴즈
CREATE TABLE `quiz` (
                        quiz_id INT AUTO_INCREMENT PRIMARY KEY,
                        summary_id INT NOT NULL,
                        title VARCHAR(255),
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                        CONSTRAINT fk_quiz_summary FOREIGN KEY (summary_id) REFERENCES summary(summary_id) ON DELETE CASCADE
);

-- 8. 질문
CREATE TABLE `question` (
                            question_id INT AUTO_INCREMENT PRIMARY KEY,
                            quiz_id INT NOT NULL,
                            question_text TEXT NOT NULL,
                            language_code VARCHAR(10) NOT NULL,
                            CONSTRAINT fk_question_quiz FOREIGN KEY (quiz_id) REFERENCES quiz(quiz_id) ON DELETE CASCADE
);

-- 9. 답변 선택지
CREATE TABLE `answeroption` (
                                answer_option_id INT AUTO_INCREMENT PRIMARY KEY,
                                question_id INT NOT NULL,
                                option_text TEXT NOT NULL,
                                is_correct BOOLEAN NOT NULL,
                                CONSTRAINT fk_answer_question FOREIGN KEY (question_id) REFERENCES question(question_id) ON DELETE CASCADE
);

-- 10. 태그
CREATE TABLE `tag` (
                       tag_id INT AUTO_INCREMENT PRIMARY KEY,
                       tag_name VARCHAR(100) UNIQUE NOT NULL
);

-- 11. 사용자 라이브러리 태그 연결
CREATE TABLE `userlibrarytag` (
                                  user_library_id INT NOT NULL,
                                  tag_id INT NOT NULL,
                                  PRIMARY KEY (user_library_id, tag_id),
                                  CONSTRAINT fk_ult_ul FOREIGN KEY (user_library_id) REFERENCES userlibrary(user_library_id) ON DELETE CASCADE,
                                  CONSTRAINT fk_ult_tag FOREIGN KEY (tag_id) REFERENCES tag(tag_id) ON DELETE CASCADE
);

-- 12. 영상 추천
CREATE TABLE `videorecommendation` (
                                       recommendation_id INT AUTO_INCREMENT PRIMARY KEY,
                                       user_id INT NOT NULL,
                                       source_video_id INT,
                                       recommended_video_id INT NOT NULL,
                                       recommendation_reason TEXT,
                                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                                       is_clicked BOOLEAN DEFAULT FALSE NOT NULL,
                                       clicked_at TIMESTAMP,
                                       CONSTRAINT fk_vr_user FOREIGN KEY (user_id) REFERENCES `user`(user_id) ON DELETE CASCADE,
                                       CONSTRAINT fk_vr_source FOREIGN KEY (source_video_id) REFERENCES video(video_id) ON DELETE SET NULL,
                                       CONSTRAINT fk_vr_recommended FOREIGN KEY (recommended_video_id) REFERENCES video(video_id) ON DELETE CASCADE
);

-- 13. 사용자 활동 로그
CREATE TABLE `useractivitylog` (
                                   log_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                   user_id INT NOT NULL,
                                   activity_type VARCHAR(50) NOT NULL,
                                   target_entity_type VARCHAR(50),
                                   target_entity_id_str VARCHAR(255),
                                   target_entity_id_int BIGINT,
                                   created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                                   activity_detail TEXT,
                                   details JSON,
                                   CONSTRAINT fk_log_user FOREIGN KEY (user_id) REFERENCES `user`(user_id) ON DELETE CASCADE
);
