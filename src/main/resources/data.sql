-- ✅ YouSum 데이터베이스 스키마 및 더미 데이터
-- Updated: 2025-06-23 - LONGTEXT 지원, 컬럼명 표준화

CREATE DATABASE IF NOT EXISTS yousum;
USE yousum;

-- 사용자 (User) 테이블
CREATE TABLE IF NOT EXISTS `user` (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(100) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- 비디오 (Video) 테이블
CREATE TABLE IF NOT EXISTS `video` (
    video_id INT AUTO_INCREMENT PRIMARY KEY,
    youtube_id VARCHAR(255) UNIQUE NOT NULL,
    title VARCHAR(255) NOT NULL,
    original_url VARCHAR(512) UNIQUE NOT NULL,
    uploader_name VARCHAR(100),
    thumbnail_url TEXT,
    view_count BIGINT,
    published_at DATETIME,
    duration_seconds INT NOT NULL,
    original_language_code VARCHAR(20) NULL
);

-- 태그 (Tag) 테이블
CREATE TABLE IF NOT EXISTS `tag` (
    tag_id INT AUTO_INCREMENT PRIMARY KEY,
    tag_name VARCHAR(100) UNIQUE NOT NULL
);

-- 오디오 트랜스크립트 (AudioTranscript) 테이블
CREATE TABLE IF NOT EXISTS `audio_transcript` (
    transcript_id INT AUTO_INCREMENT PRIMARY KEY,
    video_id INT NOT NULL UNIQUE,
    youtube_id VARCHAR(255) NOT NULL UNIQUE,
    transcript_path LONGTEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_audio_transcript_video FOREIGN KEY (video_id) REFERENCES video(video_id) ON DELETE CASCADE
);

-- ✅ 요약 (Summary) 테이블 - LONGTEXT 컬럼 적용
CREATE TABLE IF NOT EXISTS `summary` (
    summary_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    transcript_id INT NOT NULL,
    summary_text LONGTEXT NOT NULL COMMENT '요약 텍스트 (무제한 길이 지원)',
    user_prompt LONGTEXT COMMENT '사용자 프롬프트 (무제한 길이 지원)',
    language_code VARCHAR(10) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    summary_type ENUM('BASIC', 'THREE_LINE', 'KEYWORD', 'TIMELINE'),
    CONSTRAINT fk_summary_user FOREIGN KEY (user_id) REFERENCES `user`(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_summary_audiotranscript FOREIGN KEY (transcript_id) REFERENCES audio_transcript(transcript_id) ON DELETE CASCADE
);

-- 요약 저장소 (SummaryArchive) 테이블
CREATE TABLE IF NOT EXISTS `summary_archive` (
    archive_id INT AUTO_INCREMENT PRIMARY KEY,
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

-- 리마인더 (Reminder) 테이블
CREATE TABLE IF NOT EXISTS `reminder` (
    reminder_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    archive_id INT NOT NULL,
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
    CONSTRAINT fk_reminder_summary_archive FOREIGN KEY (archive_id) REFERENCES summary_archive(archive_id) ON DELETE CASCADE
);

-- 퀴즈 (Quiz) 테이블
CREATE TABLE IF NOT EXISTS `quiz` (
    quiz_id INT AUTO_INCREMENT PRIMARY KEY,
    summary_id INT NOT NULL,
    title VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_quiz_summary FOREIGN KEY (summary_id) REFERENCES summary(summary_id) ON DELETE CASCADE
);

-- 질문 (Question) 테이블
CREATE TABLE IF NOT EXISTS `question` (
    question_id INT AUTO_INCREMENT PRIMARY KEY,
    quiz_id INT NOT NULL,
    question_text TEXT NOT NULL,
    language_code VARCHAR(10) NOT NULL,
    explanation TEXT,
    CONSTRAINT fk_question_quiz FOREIGN KEY (quiz_id) REFERENCES quiz(quiz_id) ON DELETE CASCADE
);

-- 답변 선택지 (AnswerOption) 테이블
CREATE TABLE IF NOT EXISTS `answer_option` (
    answer_option_id INT AUTO_INCREMENT PRIMARY KEY,
    question_id INT NOT NULL,
    option_text TEXT NOT NULL,
    is_correct BOOLEAN NOT NULL,
    transcript_id INT NOT NULL,
    summary_text TEXT NOT NULL,
    summary_type ENUM('THREE_LINE', 'KEYWORD', 'TIMELINE'),
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_answer_question FOREIGN KEY (question_id) REFERENCES question(question_id) ON DELETE CASCADE
);

-- 요약 저장소 태그 (SummaryArchiveTag) 연결 테이블
CREATE TABLE IF NOT EXISTS `summary_archive_tag` (
    archive_id INT NOT NULL,
    tag_id INT NOT NULL,
    PRIMARY KEY (archive_id, tag_id),
    CONSTRAINT fk_summary_archive_tag_archive FOREIGN KEY (archive_id) REFERENCES summary_archive(archive_id) ON DELETE CASCADE,
    CONSTRAINT fk_summary_archive_tag_tag FOREIGN KEY (tag_id) REFERENCES tag(tag_id) ON DELETE CASCADE
);

-- 영상 추천 (VideoRecommendation) 테이블
CREATE TABLE IF NOT EXISTS `video_recommendation` (
    recommendation_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    source_video_id INT NULL,
    recommended_video_id INT NOT NULL,
    recommendation_reason TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    is_clicked BOOLEAN DEFAULT FALSE NOT NULL,
    clicked_at TIMESTAMP,
    CONSTRAINT fk_video_recommendation_user FOREIGN KEY (user_id) REFERENCES `user`(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_video_recommendation_source_video FOREIGN KEY (source_video_id) REFERENCES video(video_id) ON DELETE SET NULL,
    CONSTRAINT fk_video_recommendation_recommended_video FOREIGN KEY (recommended_video_id) REFERENCES video(video_id) ON DELETE CASCADE
);

-- 사용자 활동 로그 (UserActivityLog) 테이블
CREATE TABLE IF NOT EXISTS `user_activity_log` (
    log_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    activity_type VARCHAR(50) NOT NULL,
    target_entity_type VARCHAR(50),
    target_entity_id_str VARCHAR(255),
    target_entity_id_int BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    activity_detail TEXT,
    details JSON,
    CONSTRAINT fk_user_activity_log_user FOREIGN KEY (user_id) REFERENCES `user`(user_id) ON DELETE CASCADE
);

-- ============================================================================
-- 더미 데이터 삽입 (테스트용)
-- ============================================================================

-- ✅ 1. 사용자 데이터
INSERT IGNORE INTO user (user_id, username, email, password_hash) VALUES
(1, 'test_user', 'test@example.com', '$2a$10$hashedpasswordexample'),
(2, 'demo_user', 'demo@yousum.com', '$2a$10$anotherhashexample');

-- ✅ 2. 비디오 데이터  
INSERT IGNORE INTO video (video_id, youtube_id, title, original_url, uploader_name, thumbnail_url, view_count, published_at, duration_seconds, original_language_code) VALUES
(101, 'uEBOoH2z0VQ', '테스트 영상 - AI 기술 설명', 'https://www.youtube.com/watch?v=uEBOoH2z0VQ', '테크 채널', 'https://i.ytimg.com/vi/uEBOoH2z0VQ/maxresdefault.jpg', 15420, '2024-01-15 14:30:00', 720, 'ko'),
(102, 'CFIW0rgYF3Q', '머신러닝 기초 강의', 'https://www.youtube.com/watch?v=CFIW0rgYF3Q', 'AI 교육', 'https://i.ytimg.com/vi/CFIW0rgYF3Q/maxresdefault.jpg', 8750, '2024-02-10 10:15:00', 540, 'ko');

-- ✅ 3. 오디오 트랜스크립트 데이터
INSERT IGNORE INTO audio_transcript (transcript_id, video_id, youtube_id, transcript_path) VALUES
(151, 101, 'uEBOoH2z0VQ', 'src/main/resources/textfiles/cleaned_uEBOoH2z0VQ.txt'),
(152, 102, 'CFIW0rgYF3Q', 'src/main/resources/textfiles/cleaned_CFIW0rgYF3Q.txt');

-- ✅ 4. 요약 데이터 (LONGTEXT 지원)
INSERT IGNORE INTO summary (summary_id, user_id, transcript_id, summary_text, user_prompt, language_code, summary_type) VALUES
(201, 1, 151, 
'## AI 기술 개요

이 영상은 현재 인공지능 기술의 현황과 발전 방향에 대해 설명합니다.

### 주요 내용
- **머신러닝**: 데이터로부터 패턴을 학습하는 기술
- **딥러닝**: 신경망을 이용한 고도화된 학습 방법  
- **자연어 처리**: 컴퓨터가 인간의 언어를 이해하고 처리하는 기술

### 실제 적용 사례
1. 이미지 인식 시스템
2. 음성 인식 및 합성
3. 추천 시스템
4. 자율주행 자동차

AI 기술은 빠르게 발전하고 있으며, 앞으로 더 많은 분야에 적용될 것으로 예상됩니다.',
'AI 기술에 대해 자세히 설명해주세요. 초보자도 이해할 수 있도록 해주세요.',
'ko', 'BASIC'),

(202, 1, 152,
'**Keywords:** 머신러닝, 데이터분석, 알고리즘, 예측모델, 지도학습

**머신러닝** 기초 강의는 데이터에서 패턴을 찾아 **예측모델**을 만드는 과정을 다룹니다. **지도학습**과 비지도학습의 차이점을 설명하고, 실제 **데이터분석** 프로젝트에서 사용되는 주요 **알고리즘**들을 소개합니다.',
'머신러닝의 핵심 개념들을 키워드와 함께 요약해주세요.',
'ko', 'KEYWORD');

-- ✅ 5. 요약 저장소 데이터
INSERT IGNORE INTO summary_archive (archive_id, user_id, summary_id, user_notes, last_viewed_at) VALUES
(301, 1, 201, 'AI 기술 기초 - 중요한 내용이니 주기적으로 복습하기', '2025-06-23 15:30:00'),
(302, 1, 202, '머신러닝 공부 시작점', '2025-06-23 16:15:00');

-- ✅ 6. 태그 데이터
INSERT IGNORE INTO tag (tag_id, tag_name) VALUES
(1, '인공지능'), (2, '머신러닝'), (3, '교육'), (4, '기술'), (5, '프로그래밍'),
(6, '데이터분석'), (7, '딥러닝'), (8, '자연어처리'), (9, '컴퓨터비전'), (10, '알고리즘');

-- ✅ 7. 요약 저장소 태그 연결
INSERT IGNORE INTO summary_archive_tag (archive_id, tag_id) VALUES
(301, 1), (301, 3), (301, 4),
(302, 2), (302, 3), (302, 6);

-- ✅ 8. 리마인더 데이터
INSERT IGNORE INTO reminder (
    user_id, summary_archive_id, reminder_type, frequency_interval, 
    base_datetime_for_recurrence, next_notification_datetime, 
    reminder_note, is_active, created_at
) VALUES 
(1, 301, 'WEEKLY', 7, '2025-06-30 09:00:00', '2025-06-30 09:00:00', 'AI 기술 복습 시간', TRUE, NOW()),
(1, 302, 'DAILY', 1, '2025-06-24 19:00:00', '2025-06-24 19:00:00', '머신러닝 일일 복습', TRUE, NOW());

-- ✅ 9. 퀴즈 데이터
INSERT IGNORE INTO quiz (quiz_id, summary_id, title) VALUES
(601, 201, 'AI 기술 이해도 체크'),
(602, 202, '머신러닝 기초 퀴즈');

-- ✅ 10. 질문 데이터
INSERT IGNORE INTO question (question_id, quiz_id, question_text, language_code, explanation) VALUES
(701, 601, 'AI 기술 중에서 데이터로부터 패턴을 학습하는 기술은 무엇인가요?', 'ko', '머신러닝의 정의에 관한 문제입니다.'),
(702, 601, '자연어 처리 기술의 주요 목적은 무엇인가요?', 'ko', 'NLP의 목표에 대한 이해를 확인합니다.'),
(703, 602, '지도학습에서 가장 중요한 요소는 무엇인가요?', 'ko', '지도학습의 핵심 개념을 묻는 문제입니다.');

-- ✅ 11. 답변 선택지 데이터
INSERT IGNORE INTO answer_option (
    answer_option_id, question_id, option_text, is_correct,
    transcript_id, summary_text, summary_type, created_at
) VALUES
-- 질문 701번 선택지
(801, 701, '머신러닝', TRUE, 151, 'AI 기술 요약', 'BASIC', NOW()),
(802, 701, '블록체인', FALSE, 151, 'AI 기술 요약', 'BASIC', NOW()),
(803, 701, '클라우드 컴퓨팅', FALSE, 151, 'AI 기술 요약', 'BASIC', NOW()),
(804, 701, '사물인터넷', FALSE, 151, 'AI 기술 요약', 'BASIC', NOW()),

-- 질문 702번 선택지
(805, 702, '이미지 처리', FALSE, 151, 'AI 기술 요약', 'BASIC', NOW()),
(806, 702, '인간의 언어를 이해하고 처리', TRUE, 151, 'AI 기술 요약', 'BASIC', NOW()),
(807, 702, '음악 생성', FALSE, 151, 'AI 기술 요약', 'BASIC', NOW()),
(808, 702, '게임 개발', FALSE, 151, 'AI 기술 요약', 'BASIC', NOW()),

-- 질문 703번 선택지
(809, 703, '라벨링된 훈련 데이터', TRUE, 152, '머신러닝 기초', 'KEYWORD', NOW()),
(810, 703, '고성능 컴퓨터', FALSE, 152, '머신러닝 기초', 'KEYWORD', NOW()),
(811, 703, '빠른 인터넷', FALSE, 152, '머신러닝 기초', 'KEYWORD', NOW()),
(812, 703, '많은 메모리', FALSE, 152, '머신러닝 기초', 'KEYWORD', NOW());

-- ✅ 12. 영상 추천 데이터
INSERT IGNORE INTO video_recommendation (recommendation_id, user_id, source_video_id, recommended_video_id, recommendation_reason, is_clicked) VALUES
(901, 1, 101, 102, 'AI 기술 영상을 본 사용자에게 머신러닝 기초 강의 추천', FALSE),
(902, 1, 102, 101, '머신러닝에 관심 있는 사용자에게 AI 전반 기술 소개 추천', FALSE);

-- ✅ 13. 사용자 활동 로그 데이터
INSERT IGNORE INTO user_activity_log (user_id, activity_type, target_entity_type, target_entity_id_int, activity_detail, details) VALUES
(1, 'SUMMARY_CREATED', 'SUMMARY', 201, '요약 생성 완료: AI 기술 설명', '{"videoTitle": "테스트 영상 - AI 기술 설명", "summaryType": "BASIC", "duration": 720}'),
(1, 'SUMMARY_CREATED', 'SUMMARY', 202, '요약 생성 완료: 머신러닝 기초', '{"videoTitle": "머신러닝 기초 강의", "summaryType": "KEYWORD", "duration": 540}'),
(1, 'QUIZ_COMPLETED', 'QUIZ', 601, '퀴즈 완료: AI 기술 이해도 체크', '{"score": 85, "totalQuestions": 2, "correctAnswers": 2}'),
(1, 'ARCHIVE_CREATED', 'SUMMARY_ARCHIVE', 301, '요약 저장소에 저장', '{"summaryId": 201, "tags": ["인공지능", "교육", "기술"]}');

-- ============================================================================
-- 데이터 확인 쿼리 (선택적 실행)
-- ============================================================================

-- SELECT 'Data insertion completed successfully' as status;
-- SELECT COUNT(*) as user_count FROM user;
-- SELECT COUNT(*) as video_count FROM video;  
-- SELECT COUNT(*) as summary_count FROM summary;
-- SELECT COUNT(*) as archive_count FROM summary_archive;