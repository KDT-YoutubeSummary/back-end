package com.kdt.yts.YouSumback.controller;

import com.kdt.yts.YouSumback.model.dto.request.UserLibraryRequestDTO;
import com.kdt.yts.YouSumback.model.dto.response.UserLibraryResponseDTO;
import com.kdt.yts.YouSumback.service.UserLibraryService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/library")
@RequiredArgsConstructor
@Getter
public class UserLibraryController {

    private final UserLibraryService userLibraryService;

    // 라이브러리 등록
    @PostMapping
    public ResponseEntity<?> saveLibrary(@RequestBody UserLibraryRequestDTO request) {
        try {
            var response = userLibraryService.saveLibrary(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "code", 201,
                    "message", "라이브러리 등록 완료",
                    "data", response
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "code", 404,
                    "message", "error: " + e.getMessage(),
                    "data", Map.of()
            ));
        }
    }

    // 라이브러리 조회
    @GetMapping
    public ResponseEntity<?> getLibraries(@RequestParam("user_id") Long userId) {
        try {
            var response = userLibraryService.getLibrariesByUserId(userId);
            return ResponseEntity.ok(Map.of(
                    "code", 200,
                    "message", "라이브러리 조회 완료",
                    "data", response
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "code", 404,
                    "message", "error: " + e.getMessage(),
                    "data", Map.of()
            ));
        }
    }

    @GetMapping("/{libraryId}")
    public ResponseEntity<?> getLibraryDetail(@PathVariable Long libraryId) {
        try {
            UserLibraryResponseDTO detail = userLibraryService.getLibraryDetail(libraryId);
            return ResponseEntity.ok().body(Map.of(
                    "code", 200,
                    "message", "라이브러리 상세 조회 완료",
                    "data", detail
            ));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(Map.of(
                    "code", 404,
                    "message", "해당 라이브러리를 찾을 수 없습니다.",
                    "data", Map.of()
            ));
        }
    }

    // 라이브러리 삭제
    @DeleteMapping("/{library_id}")
    public ResponseEntity<?> deleteLibrary(@PathVariable("library_id") Long libraryId) {
        try {
            userLibraryService.deleteLibrary(libraryId);
            return ResponseEntity.ok().body(
                    Map.of("code", 200, "message", "라이브러리 삭제 완료")
            );
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(
                    Map.of("code", 404, "message", "해당 라이브러리를 찾을 수 없습니다.")
            );
        }
    }

    // 라이브러리 검색
    @GetMapping("/search")
    public ResponseEntity<?> searchLibrary(@RequestParam("user_id") Long userId,
                                           @RequestParam(required = false) String title,
                                           @RequestParam(required = false) String tags) {
        var result = userLibraryService.search(userId, title, tags);
        if (result.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "code", 404,
                    "message", "error: 해당 조건에 맞는 라이브러리가 없습니다.",
                    "data", List.of()
            ));
        }
        return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", "검색 성공",
                "data", result
        ));
    }
}