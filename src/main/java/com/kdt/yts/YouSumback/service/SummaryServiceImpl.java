package com.kdt.yts.YouSumback.service;

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

    // ChatClient.Builder를 주입받아 ChatClient 생성
    @Autowired
    public SummaryServiceImpl(ChatClient.Builder chatClientBuilder,
                              AnswerOptionRepository answerOptionRepository,
                              TagRepository tagRepository,
                              UserLibraryRepository userLibraryRepository,
                              UserLibraryTagRepository userLibraryTagRepository,
                              UserRepository userRepository,
                              AudioTranscriptRepository audioTranscriptRepository,
                              SummaryRepository summaryRepository) {
        this.chatClient = chatClientBuilder.build();
        this.answerOptionRepository = answerOptionRepository;
        this.tagRepository = tagRepository;
        this.userLibraryRepository = userLibraryRepository;
        this.userLibraryTagRepository = userLibraryTagRepository;
        this.userRepository = userRepository;
        this.audioTranscriptRepository = audioTranscriptRepository;
        this.summaryRepository = summaryRepository;
    }

    @Override
    public SummaryResponseDTO summarize(SummaryRequestDTO request) {
        String text = request.getText();
        Long transcriptId = request.getTranscriptId();
        Long userId = request.getUserId();
        String userPrompt= request.getUserPrompt();
        SummaryType summaryType = request.getSummaryType();

        // 1. GPT 요약용 프롬프트 생성
        String prompt = buildPrompt(userPrompt, summaryType);
        List<String> chunks = splitTextIntoChunks(text, 1000);
        List<String> partialSummaries = new ArrayList<>();

        for (String chunk : chunks) {
            partialSummaries.add(callOpenAISummary(prompt + "\n\n" + chunk));
        }

        String finalSummary = callOpenAISummary(prompt + "\n\n" + String.join("\n", partialSummaries));

        // 2. Summary 저장
        User user = userRepository.findById(userId).orElseThrow();
        AudioTranscript transcript = audioTranscriptRepository.findById(transcriptId).orElseThrow();

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

        // 3. 라이브러리 저장
        UserLibrary library = UserLibrary.builder()
                .user(user)
                .summary(saved)
                .lastViewedAt(LocalDateTime.now())
                .build();
        userLibraryRepository.save(library);

        // 4. 해시태그 추출 및 저장
        List<String> hashtags = extractHashtags(finalSummary, 3);
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

    @Override
    public List<Quiz> generateFromSummary(QuizRequestDTO request) {
        String prompt = "다음 내용을 바탕으로 객관식 퀴즈를 " + request.getNumberOfQuestions() + "개 만들어줘.\n"
                + "문제 형식은 다음과 같아:\n"
                + "Q: ...\n1. ...\n2. ...\n3. ...\n4. ...\n정답: ...\n\n"
                + request.getSummaryText();

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

                Question question = Question.builder()
                        .questionText(questionText)
                        .options(options)
                        .build();

                Quiz quiz = Quiz.builder()
                        .summary(Summary.builder().id(request.getSummaryId()).build())
                        .questions(List.of(question))
                        .createdAt(LocalDateTime.now())
                        .build();

                quizzes.add(quiz);

            } catch (Exception e) {
                System.out.println("⚠️ 퀴즈 파싱 실패: " + block);
                e.printStackTrace();
            }
        }

        return quizzes;
    }

    @Override
    public String callOpenAISummary(String text) {
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

    @Override
    public void generateSummary(String youtubeId, String userPrompt, SummaryType summaryType) {
        AudioTranscript transcript = audioTranscriptRepository.findByYoutubeId(youtubeId)
                .orElseThrow(() -> new RuntimeException("Transcript not found"));

        User user = userRepository.findById(1L)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String prompt = buildPrompt(userPrompt, summaryType);
        String input = prompt + "\n\n" + transcript.getTranscriptText();
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

    private String buildPrompt(String userPromp, SummaryType summaryType) {
        String typeDesc = switch (summaryType.toString()) {
            case "BULLET" -> "간결한 bullet point 형태로";
            case "PARAGRAPH" -> "문장형 요약으로";
            case "QA" -> "질문-답변 형태로";
            default -> "간단히";
        };

        String userPromptDesc = switch (userPromp.toUpperCase()) {
            case "REVIEW" -> "복습 목적에 맞춰";
            case "EXAM" -> "시험 대비를 위해";
            default -> "학습 목적에 맞게";
        };
        return String.format("%s %s 요약해줘.", userPromptDesc, typeDesc);
    }
}
