package com.kdt.yts.YouSumback.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kdt.yts.YouSumback.model.dto.request.QuizRequestDTO;
import com.kdt.yts.YouSumback.model.dto.request.SummaryRequestDTO;
import com.kdt.yts.YouSumback.model.dto.response.SummaryResponseDTO;
import com.kdt.yts.YouSumback.model.entity.*;
import com.kdt.yts.YouSumback.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SummaryServiceImpl implements SummaryService {

    private final ChatClient chatClient;
    private final AnswerOptionRepository answerOptionRepository;
    private final TagRepository tagRepository;
    private final UserLibraryRepository userLibraryRepository;
    private final UserLibraryTagRepository userLibraryTagRepository;
    private final UserRepository userRepository;
    private final AudioTranscriptRepository audioTranscriptRepository;
    private final SummaryRepository summaryRepository;
    private final UserActivityLogRepository userActivityLogRepository;
    private final ObjectMapper objectMapper;

    // ChatClient.Builder를 주입받아 ChatClient 생성
    @Autowired
    public SummaryServiceImpl(ChatClient.Builder chatClientBuilder,
                              AnswerOptionRepository answerOptionRepository,
                              TagRepository tagRepository,
                              UserLibraryRepository userLibraryRepository,
                              UserLibraryTagRepository userLibraryTagRepository,
                              UserRepository userRepository,
                              AudioTranscriptRepository audioTranscriptRepository,
                              SummaryRepository summaryRepository,
                              UserActivityLogRepository userActivityLogRepository,
                              ObjectMapper objectMapper) {
        this.chatClient = chatClientBuilder.build();
        this.answerOptionRepository = answerOptionRepository;
        this.tagRepository = tagRepository;
        this.userLibraryRepository = userLibraryRepository;
        this.userLibraryTagRepository = userLibraryTagRepository;
        this.userRepository = userRepository;
        this.audioTranscriptRepository = audioTranscriptRepository;
        this.summaryRepository = summaryRepository;
        this.userActivityLogRepository = userActivityLogRepository;
        this.objectMapper = new ObjectMapper();
    }

    // 메인 요약 메서드
    @Override
    public SummaryResponseDTO summarize(SummaryRequestDTO request) {
        String text = request.getText();
        Long transcriptId = request.getTranscriptId();
        Long userId = request.getUserId();
        String userPrompt= request.getUserPrompt();
        SummaryType summaryType = request.getSummaryType();

        // 1. GPT 요약용 프롬프트 생성
        String prompt = buildPrompt(userPrompt, summaryType);
        String fullPrompt = prompt + "\n\n" + text;

        // 텍스트가 너무 길면 청크로 나누기
        List<String> chunks = splitTextIntoChunks(text, 1000);
        List<String> partialSummaries = new ArrayList<>();

        for (String chunk : chunks) {
            partialSummaries.add(callOpenAISummary(prompt + "\n\n" + chunk));
        }

        // 2. 전체 요약 생성
        String finalSummary = callOpenAISummary(prompt + "\n\n" + String.join("\n", partialSummaries));

        // 3. Summary 저장
        User user = userRepository.findById(userId).orElseThrow();
        AudioTranscript transcript = audioTranscriptRepository.findById(transcriptId)
                .orElseThrow(() -> new RuntimeException("Transcript not found for ID = " + transcriptId));

        Summary summary = Summary.builder()
                .user(user)
                .audioTranscript(transcript)
                .summaryText(finalSummary)
                .summaryType(summaryType)
                .userPrompt(prompt)
                .createdAt(LocalDateTime.now())
                .languageCode("ko") // 예시로 한국어로 설정
                .build();
        Summary saved = summaryRepository.save(summary);

        // 4. 라이브러리 저장
        UserLibrary library = UserLibrary.builder()
                .user(user)
                .summary(saved)
                .lastViewedAt(LocalDateTime.now())
                .build();
        userLibraryRepository.save(library);

        // 5. LLM 기반 해시태그 추출 및 저장
        List<String> baseTags = tagRepository.findAll().stream().map(Tag::getTagName).toList();
        List<String> hashtags = extractTagsWithLLM(finalSummary);

        for (String keyword : hashtags) {
            Tag tag = tagRepository.findByTagName(keyword)
                    .orElseGet(() -> tagRepository.save(Tag.builder().tagName(keyword).build()));

            boolean exists = userLibraryTagRepository
                    .findByUserLibraryAndTag(library, tag)
                    .isPresent();

            if (!exists) {
                UserLibraryTag userLibraryTag = UserLibraryTag.builder()
                        .id(new UserLibraryTagId(user.getId(), tag.getId()))
                        .userLibrary(library)
                        .tag(tag)
                        .build();
                userLibraryTagRepository.save(userLibraryTag);
            }
        }
        // ✅ 활동 로그 저장
        UserActivityLog log = UserActivityLog.builder()
                .user(user)
                .activityType("SUMMARY_CREATED")
                .targetEntityType("SUMMARY")
                .targetEntityIdInt(saved.getId())
                .activityDetail("요약 생성 완료: " + summaryType)
                .details(String.format("""
    {
        "summaryType": "%s",
        "videoId": %d,
        "videoTitle": "%s"
    }
""", summaryType, transcript.getVideo().getId(), transcript.getVideo().getTitle()))
                .createdAt(LocalDateTime.now())
                .build();
        userActivityLogRepository.save(log);

        return new SummaryResponseDTO(
                saved.getId(),
                transcript.getId(),
                transcript.getVideo().getId(),
                finalSummary,
                hashtags,
                transcript.getVideo().getTitle(),
                transcript.getVideo().getThumbnailUrl(),
                transcript.getVideo().getUploaderName(),
                transcript.getVideo().getViewCount(),
                summary.getLanguageCode(),
                summary.getCreatedAt()
        );
    }

    // OpenAI API를 호출하여 요약 생성
    @Override
    public String callOpenAISummary(String text) {
        return chatClient.prompt()
                .user("다음 내용을 바탕으로 요약해줘:\n" + text)
                .call()
                .content();
    }

    // 요약 프롬프트를 생성하는 메서드 (유저의 목적과 요약 유형에 따라 다름)
    private String buildPrompt(String userPrompt, SummaryType summaryType) {
        String typeDesc = switch (summaryType) {
            case BASIC -> "간단하게 요약해줘";
            case THREE_LINE -> "3줄로 요약해줘";
            case KEYWORD -> "핵심 키워드 위주로 요약해줘";
            case TIMELINE -> "시간 순 흐름에 따라 요약해줘";
        };

        String userPromptDesc = switch (userPrompt.toUpperCase()) {
            case "REVIEW" -> "복습 목적에 맞춰";
            case "EXAM" -> "시험 대비를 위해";
            default -> "학습 목적에 맞게";
        };

        return String.format("%s %s", userPromptDesc, typeDesc);
    }

    // 텍스트를 청크로 나누는 메서드
    private List<String> splitTextIntoChunks(String text, int chunkSize) {
        List<String> chunks = new ArrayList<>();
        int length = text.length();
        for (int start = 0; start < length; start += chunkSize) {
            int end = Math.min(length, start + chunkSize);
            chunks.add(text.substring(start, end));
        }
        return chunks;
    }

    // LLM 기반 해시태그 추출
    private List<String> extractTagsWithLLM(String summaryText) {
        List<String> baseTags = List.of(
                "경제", "주식", "투자", "금융", "부동산",
                "인공지능", "머신러닝", "딥러닝", "프로그래밍", "코딩",
                "교육", "학습", "시험대비", "자기계발", "시간관리",
                "정치", "국제정세", "사회이슈", "환경", "기후변화",
                "윤리", "심리학", "철학", "문화", "역사",
                "IT기술", "데이터분석", "UX디자인", "창업", "마케팅"
        );
        String baseTagList = String.join(", ", baseTags);
        String prompt = String.format("""
                다음 내용을 보고 핵심 해시태그 3개를 추출해줘.
                아래 기본 태그 중 선택하되, 없으면 자유롭게 생성해도 돼.
                응답 형식은 해시태그 이름만 쉼표로 구분해서 줘. 예시: 투자, 인공지능, 윤리

                기본 태그: %s

                내용:
                %s
                """, baseTagList, summaryText);

        String response = chatClient.prompt().user(prompt).call().content();
        return Arrays.stream(response.split("[,\\n]"))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .limit(3)
                .toList();
    }

    // 특정 유저와 요약에 대한 UserLibrary를 찾는 메서드
    public Optional<UserLibrary> findUserLibraryByUserAndSummary(Long userId, Summary summary) {
        return userLibraryRepository.findByUser_IdAndSummary(userId, summary);
    }

//    public SummaryResponseDTO summarize(SummaryRequestDTO request) {
//        Long transcriptId = request.getTranscriptId();
//        Long userId = request.getUserId();
//        String text = request.getText();
//        String userPrompt
//      = request.getUserPrompt();
//        SummaryType summaryType = request.getSummaryType();
//
//        // 1. 요약 생성
//        List<String> chunks = splitTextIntoChunks(text, 1000);
//        List<String> partialSummaries = new ArrayList<>();
//        for (String chunk : chunks) {
//            partialSummaries.add(callOpenAISummary(chunk));
//        }
//        String finalSummary = callOpenAISummary(String.join("\n", partialSummaries));
//
//        // 2. Summary 저장
//        Summary summary = Summary.builder()
//                .user(userRepository.findById(userId).orElseThrow())
//                .audioTranscript(audioTranscriptRepository.findById(transcriptId).orElseThrow())
//                .summaryText(finalSummary)
//                .summaryType(SummaryType.valueOf("THREE_LINE")) // 예시로 THREE_LINE 사용
//                .createdAt(LocalDateTime.now())
//                .build();
//        Summary saved = summaryRepository.save(summary);
//
////        // 3. 라이브러리 찾기
////        UserLibrary library = userLibraryRepository
////                .findBySummaryUserIdAndSummaryAudioTranscriptId(userId, transcriptId)
////                .orElseThrow(() -> new RuntimeException("라이브러리 항목 없음"));
//
//        // 3. 라이브러리 새로 생성 및 저장
//        UserLibrary library = UserLibrary.builder()
//                .user(saved.getUser())
//                .summary(saved)
//                .lastViewedAt(LocalDateTime.now())
//                .build();
//        userLibraryRepository.save(library);
//
//        // 4. 해시태그 추출 및 저장
//        List<String> hashtags = extractHashtags(finalSummary, 3);
//        for (String keyword : hashtags) {
//            Tag tag = tagRepository.findByTagName(keyword)
//                    .orElseGet(() -> tagRepository.save(Tag.builder().tagName(keyword).build()));
//
//            boolean exists = userLibraryTagRepository
//                    .findByUserLibraryAndTag(library, tag)
//                    .isPresent();
//
//            if (!exists) {
//                UserLibraryTag userLibraryTag = UserLibraryTag.builder()
//                        .id(new UserLibraryTagId(library.getUser().getId(), tag.getId()))
//                        .userLibrary(library)
//                        .tag(tag)
//                        .build();
//                userLibraryTagRepository.save(userLibraryTag);
//            }
//        }
//
//        return new SummaryResponseDTO(saved.getId(), finalSummary);
//    }

    // 퀴즈 생성 메서드
    @Override
    public List<Quiz> generateFromSummary(QuizRequestDTO request) {
        // 1. Summary 가져오기
        Summary summary = summaryRepository.findById(request.getTranscriptId())
                .orElseThrow(() -> new RuntimeException("Summary not found"));

        // 2. 요약 텍스트로 퀴즈 생성 (LLM 호출)
        String prompt = "다음 내용을 바탕으로 객관식 퀴즈를 1개 만들어줘.\n"
                + "문제 형식은 다음과 같아:\n"
                + "Q: ...\n1. ...\n2. ...\n3. ...\n4. ...\n정답: ...\n\n"
                + summary.getSummaryText();

        String aiResponse = callOpenAISummary(prompt);
        String[] quizBlocks = aiResponse.split("(?=Q:)");
        List<Quiz> quizzes = new ArrayList<>();

        for (String block : quizBlocks) {
            try {
                String[] lines = block.strip().split("\n");
                if (lines.length < 6) continue;

                String questionText = lines[0].replace("Q:", "").trim();
                List<AnswerOption> options = new ArrayList<>();
                for (int i = 1; i <= 4; i++) {
                    options.add(AnswerOption.builder()
                            .optionText(lines[i].substring(2).trim())
                            .isCorrect(false)
                            .build());
                }

                int answerNum = Integer.parseInt(lines[5].replaceAll("[^0-9]", ""));
                options.get(answerNum - 1).setIsCorrect(true);


                // 해설 파싱
                String explanation = "";
                if (lines.length >= 7 && lines[6].startsWith("해설:")) {
                    explanation = lines[6].substring(3).trim();
                }

                Question question = Question.builder()
                        .questionText(questionText)
                        .options(options)
                        .explanation(explanation)
                        .languageCode("ko")
                        .build();

                Quiz quiz = Quiz.builder()
                        .summary(summary)
                        .questions(List.of(question))
                        .createdAt(LocalDateTime.now())
                        .title("AI 자동 생성 퀴즈")
                        .build();

                quizzes.add(quiz);

            } catch (Exception e) {
                System.out.println("⚠️ 퀴즈 파싱 실패: " + block);
                e.printStackTrace();
            }
        }

        return quizzes;
    }


    // LLM 기반 해시태그 추출 -> 빈도 기반으로 상위 N개 추출
    private List<String> extractHashtags(String text, int limit) {
        Map<String, Integer> freq = new HashMap<>();
        String[] words = text.toLowerCase().split("\\W+");

        for (String word : words) {
            if (word.length() >= 2 && !isStopword(word)) {
                freq.put(word, freq.getOrDefault(word, 0) + 1);
            }
        }
        return freq.entrySet().stream()
                .sorted((a, b) -> b.getValue() - a.getValue())
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    // LLM 기반 해시태그 추출 함수 보조
    private boolean isStopword(String word) {
        return List.of("그리고", "하지만", "또한", "이", "그", "저", "있는", "한다", "였다", "하는", "되어", "으로")
                .contains(word);
    }
    @Override
    public void generateSummary(String youtubeId, String userPrompt, SummaryType summaryType) {
        AudioTranscript transcript = audioTranscriptRepository.findByYoutubeId(youtubeId)
                .orElseThrow(() -> new RuntimeException("Transcript not found"));

        User user = userRepository.findById(1L)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String prompt = buildPrompt(userPrompt, summaryType);
        String input = prompt + "\n\n" + transcript.getTranscriptPath();
        String summaryText = callOpenAISummary(input);

        Summary summary = Summary.builder()
                .user(user)
                .audioTranscript(transcript)
                .summaryText(summaryText)
                .languageCode("ko")
                .summaryType((summaryType))
                .userPrompt(prompt)
                .createdAt(LocalDateTime.now())
                .build();
        Summary saved = summaryRepository.save(summary);

        UserLibrary library = UserLibrary.builder()
                .user(user)
                .summary(saved)
                .lastViewedAt(LocalDateTime.now())
                .build();
        userLibraryRepository.save(library);

        List<String> hashtags = extractHashtags(summaryText, 3);
        for (String keyword : hashtags) {
            Tag tag = tagRepository.findByTagName(keyword)
                    .orElseGet(() -> tagRepository.save(Tag.builder().tagName(keyword).build()));

            boolean exists = userLibraryTagRepository
                    .findByUserLibraryAndTag(library, tag)
                    .isPresent();

            if (!exists) {
                UserLibraryTag userLibraryTag = UserLibraryTag.builder()
                        .id(new UserLibraryTagId(user.getId(), tag.getId()))
                        .userLibrary(library)
                        .tag(tag)
                        .build();
                userLibraryTagRepository.save(userLibraryTag);
            }
        }
    }
}
