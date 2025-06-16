package com.kdt.yts.YouSumback.controller;

import com.kdt.yts.YouSumback.model.dto.request.ReminderCreateRequestDTO;
import com.kdt.yts.YouSumback.model.dto.response.ReminderResponseDTO;
import com.kdt.yts.YouSumback.model.dto.request.ReminderUpdateRequestDTO;
import com.kdt.yts.YouSumback.service.ReminderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid; // 유효성 검사 어노테이션
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reminder") // 기본 URL 경로를 설정
@RequiredArgsConstructor
@Tag(name = "리마인더", description = "리마인더 관리 API")
public class ReminderController {

    private final ReminderService reminderService;

    // 새 리마인더를 생성하는 API 엔드포인트
    @Operation(summary = "리마인더 생성", description = "새로운 리마인더를 생성합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "리마인더 생성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터")
    })
    @PostMapping // HTTP POST 요청을 처리
    public ResponseEntity<ReminderResponseDTO> createReminder(@Valid @RequestBody ReminderCreateRequestDTO request) {
        ReminderResponseDTO newReminder = reminderService.createReminder(request);
        return new ResponseEntity<>(newReminder, HttpStatus.CREATED);
    }

    // 리마인더 ID로 단일 리마인더 정보를 조회하는 API 엔드포인트
    @Operation(summary = "리마인더 조회", description = "특정 리마인더의 상세 정보를 조회합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "리마인더 조회 성공"),
            @ApiResponse(responseCode = "404", description = "리마인더를 찾을 수 없음")
    })
    @GetMapping("/{reminderId}")
    public ResponseEntity<ReminderResponseDTO> getReminderById(@PathVariable Long reminderId) {
        ReminderResponseDTO reminder = reminderService.getReminderById(reminderId);
        return ResponseEntity.ok(reminder);
    }

    // 특정 사용자의 모든 리마인더를 조회하는 API 엔드포인트
    @Operation(summary = "사용자별 리마인더 목록", description = "특정 사용자의 모든 리마인더를 조회합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "리마인더 목록 조회 성공")
    })
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ReminderResponseDTO>> getRemindersByUserId(@PathVariable Long userId) {
        List<ReminderResponseDTO> reminders = reminderService.getRemindersByUserId(userId);
        return ResponseEntity.ok(reminders);
    }

    // 리마인더 정보를 업데이트하는 API 엔드포인트
    @Operation(summary = "리마인더 수정", description = "기존 리마인더의 정보를 수정합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "리마인더 수정 성공"),
            @ApiResponse(responseCode = "404", description = "리마인더를 찾을 수 없음")
    })
    @PutMapping("/{reminderId}")
    public ResponseEntity<ReminderResponseDTO> updateReminder(@PathVariable Long reminderId,
                                                              @Valid @RequestBody ReminderUpdateRequestDTO request) {
        ReminderResponseDTO updatedReminder = reminderService.updateReminder(reminderId, request);
        return ResponseEntity.ok(updatedReminder);
    }

    // 리마인더를 삭제하는 API 엔드포인트
    @Operation(summary = "리마인더 삭제", description = "특정 리마인더를 삭제합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "리마인더 삭제 성공")
    })
    @DeleteMapping("/{reminderId}")
    public ResponseEntity<Void> deleteReminder(@PathVariable Long reminderId) {
        reminderService.deleteReminder(reminderId);
        return ResponseEntity.noContent().build();
    }
}