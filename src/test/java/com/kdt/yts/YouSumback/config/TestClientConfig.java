package com.kdt.yts.YouSumback.config;

import com.kdt.yts.YouSumback.service.client.OpenAIClient;
import com.kdt.yts.YouSumback.service.client.YouTubeClient;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import reactor.core.publisher.Mono;

import java.util.List;

@TestConfiguration
public class TestClientConfig {

    @Bean
    public OpenAIClient mockOpenAIClient() {
        return new OpenAIClient(new DummyOpenAIConfig()) {
            @Override
            public Mono<String> chat(String prompt) {
                return Mono.just("이것은 요약 결과입니다.");
            }
        };
    }

    // Dummy Config 객체
    static class DummyOpenAIConfig extends OpenAIConfig {
        @Override public String getBaseUrl() { return "http://localhost:1234"; }
        @Override public String getApiKey() { return "dummy-key"; }
        @Override public String getModel() { return "gpt-4"; }
    }

    @Bean
    public YouTubeClient mockYouTubeClient() {
        return new YouTubeClient("mock-key") {
            @Override
            public java.util.List<com.google.api.services.youtube.model.Video> searchVideosByKeyword(String keyword, long maxResults) {
                com.google.api.services.youtube.model.Video video1 = new com.google.api.services.youtube.model.Video();
                com.google.api.services.youtube.model.VideoSnippet snippet1 = new com.google.api.services.youtube.model.VideoSnippet();
                snippet1.setTitle("추천 비디오 1");
                snippet1.setDescription("설명1");
                com.google.api.services.youtube.model.Video id1 = new com.google.api.services.youtube.model.Video();
                id1.setId("video1");
                video1.setSnippet(snippet1);
                video1.setId("video1");

                com.google.api.services.youtube.model.Video video2 = new com.google.api.services.youtube.model.Video();
                com.google.api.services.youtube.model.VideoSnippet snippet2 = new com.google.api.services.youtube.model.VideoSnippet();
                snippet2.setTitle("추천 비디오 2");
                snippet2.setDescription("설명2");
                com.google.api.services.youtube.model.Video id2 = new com.google.api.services.youtube.model.Video();
                id2.setId("video2");
                video2.setSnippet(snippet2);
                video2.setId("video2");

                return List.of(video1, video2);
            }

            @Override
            public com.google.api.services.youtube.model.Video fetchVideoById(String youtubeVideoId) {
                com.google.api.services.youtube.model.Video video = new com.google.api.services.youtube.model.Video();
                com.google.api.services.youtube.model.VideoSnippet snippet = new com.google.api.services.youtube.model.VideoSnippet();
                snippet.setTitle("추천 비디오 1");
                snippet.setDescription("설명");
                video.setId(youtubeVideoId);
                video.setSnippet(snippet);
                return video;
            }
        };
    }
}
