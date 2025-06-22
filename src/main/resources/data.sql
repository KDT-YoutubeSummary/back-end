CREATE DATABASE IF NOT EXISTS yousum;
USE yousum;

-- 사용자 (User) 테이블
CREATE TABLE `user` (
                        user_id INT AUTO_INCREMENT PRIMARY KEY,
                        username VARCHAR(100) UNIQUE NOT NULL,
                        email VARCHAR(255) UNIQUE NOT NULL,
                        password_hash VARCHAR(255) NOT NULL,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- 비디오 (Video) 테이블
CREATE TABLE `video` (
                       video_id INT AUTO_INCREMENT PRIMARY KEY,
                       youtube_id  VARCHAR(255) UNIQUE NOT NULL,      --  (추가) 유튜브 영상 ID
                       title VARCHAR(255) NOT NULL,                 -- 영상 제목
                       original_url VARCHAR(512) UNIQUE NOT NULL,  -- 영상 원본 링크
                       uploader_name VARCHAR(100)             ,      -- 채널명 (업로더)
                       thumbnail_url TEXT,                          --  (추가)  썸네일 이미지 URL
                       view_count BIGINT,                           --  (추가)  조회수
                       published_at DATETIME,                       -- (추가)  업로드 날짜
                       duration_seconds INT NOT NULL,               -- 영상 길이 (초 단위)
                       original_language_code VARCHAR(20) NULL  -- 영상의 원본 언어 코드
);

-- 태그 (Tag) 테이블
CREATE TABLE `tag` (
                     tag_id INT AUTO_INCREMENT PRIMARY KEY, -- SERIAL -> INT AUTO_INCREMENT
                     tag_name VARCHAR(100) UNIQUE NOT NULL
);

-- 오디오 트랜스크립트 (AudioTranscript) 테이블
CREATE TABLE `audio_transcript` (
                     transcript_id INT AUTO_INCREMENT PRIMARY KEY, -- SERIAL -> INT AUTO_INCREMENT
                     video_id INT NOT NULL UNIQUE, -- 단일 트랜스크립트 가정 유지
                     youtube_id VARCHAR(255) NOT NULL UNIQUE,
                     transcript_path TEXT NOT NULL, -- 텍스트로 저장하지 않고 파일 경로로 저장
                     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL, -- create_at -> created_at
                     CONSTRAINT fk_audio_transcript_video FOREIGN KEY (video_id) REFERENCES video(video_id) ON DELETE CASCADE
);

-- 요약 (Summary) 테이블
CREATE TABLE `summary`(
                     summary_id INT AUTO_INCREMENT PRIMARY KEY, -- SERIAL -> INT AUTO_INCREMENT
                     user_id INT NOT NULL, -- BIGINT -> INT (User.user_id와 일관성)
                     transcript_id INT NOT NULL, -- BIGINT -> INT (AudioTranscript.transcript_id와 일관성)
                     summary_text TEXT NOT NULL,
                     user_prompt TEXT, -- (추가) 사용자 프롬프트 (사용 목적, 요청 문장 등 전체 포함 가능)
                     language_code VARCHAR(10) NULL,
                     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL, -- create_at -> created_at
                     summary_type ENUM('BASIC', 'THREE_LINE', 'KEYWORD', 'TIMELINE'),
                     CONSTRAINT fk_summary_user FOREIGN KEY (user_id) REFERENCES `user`(user_id) ON DELETE CASCADE,
                     CONSTRAINT fk_summary_audio_transcript FOREIGN KEY (transcript_id) REFERENCES audio_transcript(transcript_id) ON DELETE CASCADE
);

-- 요약 저장소 (SummaryArchive) 테이블 - 기존 user_library에서 변경
CREATE TABLE `summary_archive` (
                     summary_archive_id INT AUTO_INCREMENT PRIMARY KEY,
                     user_id INT NOT NULL,
                     summary_id INT NOT NULL,
                     user_notes TEXT,
                     last_viewed_at TIMESTAMP,
                     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                     updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                     CONSTRAINT fk_summary_archive_user FOREIGN KEY (user_id) REFERENCES `user`(user_id) ON DELETE CASCADE,
                     CONSTRAINT fk_summary_archive_summary FOREIGN KEY (summary_id) REFERENCES summary(summary_id) ON DELETE CASCADE,
                     CONSTRAINT uq_summary_archive_user_summary UNIQUE (user_id, summary_id)
);

-- 리마인더 (Reminder) 테이블 - summary_archive_id로 변경
CREATE TABLE `reminder` (
                          reminder_id INT AUTO_INCREMENT PRIMARY KEY,
                          user_id INT NOT NULL,
                          summary_archive_id INT NOT NULL,
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
                          CONSTRAINT fk_reminder_summary_archive FOREIGN KEY (summary_archive_id) REFERENCES summary_archive(summary_archive_id) ON DELETE CASCADE
);

-- 퀴즈 (Quiz) 테이블
CREATE TABLE `quiz` (
                      quiz_id INT AUTO_INCREMENT PRIMARY KEY, -- SERIAL -> INT AUTO_INCREMENT
                      summary_id INT NOT NULL, -- UNIQUE 제약 조건 제거 (하나의 요약에 여러 퀴즈 가능성)
                      title VARCHAR(255),
                      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                      CONSTRAINT fk_quiz_summary FOREIGN KEY (summary_id) REFERENCES summary(summary_id) ON DELETE CASCADE
);

-- 질문 (Question) 테이블
CREATE TABLE `question` (
                          question_id INT AUTO_INCREMENT PRIMARY KEY, -- SERIAL -> INT AUTO_INCREMENT
                          quiz_id INT NOT NULL,
                          question_text TEXT NOT NULL,
                          language_code VARCHAR(10) NOT NULL, -- VARCHAR(255) -> VARCHAR(10) (언어 코드에 적합)
                          explanation TEXT, -- (추가) 질문에 대한 설명
                          CONSTRAINT fk_question_quiz FOREIGN KEY (quiz_id) REFERENCES quiz(quiz_id) ON DELETE CASCADE
);

-- 답변 선택지 (AnswerOption) 테이블
CREATE TABLE `answer_option` (
                              answer_option_id INT AUTO_INCREMENT PRIMARY KEY, -- SERIAL -> INT AUTO_INCREMENT
                              question_id INT NOT NULL,
                              option_text TEXT NOT NULL,
                              is_correct BOOLEAN NOT NULL,
                              transcript_id BIGINT NOT NULL,
                              summary_text TEXT NOT NULL,
                              summary_type ENUM('THREE_LINE', 'KEYWORD', 'TIMELINE'),
                              created_at DATETIME NOT NULL,
                              CONSTRAINT fk_answer_question FOREIGN KEY (question_id) REFERENCES question(question_id) ON DELETE CASCADE
);

-- 요약 저장소 태그 (SummaryArchiveTag) 연결 테이블 - 기존 user_library_tag에서 변경
CREATE TABLE `summary_archive_tag` (
                                summary_archive_id INT NOT NULL,
                                tag_id INT NOT NULL,
                                PRIMARY KEY (summary_archive_id, tag_id),
                                CONSTRAINT fk_summary_archive_tag_archive FOREIGN KEY (summary_archive_id) REFERENCES summary_archive(summary_archive_id) ON DELETE CASCADE,
                                CONSTRAINT fk_summary_archive_tag_tag FOREIGN KEY (tag_id) REFERENCES tag(tag_id) ON DELETE CASCADE
);

-- 영상 추천 (VideoRecommendation) 테이블
CREATE TABLE `video_recommendation` (
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
CREATE TABLE `user_activity_log` (
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

-- 더미 데이터 삽입
-- ✅ 1. 사용자
INSERT INTO user (user_id, username, email, password_hash) VALUES
    (1, 'test_user', 'test@example.com', 'hashedpassword');

-- ✅ 2. 비디오
INSERT INTO video (video_id, youtube_id, title, original_url, uploader_name, thumbnail_url, view_count, published_at, duration_seconds, original_language_code) VALUES
    (101, 'uEBOoH2z0VQ', 'Sample Video Title', 'https://www.youtube.com/watch?v=uEBOoH2z0VQ', 'Sample Channel', 'https://i.ytimg.com/vi/uEBOoH2z0VQ/maxresdefault.jpg', 1000, '2024-01-01 12:00:00', 600, 'ko');

-- ✅ 3. 오디오 트랜스크립트
INSERT INTO audio_transcript (transcript_id, video_id, youtube_id, transcript_path) VALUES
    (151, 101, 'uEBOoH2z0VQ', 'src/main/resources/textfiles/uEBOoH2z0VQ.txt');

-- ✅ 4. 요약
INSERT INTO summary (summary_id, user_id, transcript_id, summary_text, user_prompt, language_code, summary_type) VALUES
    (201, 1, 151, 'This is a sample summary of the video content.', 'Please summarize this video', 'ko', 'BASIC');

-- ✅ 5. 요약 저장소 (기존 user_library)
INSERT INTO summary_archive (summary_archive_id, user_id, summary_id, user_notes, last_viewed_at) VALUES
    (301, 1, 201, 'Sample note', '2025-06-05 06:33:59');

-- ✅ 6. 리마인더 (summary_archive_id로 변경)
INSERT INTO reminder (
    user_id, summary_archive_id, reminder_type, frequency_interval, day_of_week, day_of_month,
    base_datetime_for_recurrence, next_notification_datetime, reminder_note, is_active, created_at, last_sent_at
) VALUES (
             1, 301, 'ONE_TIME', 1, NULL, NULL,
             '2025-06-05 06:33:00', '2025-06-05 06:38:00',
             'Test reminder at 06:38', TRUE, '2025-06-05 06:33:59', NULL
         );

-- ✅ 7. 태그
INSERT INTO tag (tag_id, tag_name) VALUES
                                       (1, '교육'), (2, '기술');

-- ✅ 8. 요약 저장소 태그 연결 (기존 user_library_tag)
INSERT INTO summary_archive_tag (summary_archive_id, tag_id) VALUES
                                                           (301, 1), (301, 2);

-- ✅ 9. 퀴즈
INSERT INTO quiz (quiz_id, summary_id, title) VALUES
    (601, 201, 'AI 기술 요약 퀴즈');

-- ✅ 10. 질문
INSERT INTO question (question_id, quiz_id, question_text, language_code, explanation) VALUES
                                                                              (701, 601, '이 영상에서 주로 다루는 기술은 무엇인가요?', 'ko', 'AI 기술에 대한 설명이 포함되어 있습니다.'),
                                                                              (702, 601, 'AI의 어떤 하위 분야에 대한 언급이 있었나요?', 'ko', 'AI의 하위 분야에 대한 설명이 포함되어 있습니다.');

-- ✅ 11. 선택지
INSERT INTO answer_option (
    answer_option_id, question_id, option_text, is_correct,
    transcript_id, summary_text, summary_type, created_at
) VALUES
      (801, 701, '인공지능', TRUE, 151, 'AI 기술 요약', 'THREE_LINE', '2025-06-05 06:33:59'),
      (802, 701, '블록체인', FALSE, 151, 'AI 기술 요약', 'THREE_LINE', '2025-06-05 06:33:59'),
      (803, 702, '머신러닝', TRUE, 151, 'AI 기술 요약', 'THREE_LINE', '2025-06-05 06:33:59'),
      (804, 702, '웹 개발', FALSE, 151, 'AI 기술 요약', 'THREE_LINE', '2025-06-05 06:33:59');

-- ✅ 12. 영상 추천
INSERT INTO video_recommendation (recommendation_id, user_id, source_video_id, recommended_video_id, recommendation_reason, is_clicked) VALUES
    (901, 1, 101, 101, 'Similar content based on AI technology', FALSE);

-- ✅ 13. 사용자 활동 로그
INSERT INTO user_activity_log (user_id, activity_type, target_entity_type, target_entity_id_int, activity_detail, details) VALUES
    (1, 'SUMMARY_CREATED', 'SUMMARY', 201, '요약 생성 완료', '{"videoTitle": "Sample Video Title", "summaryType": "BASIC"}');