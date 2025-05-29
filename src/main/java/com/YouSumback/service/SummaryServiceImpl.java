package com.YouSumback.service;

import com.YouSumback.model.dto.request.QuizRequest;
import com.YouSumback.model.dto.request.SummaryRequest;
import com.YouSumback.model.dto.response.SummaryResponse;
import com.YouSumback.model.entity.AnswerOption;
import com.YouSumback.model.entity.Quiz;
import com.YouSumback.repository.AnswerOptionRepository;
import com.theokanning.openai.completion.CompletionRequest;
import com.theokanning.openai.service.OpenAiService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SummaryServiceImpl implements SummaryService {

    private final OpenAiService openAiService;
    private final AnswerOptionRepository answerOptionRepository;

    @Override
    public SummaryResponse summarize(SummaryRequest request) {
        String text = request.getText();
        Long transcriptId = request.getTranscriptId();

        List<String> chunks = splitTextIntoChunks(text, 1000);
        List<String> partialSummaries = new ArrayList<>();
        for (String chunk : chunks) {
            partialSummaries.add(callOpenAISummary(chunk));
        }

        String finalSummary = callOpenAISummary(String.join("\n", partialSummaries));

        AnswerOption option = AnswerOption.builder()
                .transcriptId(transcriptId)
                .summaryText(finalSummary)
                .summaryType("default")
                .createdAt(LocalDateTime.now())
                .build();

        AnswerOption saved = answerOptionRepository.save(option);

        return new SummaryResponse(saved.getAnswerId(), finalSummary);
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
        CompletionRequest completionRequest = CompletionRequest.builder()
                .model("gpt-3.5-turbo")
                .prompt("다음 내용을 바탕으로 요약해줘:\n" + text)
                .maxTokens(500)
                .temperature(0.7)
                .build();

        return openAiService.createCompletion(completionRequest)
                .getChoices().get(0).getText().trim();
    }

    @Override
    public List<Quiz> generateFromSummary(QuizRequest request) {
        String prompt = "다음 내용을 바탕으로 객관식 퀴즈를 " + request.getNumberOfQuestions() + "개 만들어줘.\n"
                + "문제 형식은 다음과 같아:\n"
                + "Q: ...\n1. ...\n2. ...\n3. ...\n4. ...\n정답: ...\n\n"
                + request.getSummaryText();

        CompletionRequest completionRequest = CompletionRequest.builder()
                .model("gpt-3.5-turbo")
                .prompt(prompt)
                .maxTokens(300)
                .temperature(0.7)
                .build();

        String aiResponse = openAiService.createCompletion(completionRequest)
                .getChoices()
                .get(0)
                .getText()
                .trim();

        // TODO: aiResponse 파싱해서 Quiz 리스트로 반환
        return new ArrayList<>();
    }
}
