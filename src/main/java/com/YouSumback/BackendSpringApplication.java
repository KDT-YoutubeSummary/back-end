package com.YouSumback;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling; // 스케줄링 활성화


@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling // <-- 이 어노테이션을 추가해야 ReminderService의 @Scheduled 메서드가 동작합니다.
public class BackendSpringApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackendSpringApplication.class, args);
    }
}
