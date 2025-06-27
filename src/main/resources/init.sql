-- =============================================================================
-- YouSum 프로젝트 데이터베이스 스키마 초기화 스크립트
-- =============================================================================
-- 프로젝트: YouSum - AI 기반 유튜브 영상 요약 및 학습 지원 플랫폼
-- 생성일: 2025년 6월 23일
-- 목적: 데이터베이스 테이블 스키마 생성 (Spring Boot 초기화용)
-- 실행순서: 1. init.sql (스키마 생성) → 2. data.sql (더미 데이터 삽입)
-- =============================================================================

-- ✅ 1. 사용자 테이블 (User)
-- JWT 인증, OAuth2 구글 로그인 지원
CREATE TABLE IF NOT EXISTS user (
    user_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_email (email),
    INDEX idx_user_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='사용자 정보 및 인증 관리';

-- ✅ 2. 비디오 테이블 (Video) 
-- 유튜브 영상 메타데이터 저장
CREATE TABLE IF NOT EXISTS video (
    video_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    youtube_id VARCHAR(20) NOT NULL UNIQUE,
    title VARCHAR(500) NOT NULL,
    original_url VARCHAR(255) NOT NULL,
    uploader_name VARCHAR(100),
    thumbnail_url VARCHAR(500),
    view_count BIGINT DEFAULT 0,
    published_at TIMESTAMP NULL,
    duration_seconds INT DEFAULT 0,
    original_language_code VARCHAR(10) DEFAULT 'ko',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_video_youtube_id (youtube_id),
    INDEX idx_video_published_at (published_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='유튜브 영상 메타데이터';

-- ✅ 3. 오디오 트랜스크립트 테이블 (AudioTranscript)
-- Whisper STT 결과 텍스트 파일 경로 저장
CREATE TABLE IF NOT EXISTS audio_transcript (
    transcript_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    video_id BIGINT NOT NULL,
    youtube_id VARCHAR(20) NOT NULL,
    transcript_path VARCHAR(500) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (video_id) REFERENCES video(video_id) ON DELETE CASCADE,
    INDEX idx_transcript_video_id (video_id),
    INDEX idx_transcript_youtube_id (youtube_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Whisper STT 결과 트랜스크립트';

-- ✅ 4. 요약 테이블 (Summary)
-- GPT API 생성 요약문 저장 (BASIC, THREE_LINE, KEYWORD, TIMELINE)
CREATE TABLE IF NOT EXISTS summary (
    summary_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    transcript_id BIGINT NOT NULL,
    summary_text LONGTEXT NOT NULL,
    user_prompt VARCHAR(1000),
    language_code VARCHAR(10) DEFAULT 'ko',
    summary_type ENUM('BASIC', 'THREE_LINE', 'KEYWORD', 'TIMELINE') NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES user(user_id) ON DELETE CASCADE,
    FOREIGN KEY (transcript_id) REFERENCES audio_transcript(transcript_id) ON DELETE CASCADE,
    INDEX idx_summary_user_id (user_id),
    INDEX idx_summary_transcript_id (transcript_id),
    INDEX idx_summary_type (summary_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 생성 요약문 저장';

-- ✅ 5. 요약 저장소 테이블 (SummaryArchive)
-- 사용자별 요약 저장소 관리
CREATE TABLE IF NOT EXISTS summary_archive (
    archive_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    summary_id BIGINT NOT NULL,
    user_notes TEXT,
    last_viewed_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES user(user_id) ON DELETE CASCADE,
    FOREIGN KEY (summary_id) REFERENCES summary(summary_id) ON DELETE CASCADE,
    INDEX idx_archive_user_id (user_id),
    INDEX idx_archive_summary_id (summary_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='사용자별 요약 저장소';

-- ✅ 6. 태그 테이블 (Tag)
-- 해시태그 분류 시스템
CREATE TABLE IF NOT EXISTS tag (
    tag_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tag_name VARCHAR(50) NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_tag_name (tag_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='해시태그 분류';

-- ✅ 7. 요약 저장소-태그 연결 테이블 (SummaryArchiveTag)
-- 다대다 관계 매핑 (복합 기본키)
CREATE TABLE IF NOT EXISTS summary_archive_tag (
    archive_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    PRIMARY KEY (archive_id, tag_id),
    FOREIGN KEY (archive_id) REFERENCES summary_archive(archive_id) ON DELETE CASCADE,
    FOREIGN KEY (tag_id) REFERENCES tag(tag_id) ON DELETE CASCADE,
    INDEX idx_archive_tag_archive_id (archive_id),
    INDEX idx_archive_tag_tag_id (tag_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='요약-태그 연결 테이블';

-- ✅ 8. 리마인더 테이블 (Reminder)
-- 학습 리마인드 알림 관리 (DAILY, WEEKLY, CUSTOM)
CREATE TABLE IF NOT EXISTS reminder (
    reminder_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    summary_archive_id BIGINT NOT NULL,
    reminder_type ENUM('DAILY', 'WEEKLY', 'CUSTOM') NOT NULL,
    frequency_interval INT NOT NULL DEFAULT 1,
    base_datetime_for_recurrence TIMESTAMP NOT NULL,
    next_notification_datetime TIMESTAMP NOT NULL,
    reminder_note VARCHAR(255),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES user(user_id) ON DELETE CASCADE,
    FOREIGN KEY (summary_archive_id) REFERENCES summary_archive(archive_id) ON DELETE CASCADE,
    INDEX idx_reminder_user_id (user_id),
    INDEX idx_reminder_next_notification (next_notification_datetime),
    INDEX idx_reminder_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='학습 리마인더 관리';

-- ✅ 9. 퀴즈 테이블 (Quiz)
-- AI 생성 퀴즈 메타데이터
CREATE TABLE IF NOT EXISTS quiz (
    quiz_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    summary_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (summary_id) REFERENCES summary(summary_id) ON DELETE CASCADE,
    INDEX idx_quiz_summary_id (summary_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 생성 퀴즈';

-- ✅ 10. 질문 테이블 (Question)
-- 퀴즈별 개별 질문 저장
CREATE TABLE IF NOT EXISTS question (
    question_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    quiz_id BIGINT NOT NULL,
    question_text TEXT NOT NULL,
    language_code VARCHAR(10) DEFAULT 'ko',
    explanation TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (quiz_id) REFERENCES quiz(quiz_id) ON DELETE CASCADE,
    INDEX idx_question_quiz_id (quiz_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='퀴즈 질문';

-- ✅ 11. 답변 선택지 테이블 (AnswerOption) 
-- ⚠️  이 테이블이 누락되어 "missing table [answer_option]" 오류 발생 원인이었음
CREATE TABLE IF NOT EXISTS answer_option (
    answer_option_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    question_id BIGINT NOT NULL,
    option_text VARCHAR(500) NOT NULL,
    is_correct BOOLEAN NOT NULL DEFAULT FALSE,
    transcript_id BIGINT,
    summary_text TEXT,
    summary_type ENUM('BASIC', 'THREE_LINE', 'KEYWORD', 'TIMELINE'),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (question_id) REFERENCES question(question_id) ON DELETE CASCADE,
    FOREIGN KEY (transcript_id) REFERENCES audio_transcript(transcript_id) ON DELETE SET NULL,
    INDEX idx_answer_option_question_id (question_id),
    INDEX idx_answer_option_is_correct (is_correct)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='퀴즈 답변 선택지';

-- ✅ 12. 영상 추천 테이블 (VideoRecommendation)
-- AI 기반 맞춤형 영상 추천
CREATE TABLE IF NOT EXISTS video_recommendation (
    recommendation_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    source_video_id BIGINT NOT NULL,
    recommended_video_id BIGINT NOT NULL,
    recommendation_reason VARCHAR(500),
    is_clicked BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES user(user_id) ON DELETE CASCADE,
    FOREIGN KEY (source_video_id) REFERENCES video(video_id) ON DELETE CASCADE,
    FOREIGN KEY (recommended_video_id) REFERENCES video(video_id) ON DELETE CASCADE,
    INDEX idx_recommendation_user_id (user_id),
    INDEX idx_recommendation_source_video (source_video_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 기반 영상 추천';

-- ✅ 13. 사용자 활동 로그 테이블 (UserActivityLog)
-- 사용자 행동 패턴 분석 및 통계
CREATE TABLE IF NOT EXISTS user_activity_log (
    log_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    activity_type VARCHAR(50) NOT NULL,
    target_entity_type VARCHAR(50),
    target_entity_id_int BIGINT,
    activity_detail VARCHAR(500),
    details JSON,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES user(user_id) ON DELETE CASCADE,
    INDEX idx_activity_log_user_id (user_id),
    INDEX idx_activity_log_type (activity_type),
    INDEX idx_activity_log_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='사용자 활동 로그';

-- =============================================================================
-- 스키마 생성 완료
-- =============================================================================
-- 다음 단계: data.sql에서 더미 데이터 삽입
-- 참고: answer_option 테이블 누락으로 인한 Hibernate 오류 해결됨
-- ============================================================================= 