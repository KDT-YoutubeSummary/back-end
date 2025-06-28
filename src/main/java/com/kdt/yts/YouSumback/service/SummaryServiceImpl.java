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
import software.amazon.awssdk.core.ResponseInputStream;
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
        
        // ✅ s3Key null 체크 추가
        if (s3Key == null || s3Key.trim().isEmpty()) {
            log.error("❌ S3 Key가 null이거나 비어있습니다.");
            throw new IllegalArgumentException("S3 Key cannot be null or empty");
        }
        
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
        String prompt;
        if (summaryType == SummaryType.TIMELINE) {
            // TIMELINE 타입일 때는 VTT 텍스트를 전달하여 동적 타임라인 생성
            prompt = promptBuilder.buildPromptWithDuration(userPrompt, summaryType, text);
        } else {
            prompt = promptBuilder.buildPrompt(userPrompt, summaryType);
        }

        // ✅ 프롬프트 디버깅 로그 추가
        System.out.println("====================");
        System.out.println("🔍 요약 타입: " + summaryType);
        System.out.println("🔍 사용자 프롬프트: " + userPrompt);
        System.out.println("====================");
        System.out.println("📝 생성된 전체 프롬프트:");
        System.out.println(prompt);
        System.out.println("====================");

        String finalSummary;
        if (summaryType == SummaryType.TIMELINE || summaryType == SummaryType.KEYWORD) {
            System.out.println("✅ " + summaryType + " summary: Bypassing chunking and calling AI with full content.");
            String fullPromptForAI = prompt + "\n\n" + text;
            System.out.println("🤖 AI에게 전송할 최종 프롬프트 (첫 500자):");
            System.out.println(fullPromptForAI.substring(0, Math.min(500, fullPromptForAI.length())) + "...");
            finalSummary = callOpenAISummary(fullPromptForAI);
        } else {
            List<String> chunks = splitTextIntoChunks(text, 2000);
            List<String> partialSummaries = new ArrayList<>();

            for (int i = 0; i < chunks.size(); i++) {
                String chunk = chunks.get(i);
                String chunkPrompt = prompt + "\n\n" + chunk;
                System.out.println("🤖 청크 " + (i+1) + "/" + chunks.size() + " AI에게 전송할 프롬프트 (첫 300자):");
                System.out.println(chunkPrompt.substring(0, Math.min(300, chunkPrompt.length())) + "...");
                partialSummaries.add(callOpenAISummary(chunkPrompt));
                
                try {
                    // 429 Too Many Requests 방지를 위한 1초 대기
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("API 호출 간 대기 중 오류 발생", e);
                    throw new RuntimeException("API call delay was interrupted", e);
                }
            }
            
            // 최종 합치기에서도 TIMELINE 타입인 경우 VTT 텍스트 전달
            String finalSummaryPrompt;
            if (summaryType == SummaryType.TIMELINE) {
                finalSummaryPrompt = promptBuilder.buildMergePromptWithDuration(partialSummaries, summaryType, text);
            } else {
                finalSummaryPrompt = promptBuilder.buildMergePrompt(partialSummaries, summaryType);
            }
            System.out.println("🔄 최종 요약 합치기 프롬프트:");
            System.out.println(finalSummaryPrompt.substring(0, Math.min(300, finalSummaryPrompt.length())) + "...");
            finalSummary = callOpenAISummary(finalSummaryPrompt);
        }
        log.info("✅ 최종 요약 생성 완료. 길이: {}", finalSummary.length());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found for ID: " + userId));

        // ✅ 안전한 Video 접근 및 언어 코드 추출
        Video video = transcript.getVideo();
        if (video == null) {
            throw new RuntimeException("Video information is missing for transcript: " + transcript.getId());
        }
        
        Summary summary = Summary.builder()
                .user(user)
                .audioTranscript(transcript)
                .summaryText(finalSummary)
                .summaryType(summaryType)
                .userPrompt(userPrompt)
                .createdAt(LocalDateTime.now())
                .languageCode(video.getOriginalLanguageCode() != null ? video.getOriginalLanguageCode() : "ko")
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
            if (!summaryArchiveTagRepository.existsById(new SummaryArchiveTagId(archive.getId(), tag.getId()))) {
                SummaryArchiveTag summaryArchiveTag = SummaryArchiveTag.builder()
                        .summaryArchive(archive)
                        .tag(tag)
                        .build();
                summaryArchiveTagRepository.save(summaryArchiveTag);
            }
        }
        log.info("✅ 태그 처리 완료.");

        // ✅ 안전한 UserActivityLog 생성 - video 참조 사용
        UserActivityLog activityLog = UserActivityLog.builder()
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
""", summaryType, video.getId(), video.getTitle() != null ? video.getTitle() : "제목 없음"))
                .createdAt(LocalDateTime.now())
                .build();
        userActivityLogRepository.save(activityLog);
        log.info("✅ 사용자 활동 로그 저장 완료.");

        // ✅ 안전한 DTO 생성 - video는 이미 null 체크 완료
        return SummaryResponseDTO.builder()
                .summaryId(saved.getId())
                .transcriptId(transcript.getId())
                .videoId(video.getId())
                .summary(finalSummary)
                .tags(hashtags)
                .title(video.getTitle() != null ? video.getTitle() : "제목 없음")
                .thumbnailUrl(video.getThumbnailUrl() != null ? video.getThumbnailUrl() : "")
                .uploaderName(video.getUploaderName() != null ? video.getUploaderName() : "알 수 없음")
                .viewCount(video.getViewCount() != null ? video.getViewCount() : 0L)
                .languageCode(video.getOriginalLanguageCode() != null ? video.getOriginalLanguageCode() : "ko")
                .createdAt(summary.getCreatedAt())
                .build();
    }

    public static class PromptBuilder {
        public String buildPrompt(String userPrompt, SummaryType summaryType) {
            return buildPromptWithDuration(userPrompt, summaryType, null);
        }

        public String buildPromptWithDuration(String userPrompt, SummaryType summaryType, String vttText) {
            String baseInstruction = "당신은 전문적인 콘텐츠 요약 AI입니다. 제공된 텍스트를 아래 지침에 따라 정확히 요약해주세요.";

            String typeSpecificInstruction;
            switch (summaryType) {
                case BASIC:
                    typeSpecificInstruction = """
                
【기본 요약 지침】
아래 형식을 정확히 지켜서 요약해주세요:

## 핵심 내용
- 첫 번째 주요 포인트 (구체적인 내용)
- 두 번째 주요 포인트 (구체적인 내용)  
- 세 번째 주요 포인트 (구체적인 내용)

## 결론
실무에서 활용 가능한 방법이나 핵심 결론을 제시해주세요.

## 추천 학습
관련 주제나 추가 학습 방향을 제안해주세요.

※ 위 형식을 반드시 지켜주세요.
    """;
                    break;

                case THREE_LINE:
                    typeSpecificInstruction = """
                
【3줄 요약 지침】
반드시 아래 형식으로 정확히 3줄만 작성해주세요:

1. [첫 번째 핵심 내용을 한 줄로 명확히]
2. [두 번째 핵심 내용을 한 줄로 명확히]  
3. [세 번째 핵심 내용 또는 결론을 한 줄로 명확히]

**추가 포인트:**
3줄 요약을 보완하는 중요한 내용이나 실무 적용 팁을 간단히 추가해주세요.

※ 정확히 3줄 형식을 지켜주세요.
    """;
                    break;

                case KEYWORD:
                    typeSpecificInstruction = """
                
【키워드 추출 지침】
아래 형식을 정확히 지켜서 키워드를 추출해주세요:

## 핵심 키워드
1. **키워드1** - 이 키워드가 중요한 이유와 의미
2. **키워드2** - 이 키워드가 중요한 이유와 의미
3. **키워드3** - 이 키워드가 중요한 이유와 의미
4. **키워드4** - 이 키워드가 중요한 이유와 의미
5. **키워드5** - 이 키워드가 중요한 이유와 의미

## 키워드 연관성
5개 키워드들이 어떻게 서로 연결되어 있고, 전체 내용의 맥락에서 어떤 의미를 가지는지 설명해주세요.

※ 정확히 5개의 키워드를 추출해주세요.
    """;
                    break;

                case TIMELINE:
                    if (vttText != null) {
                        int durationSeconds = parseVideoDurationFromVTT(vttText);
                        typeSpecificInstruction = "\n【타임라인 요약 지침】\n영상의 시간 흐름에 따라 아래 형식으로 정리해주세요:\n\n" +
                              generateDynamicTimeline(durationSeconds);
                    } else {
                        typeSpecificInstruction = """
                
【타임라인 요약 지침】
영상의 시간 흐름에 따라 아래 형식으로 정리해주세요:

## 타임라인
**0~5분:** 영상 초반부의 주요 내용과 도입부 핵심 사항
**5~10분:** 영상 중반부의 핵심 내용과 주요 논점  
**10~15분:** 영상 후반부의 중요 내용과 발전된 논의
**15분 이후:** 마무리 내용과 결론 부분

## 핵심 포인트
전체 타임라인에서 가장 중요한 2-3가지 핵심 메시지를 정리해주세요.

※ 시간대별 구분을 명확히 해주세요.
    """;
                    }
                    break;

                default:
                    typeSpecificInstruction = "";
                    break;
            }

            String userRequest = userPrompt != null && !userPrompt.trim().isEmpty()
                ? userPrompt
                : "영상 내용을 요약해주세요";

            return String.format("""
%s

%s

【사용자 요청사항】
%s

【중요 안내】
- 위에 제시된 형식을 반드시 준수해주세요
- 각 섹션의 제목(##, **)을 정확히 사용해주세요
- 불필요한 부연설명은 피하고 핵심 내용만 간결하게 작성해주세요
- 한국어로 자연스럽게 작성해주세요

【요약할 내용】
==========================================
""", baseInstruction, typeSpecificInstruction, userRequest);
        }

        public String buildMergePrompt(List<String> summaries, SummaryType summaryType) {
            return buildMergePromptWithDuration(summaries, summaryType, null);
        }

        public String buildMergePromptWithDuration(List<String> summaries, SummaryType summaryType, String vttText) {
            String baseInstruction = "당신은 전문적인 콘텐츠 요약 AI입니다. 제공된 텍스트를 아래 지침에 따라 정확히 요약해주세요.";

            String typeSpecificInstruction;
            switch (summaryType) {
                case BASIC:
                    typeSpecificInstruction = """
                
【기본 요약 지침】
아래 형식을 정확히 지켜서 요약해주세요:

## 핵심 내용
- 첫 번째 주요 포인트 (구체적인 내용)
- 두 번째 주요 포인트 (구체적인 내용)  
- 세 번째 주요 포인트 (구체적인 내용)

## 결론
실무에서 활용 가능한 방법이나 핵심 결론을 제시해주세요.

## 추천 학습
관련 주제나 추가 학습 방향을 제안해주세요.

※ 위 형식을 반드시 지켜주세요.
    """;
                    break;

                case THREE_LINE:
                    typeSpecificInstruction = """
                
【3줄 요약 지침】
반드시 아래 형식으로 정확히 3줄만 작성해주세요:

1. [첫 번째 핵심 내용을 한 줄로 명확히]
2. [두 번째 핵심 내용을 한 줄로 명확히]  
3. [세 번째 핵심 내용 또는 결론을 한 줄로 명확히]

**추가 포인트:**
3줄 요약을 보완하는 중요한 내용이나 실무 적용 팁을 간단히 추가해주세요.

※ 정확히 3줄 형식을 지켜주세요.
    """;
                    break;

                case KEYWORD:
                    typeSpecificInstruction = """
                
【키워드 추출 지침】
아래 형식을 정확히 지켜서 키워드를 추출해주세요:

## 핵심 키워드
1. **키워드1** - 이 키워드가 중요한 이유와 의미
2. **키워드2** - 이 키워드가 중요한 이유와 의미
3. **키워드3** - 이 키워드가 중요한 이유와 의미
4. **키워드4** - 이 키워드가 중요한 이유와 의미
5. **키워드5** - 이 키워드가 중요한 이유와 의미

## 키워드 연관성
5개 키워드들이 어떻게 서로 연결되어 있고, 전체 내용의 맥락에서 어떤 의미를 가지는지 설명해주세요.

※ 정확히 5개의 키워드를 추출해주세요.
    """;
                    break;

                case TIMELINE:
                    if (vttText != null) {
                        int durationSeconds = parseVideoDurationFromVTT(vttText);
                        typeSpecificInstruction = "\n【타임라인 요약 지침】\n영상의 시간 흐름에 따라 아래 형식으로 정리해주세요:\n\n" +
                              generateDynamicTimeline(durationSeconds);
                    } else {
                        typeSpecificInstruction = """
                
【타임라인 요약 지침】
영상의 시간 흐름에 따라 아래 형식으로 정리해주세요:

## 타임라인
**0~5분:** 영상 초반부의 주요 내용과 도입부 핵심 사항
**5~10분:** 영상 중반부의 핵심 내용과 주요 논점  
**10~15분:** 영상 후반부의 중요 내용과 발전된 논의
**15분 이후:** 마무리 내용과 결론 부분

## 핵심 포인트
전체 타임라인에서 가장 중요한 2-3가지 핵심 메시지를 정리해주세요.

※ 시간대별 구분을 명확히 해주세요.
    """;
                    }
                    break;

                default:
                    typeSpecificInstruction = "";
                    break;
            }

            String userRequest = "다음은 각 부분에 대한 요약입니다. 이 요약들을 하나로 합쳐서 자연스러운 최종 요약을 만들어주세요.";

            return String.format("""
%s

%s

【사용자 요청사항】
%s

【중요 안내】
- 위에 제시된 형식을 반드시 준수해주세요
- 각 섹션의 제목(##, **)을 정확히 사용해주세요
- 불필요한 부연설명은 피하고 핵심 내용만 간결하게 작성해주세요
- 한국어로 자연스럽게 작성해주세요

【합칠 부분별 요약들】
==========================================
%s
""", baseInstruction, typeSpecificInstruction, userRequest, String.join("\n\n---\n\n", summaries));
        }

        /**
         * VTT 자막에서 영상의 총 길이(초)를 파싱합니다.
         */
        private int parseVideoDurationFromVTT(String vttText) {
            // ✅ VTT 텍스트 null 체크 추가
            if (vttText == null || vttText.trim().isEmpty()) {
                System.err.println("⚠️ VTT 텍스트가 null이거나 비어있음. 기본값 5분 반환");
                return 300; // 기본값 5분
            }
            
            try {
                String[] lines = vttText.split("\\r?\\n");
                int maxSeconds = 0;

                for (String line : lines) {
                    // ✅ 라인별 null 체크 추가
                    if (line == null) continue;
                    
                    // 타임스탬프 라인 찾기: "00:01:23.456 --> 00:02:34.567" 형식
                    if (line.contains("-->")) {
                        String[] timeParts = line.split("-->");
                        if (timeParts.length >= 2 && timeParts[1] != null) {
                            String endTime = timeParts[1].trim();
                            int seconds = parseTimeToSeconds(endTime);
                            maxSeconds = Math.max(maxSeconds, seconds);
                        }
                    }
                }

                System.out.println("🕐 VTT에서 파싱된 영상 길이: " + maxSeconds + "초 (" + formatDuration(maxSeconds) + ")");
                return maxSeconds;
            } catch (Exception e) {
                System.err.println("⚠️ VTT 파싱 중 오류: " + e.getMessage());
                return 300; // 기본값 5분
            }
        }

        /**
         * "00:01:23.456" 형식의 시간을 초로 변환합니다.
         */
        private int parseTimeToSeconds(String timeStr) {
            try {
                // "00:01:23.456" -> ["00", "01", "23.456"]
                String[] parts = timeStr.split(":");
                if (parts.length >= 3) {
                    int hours = Integer.parseInt(parts[0]);
                    int minutes = Integer.parseInt(parts[1]);
                    double seconds = Double.parseDouble(parts[2]);
                    return (int) (hours * 3600 + minutes * 60 + seconds);
                }
            } catch (Exception e) {
                System.err.println("⚠️ 시간 파싱 오류: " + timeStr);
            }
            return 0;
        }

        /**
         * 초를 "X분 Y초" 형식으로 포맷팅합니다.
         */
        private String formatDuration(int totalSeconds) {
            int minutes = totalSeconds / 60;
            int seconds = totalSeconds % 60;
            if (minutes > 0) {
                return minutes + "분 " + seconds + "초";
            } else {
                return seconds + "초";
            }
        }

        /**
         * 영상 길이에 따라 동적 타임라인 구간을 생성합니다.
         */
        private String generateDynamicTimeline(int durationSeconds) {
            if (durationSeconds <= 60) {
                // 1분 이하: 2구간
                int mid = durationSeconds / 2;
                return String.format("""
## 타임라인
**0초~%s:** 영상 전반부의 주요 내용과 도입부 핵심 사항
**%s~%s:** 영상 후반부의 핵심 내용과 결론 부분

## 핵심 포인트
전체 타임라인에서 가장 중요한 2-3가지 핵심 메시지를 정리해주세요.

※ 실제 영상 시간에 맞춰 정확히 구분해주세요.""",
                    formatDuration(mid), formatDuration(mid), formatDuration(durationSeconds));

            } else if (durationSeconds <= 180) {
                // 3분 이하: 3구간
                int third = durationSeconds / 3;
                return String.format("""
## 타임라인
**0초~%s:** 영상 초반부의 주요 내용과 도입부
**%s~%s:** 영상 중반부의 핵심 내용과 주요 논점
**%s~%s:** 영상 후반부의 중요 내용과 결론

## 핵심 포인트
전체 타임라인에서 가장 중요한 2-3가지 핵심 메시지를 정리해주세요.

※ 실제 영상 시간에 맞춰 정확히 구분해주세요.""",
                    formatDuration(third), formatDuration(third), formatDuration(third * 2),
                    formatDuration(third * 2), formatDuration(durationSeconds));

            } else if (durationSeconds <= 600) {
                // 10분 이하: 4구간
                int quarter = durationSeconds / 4;
                return String.format("""
## 타임라인
**0초~%s:** 영상 초반부의 주요 내용과 도입부 핵심 사항
**%s~%s:** 영상 전반 중반부의 핵심 내용과 주요 논점
**%s~%s:** 영상 후반 중반부의 중요 내용과 발전된 논의
**%s~%s:** 영상 마무리 부분의 결론과 핵심 정리

## 핵심 포인트
전체 타임라인에서 가장 중요한 2-3가지 핵심 메시지를 정리해주세요.

※ 실제 영상 시간에 맞춰 정확히 구분해주세요.""",
                    formatDuration(quarter), formatDuration(quarter), formatDuration(quarter * 2),
                    formatDuration(quarter * 2), formatDuration(quarter * 3),
                    formatDuration(quarter * 3), formatDuration(durationSeconds));

            } else {
                // 10분 초과: 5구간
                int fifth = durationSeconds / 5;
                return String.format("""
## 타임라인
**0초~%s:** 영상 도입부와 초반 핵심 내용
**%s~%s:** 영상 전반부의 주요 논점과 설명
**%s~%s:** 영상 중반부의 핵심 내용과 발전된 논의
**%s~%s:** 영상 후반부의 중요 내용과 심화 논의
**%s~%s:** 영상 마무리와 결론 부분

## 핵심 포인트
전체 타임라인에서 가장 중요한 2-3가지 핵심 메시지를 정리해주세요.

※ 실제 영상 시간에 맞춰 정확히 구분해주세요.""",
                    formatDuration(fifth), formatDuration(fifth), formatDuration(fifth * 2),
                    formatDuration(fifth * 2), formatDuration(fifth * 3),
                    formatDuration(fifth * 3), formatDuration(fifth * 4),
                    formatDuration(fifth * 4), formatDuration(durationSeconds));
            }
        }
    }

    private List<String> extractTagsWithLLM(String summaryText) {
        // ✅ 입력 매개변수 null 체크
        if (summaryText == null || summaryText.trim().isEmpty()) {
            log.warn("요약 텍스트가 null이거나 비어있어 빈 태그 목록을 반환합니다.");
            return new ArrayList<>();
        }
        
        String prompt = "다음 요약문에서 키워드 태그 3개를 쉼표로 구분해서 추출해줘. 예: 주식, 경제, 금리\n\n" + summaryText;
        String response = callOpenAISummary(prompt);
        
        // ✅ OpenAI 응답 null 체크
        if (response == null || response.trim().isEmpty()) {
            log.warn("OpenAI 응답이 null이거나 비어있어 빈 태그 목록을 반환합니다.");
            return new ArrayList<>();
        }
        
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
        System.out.println("📤 OpenAI API 호출 중...");
        System.out.println("프롬프트 길이: " + fullPrompt.length() + " 문자");

        int maxRetries = 1; // 재시도 1회로 줄임
        String response = null;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                System.out.println("🔄 시도 " + attempt + "/" + maxRetries);
                response = openAIClient.chat(fullPrompt).block();

                System.out.println("📥 OpenAI API 응답 받음!");
                System.out.println("응답 길이: " + (response != null ? response.length() : 0) + " 문자");
                System.out.println("응답 내용 (첫 500자):");
                System.out.println(response != null && response.length() > 500
                    ? response.substring(0, 500) + "..."
                    : response);

                // 응답 품질 검증 (매우 관대하게)
                if (isValidSummaryResponse(response)) {
                    System.out.println("✅ 응답 품질 검증 통과!");
                    break;
                } else {
                    System.out.println("⚠️ 응답 품질이 기준에 미달하지만 1회만 재시도하므로 그대로 사용합니다.");
                    break; // 재시도하지 않고 그대로 사용
                }

            } catch (Exception e) {
                System.err.println("❌ OpenAI API 호출 중 오류 발생 (시도 " + attempt + "/" + maxRetries + "): " + e.getMessage());
                if (attempt == maxRetries) {
                    throw new RuntimeException("OpenAI API 호출 실패", e);
                }
                try {
                    Thread.sleep(2000); // 2초 대기 후 재시도
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("재시도 중 인터럽트 발생", ie);
                }
            }
        }

        System.out.println("=".repeat(50));
        return response;
    }

    private boolean isValidSummaryResponse(String response) {
        if (response == null || response.trim().isEmpty()) {
            return false;
        }

        // 매우 기본적인 검증만 수행
        if (response.length() < 30) {
            System.out.println("🔍 검증 실패: 응답이 너무 짧음 (" + response.length() + "자)");
            return false;
        }

        // 한글이 조금이라도 있는지만 확인
        boolean hasKorean = response.chars()
                .anyMatch(c -> c >= 0xAC00 && c <= 0xD7A3);

        if (!hasKorean) {
            System.out.println("🔍 검증 실패: 한글 내용이 없음");
            return false;
        }

        System.out.println("🔍 응답 검증: 통과 (길이: " + response.length() + "자)");
        return true;
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