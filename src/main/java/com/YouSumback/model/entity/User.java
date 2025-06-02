package com.YouSumback.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "`user`") // 테이블 이름이 'user'이므로 백틱(``)으로 감싸줍니다. (SQL 키워드와 겹칠 수 있으므로)
@Getter // 모든 필드에 대한 getter를 자동으로 생성합니다.
@NoArgsConstructor // 기본 생성자
@AllArgsConstructor // 모든 필드를 인자로 받는 생성자

public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long Id; // 리마인더에서 참조하는 user_id

    @Column(name = "username", length = 100, nullable = false, unique = true)
    private String username; // 사용자명

    @Column(name = "email", length = 255, nullable = false, unique = true)
    private String email; // 이메일

    @Column(name = "password_hash", length = 255, nullable = false)
    private String passwordHash; // 해시된 비밀번호

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createAt; // 가입 일시

}
