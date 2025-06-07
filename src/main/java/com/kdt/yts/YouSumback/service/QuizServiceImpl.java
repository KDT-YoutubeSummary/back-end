package com.kdt.yts.YouSumback.service;

import com.kdt.yts.YouSumback.model.dto.request.QuizRequestDTO;
import com.kdt.yts.YouSumback.model.dto.response.AnswerOptionDTO;
import com.kdt.yts.YouSumback.model.dto.response.QuestionDTO;
import com.kdt.yts.YouSumback.model.dto.response.QuizResponseDTO;
import com.kdt.yts.YouSumback.model.entity.AnswerOption;
import com.kdt.yts.YouSumback.model.entity.Question;
import com.kdt.yts.YouSumback.model.entity.Quiz;
import com.kdt.yts.YouSumback.model.entity.Summary;
import com.kdt.yts.YouSumback.repository.AnswerOptionRepository;
import com.kdt.yts.YouSumback.repository.QuestionRepository;
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
    private final AnswerOptionRepository answerOptionRepository;
    private final QuestionRepository questionRepository;

    @Override
    @Transactional
    public List<QuizResponseDTO> generateFromSummary(QuizRequestDTO request) {
        // 1. summary 가져오기
        Summary summary = summaryRepository.findById(request.getTranscriptId())
                .orElseThrow(() -> new RuntimeException("Summary not found"));

        // 2. 요약 텍스트로 퀴즈 생성 (LLM 호출)
        String aiResponse = summaryService.callOpenAISummary(summary.getSummaryText());

        // 3. 파싱
        List<QuizParsedResult> parsedList = parseQuizFromAiResponse(aiResponse);

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

        // DTO로 변환
        List<QuestionDTO> questionDTOs = quiz.getQuestions().stream()
                .map(q -> new QuestionDTO(
                        q.getQuestionText(),
                        q.getOptions().stream()
                                .map(opt -> new AnswerOptionDTO(opt.getOptionText(), opt.getIsCorrect()))
                                .toList()
                ))
                .toList();

        return List.of(new QuizResponseDTO(
                quiz.getTitle(),
                quiz.getCreatedAt(),
                questionDTOs
        ));
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
                options.add(lines[i].substring(2).trim());
            }
            int answerIndex = Integer.parseInt(lines[5].replace("정답:", "").trim());

            String explanation = "";
            if (lines.length >= 7 && lines[6].startsWith("해설:")) {
                explanation = lines[6].substring(3).trim();
            }

            QuizParsedResult result = new QuizParsedResult();
            result.setQuestion(question);
            result.setOptions(options);
            result.setAnswerIndex(answerIndex);
            result.setExplanation(explanation);
            results.add(result);
        }

        return results;
    }

    // 퀴즈 답안 확인
    public boolean checkAnswer(Long questionId, Long selectedOptionId) {
        AnswerOption selected = answerOptionRepository.findById(selectedOptionId)
                .orElseThrow(() -> new RuntimeException("선택한 보기 없음"));

        if (!selected.getQuestion().getId().equals(questionId)) {
            throw new RuntimeException("질문과 보기가 일치하지 않음");
        }

        return selected.getIsCorrect();
    }

    public String getExplanation(Long questionId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("질문 없음"));
        return question.getExplanation();
    }

    //  퀴즈 조회
    @Override
    @Transactional
    public QuizResponseDTO getQuizBySummaryId(Long summaryId) {
        Quiz quiz = quizRepository.findBySummaryId(summaryId)
                .orElseThrow(() -> new RuntimeException("해당 summaryId에 대한 퀴즈가 없습니다."));

        List<QuestionDTO> questionDTOs = quiz.getQuestions().stream()
                .map(q -> new QuestionDTO(
                        q.getQuestionText(),
                        q.getOptions().stream()
                                .map(opt -> new AnswerOptionDTO(opt.getOptionText(), opt.getIsCorrect()))
                                .toList()
                ))
                .toList();

        return new QuizResponseDTO(
                quiz.getTitle(),
                quiz.getCreatedAt(),
                questionDTOs
        );
    }

    // 모든 퀴즈 조회
    @Override
    public List<QuizResponseDTO> getAllQuizzes() {
        return quizRepository.findAll().stream().map(quiz -> {
            List<QuestionDTO> questions = quiz.getQuestions().stream()
                    .map(q -> new QuestionDTO(
                            q.getQuestionText(),
                            q.getOptions().stream()
                                    .map(opt -> new AnswerOptionDTO(opt.getOptionText(), opt.getIsCorrect()))
                                    .toList()
                    ))
                    .toList();
            return new QuizResponseDTO(quiz.getTitle(), quiz.getCreatedAt(), questions);
        }).toList();
    }


    // 퀴즈 삭제
    @Transactional
    @Override
    public void deleteQuizBySummaryId(Long summaryId) {
        Quiz quiz = quizRepository.findBySummaryId(summaryId)
                .orElseThrow(() -> new RuntimeException("퀴즈 없음"));
        quizRepository.delete(quiz);
    }


    // 내부 DTO 클래스
    public static class QuizParsedResult {
        private String question;
        private List<String> options;
        private int answerIndex;
        private String explanation;

        public String getQuestion() {
            return question;
        }

        public void setQuestion(String question) {
            this.question = question;
        }

        public List<String> getOptions() {
            return options;
        }

        public void setOptions(List<String> options) {
            this.options = options;
        }

        public int getAnswerIndex() {
            return answerIndex;
        }

        public void setAnswerIndex(int answerIndex) {
            this.answerIndex = answerIndex;
        }

        public String getExplanation() {
            return explanation;
        }

        public void setExplanation(String explanation) {
            this.explanation = explanation;
        }
    }
}
