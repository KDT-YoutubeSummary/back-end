package com.YouSumback.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "`user`")
@Getter
@Setter
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id") // 이 필드명이 핵심입니다!
    private Long userId;

    @Column(name = "username", length = 100, nullable = false)
    private String username; // 사용자명

    @Column(name = "email", length = 255, nullable = false)
    private String email; // 이메일

    @Column(name = "password_hash", length = 255, nullable = false)
    private String passwordHash; // 해시된 비밀번호

    @Column(name = "create_at", nullable = false)
    private LocalDateTime createAt; // 가입 일시

}
