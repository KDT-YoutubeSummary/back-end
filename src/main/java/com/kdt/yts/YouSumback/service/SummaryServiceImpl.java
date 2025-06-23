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

        String prompt = buildPrompt(userPrompt, summaryType);

        String finalSummary;
        if (summaryType == SummaryType.TIMELINE) {
            System.out.println("✅ TIMELINE summary: Bypassing chunking and calling AI with full VTT content.");
            finalSummary = callOpenAISummary(prompt + "\n\n" + text);
        } else {
            List<String> chunks = splitTextIntoChunks(text, 1000);
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