package com.YouSumback.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
public class User {
    @Id
    @Column(name = "id", nullable = false)
    private int id; // 사용자 식별자

    @Column(name = "username", length = 100, nullable = false)
    private String username; // 사용자명

    @Column(name = "email", length = 255, nullable = false)
    private String email; // 이메일

    @Column(name = "password_hash", length = 255, nullable = false)
    private String passwordHash; // 해시된 비밀번호

    @Column(name = "create_at", nullable = false)
    private LocalDateTime createAt; // 가입 일시

}
