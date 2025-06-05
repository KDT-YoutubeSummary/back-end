package com.kdt.yts.YouSumback.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class User {
    @Id
    @Column(name = "user_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer userId; // 사용자 식별자 (PK)

    @Column(name = "username", length = 100, nullable = false, unique = true)
    private String username; // 사용자명

    @Column(name = "email", length = 255, nullable = false, unique = true)
    private String email; // 이메일

    @Column(name = "password_hash", length = 255, nullable = false)
    private String passwordHash; // 해시된 비밀번호

    @Column(name = "created_at", nullable = false)
    private java.sql.Timestamp createdAt; // 가입 일시

}



