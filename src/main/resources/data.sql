-- ✅ YouSum 더미 데이터 삽입
-- init.sql에서 테이블 생성 후 실행되는 데이터 삽입 스크립트

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