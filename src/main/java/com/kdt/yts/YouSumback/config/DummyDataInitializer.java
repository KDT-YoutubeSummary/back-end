package com.kdt.yts.YouSumback.config;

import com.kdt.yts.YouSumback.model.entity.*;
import com.kdt.yts.YouSumback.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class DummyDataInitializer {

    private final UserRepository userRepository;
    private final VideoRepository videoRepository;
    private final AudioTranscriptRepository transcriptRepository;
    private final SummaryRepository summaryRepository;
    private final TagRepository tagRepository;
    private final UserLibraryRepository userLibraryRepository;
    private final UserLibraryTagRepository userLibraryTagRepository;

    @Bean
    public CommandLineRunner initData() {
        return args -> {
            // 1. User
            User user = new User();
            user.setUserName("testuser");
            user.setEmail("test@example.com");
            user.setPasswordHash("hashed1234");
            user.setCreatedAt(LocalDateTime.now());
            user = userRepository.save(user);
            System.out.println("[✅ User 저장됨] ID: " + user.getUserId());

            // 2. Video
            Video video = new Video();
            video.setVideoId("vid123");
            video.setYoutubeId("abc123test");
            video.setTitle("AI 시대의 시작");
            video.setOriginalUrl("https://youtube.com/watch?v=abc123test");
            video.setUploaderName("TechWorld");
            video.setThumbnailUrl("https://img.youtube.com/vi/abc123test/0.jpg");
            video.setViewCount(987654L);
            video.setPublishedAt(LocalDateTime.of(2024, 1, 1, 0, 0));
            video = videoRepository.save(video);
            System.out.println("[✅ Video 저장됨] ID: " + video.getVideoId());

            // 3. AudioTranscript
            AudioTranscript transcript = new AudioTranscript();
            transcript.setVideo(video);
            transcript.setTranscriptText("AI is transforming the world.");
            transcript.setCreatedAt(LocalDateTime.now());
            transcript = transcriptRepository.save(transcript);
            System.out.println("[✅ Transcript 저장됨] ID: " + transcript.getTranscriptId());

            // 4. Summary
            Summary summary = new Summary();
            summary.setUser(user);
            summary.setTranscript(transcript);
            summary.setSummaryText("이 영상은 인공지능의 시대를 소개합니다.");
            summary.setUserPrompt("간단한 요약 부탁해");
            summary.setLanguageCode("ko");
            summary.setSummaryType("basic");
            summary.setCreatedAt(LocalDateTime.now());
            summary = summaryRepository.save(summary);
            System.out.println("[✅ Summary 저장됨] ID: " + summary.getSummaryId());

            // 5. Tags
            Tag tag1 = tagRepository.save(new Tag("AI"));
            Tag tag2 = tagRepository.save(new Tag("기술"));
            Tag tag3 = tagRepository.save(new Tag("트렌드"));
            System.out.println("[✅ Tags 저장됨] IDs: " + tag1.getTagId() + ", " + tag2.getTagId() + ", " + tag3.getTagId());

             // 6. UserLibrary
            UserLibrary library = new UserLibrary();
            library.setUser(user);
            library.setSummary(summary);
            library.setSavedAt(LocalDateTime.now());
            library.setUserNotes("학습용 영상입니다");
            library.setLastViewedAt(LocalDateTime.now());
            library = userLibraryRepository.save(library);
            System.out.println("[✅ UserLibrary 저장됨] ID: " + library.getUserLibraryId());

            // 7. UserLibraryTag
            userLibraryTagRepository.saveAll(List.of(
                    new UserLibraryTag(library, tag1),
                    new UserLibraryTag(library, tag2),
                    new UserLibraryTag(library, tag3)
            ));
            System.out.println("[✅ UserLibraryTag 저장됨] -> 라이브러리 ID: " + library.getUserLibraryId());
        };
    }
}