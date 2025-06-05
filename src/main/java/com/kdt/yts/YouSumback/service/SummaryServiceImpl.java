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

        // 1. 요약 생성
        List<String> chunks = splitTextIntoChunks(text, 1000);
        List<String> partialSummaries = new ArrayList<>();
        for (String chunk : chunks) {
            partialSummaries.add(callOpenAISummary(chunk));
        }
        String finalSummary = callOpenAISummary(String.join("\n", partialSummaries));

        // 2. Summary 저장
        Summary summary = Summary.builder()
                .user(userRepository.findById(userId).orElseThrow())
                .audioTranscript(audioTranscriptRepository.findById(transcriptId).orElseThrow())
                .summaryText(finalSummary)
                .languageCode(request.getLanguageCode())
                .summaryType("default")
                .createdAt(LocalDateTime.now())
                .build();
        Summary saved = summaryRepository.save(summary);

        // 3. 라이브러리 찾기
        UserLibrary library = userLibraryRepository
                .findBySummaryUserUserIdAndSummaryAudioTranscriptTranscriptId(userId, transcriptId)
                .orElseThrow(() -> new RuntimeException("라이브러리 항목 없음"));

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
                        .id(new UserLibraryTagId(library.getUser().getId(), tag.getId()))
                        .userLibrary(library)
                        .tag(tag)
                        .build();
                userLibraryTagRepository.save(userLibraryTag);
            }
        }

        return new SummaryResponseDTO(saved.getId(), finalSummary);
    }

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
}
