package com.kdt.yts.YouSumback.service;

import com.kdt.yts.YouSumback.model.dto.request.UserAnswer;
import com.kdt.yts.YouSumback.model.dto.response.OptionDto;
import com.kdt.yts.YouSumback.model.dto.response.QuestionWithOptionsResponse;
import com.kdt.yts.YouSumback.model.dto.response.QuizResultResponse;
import com.kdt.yts.YouSumback.model.entity.AnswerOption;
import com.kdt.yts.YouSumback.model.entity.Question;
import com.kdt.yts.YouSumback.model.entity.Quiz;
import com.kdt.yts.YouSumback.model.entity.Summary;
import com.kdt.yts.YouSumback.model.entity.Tag;
import com.kdt.yts.YouSumback.model.entity.UserLibrary;
import com.kdt.yts.YouSumback.model.entity.UserLibraryTag;
import com.kdt.yts.YouSumback.model.entity.UserLibraryTagId;
import com.kdt.yts.YouSumback.repository.AnswerOptionRepository;
import com.kdt.yts.YouSumback.repository.AudioTranscriptRepository;
import com.kdt.yts.YouSumback.repository.QuizRepository;
import com.kdt.yts.YouSumback.repository.SummaryRepository;
import com.kdt.yts.YouSumback.repository.TagRepository;
import com.kdt.yts.YouSumback.repository.UserLibraryRepository;
import com.kdt.yts.YouSumback.repository.UserLibraryTagRepository;
import com.kdt.yts.YouSumback.repository.UserRepository;
import com.kdt.yts.YouSumback.model.dto.request.QuizRequestDTO;
import com.kdt.yts.YouSumback.model.dto.request.SummaryRequestDTO;
import com.kdt.yts.YouSumback.model.dto.response.SummaryResponseDTO;
import com.kdt.yts.YouSumback.model.entity.*;
import com.kdt.yts.YouSumback.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    // 지침 + 프롬프트 템플릿을 반환 (text는 포함 X)
    private String buildPrompt(String userPrompt, SummaryType summaryType) {
        String formatInstruction = switch (summaryType) {
            case BASIC -> "전체 내용을 한눈에 이해할 수 있게 요약해줘. 길이는 4~5문장 이내로 해줘.";
            case THREE_LINE -> "가장 중요한 내용을 3줄로 요약해줘. 각 줄은 한 문장으로 해줘.";
            case KEYWORD -> "핵심 키워드를 3~5개 뽑아줘. 각 키워드는 간단한 설명과 함께 적어줘.";
            case TIMELINE -> "시간 순서대로 사건이나 내용 흐름을 정리해줘. 각 항목은 간결한 문장으로 정리해줘.";
        };

        return String.format("""
        당신은 유튜브 영상 자막을 분석하여 사용자의 학습 목적에 맞는 요약을 생성하는 전문가입니다.

        아래는 사용자의 학습 목적입니다:
        \"%s\"

        요약은 다음 지침을 따라주세요:
        - 문장은 짧고 명확하게 작성할 것
        - 불필요한 반복은 제외할 것
        - 중요한 개념이나 주장 위주로 요약할 것
        - %s

        다음은 유튜브 영상의 전체 스크립트입니다:
        ----
        {TEXT}
        ----
        """, userPrompt.trim(), formatInstruction);
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

    // 퀴즈 생성 메서드
    @Transactional
    public List<Quiz> generateFromSummary(QuizRequestDTO request) {
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
        // 4) “Q:”가 시작되는 부분을 기준으로 분리 (Q:를 블록에 그대로 남김)
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
                // “Q:”부터 시작하므로, 첫 줄에서 질문을 꺼낸다.
                String[] lines = block.split("\\r?\\n");
                if (lines.length < 2) {
                    System.out.println("⚠️ 블록 라인 부족: " + block);
                    continue;
                }
                // 6-1) 질문 추출: 첫 번째 줄에서 “Q:” 이후 부분
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

                    // 보기가 “숫자. 텍스트” 형태인지 확인
                    if (line.matches("^[0-9]+\\.\\s+.*")) {
                        // “1. 자연어 처리” → “자연어 처리”
                        String optText = line.replaceFirst("^[0-9]+\\.\\s*", "");
                        AnswerOption opt = AnswerOption.builder()
                                .optionText(optText)
                                .isCorrect(false)
                                .build();
                        options.add(opt);
                    }
                    // 정답이 “정답: 숫자” 형태인지 확인
                    else if (line.startsWith("정답")) {
                        String digits = line.replaceAll("[^0-9]", "");
                        if (!digits.isEmpty()) {
                            answerNum = Integer.parseInt(digits);
                        }
                    }
                    // 그 외 라인은 무시
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
            return List.of(savedQuiz);
        } catch (Exception saveEx) {
            System.out.println("❌ Quiz 저장 중 예외:");
            saveEx.printStackTrace();
            throw saveEx;
        }
    }

    @Override
    public QuizResultResponse checkQuizAnswers(int quizId, List<UserAnswer> answers) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("퀴즈 없음"));

        int score = 0;
        List<Boolean> results = new ArrayList<>();

        for (UserAnswer ua : answers) {
            AnswerOption selectedOption = answerOptionRepository.findById(ua.getAnswerOptionId())
                    .orElseThrow(() -> new RuntimeException("선택한 보기 없음"));

            boolean correct = Boolean.TRUE.equals(selectedOption.getIsCorrect());
            results.add(correct);
            if (correct) score++;
        }

        return new QuizResultResponse(score, results);
    }

    @Override
    public List<QuestionWithOptionsResponse> getQuestionsFromUserAnswers(List<UserAnswer> answers) {
        List<QuestionWithOptionsResponse> response = new ArrayList<>();

        for (UserAnswer ua : answers) {
            AnswerOption selectedOption = answerOptionRepository.findById(ua.getAnswerOptionId())
                    .orElseThrow(() -> new RuntimeException("선택한 보기 없음"));

            Question q = selectedOption.getQuestion();

            List<OptionDto> optionDtos = q.getOptions().stream()
                    .map(opt -> new OptionDto(opt.getId(), opt.getOptionText()))
                    .toList();

            response.add(new QuestionWithOptionsResponse(q.getId(), q.getQuestionText(), optionDtos));
        }

        return response;
    }

    @Override
    public String callOpenAISummary(String text) {
        // 기존 요약용 AI 호출
        return chatClient.prompt()
                .user("다음 내용을 바탕으로 요약해줘:\n" + text)
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
}
