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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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
    private final VideoRepository videoRepository;

    @Override
    @Transactional
    public SummaryResponseDTO summarize(SummaryRequestDTO request, Long userId) {
        String originalUrl = request.getOriginalUrl();
        String userPrompt = request.getUserPrompt();
        SummaryType summaryType = request.getSummaryType();

        System.out.println(">>> SummaryServiceImpl.summarize 진입 - URL: " + originalUrl + ", User ID: " + userId);

        AudioTranscript transcript = audioTranscriptRepository.findByVideo_OriginalUrl(originalUrl)
                .orElseThrow(() -> new RuntimeException("AudioTranscript not found for URL: " + originalUrl));

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
                    Path cleanedPath = Paths.get(transcript.getTranscriptPath());
                    System.err.println("⚠️ VTT file not found for TIMELINE summary at " + vttPath + ". Falling back to cleaned text from " + cleanedPath);
                    text = Files.readString(cleanedPath, StandardCharsets.UTF_8);
                }
            } catch (IOException e) {
                System.err.println("❌ Error reading transcript file for TIMELINE summary: " + e.getMessage());
                throw new RuntimeException("Failed to read transcript file for TIMELINE summary.", e);
            }
        } else {
            if (transcript.getTranscriptPath() == null || transcript.getTranscriptPath().isEmpty()) {
                System.err.println("❌ AudioTranscript has no file path for URL: " + originalUrl);
                throw new RuntimeException("No transcript file path found for URL: " + originalUrl + ". Summary failed.");
            }

            try {
                Path filePath = Paths.get(transcript.getTranscriptPath());
                text = Files.readString(filePath, StandardCharsets.UTF_8);
                System.out.println("✅ Transcript text loaded from file path: " + filePath);
            } catch (IOException e) {
                System.err.println("❌ Error reading transcript file from path: " + transcript.getTranscriptPath() + " - " + e.getMessage());
                throw new RuntimeException("Failed to read transcript text from file.", e);
            }
        }
        Long transcriptId = transcript.getId();

        System.out.println("✅ Transcript found/processed. ID: " + transcriptId);

        PromptBuilder promptBuilder = new PromptBuilder();
        String prompt;
        if (summaryType == SummaryType.TIMELINE) {
            // TIMELINE 타입일 때는 VTT 텍스트를 전달하여 동적 타임라인 생성
            prompt = promptBuilder.buildPromptWithDuration(userPrompt, summaryType, text);
        } else {
            prompt = promptBuilder.buildPrompt(userPrompt, summaryType);
        }

        // ✅ 프롬프트 디버깅 로그 추가
        System.out.println("====================");
        System.out.println("🔍 요약 타입: " + summaryType);
        System.out.println("🔍 사용자 프롬프트: " + userPrompt);
        System.out.println("====================");
        System.out.println("📝 생성된 전체 프롬프트:");
        System.out.println(prompt);
        System.out.println("====================");

        String finalSummary;
        if (summaryType == SummaryType.TIMELINE || summaryType == SummaryType.KEYWORD) {
            System.out.println("✅ " + summaryType + " summary: Bypassing chunking and calling AI with full content.");
            String fullPromptForAI = prompt + "\n\n" + text;
            System.out.println("🤖 AI에게 전송할 최종 프롬프트 (첫 500자):");
            System.out.println(fullPromptForAI.substring(0, Math.min(500, fullPromptForAI.length())) + "...");
            finalSummary = callOpenAISummary(fullPromptForAI);
        } else {
            List<String> chunks = splitTextIntoChunks(text, 2000);
            List<String> partialSummaries = new ArrayList<>();

            for (int i = 0; i < chunks.size(); i++) {
                String chunk = chunks.get(i);
                String chunkPrompt = prompt + "\n\n" + chunk;
                System.out.println("🤖 청크 " + (i+1) + "/" + chunks.size() + " AI에게 전송할 프롬프트 (첫 300자):");
                System.out.println(chunkPrompt.substring(0, Math.min(300, chunkPrompt.length())) + "...");
                partialSummaries.add(callOpenAISummary(chunkPrompt));
            }
            // 최종 합치기에서도 TIMELINE 타입인 경우 VTT 텍스트 전달
            String finalSummaryPrompt;
            if (summaryType == SummaryType.TIMELINE) {
                finalSummaryPrompt = promptBuilder.buildMergePromptWithDuration(partialSummaries, summaryType, text);
            } else {
                finalSummaryPrompt = promptBuilder.buildMergePrompt(partialSummaries, summaryType);
            }
            System.out.println("🔄 최종 요약 합치기 프롬프트:");
            System.out.println(finalSummaryPrompt.substring(0, Math.min(300, finalSummaryPrompt.length())) + "...");
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

        SummaryArchive archive = new SummaryArchive();
        archive.setUser(user);
        archive.setSummary(saved);
        archive.setLastViewedAt(LocalDateTime.now());
        summaryArchiveRepository.save(archive);
        System.out.println("✅ SummaryArchive Saved. User ID: " + user.getId() + ", Summary ID: " + saved.getId());

        List<String> hashtags = extractTagsWithLLM(finalSummary).stream().distinct().toList();
        System.out.println("✅ Hashtags Extracted: " + hashtags);

        for (String keyword : hashtags) {
            Tag tag = findOrCreateTag(keyword);

            SummaryArchiveTag summaryArchiveTag = new SummaryArchiveTag(archive.getId(), tag.getId());
            summaryArchiveTagRepository.save(summaryArchiveTag);
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
            return buildPromptWithDuration(userPrompt, summaryType, null);
        }
        
        public String buildPromptWithDuration(String userPrompt, SummaryType summaryType, String vttText) {
            String baseInstruction = "당신은 전문적인 콘텐츠 요약 AI입니다. 제공된 텍스트를 아래 지침에 따라 정확히 요약해주세요.";
            
            String typeSpecificInstruction = switch (summaryType) {
    
                case BASIC -> """
                
【기본 요약 지침】
아래 형식을 정확히 지켜서 요약해주세요:

## 핵심 내용
- 첫 번째 주요 포인트 (구체적인 내용)
- 두 번째 주요 포인트 (구체적인 내용)  
- 세 번째 주요 포인트 (구체적인 내용)

## 결론
실무에서 활용 가능한 방법이나 핵심 결론을 제시해주세요.

## 추천 학습
관련 주제나 추가 학습 방향을 제안해주세요.

※ 위 형식을 반드시 지켜주세요.
    """;
    
                case THREE_LINE -> """
                
【3줄 요약 지침】
반드시 아래 형식으로 정확히 3줄만 작성해주세요:

1. [첫 번째 핵심 내용을 한 줄로 명확히]
2. [두 번째 핵심 내용을 한 줄로 명확히]  
3. [세 번째 핵심 내용 또는 결론을 한 줄로 명확히]

**추가 포인트:**
3줄 요약을 보완하는 중요한 내용이나 실무 적용 팁을 간단히 추가해주세요.

※ 정확히 3줄 형식을 지켜주세요.
    """;
    
                case KEYWORD -> """
                
【키워드 추출 지침】
아래 형식을 정확히 지켜서 키워드를 추출해주세요:

## 핵심 키워드
1. **키워드1** - 이 키워드가 중요한 이유와 의미
2. **키워드2** - 이 키워드가 중요한 이유와 의미
3. **키워드3** - 이 키워드가 중요한 이유와 의미
4. **키워드4** - 이 키워드가 중요한 이유와 의미
5. **키워드5** - 이 키워드가 중요한 이유와 의미

## 키워드 연관성
5개 키워드들이 어떻게 서로 연결되어 있고, 전체 내용의 맥락에서 어떤 의미를 가지는지 설명해주세요.

※ 정확히 5개의 키워드를 추출해주세요.
    """;
    
                case TIMELINE -> {
                    if (vttText != null) {
                        int durationSeconds = parseVideoDurationFromVTT(vttText);
                        yield "\n【타임라인 요약 지침】\n영상의 시간 흐름에 따라 아래 형식으로 정리해주세요:\n\n" + 
                              generateDynamicTimeline(durationSeconds);
                    } else {
                        yield """
                
【타임라인 요약 지침】
영상의 시간 흐름에 따라 아래 형식으로 정리해주세요:

## 타임라인
**0~5분:** 영상 초반부의 주요 내용과 도입부 핵심 사항
**5~10분:** 영상 중반부의 핵심 내용과 주요 논점  
**10~15분:** 영상 후반부의 중요 내용과 발전된 논의
**15분 이후:** 마무리 내용과 결론 부분

## 핵심 포인트
전체 타임라인에서 가장 중요한 2-3가지 핵심 메시지를 정리해주세요.

※ 시간대별 구분을 명확히 해주세요.
    """;
                    }
                }
            };
            
            String userRequest = userPrompt != null && !userPrompt.trim().isEmpty() 
                ? userPrompt 
                : "영상 내용을 요약해주세요";
    
            return String.format("""
%s

%s

【사용자 요청사항】
%s

【중요 안내】
- 위에 제시된 형식을 반드시 준수해주세요
- 각 섹션의 제목(##, **)을 정확히 사용해주세요
- 불필요한 부연설명은 피하고 핵심 내용만 간결하게 작성해주세요
- 한국어로 자연스럽게 작성해주세요

【요약할 내용】
==========================================
""", baseInstruction, typeSpecificInstruction, userRequest);
        }

        public String buildMergePrompt(List<String> summaries, SummaryType summaryType) {
            return buildMergePromptWithDuration(summaries, summaryType, null);
        }
        
        public String buildMergePromptWithDuration(List<String> summaries, SummaryType summaryType, String vttText) {
            String baseInstruction = "당신은 전문적인 콘텐츠 요약 AI입니다. 제공된 텍스트를 아래 지침에 따라 정확히 요약해주세요.";
            
            String typeSpecificInstruction = switch (summaryType) {
    
                case BASIC -> """
                
【기본 요약 지침】
아래 형식을 정확히 지켜서 요약해주세요:

## 핵심 내용
- 첫 번째 주요 포인트 (구체적인 내용)
- 두 번째 주요 포인트 (구체적인 내용)  
- 세 번째 주요 포인트 (구체적인 내용)

## 결론
실무에서 활용 가능한 방법이나 핵심 결론을 제시해주세요.

## 추천 학습
관련 주제나 추가 학습 방향을 제안해주세요.

※ 위 형식을 반드시 지켜주세요.
    """;
    
                case THREE_LINE -> """
                
【3줄 요약 지침】
반드시 아래 형식으로 정확히 3줄만 작성해주세요:

1. [첫 번째 핵심 내용을 한 줄로 명확히]
2. [두 번째 핵심 내용을 한 줄로 명확히]  
3. [세 번째 핵심 내용 또는 결론을 한 줄로 명확히]

**추가 포인트:**
3줄 요약을 보완하는 중요한 내용이나 실무 적용 팁을 간단히 추가해주세요.

※ 정확히 3줄 형식을 지켜주세요.
    """;
    
                case KEYWORD -> """
                
【키워드 추출 지침】
아래 형식을 정확히 지켜서 키워드를 추출해주세요:

## 핵심 키워드
1. **키워드1** - 이 키워드가 중요한 이유와 의미
2. **키워드2** - 이 키워드가 중요한 이유와 의미
3. **키워드3** - 이 키워드가 중요한 이유와 의미
4. **키워드4** - 이 키워드가 중요한 이유와 의미
5. **키워드5** - 이 키워드가 중요한 이유와 의미

## 키워드 연관성
5개 키워드들이 어떻게 서로 연결되어 있고, 전체 내용의 맥락에서 어떤 의미를 가지는지 설명해주세요.

※ 정확히 5개의 키워드를 추출해주세요.
    """;
    
                case TIMELINE -> {
                    if (vttText != null) {
                        int durationSeconds = parseVideoDurationFromVTT(vttText);
                        yield "\n【타임라인 요약 지침】\n영상의 시간 흐름에 따라 아래 형식으로 정리해주세요:\n\n" + 
                              generateDynamicTimeline(durationSeconds);
                    } else {
                        yield """
                
【타임라인 요약 지침】
영상의 시간 흐름에 따라 아래 형식으로 정리해주세요:

## 타임라인
**0~5분:** 영상 초반부의 주요 내용과 도입부 핵심 사항
**5~10분:** 영상 중반부의 핵심 내용과 주요 논점  
**10~15분:** 영상 후반부의 중요 내용과 발전된 논의
**15분 이후:** 마무리 내용과 결론 부분

## 핵심 포인트
전체 타임라인에서 가장 중요한 2-3가지 핵심 메시지를 정리해주세요.

※ 시간대별 구분을 명확히 해주세요.
    """;
                    }
                }
            };
            
            String userRequest = "다음은 각 부분에 대한 요약입니다. 이 요약들을 하나로 합쳐서 자연스러운 최종 요약을 만들어주세요.";
    
            return String.format("""
%s

%s

【사용자 요청사항】
%s

【중요 안내】
- 위에 제시된 형식을 반드시 준수해주세요
- 각 섹션의 제목(##, **)을 정확히 사용해주세요
- 불필요한 부연설명은 피하고 핵심 내용만 간결하게 작성해주세요
- 한국어로 자연스럽게 작성해주세요

【합칠 부분별 요약들】
==========================================
%s
""", baseInstruction, typeSpecificInstruction, userRequest, String.join("\n\n---\n\n", summaries));
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
        return summaryArchiveRepository.findByUser_IdAndSummary_Id(userId, summary.getId());
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
                    continue; // "Q:"로 시작하지 않으면 스킵
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
                    continue; // 정답 파싱 실패 시 이 블록 스킵
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
                    // 이 부분은 QuestionDTO를 생성해야 합니다.
                    // 그러나 반환 타입이 List<QuizResponseDTO>이므로, 전체 구조를 맞춰야 합니다.
                    // 여기서는 임시로 null을 반환하고 전체 로직을 수정합니다.
                    // 실제로는 Quiz를 DTO로 변환하는 로직이 필요합니다.
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
        System.out.println("📤 OpenAI API 호출 중...");
        System.out.println("프롬프트 길이: " + fullPrompt.length() + " 문자");
        
        int maxRetries = 1; // 재시도 1회로 줄임
        String response = null;
        
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                System.out.println("🔄 시도 " + attempt + "/" + maxRetries);
                response = openAIClient.chat(fullPrompt).block();
                
                System.out.println("📥 OpenAI API 응답 받음!");
                System.out.println("응답 길이: " + (response != null ? response.length() : 0) + " 문자");
                System.out.println("응답 내용 (첫 500자):");
                System.out.println(response != null && response.length() > 500 
                    ? response.substring(0, 500) + "..." 
                    : response);
                
                // 응답 품질 검증 (매우 관대하게)
                if (isValidSummaryResponse(response)) {
                    System.out.println("✅ 응답 품질 검증 통과!");
                    break;
                } else {
                    System.out.println("⚠️ 응답 품질이 기준에 미달하지만 1회만 재시도하므로 그대로 사용합니다.");
                    break; // 재시도하지 않고 그대로 사용
                }
                
            } catch (Exception e) {
                System.err.println("❌ OpenAI API 호출 중 오류 발생 (시도 " + attempt + "/" + maxRetries + "): " + e.getMessage());
                if (attempt == maxRetries) {
                    throw new RuntimeException("OpenAI API 호출 실패", e);
                }
                try {
                    Thread.sleep(2000); // 2초 대기 후 재시도
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("재시도 중 인터럽트 발생", ie);
                }
            }
        }
        
        System.out.println("=".repeat(50));
        return response;
    }
    
    private boolean isValidSummaryResponse(String response) {
        if (response == null || response.trim().isEmpty()) {
            return false;
        }
        
        // 매우 기본적인 검증만 수행
        if (response.length() < 30) {
            System.out.println("🔍 검증 실패: 응답이 너무 짧음 (" + response.length() + "자)");
            return false;
        }
        
        // 한글이 조금이라도 있는지만 확인
        boolean hasKorean = response.chars()
                .anyMatch(c -> c >= 0xAC00 && c <= 0xD7A3);
        
        if (!hasKorean) {
            System.out.println("🔍 검증 실패: 한글 내용이 없음");
            return false;
        }
        
        System.out.println("🔍 응답 검증: 통과 (길이: " + response.length() + "자)");
        return true;
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

    /**
     * VTT 자막에서 영상의 총 길이(초)를 파싱합니다.
     */
    private int parseVideoDurationFromVTT(String vttText) {
        try {
            String[] lines = vttText.split("\\r?\\n");
            int maxSeconds = 0;
            
            for (String line : lines) {
                // 타임스탬프 라인 찾기: "00:01:23.456 --> 00:02:34.567" 형식
                if (line.contains("-->")) {
                    String[] timeParts = line.split("-->");
                    if (timeParts.length >= 2) {
                        String endTime = timeParts[1].trim();
                        int seconds = parseTimeToSeconds(endTime);
                        maxSeconds = Math.max(maxSeconds, seconds);
                    }
                }
            }
            
            System.out.println("🕐 VTT에서 파싱된 영상 길이: " + maxSeconds + "초 (" + formatDuration(maxSeconds) + ")");
            return maxSeconds;
        } catch (Exception e) {
            System.err.println("⚠️ VTT 파싱 중 오류: " + e.getMessage());
            return 300; // 기본값 5분
        }
    }
    
    /**
     * "00:01:23.456" 형식의 시간을 초로 변환합니다.
     */
    private int parseTimeToSeconds(String timeStr) {
        try {
            // "00:01:23.456" -> ["00", "01", "23.456"]
            String[] parts = timeStr.split(":");
            if (parts.length >= 3) {
                int hours = Integer.parseInt(parts[0]);
                int minutes = Integer.parseInt(parts[1]);
                double seconds = Double.parseDouble(parts[2]);
                return (int) (hours * 3600 + minutes * 60 + seconds);
            }
        } catch (Exception e) {
            System.err.println("⚠️ 시간 파싱 오류: " + timeStr);
        }
        return 0;
    }
    
    /**
     * 초를 "X분 Y초" 형식으로 포맷팅합니다.
     */
    private String formatDuration(int totalSeconds) {
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        if (minutes > 0) {
            return minutes + "분 " + seconds + "초";
        } else {
            return seconds + "초";
        }
    }
    
    /**
     * 영상 길이에 따라 동적 타임라인 구간을 생성합니다.
     */
    private String generateDynamicTimeline(int durationSeconds) {
        if (durationSeconds <= 60) {
            // 1분 이하: 2구간
            int mid = durationSeconds / 2;
            return String.format("""
## 타임라인
**0초~%s:** 영상 전반부의 주요 내용과 도입부 핵심 사항
**%s~%s:** 영상 후반부의 핵심 내용과 결론 부분

## 핵심 포인트
전체 타임라인에서 가장 중요한 2-3가지 핵심 메시지를 정리해주세요.

※ 실제 영상 시간에 맞춰 정확히 구분해주세요.""", 
                formatDuration(mid), formatDuration(mid), formatDuration(durationSeconds));
                
        } else if (durationSeconds <= 180) {
            // 3분 이하: 3구간
            int third = durationSeconds / 3;
            return String.format("""
## 타임라인
**0초~%s:** 영상 초반부의 주요 내용과 도입부
**%s~%s:** 영상 중반부의 핵심 내용과 주요 논점
**%s~%s:** 영상 후반부의 중요 내용과 결론

## 핵심 포인트
전체 타임라인에서 가장 중요한 2-3가지 핵심 메시지를 정리해주세요.

※ 실제 영상 시간에 맞춰 정확히 구분해주세요.""", 
                formatDuration(third), formatDuration(third), formatDuration(third * 2), 
                formatDuration(third * 2), formatDuration(durationSeconds));
                
        } else if (durationSeconds <= 600) {
            // 10분 이하: 4구간
            int quarter = durationSeconds / 4;
            return String.format("""
## 타임라인
**0초~%s:** 영상 초반부의 주요 내용과 도입부 핵심 사항
**%s~%s:** 영상 전반 중반부의 핵심 내용과 주요 논점
**%s~%s:** 영상 후반 중반부의 중요 내용과 발전된 논의
**%s~%s:** 영상 마무리 부분의 결론과 핵심 정리

## 핵심 포인트
전체 타임라인에서 가장 중요한 2-3가지 핵심 메시지를 정리해주세요.

※ 실제 영상 시간에 맞춰 정확히 구분해주세요.""", 
                formatDuration(quarter), formatDuration(quarter), formatDuration(quarter * 2),
                formatDuration(quarter * 2), formatDuration(quarter * 3), 
                formatDuration(quarter * 3), formatDuration(durationSeconds));
                
        } else {
            // 10분 초과: 5구간
            int fifth = durationSeconds / 5;
            return String.format("""
## 타임라인
**0초~%s:** 영상 도입부와 초반 핵심 내용
**%s~%s:** 영상 전반부의 주요 논점과 설명
**%s~%s:** 영상 중반부의 핵심 내용과 발전된 논의
**%s~%s:** 영상 후반부의 중요 내용과 심화 논의
**%s~%s:** 영상 마무리와 결론 부분

## 핵심 포인트
전체 타임라인에서 가장 중요한 2-3가지 핵심 메시지를 정리해주세요.

※ 실제 영상 시간에 맞춰 정확히 구분해주세요.""", 
                formatDuration(fifth), formatDuration(fifth), formatDuration(fifth * 2),
                formatDuration(fifth * 2), formatDuration(fifth * 3), 
                formatDuration(fifth * 3), formatDuration(fifth * 4),
                formatDuration(fifth * 4), formatDuration(durationSeconds));
        }
    }

}