package com.kdt.yts.YouSumback.service;

import com.kdt.yts.YouSumback.model.dto.request.UserAnswerDTO;
import com.kdt.yts.YouSumback.model.dto.response.*;
import com.kdt.yts.YouSumback.model.entity.*;
import com.kdt.yts.YouSumback.repository.*;
import com.kdt.yts.YouSumback.model.dto.request.QuizRequestDTO;
import com.kdt.yts.YouSumback.model.dto.request.SummaryRequestDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException; // IOException import
import java.nio.charset.StandardCharsets; // StandardCharsets import
import java.nio.file.Files; // Files import
import java.nio.file.Path; // Path import
import java.nio.file.Paths; // Paths import

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
    private final QuizRepository quizRepository;
    private final UserActivityLogRepository userActivityLogRepository;

    // ChatClient.Builder를 주입받아 ChatClient 생성
    @Autowired
    public SummaryServiceImpl(
            ChatClient.Builder chatClientBuilder,
            AnswerOptionRepository answerOptionRepository,
            TagRepository tagRepository,
            UserLibraryRepository userLibraryRepository,
            UserLibraryTagRepository userLibraryTagRepository,
            UserRepository userRepository,
            AudioTranscriptRepository audioTranscriptRepository,
            SummaryRepository summaryRepository,
            UserActivityLogRepository userActivityLogRepository,
            QuizRepository quizRepository
    ) {
        this.chatClient = chatClientBuilder.build();
        this.answerOptionRepository = answerOptionRepository;
        this.tagRepository = tagRepository;
        this.userLibraryRepository = userLibraryRepository;
        this.userLibraryTagRepository = userLibraryTagRepository;
        this.userRepository = userRepository;
        this.audioTranscriptRepository = audioTranscriptRepository;
        this.summaryRepository = summaryRepository;
        this.userActivityLogRepository = userActivityLogRepository;
        this.quizRepository = quizRepository;
    }

    // 메인 요약 메서드
    @Override
    @Transactional // 트랜잭션 필요
    public SummaryResponseDTO summarize(SummaryRequestDTO request, Long userId) { // ✅ userId 파라미터 추가
        String originalUrl = request.getOriginalUrl(); // SummaryRequestDTO에서 originalUrl을 가져옴
        String userPrompt = request.getUserPrompt();
        SummaryType summaryType = request.getSummaryType();

        System.out.println(">>> SummaryServiceImpl.summarize 진입 - URL: " + originalUrl + ", User ID: " + userId);

        // 1. originalUrl을 사용하여 AudioTranscript 조회 또는 생성
        AudioTranscript transcript = audioTranscriptRepository.findByVideo_OriginalUrl(originalUrl)
                .orElseThrow(() -> {
                    System.err.println("❌ Transcript not found for URL: " + originalUrl + ". YouTube API 호출 로직 및 저장 필요.");
                    return new RuntimeException("YouTube video transcript not found or not processed for URL: " + originalUrl);
                });

        // 텍스트 파일 경로에서 실제 텍스트 내용을 불러오는 로직
        String text;
        if (summaryType == SummaryType.TIMELINE) {
            String videoId = transcript.getVideo().getYoutubeId();
            String lang = transcript.getVideo().getOriginalLanguageCode();
            Path vttPath = Paths.get("src", "main", "resources", "textfiles", videoId + "." + lang + ".vtt");

            try {
                if (Files.exists(vttPath)) {
                    text = Files.readString(vttPath, StandardCharsets.UTF_8);
                    System.out.println("✅ TIMELINE summary: Loaded VTT file from: " + vttPath);
                } else {
                    // Fallback to cleaned text if VTT not found.
                    Path cleanedPath = Paths.get(transcript.getTranscriptPath());
                    System.err.println("⚠️ VTT file not found for TIMELINE summary at " + vttPath + ". Falling back to cleaned text from " + cleanedPath);
                    text = Files.readString(cleanedPath, StandardCharsets.UTF_8);
                }
            } catch (IOException e) {
                System.err.println("❌ Error reading transcript file for TIMELINE summary: " + e.getMessage());
                throw new RuntimeException("Failed to read transcript file for TIMELINE summary.", e);
            }
        } else {
            // ✅ 파일 경로가 유효한지 확인하고, 유효하지 않으면 즉시 에러 발생
            if (transcript.getTranscriptPath() == null || transcript.getTranscriptPath().isEmpty()) {
                System.err.println("❌ AudioTranscript has no file path for URL: " + originalUrl);
                throw new RuntimeException("No transcript file path found for URL: " + originalUrl + ". Summary failed.");
            }

            try {
                Path filePath = Paths.get(transcript.getTranscriptPath()); // AudioTranscript에서 파일 경로를 가져옴
                text = Files.readString(filePath, StandardCharsets.UTF_8); // 파일에서 텍스트 읽기
                System.out.println("✅ Transcript text loaded from file path: " + filePath);
            } catch (IOException e) {
                System.err.println("❌ Error reading transcript file from path: " + transcript.getTranscriptPath() + " - " + e.getMessage());
                throw new RuntimeException("Failed to read transcript text from file.", e);
            }
        }
        Long transcriptId = transcript.getId();    // 찾은 transcript에서 ID 추출

        System.out.println("✅ Transcript found/processed. ID: " + transcriptId);

        // 2. GPT 요약용 프롬프트 생성 (기존 로직)
        String prompt = buildPrompt(userPrompt, summaryType);

        String finalSummary;
        if (summaryType == SummaryType.TIMELINE) {
            System.out.println("✅ TIMELINE summary: Bypassing chunking and calling AI with full VTT content.");
            finalSummary = callOpenAISummary(prompt + "\n\n" + text);
        } else {
            // 텍스트가 너무 길면 청크로 나누기 (기존 로직)
            List<String> chunks = splitTextIntoChunks(text, 1000);
            List<String> partialSummaries = new ArrayList<>();

            for (String chunk : chunks) {
                partialSummaries.add(callOpenAISummary(prompt + "\n\n" + chunk));
            }

            // 2. 전체 요약 생성 (기존 로직)
            String finalSummaryPrompt = "다음은 각 부분에 대한 요약입니다. 이 요약들을 하나로 합쳐서 자연스러운 최종 요약을 만들어주세요:\n\n" + String.join("\n---\n", partialSummaries);
            finalSummary = callOpenAISummary(finalSummaryPrompt);
        }
        System.out.println("✅ Final Summary Generated. Length: " + finalSummary.length());

        // 3. Summary 저장 (기존 로직, userId 및 transcript 사용)
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found for ID: " + userId));

        Summary summary = Summary.builder()
                .user(user)
                .audioTranscript(transcript) // 찾은 transcript 사용
                .summaryText(finalSummary)
                .summaryType(summaryType)
                .userPrompt(userPrompt)
                .createdAt(LocalDateTime.now())
                .languageCode(transcript.getVideo().getOriginalLanguageCode())
                .build();
        Summary saved = summaryRepository.save(summary);
        System.out.println("✅ Summary Saved. ID: " + saved.getId());

        // 4. 라이브러리 저장 (기존 로직)
        UserLibrary library = UserLibrary.builder()
                .user(user)
                .summary(saved)
                .lastViewedAt(LocalDateTime.now())
                .build();
        userLibraryRepository.save(library);
        System.out.println("✅ UserLibrary Saved. User ID: " + user.getId() + ", Summary ID: " + saved.getId());

        // 5. LLM 기반 해시태그 추출 및 저장 (기존 로직)
        List<String> hashtags = extractTagsWithLLM(finalSummary).stream().distinct().toList();
        System.out.println("✅ Hashtags Extracted: " + hashtags);

        for (String keyword : hashtags) {
            Tag tag = findOrCreateTag(keyword);

            boolean exists = userLibraryTagRepository
                    .findByUserLibraryAndTag(library, tag)
                    .isPresent();

            if (!exists) {
                UserLibraryTag userLibraryTag = UserLibraryTag.builder()
                        .id(new UserLibraryTagId(library.getId(), tag.getId()))
                        .userLibrary(library)
                        .tag(tag)
                        .build();
                userLibraryTagRepository.save(userLibraryTag);
            }
        }
        System.out.println("✅ Tags Processed.");

        // 활동 로그 저장 (기존 로직)
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
        System.out.println("✅ UserActivityLog Saved.");

        // 최종 응답 DTO 반환 (기존 로직, transcript 및 summary 사용)
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
                transcript.getVideo().getOriginalLanguageCode(),
                summary.getCreatedAt()
        );
    }

    // 지침 + 프롬프트 템플릿을 반환 (text는 포함 X)
    private String buildPrompt(String userPrompt, SummaryType summaryType) {
        String formatInstruction = switch (summaryType) {
            case BASIC -> """
                    **🔹 기본 요약 (BASIC)**
                    - 자연스러운 단락 형태의 요약
                    - 마크다운 문단 스타일 유지 (줄바꿈은 문단 단위)
                    """;
            case THREE_LINE -> """
                    **🔹 3줄 요약 (THREE_LINE)**
                    - 핵심 내용을 세 문장으로 나눠 줄바꿈하여 출력
                    - 각 문장은 줄바꿈(\\n)으로 구분
                    - 예:
                      첫 번째 핵심 내용 요약.\\n
                      두 번째 핵심 내용 요약.\\n
                      세 번째 핵심 내용 요약.
                    """;
            case KEYWORD -> """
                    **🔹 키워드 요약 (KEYWORD)**
                    - 상단에 관련 핵심 키워드 3~5개 나열 (쉼표로 구분)
                    - 그 아래 일반 요약 문단 출력
                    - 요약 문단 안에 등장하는 키워드는 굵은 글씨 처리
                    - 마크다운 예시:
                      **Keywords:** AI, 요약, 학습, 유튜브, 자동화

                      본 영상은 **AI** 기술을 활용해 **요약**을 자동으로 수행하며, 사용자의 **학습** 효율을 높이는 **자동화**된 구조를 설명합니다.
                    """;
            case TIMELINE -> """
                    **🔹 타임라인 요약 (TIMELINE)**
                    - 아래 WEBVTT 형식 스크립트의 내용을 **타임스탬프(예: 00:00:15.480 --> 00:00:17.440)를 기준으로** 시간의 흐름에 따라 요약해주세요.
                    - 각 타임스탬프의 내용은 해당 시간대의 핵심적인 내용을 한 문장으로 간결하게 요약해야 합니다.
                    - **WEBVTT 헤더나 NOTE, STYLE 같은 메타데이터는 무시하고, 타임스탬프와 대화 내용만 사용해주세요.**
                    - 최종 결과는 마크다운 목록 형식으로 정리해주세요.
                    - 형식 예시:
                      - **00:15** - 선루프 틸팅 기능 설명
                      - **01:05** - 김서림 방지 버튼 사용법
                      - **01:50** - 유막 제거의 중요성 언급
                    """;
        };

        return String.format("""
        아래 텍스트를 기반으로 요약을 생성해주세요.
        요약은 요약 유형(SummaryType) 에 따라 각각 다른 포맷으로 마크다운 형식으로 출력해 주세요.
        요약 텍스트는 학습 보조 목적이며, 사용자가 읽기 편하고 시각적으로 명확하게 전달될 수 있도록 구성해주세요.

        사용자 맞춤 요청: "%s"

        %s

        ⚠️ 출력은 반드시 마크다운 스타일을 지켜주세요.
        ⚠️ 불필요한 문장이나 형식은 생략하고, 위의 포맷만 충실히 반영해주세요.

        다음은 유튜브 영상의 전체 스크립트입니다:
        ----
        """, userPrompt, formatInstruction);
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
다음 내용을 대표하는 핵심 해시태그 3개를 추출해줘.
**반드시 아래 기본 태그 목록 안에서만 골라야 해.**
응답 형식은 해시태그 이름만 쉼표로 구분해서 줘. 예시: 투자, 인공지능, 윤리

[기본 태그 목록]
%s

[요약 내용]
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

    // 퀴즈 생성 메서드
    @Transactional
    public List<QuizResponseDTO> generateFromSummary(QuizRequestDTO request) {
        // 1) Summary 엔티티 조회
        Summary summary = summaryRepository.findById(request.getSummaryId())
                .orElseThrow(() -> new RuntimeException("Summary not found"));

        // 2) 퀴즈용 프롬프트 생성
        String prompt = String.format("""
아래 요약문을 바탕으로 객관식 퀴즈를 %d개 만들어줘.
반드시 아래 형식만 지켜서 출력해줘. 불필요한 설명은 쓰지 마.

Q: 인공지능이 최근 발전한 분야는 무엇인가요?
1. 자연어 처리
2. 농업 기술
3. 고전 문학
4. 스포츠 분석
정답: 1

Q: 인공지능의 발전으로 등장한 서비스가 아닌 것은?
1. 챗봇
2. 기계 번역
3. 음성 인식
4. 손글씨 연습장
정답: 4

[요약문 시작]
%s
[요약문 끝]
""", request.getNumberOfQuestions(), summary.getSummaryText());

        // 3) 실제 AI 호출
        System.out.println(">>>> Sending Quiz Prompt to AI:\n" + prompt);
        String aiResponseQuiz = chatClient.prompt()
                .user(prompt)
                .call()
                .content();
        System.out.println(">>>> AI Quiz Response:\n" + aiResponseQuiz);
        // 4) "Q:"가 시작되는 부분을 기준으로 분리 (Q:를 블록에 그대로 남김)
        String[] rawBlocks = aiResponseQuiz.split("(?m)(?=Q:)");
        List<String> quizBlocks = new ArrayList<>();
        for (String b : rawBlocks) {
            String trimmed = b.strip();
            if (!trimmed.isEmpty()) {
                quizBlocks.add(trimmed);
            }
        }

        // 5) Quiz 엔티티 초기화
        Quiz quiz = Quiz.builder()
                .summary(summary)
                .title("AI 자동 생성 퀴즈")
                .createdAt(LocalDateTime.now())
                .build();

        // 6) 블록별로 Question + AnswerOption 생성
        for (String block : quizBlocks) {
            try {
                // "Q:"부터 시작하므로, 첫 줄에서 질문을 꺼낸다.
                String[] lines = block.split("\\r?\\n");
                if (lines.length < 2) {
                    System.out.println("⚠️ 블록 라인 부족: " + block);
                    continue;
                }
                // 6-1) 질문 추출: 첫 번째 줄에서 "Q:" 이후 부분
                String firstLine = lines[0].trim();
                String questionText;
                if (firstLine.startsWith("Q:")) {
                    questionText = firstLine.substring(2).trim();
                } else {
                    System.out.println("⚠️ 질문 포맷 불일치: " + firstLine);
                    continue;
                }

                // 6-2) 보기와 정답 추출 준비
                List<AnswerOption> options = new ArrayList<>();
                int answerNum = -1;

                // 6-3) 두 번째 줄부터 마지막 줄까지 순회
                for (int i = 1; i < lines.length; i++) {
                    String line = lines[i].trim();
                    if (line.isEmpty()) {
                        continue;
                    }

                    // 보기가 "숫자. 텍스트" 형태인지 확인
                    if (line.matches("^[0-9]+\\.\\s+.*")) {
                        // "1. 자연어 처리" → "자연어 처리"
                        String optText = line.replaceFirst("^[0-9]+\\.\\s*", "");
                        AnswerOption opt = AnswerOption.builder()
                                .optionText(optText)
                                .isCorrect(false)
                                .createdAt(LocalDateTime.now())
                                .transcriptId(summary.getAudioTranscript().getId())
                                .summaryText(summary.getSummaryText())
                                .summaryType(summary.getSummaryType())
                                .build();
                        options.add(opt);
                    }
                    // 정답이 "정답: 숫자" 형태인지 확인
                    else if (line.startsWith("정답")) {
                        String digits = line.replaceAll("[^0-9]", "");
                        if (!digits.isEmpty()) {
                            answerNum = Integer.parseInt(digits);
                        }
                    }
                }

                // 6-4) 유효성 검사: 질문, 보기 4개, 정답(1~4)
                if (questionText.isBlank()) {
                    System.out.println("⚠️ 질문이 비어 있음: " + block);
                    continue;
                }
                if (options.size() != 4) {
                    System.out.println("⚠️ 보기 개수 불일치(4개 아님): size=" + options.size() + " → " + block);
                    continue;
                }
                if (answerNum < 1 || answerNum > 4) {
                    System.out.println("⚠️ 정답 번호 범위 외: " + answerNum + " → " + block);
                    continue;
                }

                // 6-5) 정답 표시
                options.get(answerNum - 1).setIsCorrect(true);

                // 6-6) Question 엔티티 생성 및 연관 관계 설정
                Question question = Question.builder()
                        .questionText(questionText)
                        .languageCode("ko")
                        .build();
                question.setQuiz(quiz);

                for (AnswerOption opt : options) {
                    opt.setQuestion(question);
                }
                question.setOptions(options);

                // 6-7) Quiz.questions 목록에 추가
                quiz.getQuestions().add(question);

            } catch (Exception ex) {
                System.out.println("❌ 파싱 예외 발생 블록:\n" + block);
                ex.printStackTrace();
            }
        }

        // 7) 저장 (cascade = ALL 덕분에 Question/AnswerOption 전체가 함께 INSERT)
        try {
            Quiz savedQuiz = quizRepository.save(quiz);
            System.out.println("✅ Saved Quiz id = " + savedQuiz.getId());
            return List.of(convertToDTO(savedQuiz));
        } catch (Exception saveEx) {
            System.out.println("❌ Quiz 저장 중 예외:");
            saveEx.printStackTrace();
            throw saveEx;
        }
    }

    private QuizResponseDTO convertToDTO(Quiz quiz) {
        return new QuizResponseDTO(
                quiz.getId(), // 이제 맞음!
                quiz.getTitle(),
                quiz.getCreatedAt(),
                quiz.getQuestions().stream().map(q ->
                        new QuestionDTO(
                                q.getId(), // questionId
                                q.getQuestionText(),
                                q.getOptions().stream().map(o ->
                                        new OptionDTO(
                                                o.getId(), // answerOptionId
                                                o.getOptionText()
                                        )
                                ).toList()
                        )
                ).toList()
        );
    }

    @Transactional
    @Override
    public QuizResultResponseDTO checkQuizAnswers(Long quizId, List<UserAnswerDTO> answers) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("퀴즈 없음"));

        int score = 0;
        List<Boolean> results = new ArrayList<>();

        for (UserAnswerDTO ua : answers) {
            AnswerOption selectedOption = answerOptionRepository.findById(ua.getAnswerOptionId())
                    .orElseThrow(() -> new RuntimeException("선택한 보기 없음"));

            boolean correct = Boolean.TRUE.equals(selectedOption.getIsCorrect());
            results.add(correct);
            if (correct) score++;
        }

        return new QuizResultResponseDTO(score, results);
    }

    @Transactional
    @Override
    public List<QuestionWithOptionsResponseDTO> getQuestionsFromUserAnswers(List<UserAnswerDTO> answers) {
        List<QuestionWithOptionsResponseDTO> response = new ArrayList<>();

        for (UserAnswerDTO ua : answers) {
            AnswerOption selectedOption = answerOptionRepository.findById(ua.getAnswerOptionId())
                    .orElseThrow(() -> new RuntimeException("선택한 보기 없음"));

            Question q = selectedOption.getQuestion();

            List<OptionDTO> optionDtos = q.getOptions().stream()
                    .map(opt -> new OptionDTO(opt.getId(), opt.getOptionText()))
                    .toList();

            response.add(new QuestionWithOptionsResponseDTO(q.getId(), optionDtos));
        }

        return response;
    }

    @Override
    public String callOpenAISummary(String fullPrompt) {
        // 기존 요약용 AI 호출
        return chatClient.prompt()
                .user(fullPrompt)
                .call()
                .content();
    }

    private List<String> splitTextIntoChunks(String text, int chunkSize) {
        List<String> chunks = new ArrayList<>();
        int length = text.length();
        for (int start = 0; start < length; start += chunkSize) {
            int end = Math.min(length, start + chunkSize);
            chunks.add(text.substring(start, end));
        }
        return chunks;
    }

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

    private boolean isStopword(String word) {
        return List.of("그리고", "하지만", "또한", "이", "그", "저", "있는", "한다", "였다", "하는", "되어", "으로")
                .contains(word);
    }

    private synchronized Tag findOrCreateTag(String tagName) {
        return tagRepository.findByTagName(tagName)
                .orElseGet(() -> tagRepository.save(Tag.builder().tagName(tagName).build()));
    }
}