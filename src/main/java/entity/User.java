package entity;

import jakarta.persistence.*;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.time.LocalDateTime;

@Entity
public class User {

    @Id
    @Column(name="user_id", length = 20, nullable = false)
    private String id; // 유저 아이디

    @Column(name = "username", length = 100, nullable = false)
    private String userName; // 유저 이름

    @Column(name = "email", length = 255, nullable = false)
    private String email; // 이메일

    @Column(name = "password_hash", length = 255, nullable = false)
    private String passwordHash; // 비밀번호

    @Column(name = "create_at", nullable = false)
    private LocalDateTime createdAt; // 생성 시간

}
