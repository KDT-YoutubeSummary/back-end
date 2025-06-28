package com.kdt.yts.YouSumback.service;

import com.kdt.yts.YouSumback.model.dto.request.UserAnswerDTO;
import com.kdt.yts.YouSumback.model.dto.response.*;
import com.kdt.yts.YouSumback.model.entity.*;
import com.kdt.yts.YouSumback.repository.*;
import com.kdt.yts.YouSumback.model.dto.request.QuizRequestDTO;
import com.kdt.yts.YouSumback.model.dto.request.SummaryRequestDTO;
import com.kdt.yts.YouSumback.service.client.OpenAIClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
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
        log.info("✅ S3에서 파일 읽기 시도. Key: {}", s3Key);
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket("yousum-s3")
                    .key(s3Key)
                    .build();

            ResponseBytes<GetObjectResponse> objectBytes = s3Client.getObjectAsBytes(getObjectRequest);
            byte[] data = objectBytes.asByteArray();
            log.info("✅ S3 파일 읽기 성공. 파일 크기: {} bytes", data.length);
            return new String(data, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("❌ S3 파일 읽기 중 심각한 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to read file from S3: " + s3Key, e);
        }
    }

    @Override
    @Transactional
    public SummaryResponseDTO summarize(SummaryRequestDTO request, Long userId) {
        String originalUrl = request.getOriginalUrl();
        String userPrompt = request.getUserPrompt();
        SummaryType summaryType = request.getSummaryType();

        // [수정 1] URL에서 '&' 이후의 불필요한 파라미터를 모두 제거합니다.
        String cleanUrl = originalUrl.split("&")[0];
        log.info(">>> SummaryServiceImpl.summarize 진입 - 원본 URL: {}, 정리된 URL: {}, User ID: {}", originalUrl, cleanUrl, userId);

        // [수정 2] 정리된 URL을 사용하여 데이터베이스를 조회합니다.
        AudioTranscript transcript = audioTranscriptRepository.findByVideo_OriginalUrl(cleanUrl)
                .orElseThrow(() -> new RuntimeException("AudioTranscript not found for URL: " + cleanUrl));

        if (transcript.getTranscriptPath() == null || transcript.getTranscriptPath().isEmpty()) {
            log.error("❌ AudioTranscript에 파일 경로가 없습니다: {}", cleanUrl);
            throw new RuntimeException("No transcript file path found for URL: " + cleanUrl + ". Summary failed.");
        }

        String text = readTextFromS3(transcript.getTranscriptPath());
        log.info("✅ Transcript text loaded from S3. ID: {}", transcript.getId());

        PromptBuilder promptBuilder = new PromptBuilder();
        String prompt = promptBuilder.buildPrompt(userPrompt, summaryType);

        String finalSummary;
        if (summaryType == SummaryType.TIMELINE) {
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
        log.info("✅ 최종 요약 생성 완료. 길이: {}", finalSummary.length());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found for ID: " + userId));

        Summary summary = Summary.builder()
                .user(user)
                .audioTranscript(transcript)
                .summaryText(finalSummary)
                .summaryType(summaryType)
                .userPrompt(userPrompt)
                .createdAt(LocalDateTime.now())
                .languageCode(transcript.getVideo().getOriginalLanguageCode() != null ? transcript.getVideo().getOriginalLanguageCode() : "ko")
                .build();
        Summary saved = summaryRepository.save(summary);
        log.info("✅ 요약 저장 완료. ID: {}", saved.getId());

        // [수정 3] Repository 메서드 이름을 findByUserIdAndSummaryId로 수정
        SummaryArchive archive = summaryArchiveRepository.findByUserIdAndSummaryId(user.getId(), saved.getId())
                .orElseGet(() -> SummaryArchive.builder().user(user).summary(saved).build());

        archive.setLastViewedAt(LocalDateTime.now());
        summaryArchiveRepository.save(archive);
        log.info("✅ 요약 아카이브 저장/업데이트 완료. User ID: {}, Summary ID: {}", user.getId(), saved.getId());

        List<String> hashtags = extractTagsWithLLM(finalSummary).stream().distinct().toList();
        log.info("✅ 해시태그 추출 완료: {}", hashtags);

        for (String keyword : hashtags) {
            Tag tag = findOrCreateTag(keyword);
            SummaryArchiveTagId summaryArchiveTagId = new SummaryArchiveTagId(archive.getId(), tag.getId());
            if (!summaryArchiveTagRepository.existsById(summaryArchiveTagId)) {
                SummaryArchiveTag summaryArchiveTag = SummaryArchiveTag.builder()
                        .id(summaryArchiveTagId)
                        .summaryArchive(archive)
                        .tag(tag)
                        .build();
                summaryArchiveTagRepository.save(summaryArchiveTag);
            }
        }
        log.info("✅ 태그 처리 완료.");

        // [수정 4] UserActivityLog 저장 로직을 원본에 맞게 수정
        UserActivityLog activityLog = UserActivityLog.builder()
                .user(user)
                .activityType("SUMMARY_CREATED")
                .targetEntityType("SUMMARY")
                .targetEntityIdInt(saved.getId())
                .activityDetail("요약 생성 완료: " + summaryType)
                .createdAt(LocalDateTime.now())
                .build();
        userActivityLogRepository.save(activityLog);
        log.info("✅ 사용자 활동 로그 저장 완료.");

        // [수정 5] SummaryResponseDTO 생성 방식을 빌더 대신 원본의 생성자로 수정
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

    public static class PromptBuilder {
        public String buildPrompt(String userPrompt, SummaryType summaryType) {
            String prompt = (userPrompt != null && !userPrompt.isEmpty()) ? userPrompt + "\n" : "";
            String formatInstruction = "다음 텍스트를 요약해줘: ";
            // Simplified prompt logic from original for compatibility
            return prompt + formatInstruction;
        }
    }

    private List<String> extractTagsWithLLM(String summaryText) {
        String prompt = "다음 요약문에서 키워드 태그 3개를 쉼표로 구분해서 추출해줘. 예: 주식, 경제, 금리\n\n" + summaryText;
        String response = callOpenAISummary(prompt);
        return Arrays.stream(response.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    @Override
    public Optional<SummaryArchive> findSummaryArchiveByUserAndSummary(Long userId, Summary summary) {
        // [수정 6] Repository 메서드 이름을 findByUserIdAndSummaryId로 수정
        return summaryArchiveRepository.findByUserIdAndSummaryId(userId, summary.getId());
    }

    // --- 미사용/원본에 없는 메서드 처리 ---
    @Override
    public List<QuizResponseDTO> generateFromSummary(QuizRequestDTO request) {
        throw new UnsupportedOperationException("Not implemented.");
    }
    @Override
    public QuizResultResponseDTO checkQuizAnswers(Long quizId, List<UserAnswerDTO> userAnswers) {
        throw new UnsupportedOperationException("Not implemented.");
    }
    @Override
    public List<QuestionWithOptionsResponseDTO> getQuestionsFromUserAnswers(List<UserAnswerDTO> answers) {
        throw new UnsupportedOperationException("Not implemented.");
    }

    @Override
    public String callOpenAISummary(String fullPrompt) {
        return openAIClient.chat(fullPrompt).block();
    }

    private List<String> splitTextIntoChunks(String text, int chunkSizeInWords) {
        List<String> chunks = new ArrayList<>();
        String[] words = text.split("\\s+");
        int wordCount = words.length;

        for (int i = 0; i < wordCount; i += chunkSizeInWords) {
            int end = Math.min(i + chunkSizeInWords, wordCount);
            chunks.add(String.join(" ", Arrays.copyOfRange(words, i, end)));
        }
        return chunks;
    }

    private synchronized Tag findOrCreateTag(String tagName) {
        // [수정 7] Repository 메서드 이름과 Entity 필드 이름을 원본에 맞게 수정
        return tagRepository.findByTagName(tagName)
                .orElseGet(() -> tagRepository.save(Tag.builder().tagName(tagName).build()));
    }
}
