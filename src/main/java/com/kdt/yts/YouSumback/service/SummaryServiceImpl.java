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

        String cleanUrl = originalUrl.split("&")[0];
        log.info(">>> SummaryServiceImpl.summarize 진입 - 원본 URL: {}, 정리된 URL: {}, User ID: {}", originalUrl, cleanUrl, userId);

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
                try {
                    // 429 Too Many Requests 방지를 위한 1초 대기
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("API 호출 간 대기 중 오류 발생", e);
                    throw new RuntimeException("API call delay was interrupted", e);
                }
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

        // [오류 수정] findByUser_UserIdAndSummary_Id -> findByUserIdAndSummaryId로 수정
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

        // [오류 수정] .summaryArchive(archive) 대신 올바른 필드를 사용하도록 수정
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

        // [오류 수정] DTO 생성 시 .hashtags() 대신 .tags()를 사용
        return SummaryResponseDTO.builder()
                .summaryId(saved.getId())
                .transcriptId(transcript.getId())
                .videoId(transcript.getVideo().getId())
                .summary(finalSummary)
                .tags(hashtags) // .hashtags -> .tags
                .title(transcript.getVideo().getTitle())
                .thumbnailUrl(transcript.getVideo().getThumbnailUrl())
                .uploaderName(transcript.getVideo().getUploaderName())
                .viewCount(transcript.getVideo().getViewCount())
                .languageCode(transcript.getVideo().getOriginalLanguageCode())
                .createdAt(summary.getCreatedAt())
                .build();
    }

    public static class PromptBuilder {
        public String buildPrompt(String userPrompt, SummaryType summaryType) {
            String prompt = (userPrompt != null && !userPrompt.isEmpty()) ? userPrompt + "\n" : "";
            // Simplified prompt logic
            return prompt + "다음 텍스트를 요약해줘: ";
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
        // [오류 수정] findByUser_UserIdAndSummary_Id -> findByUserIdAndSummaryId
        return summaryArchiveRepository.findByUserIdAndSummaryId(userId, summary.getId());
    }

    // ... (generateFromSummary, checkQuizAnswers 등 나머지 메서드는 이전과 동일하게 유지)
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
        for (int i = 0; i < words.length; i += chunkSizeInWords) {
            int end = Math.min(i + chunkSizeInWords, words.length);
            chunks.add(String.join(" ", Arrays.copyOfRange(words, i, end)));
        }
        return chunks;
    }
    private synchronized Tag findOrCreateTag(String tagName) {
        // [오류 수정] findByName -> findByTagName, .name -> .tagName
        return tagRepository.findByTagName(tagName)
                .orElseGet(() -> tagRepository.save(Tag.builder().tagName(tagName).build()));
    }
}
