-- src/test/resources/data.sql
-- Test data for H2

-- 사용자
INSERT INTO USER (USER_ID, USER_NAME, EMAIL, PASSWORD, ROLE) VALUES
(1, 'testuser', 'test@example.com', '$2a$10$f.wS3yV.n6c5G5w5q8r.W.VbV.VbV.VbV.VbV.VbV.VbV.Vb', 'USER'),
(10, 'testuser10', 'test10@example.com', '$2a$10$f.wS3yV.n6c5G5w5q8r.W.VbV.VbV.VbV.VbV.VbV.VbV.Vb', 'USER');

-- 비디오
INSERT INTO VIDEO (VIDEO_ID, YOUTUBE_ID, TITLE, UPLOADER_NAME, ORIGINAL_URL, PUBLISHED_AT, ORIGINAL_LANGUAGE_CODE, DURATION_SECONDS) VALUES
(1, 'testVideoId', 'Test Video Title', 'Test Channel', 'http://youtu.be/testVideoId', CURRENT_TIMESTAMP, 'en', 120),
(10, 'testVideoId10', 'Test Video Title 10', 'Test Channel 10', 'http://youtu.be/testVideoId10', CURRENT_TIMESTAMP, 'en', 120);

-- 요약
INSERT INTO SUMMARY (SUMMARY_ID, VIDEO_ID, CONTENT, TYPE, USER_ID) VALUES
(1, 1, 'This is a test summary content.', 'BASIC', 1),
(10, 10, 'This is a test summary for search.', 'BASIC', 10),
(11, 10, 'This is a summary for quiz.', 'BASIC', 10);

-- 태그
INSERT INTO TAG (TAG_ID, TAG_NAME) VALUES
(10, 'AI-Test'),
(11, 'Tech-Test'),
(12, 'Java-Test');

-- 요약 저장소
INSERT INTO SUMMARY_ARCHIVE (ARCHIVE_ID, USER_ID, SUMMARY_ID, USER_NOTES, TITLE, CREATED_AT, UPDATED_AT, LAST_VIEWED_AT) VALUES
(10, 10, 10, 'My test note.', 'AI Summary Test', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 요약 저장소-태그 매핑
INSERT INTO SUMMARY_ARCHIVE_TAG (SUMMARY_ARCHIVE_ID, TAG_ID) VALUES
(10, 10),
(10, 11);

-- 퀴즈
INSERT INTO QUIZ (QUIZ_ID, SUMMARY_ID, CREATED_AT) VALUES
(10, 11, CURRENT_TIMESTAMP);

-- 질문
INSERT INTO QUESTION (QUESTION_ID, QUIZ_ID, TEXT) VALUES
(10, 10, 'Is this a test for quiz submission?'),
(11, 10, 'What is the answer to everything?');

-- 답변 옵션
INSERT INTO ANSWER_OPTION (OPTION_ID, QUESTION_ID, TEXT, IS_CORRECT) VALUES
(10, 10, 'Yes, it is.', TRUE),
(11, 10, 'No, it is not.', FALSE),
(12, 11, '42', TRUE),
(13, 11, 'Pi', FALSE);

-- 영상 추천
INSERT INTO VIDEO_RECOMMENDATION (RECOMMENDATION_ID, USER_ID, VIDEO_ID, REASON, CREATED_AT, IS_DELETED) VALUES
(10, 10, 10, 'Because you are testing', CURRENT_TIMESTAMP, FALSE); 