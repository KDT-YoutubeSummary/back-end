package com.kdt.yts.YouSumback.service;

import com.kdt.yts.YouSumback.model.dto.request.UserAnswer;
import com.kdt.yts.YouSumback.model.dto.request.QuizRequest;
import com.kdt.yts.YouSumback.model.dto.request.SummaryRequest;
import com.kdt.yts.YouSumback.model.dto.response.QuizResultResponse;
import com.kdt.yts.YouSumback.model.dto.response.SummaryResponse;
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
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

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
        this.quizRepository = quizRepository;
    }

    @Override
    public SummaryResponse summarize(SummaryRequest request) {
        String text = request.getText();
        Integer transcriptId = request.getTranscriptId();  // 이미 Integer
        Integer userId      = request.getUserId();

        // 1. 요약 생성
        List<String> chunks = splitTextIntoChunks(text, 1000);
        List<String> partialSummaries = new ArrayList<>();
        for (String chunk : chunks) {
            partialSummaries.add(callOpenAISummary(chunk));
        }
        String finalSummary = callOpenAISummary(String.join("\n", partialSummaries));

        boolean userExists         = userRepository.existsById(userId);
        boolean transcriptExists   = audioTranscriptRepository.existsById(transcriptId);
        System.out.println("▶ userExists("+userId+")?        = " + userExists);
        System.out.println("▶ transcriptExists("+transcriptId+")? = " + transcriptExists);

        // 2. Summary 저장
        Summary summary = Summary.builder()
                .user(userRepository.findById(userId).orElseThrow())
                .audioTranscript(audioTranscriptRepository.findById(transcriptId).orElseThrow())
                .summaryText(finalSummary)
                .languageCode(request.getLanguageCode())
                .summaryType("default")
                .createdAt(LocalDateTime.now())
                .build();

        Summary savedSummary = summaryRepository.save(summary);
        Integer summaryId = savedSummary.getSummaryId();

        // 3. UserLibrary 조회 또는 생성
        UserLibrary library = userLibraryRepository
                .findByUserUserIdAndSummarySummaryId(userId, summaryId)
                .orElseGet(() -> {
                    UserLibrary newLibrary = UserLibrary.builder()
                            .user(userRepository.findById(userId).orElseThrow()) // userId 역시 Integer로 찾아야 합니다
                            .summary(savedSummary)
                            .savedAt(LocalDateTime.now())
                            .build();
                    return userLibraryRepository.save(newLibrary);
                });
        System.out.println("▶ userExists(" + userId + ")? = " + userExists);
        System.out.println("▶ transcriptExists(" + transcriptId + ")? = " + transcriptExists);
        System.out.println("▶ Creating new UserLibrary for user=" + userId + ", summary=" + summaryId);


        // 4. 해시태그 추출 및 저장
        List<String> hashtags = extractHashtagsWithGPT(finalSummary, 3);
        for (String keyword : hashtags) {
            Tag tag = tagRepository.findByTagName(keyword)
                    .orElseGet(() -> tagRepository.save(Tag.builder().tagName(keyword).build()));

            boolean exists = userLibraryTagRepository
                    .findByUserLibraryAndTag(library, tag)
                    .isPresent();

            if (!exists) {
                UserLibraryTag userLibraryTag = UserLibraryTag.builder()
                        .id(new UserLibraryTagId(library.getUserLibraryId(), tag.getTagId()))
                        .userLibrary(library)
                        .tag(tag)
                        .build();
                userLibraryTagRepository.save(userLibraryTag);
            }
        }

        return new SummaryResponse(savedSummary.getSummaryId(), finalSummary);
    }

    @Override
    @Transactional
    public List<Quiz> generateFromSummary(QuizRequest request) {
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
            System.out.println("✅ Saved Quiz id = " + savedQuiz.getQuizId());
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
    public String callOpenAISummary(String text) {
        // 기존 요약용 AI 호출
        return chatClient.prompt()
                .user("다음 내용을 바탕으로 요약해줘:\n" + text)
                .call()
                .content();
    }

    /**
     * 긴 텍스트를 지정된 크기(chunkSize)로 분할하는 헬퍼 메서드
     */
    private List<String> splitTextIntoChunks(String text, int chunkSize) {
        List<String> chunks = new ArrayList<>();
        int length = text.length();
        for (int start = 0; start < length; start += chunkSize) {
            int end = Math.min(length, start + chunkSize);
            chunks.add(text.substring(start, end));
        }
        return chunks;
    }

    /**
     * 텍스트에서 빈도 높은 단어를 추출해 해시태그로 사용할 키워드 리스트를 반환하는 헬퍼 메서드
     */
    public List<String> extractHashtagsWithGPT(String text, int limit) {
        String prompt = String.format("다음 텍스트에서 주요 키워드를 추출하여 #%s개 태그 형식으로 반환해줘: %s", limit, text);

        String response = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        // GPT 응답을 파싱하여 해시태그 리스트로 반환
        return List.of(response.split("\\s+#")).stream()
                .map(tag -> tag.replace("#", "").trim())
                .filter(tag -> !tag.isEmpty())
                .limit(limit)
                .toList();
    }


}
