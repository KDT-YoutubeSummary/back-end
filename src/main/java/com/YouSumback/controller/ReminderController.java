package com.YouSumback.controller;

import com.YouSumback.model.dto.request.ReminderCreateRequest;
import com.YouSumback.model.dto.response.ReminderResponse;
import com.YouSumback.model.dto.request.ReminderUpdateRequest;
import com.YouSumback.service.ReminderService;
import jakarta.validation.Valid; // 유효성 검사 어노테이션
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reminder") // 기본 URL 경로를 설정
@RequiredArgsConstructor
public class ReminderController {

    private final ReminderService reminderService;

    // 새 리마인더를 생성하는 API 엔드포인트
    @PostMapping // HTTP POST 요청을 처리
    public ResponseEntity<ReminderResponse> createReminder(@Valid @RequestBody ReminderCreateRequest request) {
        ReminderResponse newReminder = reminderService.createReminder(request);
        return new ResponseEntity<>(newReminder, HttpStatus.CREATED);
    }

    // 리마인더 ID로 단일 리마인더 정보를 조회하는 API 엔드포인트
    @GetMapping("/{reminderId}")
    public ResponseEntity<ReminderResponse> getReminderById(@PathVariable Long reminderId) {
        ReminderResponse reminder = reminderService.getReminderById(reminderId);
        return ResponseEntity.ok(reminder);
    }

    // 특정 사용자의 모든 리마인더를 조회하는 API 엔드포인트
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ReminderResponse>> getRemindersByUserId(@PathVariable Long userId) {
        List<ReminderResponse> reminders = reminderService.getRemindersByUserId(userId);
        return ResponseEntity.ok(reminders);
    }

    // 리마인더 정보를 업데이트하는 API 엔드포인트
    @PutMapping("/{reminderId}")
    public ResponseEntity<ReminderResponse> updateReminder(@PathVariable Long reminderId,
                                                           @Valid @RequestBody ReminderUpdateRequest request) {
        ReminderResponse updatedReminder = reminderService.updateReminder(reminderId, request);
        return ResponseEntity.ok(updatedReminder);
    }

    // 리마인더를 삭제하는 API 엔드포인트
    @DeleteMapping("/{reminderId}")
    public ResponseEntity<Void> deleteReminder(@PathVariable Long reminderId) {
        reminderService.deleteReminder(reminderId);
        return ResponseEntity.noContent().build();
    }
}