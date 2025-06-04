package com.kdt.yts.YouSumback.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
@Table(name ="user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id", nullable = false)
    private Long userId; // 사용자 식별자

    @Column(name = "username", length = 100, nullable = false)
    private String userName; // 사용자명

    @Column(name = "email", length = 255, nullable = false)
    private String email; // 이메일

    @Column(name = "password_hash", length = 255, nullable = false)
    private String passwordHash; // 해시된 비밀번호

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt; // 가입 일시

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
