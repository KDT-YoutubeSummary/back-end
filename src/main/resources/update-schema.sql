-- ✅ YouSum 데이터베이스 스키마 업데이트 스크립트
-- Summary 테이블의 컬럼 타입 변경으로 긴 텍스트 지원

-- 1. summary 테이블의 summary_text 컬럼을 LONGTEXT로 변경
ALTER TABLE `summary` 
MODIFY COLUMN `summary_text` LONGTEXT NOT NULL COMMENT '요약 텍스트 (긴 텍스트 지원)';

-- 2. summary 테이블의 user_prompt 컬럼을 LONGTEXT로 변경
ALTER TABLE `summary` 
MODIFY COLUMN `user_prompt` LONGTEXT COMMENT '사용자 프롬프트 (긴 텍스트 지원)';

-- 3. 기존 데이터가 있는지 확인하고 백업 (선택사항)
-- SELECT COUNT(*) as summary_count FROM summary;

-- 4. 업데이트 완료 확인
DESCRIBE `summary`;

-- 5. 관련 테이블들도 확인 (이미 LONGTEXT로 설정된 것들)
-- audio_transcript 테이블의 transcript_path는 이미 LONGTEXT
-- question 테이블의 question_text는 이미 TEXT
-- answer_option 테이블의 option_text는 이미 TEXT

SHOW WARNINGS; 