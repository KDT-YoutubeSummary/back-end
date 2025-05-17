package DTO;

import java.time.LocalDateTime;

public class UserDTO {

    // 필드
    private String id; // 유저 아이디
    private String userName; // 유저 이름
    private String email; // 이메일
    private String passwordHash; // 비밀번호
    private LocalDateTime createdAt; // 생성 시간

    // 생성자
    public UserDTO() {}

    // getter
    public String getId() {
        return id;
    }
    public String getUserName() {
        return userName;
    }
    public String getEmail() {
        return email;
    }
    public String getPasswordHash() {
        return passwordHash;
    }
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    // setter
    public void setId(String id) {
        this.id = id;
    }
    public void setUserName(String userName) {
        this.userName = userName;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
