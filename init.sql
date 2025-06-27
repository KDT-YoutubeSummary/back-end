-- YouSum Database 초기화 스크립트
-- Docker MySQL 컨테이너 시작 시 자동 실행

-- UTF8MB4 설정
ALTER DATABASE yousum CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- 기본 사용자 생성 (이미 존재할 수 있으므로 IF NOT EXISTS 사용)
CREATE USER IF NOT EXISTS 'yousum'@'%' IDENTIFIED BY 'yousum123!';
GRANT ALL PRIVILEGES ON yousum.* TO 'yousum'@'%';
FLUSH PRIVILEGES;

-- 테이블 생성 (Spring Boot가 자동으로 생성하지만, 명시적으로 정의)
USE yousum;

-- User 테이블
CREATE TABLE IF NOT EXISTS user (
    id BIGINT NOT NULL AUTO_INCREMENT,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255),
    nickname VARCHAR(100) NOT NULL,
    profile_image_url VARCHAR(500),
    oauth_provider VARCHAR(50),
    oauth_provider_id VARCHAR(255),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_email (email),
    INDEX idx_oauth (oauth_provider, oauth_provider_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Video 테이블
CREATE TABLE IF NOT EXISTS video (
    id BIGINT NOT NULL AUTO_INCREMENT,
    youtube_id VARCHAR(20) NOT NULL UNIQUE,
    title VARCHAR(500) NOT NULL,
    description TEXT,
    channel_name VARCHAR(255),
    duration_seconds INT,
    upload_date DATETIME,
    thumbnail_url VARCHAR(500),
    view_count BIGINT DEFAULT 0,
    like_count BIGINT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_youtube_id (youtube_id),
    INDEX idx_channel (channel_name),
    INDEX idx_upload_date (upload_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Audio Transcript 테이블
CREATE TABLE IF NOT EXISTS audio_transcript (
    id BIGINT NOT NULL AUTO_INCREMENT,
    video_id BIGINT NOT NULL,
    original_text LONGTEXT,
    cleaned_text LONGTEXT,
    language VARCHAR(10) DEFAULT 'ko',
    processing_status VARCHAR(50) DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    FOREIGN KEY (video_id) REFERENCES video(id) ON DELETE CASCADE,
    INDEX idx_video_id (video_id),
    INDEX idx_status (processing_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Summary 테이블
CREATE TABLE IF NOT EXISTS summary (
    id BIGINT NOT NULL AUTO_INCREMENT,
    video_id BIGINT NOT NULL,
    summary_type ENUM('BASIC', 'THREE_LINE', 'KEYWORD', 'TIMELINE') NOT NULL,
    content LONGTEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    FOREIGN KEY (video_id) REFERENCES video(id) ON DELETE CASCADE,
    INDEX idx_video_type (video_id, summary_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 기본 더미 데이터 삽입 (선택사항)
INSERT IGNORE INTO user (email, nickname, password, is_active) 
VALUES ('admin@yousum.com', 'YouSum Admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9tYjnZr0q.8tULy', TRUE);

-- 테이블 정보 확인
SELECT 'Database initialization completed' as status; 