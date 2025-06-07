package com.kdt.yts.YouSumback.service.client;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoListResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class YouTubeClient {

    private final String apiKey;
    private final YouTube youtubeService;

    public YouTubeClient(@Value("${youtube.api-key}") String apiKey) {
        this.apiKey = apiKey;
        try {
            this.youtubeService = new YouTube.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    JacksonFactory.getDefaultInstance(),
                    request -> {}
            ).setApplicationName("YouSum-backend").build();
        } catch (Exception e) {
            throw new RuntimeException("YouTube service 생성 중 에러 발생", e);
        }
    }

    public Video fetchVideoById(String youtubeVideoId) throws Exception {
        YouTube.Videos.List request = youtubeService.videos()
                .list("snippet,statistics")
                .setId(youtubeVideoId)
                .setKey(apiKey);

        VideoListResponse response = request.execute();

        if (response.getItems().isEmpty()) return null;
        return response.getItems().get(0);
    }

    public java.util.List<Video> searchVideosByKeyword(String keyword, long maxResults) throws Exception {
        YouTube.Search.List search = youtubeService.search()
                .list("id")
                .setQ(keyword)
                .setType("video")
                .setMaxResults(maxResults)
                .setKey(apiKey);

        com.google.api.services.youtube.model.SearchListResponse searchResponse = search.execute();
        java.util.List<String> videoIds = new java.util.ArrayList<>();
        for (com.google.api.services.youtube.model.SearchResult result : searchResponse.getItems()) {
            videoIds.add(result.getId().getVideoId());
        }
        if (videoIds.isEmpty()) return java.util.Collections.emptyList();
        YouTube.Videos.List videosList = youtubeService.videos()
                .list("snippet,statistics")
                .setId(String.join(",", videoIds))
                .setKey(apiKey);
        VideoListResponse videosResponse = videosList.execute();
        return videosResponse.getItems();
    }
}
