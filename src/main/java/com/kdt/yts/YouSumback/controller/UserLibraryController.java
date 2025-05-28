package com.kdt.yts.YouSumback.controller;

import com.kdt.yts.YouSumback.model.dto.request.UserLibraryRequestDTO;
import com.kdt.yts.YouSumback.model.dto.response.ApiResponse;
import com.kdt.yts.YouSumback.model.dto.response.UserLibraryResponseDTO;
import com.kdt.yts.YouSumback.service.UserLibraryService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/library")
@RequiredArgsConstructor
@Getter
public class UserLibraryController {

    private final UserLibraryService userLibraryService;

    // 라이브러리 저장 API
    @PostMapping
    public ResponseEntity<ApiResponse<UserLibraryResponseDTO>> saveLibrary(@RequestBody UserLibraryRequestDTO request) {
        UserLibraryResponseDTO saved = userLibraryService.saveLibrary(request);
        ApiResponse<UserLibraryResponseDTO> response = new ApiResponse<>(201, "라이브러리 등록 완료", saved);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 라이브러리 조회 API
    @GetMapping
    public ResponseEntity<ApiResponse<List<UserLibraryResponseDTO>>> getLibraries(@RequestParam("user_id") int userId) {
        List<UserLibraryResponseDTO> libraryList = userLibraryService.getLibrariesByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success(200, "라이브러리 조회 성공", libraryList));
    }

    // 라이브러리 삭제 API
    @DeleteMapping("/{libraryId}")
    public ResponseEntity<Void> deleteLibrary(@PathVariable Long libraryId) {
        userLibraryService.deleteLibraryById(libraryId);
        return ResponseEntity.noContent().build();
    }
}
