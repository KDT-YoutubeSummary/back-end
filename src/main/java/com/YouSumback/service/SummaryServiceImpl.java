package com.YouSumback.service;

import com.YouSumback.model.dto.request.QuizRequest;
import com.YouSumback.model.dto.request.SummaryRequest;
import com.YouSumback.model.dto.response.SummaryResponse;
import com.YouSumback.model.entity.*;
import com.YouSumback.repository.*;


import lombok.RequiredArgsConstructor;
import org.springframework.ai.openai.OpenAiChatClient;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SummaryServiceImpl implements SummaryService {

    private final OpenAiChatClient chatClient;
    private final AnswerOptionRepository answerOptionRepository;
    private final TagRepository tagRepository;
    private final UserLibraryRepository userLibraryRepository;
    private final UserLibraryTagRepository userLibraryTagRepository;
    private final UserRepository userRepository;
    private final AudioTranscriptRepository audioTranscriptRepository;
    private final SummaryRepository summaryRepository;


    @Override
    public SummaryResponse summarize(SummaryRequest request) {
        String text = request.getText();
        Long transcriptId = request.getTranscriptId();
        Long userId = request.getUserId();

        // 1. OpenAI를 사용해 요약 텍스트 생성
        List<String> chunks = splitTextIntoChunks(text, 1000);
        List<String> partialSummaries = new ArrayList<>();
        for (String chunk : chunks) {
            partialSummaries.add(callOpenAISummary(chunk));
        }

        String finalSummary = callOpenAISummary(String.join("\n", partialSummaries));

        // 2. Summary 엔티티 저장
        Summary summary = Summary.builder()
                .user(userRepository.findById(userId).orElseThrow())
                .audioTranscript(audioTranscriptRepository.findById(transcriptId).orElseThrow())
                .summaryText(finalSummary)
                .languageCode(request.getLanguageCode())
                .summaryType("default")
                .createdAt(LocalDateTime.now())
                .build();

        Summary saved = summaryRepository.save(summary);

        // 3. 유저 라이브러리에서 해당 summary 참조 찾기
        UserLibrary library = userLibraryRepository.findBySummaryUserUserIdAndSummaryAudioTranscriptTranscriptId(userId, transcriptId)
                .orElseThrow(() -> new RuntimeException("라이브러리 항목 없음"));

        // 4. 요약 내용에서 태그 추출
        List<String> hashtags = extractHashtags(finalSummary, 3);
        for (String keyword : hashtags) {
            Tag tag = tagRepository.findByTagName(keyword)
                    .orElseGet(() -> tagRepository.save(Tag.builder().tagName(keyword).build()));

            boolean exists = userLibraryTagRepository.findByUserLibraryAndTag(library, tag).isPresent();
            if (!exists) {
                UserLibraryTag userLibraryTag = UserLibraryTag.builder()
                        .id(new UserLibraryTagId(library.getUserLibraryId(), tag.getTagId()))
                        .userLibrary(library)
                        .tag(tag)
                        .build();
                userLibraryTagRepository.save(userLibraryTag);
            }
        }

        // 5. 최종 응답
        return new SummaryResponse(saved.getSummaryId(), finalSummary);

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

    @Override
    public String callOpenAISummary(String text) {
        return chatClient.call("다음 내용을 바탕으로 요약해줘:\n" + text);
    }

    @Override
    public List<Quiz> generateFromSummary(QuizRequest request) {
        String prompt = "다음 내용을 바탕으로 객관식 퀴즈를 " + request.getNumberOfQuestions() + "개 만들어줘.\n"
                + "문제 형식은 다음과 같아:\n"
                + "Q: ...\n1. ...\n2. ...\n3. ...\n4. ...\n정답: ...\n\n"
                + request.getSummaryText();

        String aiResponse = chatClient.call(prompt);

        // 1. 퀴즈 분할
        String[] quizBlocks = aiResponse.split("(?=Q:)"); // Q:로 시작하는 단락 분리
        List<Quiz> quizzes = new ArrayList<>();

        for (String block : quizBlocks) {
            try {
                String[] lines = block.strip().split("\n");

                String questionText = lines[0].replace("Q:", "").trim();
                List<AnswerOption> options = new ArrayList<>();
                int correctIndex = -1;

                for (int i = 1; i <= 4; i++) {
                    String optionText = lines[i].substring(2).trim(); // "1. 보기" → "보기"
                    AnswerOption option = AnswerOption.builder()
                            .optionText(optionText)
                            .isCorrect(false)
                            .build();
                    options.add(option);
                }

                // 정답 번호 추출
                String answerLine = lines[5];
                int answerNum = Integer.parseInt(answerLine.replaceAll("[^0-9]", ""));
                options.get(answerNum - 1).setIsCorrect(true);

                // 퀴즈 & 연관된 보기 구성
                Question question = Question.builder()
                        .questionText(questionText)
                        .options(options)
                        .build();

                Quiz quiz = Quiz.builder()
                        .summary(Summary.builder().summaryId(request.getSummaryId()).build())
                        .questions(List.of(question))
                        .createdAt(LocalDateTime.now())
                        .build();

                quizzes.add(quiz);
            } catch (Exception e) {
                System.out.println("⚠️ 퀴즈 파싱 실패: " + block);
                e.printStackTrace();
            }
        }

        // TODO: QuizRepository.saveAll(quizzes); 저장 로직 필요
        return quizzes;
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
