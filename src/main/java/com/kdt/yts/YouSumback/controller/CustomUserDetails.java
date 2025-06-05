package com.kdt.yts.YouSumback.controller;

import com.kdt.yts.YouSumback.model.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

@Getter
// CustomUserDetails 클래스는 UserDetails 인터페이스를 구현하여
// Spring Security에서 사용자 정보를 제공하는 역할을 합니다.
public class CustomUserDetails implements UserDetails {

    private final User user;

    public CustomUserDetails(User user) {
        this.user = user;
    }

    public Long getUserId() {
        return user.getId();
    }

    public String getEmail() {
        return user.getEmail();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.emptyList(); // 권한 안 쓸 거면 이렇게 비워도 OK
    }

    @Override
    public String getPassword() {
        return user.getPasswordHash(); // 비밀번호 해시
    }

    @Override
    public String getUsername() {
        return user.getEmail(); // 유저 이름 대신 이메일 사용
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
