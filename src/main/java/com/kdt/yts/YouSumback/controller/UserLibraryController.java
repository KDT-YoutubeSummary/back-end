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
import java.util.Map;

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

    // 라이브러리 검색 API (제목 또는 태그로 검색)
    // 검색 조건은 선택적이며, 둘 중 하나 또는 둘 다 제공될 수 있음
    // 검색 조건 없으면 빈 리스트
    @GetMapping("/search")
    public ResponseEntity<?> searchLibrary(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String tags
    ) {
        List<UserLibraryResponseDTO> result = userLibraryService.search(title, tags);
        return ResponseEntity.ok(
                Map.of(
                        "code", 200,
                        "data", result,
                        "msg", "ok"
                )
        );
    }
}
