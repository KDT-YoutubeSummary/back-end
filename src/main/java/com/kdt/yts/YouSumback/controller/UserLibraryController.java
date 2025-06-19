package com.kdt.yts.YouSumback.controller;

import com.kdt.yts.YouSumback.model.dto.request.UserLibraryRequestDTO;
import com.kdt.yts.YouSumback.model.dto.request.UserNoteUpdateRequestDTO;
import com.kdt.yts.YouSumback.model.dto.response.TagStatResponseDTO;
import com.kdt.yts.YouSumback.model.dto.response.UserLibraryResponseDTO;
import com.kdt.yts.YouSumback.security.CustomUserDetails;
import com.kdt.yts.YouSumback.service.UserLibraryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/libraries")
@RequiredArgsConstructor
@Tag(name = "유저 라이브러리", description = "사용자 라이브러리 관련 API")
//@Getter
// UserLibraryController는 사용자 라이브러리 관련 API를 처리하는 컨트롤러입니다.
public class UserLibraryController {

    private final UserLibraryService userLibraryService;

    // 라이브러리 등록
    @PostMapping
    @Operation(summary = "라이브러리 등록", description = "요청 정보를 기반으로 새로운 라이브러리를 생성합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "라이브러리 등록 성공"),
            @ApiResponse(responseCode = "404", description = "요청 데이터 오류")
    })
    public ResponseEntity<?> saveLibrary(@RequestBody UserLibraryRequestDTO request, Authentication auth) {
        try {
            Long userId = getUserIdFromAuth(auth);
//            request.setUserId(userId);  // DTO에 userId 주입
            var response = userLibraryService.saveLibrary(userId, request);
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

    // 라이브러리 전체 조회
    @GetMapping
    @Operation(summary = "사용자 라이브러리 전체 조회", description = "인증된 사용자의 라이브러리 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    public ResponseEntity<?> getLibraries(Authentication auth) {
        Long userId = getUserIdFromAuth(auth);
        var response = userLibraryService.getLibrariesByUserId(userId);
        return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", "라이브러리 조회 완료",
                "data", response
        ));
    }

    // 라이브러리 상세 조회
    @GetMapping("/{libraryId}")
    @Operation(summary = "라이브러리 상세 조회", description = "라이브러리 ID를 통해 상세 정보를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "상세 조회 성공"),
            @ApiResponse(responseCode = "403", description = "접근 권한 없음"),
            @ApiResponse(responseCode = "404", description = "라이브러리를 찾을 수 없음")
    })
    public ResponseEntity<?> getLibraryDetail(@Parameter(description = "조회할 라이브러리 ID", example = "1") @PathVariable Long libraryId,
                                              Authentication authentication) {
        Long userId = getUserIdFromAuth(authentication); // 🔐 인증된 사용자 ID 추출

        try {
            UserLibraryResponseDTO detail = userLibraryService.getLibraryDetail(libraryId, userId);
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
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of(
                    "code", 403,
                    "message", "해당 라이브러리에 대한 권한이 없습니다.",
                    "data", Map.of()
            ));
        }
    }

    // 라이브러리 삭제
    @DeleteMapping("/{library_id}")
    @Operation(summary = "라이브러리 삭제", description = "라이브러리 ID에 해당하는 라이브러리를 삭제합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "삭제 성공"),
            @ApiResponse(responseCode = "403", description = "삭제 권한 없음"),
            @ApiResponse(responseCode = "404", description = "라이브러리를 찾을 수 없음")
    })
    public ResponseEntity<?> deleteLibrary(@Parameter(description = "삭제할 라이브러리 ID", example = "1") @PathVariable("library_id") Long libraryId,
                                                   Authentication authentication) {
        Long userId = getUserIdFromAuth(authentication); // 🔐 토큰 기반 인증 유저 ID 추출

        try {
            userLibraryService.deleteLibrary(libraryId, userId); // ✅ 사용자 ID 기반 권한 검증 수행
            return ResponseEntity.ok(Map.of(
                    "code", 200,
                    "message", "라이브러리 삭제 완료"
            ));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(Map.of(
                    "code", 404,
                    "message", "해당 라이브러리를 찾을 수 없습니다."
            ));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of(
                    "code", 403,
                    "message", "해당 라이브러리에 대한 권한이 없습니다."
            ));
        }
    }


    // 라이브러리 검색
    @GetMapping("/search")
    @Operation(summary = "라이브러리 검색", description = "제목 또는 태그로 라이브러리를 검색합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "검색 성공"),
            @ApiResponse(responseCode = "404", description = "검색 결과 없음")
    })
    public ResponseEntity<?> searchLibrary(Authentication auth,
                                           @Parameter(description = "검색할 제목", example = "AI 요약") @RequestParam(required = false) String title,
                                           @Parameter(description = "검색할 태그", example = "GPT, 요약") @RequestParam(required = false) String tags) {
        Long userId = getUserIdFromAuth(auth);
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

    // 태그 통계 조회
    @GetMapping("/stat/tags")
    @Operation(summary = "태그 통계 조회", description = "사용자의 라이브러리에서 태그 통계를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "통계 조회 성공")
    })
    public ResponseEntity<?> getTagStatistics(Authentication auth) {
        Long userId = getUserIdFromAuth(auth);
        List<TagStatResponseDTO> tagStats = userLibraryService.getTagStatsByUser(userId);

        return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", "태그 통계 조회 성공",
                "data", tagStats
        ));
    }

    // 사용자 메모 업데이트
    @PatchMapping("/notes")
    @Operation(summary = "라이브러리 메모 추가 및 수정", description = "해당 라이브러리에 대한 사용자의 메모를 추가 및 수정합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "메모 수정 성공"),
            @ApiResponse(responseCode = "404", description = "라이브러리를 찾을 수 없음")
    })
    public ResponseEntity<String> updateUserNote(@RequestBody UserNoteUpdateRequestDTO requestDTO, Authentication auth) {
        Long userId = getUserIdFromAuth(auth);
        userLibraryService.updateUserNotes(userId, requestDTO);
        return ResponseEntity.ok("메모가 성공적으로 업데이트되었습니다.");
    }

    // 🔐 공통: 인증 객체에서 userId 추출
    Long getUserIdFromAuth(Authentication auth) {
        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        return userDetails.getUserId(); // ✅ userId는 변하지 않음 (PK 기반)
    }
}
