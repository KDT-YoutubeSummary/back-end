package com.YouSumback.service;

import com.YouSumback.model.dto.request.QuizRequest;
import com.YouSumback.model.entity.*;
import com.YouSumback.repository.QuizRepository;
import com.YouSumback.repository.SummaryRepository;
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

        // 2. 파싱
        List<QuizParsedResult> parsedList = parseQuizFromAiResponse(aiResponse);

        // 3. summary 가져오기
        Summary summary = summaryRepository.findById(request.getTranscriptId().intValue())
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
        String[] blocks = aiResponse.split("\\n\\n");

        for (String block : blocks) {
            String[] lines = block.strip().split("\\n");
            if (lines.length < 6 || !lines[0].startsWith("Q:")) continue;

            String question = lines[0].substring(2).trim();
            List<String> options = new ArrayList<>();
            for (int i = 1; i <= 4; i++) {
                String opt = lines[i].substring(2).trim();
                options.add(opt);
            }
            int answerIndex = Integer.parseInt(lines[5].replace("정답:", "").trim());

            QuizParsedResult result = new QuizParsedResult();
            result.setQuestion(question);
            result.setOptions(options);
            result.setAnswerIndex(answerIndex);
            results.add(result);
        }

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
