package com.kdt.yts.YouSumback.controller;

import com.kdt.yts.YouSumback.model.dto.response.UserActivityLogDTO;
import com.kdt.yts.YouSumback.model.dto.response.UserInfoDTO;
import com.kdt.yts.YouSumback.security.CustomUserDetails;
import com.kdt.yts.YouSumback.service.LogService;
import com.kdt.yts.YouSumback.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "마이페이지", description = "마이페이지 관련 API")
public class MypageController {

    private final UserService userService;
    private final LogService logService;

    @Operation(
            summary = "마이페이지 조회",
            description = "사용자의 정보와 최근 활동 기록을 조회합니다",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "마이페이지 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
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
