package com.YouSumback;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling; // 스케줄링 활성화

@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling // ReminderService의 @Scheduled 메서드가 동작하기 위해 필요
public class BackendSpringApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackendSpringApplication.class, args);
    }
}
