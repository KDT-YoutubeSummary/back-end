package com.kdt.yts.YouSumback.controller;

import com.kdt.yts.YouSumback.model.dto.response.UserActivityLogDTO;
import com.kdt.yts.YouSumback.model.dto.response.UserInfoDTO;
import com.kdt.yts.YouSumback.security.CustomUserDetails;
import com.kdt.yts.YouSumback.service.LogService;
import com.kdt.yts.YouSumback.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MypageController {

    private final UserService userService;
    private final LogService logService;

    @GetMapping("/mypage")
    public ResponseEntity<?> getMypage(Authentication auth) {
        Long userId = getUserIdFromAuth(auth); // 커스텀 유저 디테일에서 꺼내기

        UserInfoDTO userInfo = userService.getUserInfo(userId);
        List<UserActivityLogDTO> recentLogs = logService.getRecentLogs(userId);

        return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", "마이페이지 조회 성공",
                "data", Map.of(
                        "userInfo", userInfo,
                        "recentLogs", recentLogs
                )
        ));
    }

    private Long getUserIdFromAuth(Authentication auth) {
        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        return userDetails.getUserId();
    }
}
