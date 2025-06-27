package com.kdt.yts.YouSumback.service;

import com.kdt.yts.YouSumback.model.dto.request.UserAnswerDTO;
import com.kdt.yts.YouSumback.model.dto.response.*;
import com.kdt.yts.YouSumback.model.entity.*;
import com.kdt.yts.YouSumback.repository.*;
import com.kdt.yts.YouSumback.model.dto.request.QuizRequestDTO;
import com.kdt.yts.YouSumback.model.dto.request.SummaryRequestDTO;
import com.kdt.yts.YouSumback.service.client.OpenAIClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SummaryServiceImpl implements SummaryService {

    private final OpenAIClient openAIClient;
    private final AnswerOptionRepository answerOptionRepository;
    private final TagRepository tagRepository;
    private final SummaryArchiveRepository summaryArchiveRepository;
    private final SummaryArchiveTagRepository summaryArchiveTagRepository;
    private final UserRepository userRepository;
    private final AudioTranscriptRepository audioTranscriptRepository;
    private final SummaryRepository summaryRepository;
    private final QuizRepository quizRepository;
    private final UserActivityLogRepository userActivityLogRepository;
    private final S3Client s3Client;

    private String readTextFromS3(String s3Key) {
        System.out.println("✅ S3에서 파일 읽기 시도. Key: " + s3Key);
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket("yousum-s3")
                    .key(s3Key)
                    .build();

            ResponseBytes<GetObjectResponse> objectBytes = s3Client.getObjectAsBytes(getObjectRequest);
            byte[] data = objectBytes.asByteArray();
            System.out.println("✅ S3 파일 읽기 성공. 파일 크기: " + data.length + " bytes");
            return new String(data, StandardCharsets.UTF_8);
        } catch (Exception e) {
            System.err.println("❌ S3 파일 읽기 중 심각한 오류 발생: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to read file from S3: " + s3Key, e);
        }
    }

    @Override
    @Transactional
    public SummaryResponseDTO summarize(SummaryRequestDTO request, Long userId) {
        String originalUrl = request.getOriginalUrl();
        String userPrompt = request.getUserPrompt();
        SummaryType summaryType = request.getSummaryType();

        System.out.println(">>> SummaryServiceImpl.summarize 진입 - URL: " + originalUrl + ", User ID: " + userId);

        AudioTranscript transcript = audioTranscriptRepository.findByVideo_OriginalUrl(originalUrl)
                .orElseThrow(() -> new RuntimeException("AudioTranscript not found for URL: " + originalUrl));

        if (transcript.getTranscriptPath() == null || transcript.getTranscriptPath().isEmpty()) {
            System.err.println("❌ AudioTranscript에 파일 경로가 없습니다: " + originalUrl);
            throw new RuntimeException("No transcript file path found for URL: " + originalUrl + ". Summary failed.");
        }

        String text = readTextFromS3(transcript.getTranscriptPath());
        Long transcriptId = transcript.getId();

        System.out.println("✅ Transcript text loaded from S3. ID: " + transcriptId);

        PromptBuilder promptBuilder = new PromptBuilder();
        String prompt = promptBuilder.buildPrompt(userPrompt, summaryType);

        String finalSummary;
        if (summaryType == SummaryType.TIMELINE) {
            System.out.println("✅ TIMELINE summary: Bypassing chunking and calling AI with full VTT content.");
            finalSummary = callOpenAISummary(prompt + "\n\n" + text);
        } else {
            List<String> chunks = splitTextIntoChunks(text, 2000);
            List<String> partialSummaries = new ArrayList<>();

            for (String chunk : chunks) {
                partialSummaries.add(callOpenAISummary(prompt + "\n\n" + chunk));
            }
            String finalSummaryPrompt = "다음은 각 부분에 대한 요약입니다. 이 요약들을 하나로 합쳐서 자연스러운 최종 요약을 만들어주세요:\n\n" + String.join("\n---\n", partialSummaries);
            finalSummary = callOpenAISummary(finalSummaryPrompt);
        }
        System.out.println("✅ Final Summary Generated. Length: " + finalSummary.length());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found for ID: " + userId));

        String videoLanguageCode = transcript.getVideo().getOriginalLanguageCode();
        Summary summary = Summary.builder()
                .user(user)
                .audioTranscript(transcript)
                .summaryText(finalSummary)
                .summaryType(summaryType)
                .userPrompt(userPrompt)
                .createdAt(LocalDateTime.now())
                .languageCode(videoLanguageCode != null ? videoLanguageCode : "ko")
                .build();
        Summary saved = summaryRepository.save(summary);
        System.out.println("✅ Summary Saved. ID: " + saved.getId());

        SummaryArchive archive = summaryArchiveRepository.findByUserIdAndSummaryId(user.getId(), saved.getId())
                .orElseGet(SummaryArchive::new);

        archive.setUser(user);
        archive.setSummary(saved);
        archive.setLastViewedAt(LocalDateTime.now());
        summaryArchiveRepository.save(archive);
        System.out.println("✅ SummaryArchive Saved/Updated. User ID: " + user.getId() + ", Summary ID: " + saved.getId());

        List<String> hashtags = extractTagsWithLLM(finalSummary).stream().distinct().toList();
        System.out.println("✅ Hashtags Extracted: " + hashtags);

        for (String keyword : hashtags) {
            Tag tag = findOrCreateTag(keyword);

            SummaryArchiveTagId summaryArchiveTagId = new SummaryArchiveTagId(archive.getId(), tag.getId());
            if (!summaryArchiveTagRepository.existsById(summaryArchiveTagId)) {
                SummaryArchiveTag summaryArchiveTag = new SummaryArchiveTag();
                summaryArchiveTag.setSummaryArchive(archive);
                summaryArchiveTag.setTag(tag);
                summaryArchiveTagRepository.save(summaryArchiveTag);
            }
        }
        System.out.println("✅ Tags Processed.");

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

    public class PromptBuilder {
        public String buildPrompt(String userPrompt, SummaryType summaryType) {
            String formatInstruction = switch (summaryType) {
                case BASIC -> """
    ## 📚 기본 요약
    
    ### 핵심 내용
    - [첫 번째 주요 포인트와 구체적 설명]
    - [두 번째 주요 포인트와 실제 예시]
    - [세 번째 주요 포인트와 적용 방안]
    
    ### 결론 및 활용
    [핵심 결론과 실무 적용 방법]
    
    ### 관련 학습
    [추가로 알아볼 만한 관련 주제나 개념]
    """;

                case THREE_LINE -> """
    ## 📝 3줄 요약
    
    1. [핵심 개념이나 원리]
    2. [구체적 방법이나 사례]
    3. [결론이나 적용방안]
    
    ### 추가 포인트
    [3줄 요약을 보완하는 중요한 내용]
    """;

                case KEYWORD -> """
    ## 🔑 키워드 요약
    
    ### 핵심 키워드
    - **키워드1**: [정의와 의미]
    - **키워드2**: [정의와 의미]
    - **키워드3**: [정의와 의미]
    - **키워드4**: [정의와 의미]
    
    ### 키워드 활용
    [키워드들이 어떻게 연결되고 실제로 활용되는지 설명]
    """;

                case TIMELINE -> """
    ## ⏰ 타임라인 요약
    
    ### 시간순 주요 내용
    | 시간 | 주요 내용 |
    |------|-----------|
    | 00:00-05:00 | [초반부 핵심 내용] |
    | 05:00-10:00 | [중반부 핵심 내용] |  
    | 10:00-15:00 | [후반부 핵심 내용] |
    
    ### 핵심 포인트
    [전체 흐름에서 가장 중요한 포인트들]
    """;
            };

            return String.format("""
    학습용 요약을 작성해주세요.
    
    사용자 요청: %s
    
    지시사항:
    1. 아래 형식을 정확히 따르세요
    2. 간결하고 명확하게 작성하세요
    3. 학습에 도움되는 핵심 내용을 포함하세요
    
    형식:
    %s
    
    요약 대상 내용:
    ---
    """, userPrompt, formatInstruction);
        }
    }

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

        String response = openAIClient.chat(prompt).block();
        return Arrays.stream(response.split("[,\\n]"))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .limit(3)
                .toList();
    }

    public Optional<SummaryArchive> findSummaryArchiveByUserAndSummary(Long userId, Summary summary) {
        return summaryArchiveRepository.findByUserIdAndSummaryId(userId, summary.getId());
    }

    @Transactional
    public List<QuizResponseDTO> generateFromSummary(QuizRequestDTO request) {
        Summary summary = summaryRepository.findById(request.getSummaryId())
                .orElseThrow(() -> new RuntimeException("Summary not found"));

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

        System.out.println(">>>> Sending Quiz Prompt to AI:\n" + prompt);
        String aiResponseQuiz = openAIClient.chat(prompt).block();
        System.out.println(">>>> AI Quiz Response:\n" + aiResponseQuiz);
        String[] rawBlocks = aiResponseQuiz.split("(?m)(?=Q:)");
        List<String> quizBlocks = new ArrayList<>();
        for (String b : rawBlocks) {
            String trimmed = b.strip();
            if (!trimmed.isEmpty()) {
                quizBlocks.add(trimmed);
            }
        }

        Quiz quiz = Quiz.builder()
                .summary(summary)
                .title("AI 자동 생성 퀴즈")
                .createdAt(LocalDateTime.now())
                .build();

        List<Question> questionList = new ArrayList<>();
        for (String block : quizBlocks) {
            try {
                String[] lines = block.split("\\r?\\n");
                if (lines.length < 2) {
                    System.out.println("⚠️ 블록 라인 부족: " + block);
                    continue;
                }
                String firstLine = lines[0].trim();
                String questionText;
                if (firstLine.startsWith("Q:")) {
                    questionText = firstLine.substring(2).trim();
                } else {
                    continue;
                }

                List<AnswerOption> options = new ArrayList<>();
                Integer answerIndex = null;
                for (String line : Arrays.copyOfRange(lines, 1, lines.length)) {
                    line = line.trim();
                    if (line.matches("^[0-9]+\\.\\s+.*")) {
                        String optText = line.replaceFirst("^[0-9]+\\.\\s*", "");
                        AnswerOption opt = AnswerOption.builder()
                                .optionText(optText)
                                .isCorrect(false)
                                .build();
                        options.add(opt);
                    }
                    else if (line.startsWith("정답")) {
                        String digits = line.replaceAll("[^0-9]", "");
                        if (!digits.isEmpty()) {
                            answerIndex = Integer.parseInt(digits);
                        }
                    }
                }
                if (answerIndex != null && answerIndex > 0 && answerIndex <= options.size()) {
                    options.get(answerIndex - 1).setIsCorrect(true);
                } else {
                    System.out.println("⚠️ 정답 인덱스 파싱 실패: " + block);
                    continue;
                }
                Question q = Question.builder()
                        .quiz(quiz)
                        .questionText(questionText)
                        .languageCode("ko")
                        .options(options)
                        .build();

                for (AnswerOption option : options) {
                    option.setQuestion(q);
                }
                questionList.add(q);

            } catch (Exception e) {
                System.err.println("퀴즈 블록 파싱 중 예외 발생: " + block);
                e.printStackTrace();
            }
        }
        quiz.setQuestions(questionList);

        quizRepository.save(quiz);

        return quiz.getQuestions().stream()
                .map(q -> {
                    List<OptionDTO> optionDTOs = q.getOptions().stream()
                            .map(o -> new OptionDTO(o.getId(), o.getOptionText()))
                            .collect(Collectors.toList());
                    return new QuizResponseDTO(quiz.getId(), quiz.getTitle(), quiz.getCreatedAt(), List.of(new QuestionDTO(q.getId(), q.getQuestionText(), optionDTOs)));
                })
                .collect(Collectors.toList());
    }

    private boolean isRelated(String tag, String[] words) {
        return Arrays.stream(words)
                .anyMatch(word -> tag.toLowerCase().contains(word) || word.toLowerCase().contains(tag));
    }

    @Transactional
    @Override
    public QuizResultResponseDTO checkQuizAnswers(Long quizId, List<UserAnswerDTO> userAnswers) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("퀴즈를 찾을 수 없습니다."));

        int correctCount = 0;
        List<Boolean> results = new ArrayList<>();

        for (UserAnswerDTO userAnswer : userAnswers) {
            AnswerOption selectedOption = answerOptionRepository.findById(userAnswer.getAnswerOptionId())
                    .orElseThrow(() -> new RuntimeException("선택한 보기를 찾을 수 없습니다."));

            boolean isCorrect = selectedOption.getIsCorrect();
            if (isCorrect) {
                correctCount++;
            }
            results.add(isCorrect);
        }

        int score = (int) ((double) correctCount / quiz.getQuestions().size() * 100);

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
        return openAIClient.chat(fullPrompt).block();
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

    private synchronized Tag findOrCreateTag(String tagName) {
        return tagRepository.findByTagName(tagName)
                .orElseGet(() -> tagRepository.save(Tag.builder().tagName(tagName).build()));
    }
}

