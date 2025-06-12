package com.kdt.yts.YouSumback.service;

import com.kdt.yts.YouSumback.model.dto.request.QuizRequest;
import com.kdt.yts.YouSumback.model.entity.AnswerOption;
import com.kdt.yts.YouSumback.model.entity.Question;
import com.kdt.yts.YouSumback.model.entity.Quiz;
import com.kdt.yts.YouSumback.model.entity.Summary;
import com.kdt.yts.YouSumback.repository.QuizRepository;
import com.kdt.yts.YouSumback.repository.SummaryRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class QuizServiceImpl implements QuizService {

    private final SummaryService summaryService;
    private final SummaryRepository summaryRepository;
    private final QuizRepository quizRepository;

    @Override
    @Transactional
    public List<Quiz> generateFromSummary(QuizRequest request) {
        // 1. AI 요약으로부터 퀴즈 생성
        String aiResponse = summaryService.callOpenAISummary(request.getSummaryText());
        System.out.println(">>>> AI Response:\n" + aiResponse);


        // 2. 파싱
        List<QuizParsedResult> parsedList = parseQuizFromAiResponse(aiResponse);

        // 3. summary 가져오기
        Summary summary = summaryRepository.findById(request.getSummaryId())
                .orElseThrow(() -> new RuntimeException("Summary not found"));


        // 4. Quiz 엔티티 구성
        Quiz quiz = new Quiz();
        quiz.setSummary(summary);
        quiz.setTitle("AI 자동 생성 퀴즈");
        quiz.setCreatedAt(LocalDateTime.now());

        List<Question> questionList = new ArrayList<>();
        for (QuizParsedResult parsed : parsedList) {
            Question q = new Question();
            q.setQuiz(quiz);
            q.setQuestionText(parsed.getQuestion());
            q.setLanguageCode("ko");

            List<AnswerOption> options = new ArrayList<>();
            for (int i = 0; i < parsed.getOptions().size(); i++) {
                AnswerOption option = new AnswerOption();
                option.setQuestion(q);
                option.setOptionText(parsed.getOptions().get(i));
                option.setIsCorrect((i + 1) == parsed.getAnswerIndex());
                options.add(option);
            }
            q.setOptions(options);
            questionList.add(q);
        }
        quiz.setQuestions(questionList);

        quizRepository.save(quiz);
        return List.of(quiz);

    }

    private List<QuizParsedResult> parseQuizFromAiResponse(String aiResponse) {
        List<QuizParsedResult> results = new ArrayList<>();
        String[] blocks = aiResponse.split("\\n\\n|\\r\\n\\r\\n"); // 윈도우/유닉스 줄바꿈 모두 대응

        for (String block : blocks) {
            System.out.println("Parsing block:\n" + block);
            String[] lines = block.strip().split("\\n|\\r\\n");
            String question = null;
            List<String> options = new ArrayList<>();
            int answerIndex = -1;

            // 문제, 보기, 정답 줄을 동적으로 탐색
            for (String line : lines) {
                if (line.trim().startsWith("Q:")) {
                    question = line.trim().substring(2).trim();
                } else if (line.trim().matches("^\\d+\\..*")) {
                    // "1. 보기" 형식
                    String opt = line.trim().substring(2).trim();
                    options.add(opt);
                } else if (line.trim().startsWith("정답:")) {
                    try {
                        answerIndex = Integer.parseInt(line.trim().replaceAll("[^0-9]", ""));
                    } catch (Exception e) {
                        answerIndex = -1;
                    }
                }
            }

            if (question != null && options.size() == 4 && answerIndex >= 1 && answerIndex <= 4) {
                QuizParsedResult result = new QuizParsedResult();
                result.setQuestion(question);
                result.setOptions(options);
                result.setAnswerIndex(answerIndex);
                results.add(result);
            } else {
                System.out.println("Skipping block due to format mismatch");
            }
        }

        System.out.println("Parsed quiz count: " + results.size());
        return results;
    }



    // 내부 DTO 클래스
    public static class QuizParsedResult {
        private String question;
        private List<String> options;
        private int answerIndex;

        public String getQuestion() { return question; }
        public void setQuestion(String question) { this.question = question; }

        public List<String> getOptions() { return options; }
        public void setOptions(List<String> options) { this.options = options; }

        public int getAnswerIndex() { return answerIndex; }
        public void setAnswerIndex(int answerIndex) { this.answerIndex = answerIndex; }
    }
}
