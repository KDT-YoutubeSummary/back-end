package com.kdt.yts.YouSumback.controller;

import com.kdt.yts.YouSumback.model.dto.request.SummaryArchiveRequestDTO;
import com.kdt.yts.YouSumback.model.dto.request.UserNoteUpdateRequestDTO;
import com.kdt.yts.YouSumback.model.dto.response.TagStatResponseDTO;
import com.kdt.yts.YouSumback.model.dto.response.SummaryArchiveResponseDTO;
import com.kdt.yts.YouSumback.security.CustomUserDetails;
import com.kdt.yts.YouSumback.service.SummaryArchiveService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/summary-archives")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "요약 저장소", description = "사용자 요약 저장소 관련 API")
// SummaryArchiveController는 사용자 요약 저장소 관련 API를 처리하는 컨트롤러입니다.
public class SummaryArchiveController {

    private final SummaryArchiveService summaryArchiveService;

    // 요약 저장소 등록
    @PostMapping
    @Operation(summary = "요약 저장소 등록", description = "요청 정보를 기반으로 새로운 요약 저장소를 생성합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "요약 저장소 등록 성공"),
            @ApiResponse(responseCode = "404", description = "요청 데이터 오류"),
            @ApiResponse(responseCode = "409", description = "이미 저장된 요약")
    })
    public ResponseEntity<?> saveArchive(@RequestBody SummaryArchiveRequestDTO request, Authentication auth) {
        try {
            Long userId = getUserIdFromAuth(auth);
            var response = summaryArchiveService.saveArchive(userId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "code", 201,
                    "message", "요약 저장소 등록 완료",
                    "data", response
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "code", 404,
                    "message", "error: " + e.getMessage(),
                    "data", Map.of()
            ));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "code", 409,
                    "message", "error: " + e.getMessage(),
                    "data", Map.of()
            ));
        }
    }

    // 요약 저장소 전체 조회
    @GetMapping
    @Operation(summary = "사용자 요약 저장소 전체 조회", description = "인증된 사용자의 요약 저장소 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    public ResponseEntity<?> getArchives(Authentication auth) {
        Long userId = getUserIdFromAuth(auth);
        var response = summaryArchiveService.getArchivesByUserId(userId);
        return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", "요약 저장소 조회 완료",
                "data", response
        ));
    }

    // 요약 저장소 상세 조회
    @GetMapping("/{archiveId}")
    @Operation(summary = "요약 저장소 상세 조회", description = "요약 저장소 ID를 통해 상세 정보를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "상세 조회 성공"),
            @ApiResponse(responseCode = "403", description = "접근 권한 없음"),
            @ApiResponse(responseCode = "404", description = "요약 저장소를 찾을 수 없음")
    })
    public ResponseEntity<?> getArchiveDetail(@Parameter(description = "조회할 요약 저장소 ID", example = "1") @PathVariable Long archiveId,
                                              Authentication authentication) {
        Long userId = getUserIdFromAuth(authentication);

        try {
            SummaryArchiveResponseDTO detail = summaryArchiveService.getArchiveDetail(archiveId, userId);
            return ResponseEntity.ok().body(Map.of(
                    "code", 200,
                    "message", "요약 저장소 상세 조회 완료",
                    "data", detail
            ));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(Map.of(
                    "code", 404,
                    "message", "해당 요약 저장소를 찾을 수 없습니다.",
                    "data", Map.of()
            ));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of(
                    "code", 403,
                    "message", "해당 요약 저장소에 대한 권한이 없습니다.",
                    "data", Map.of()
            ));
        }
    }

    // 요약 저장소 삭제
    @DeleteMapping("/{archiveId}")
    @Operation(summary = "요약 저장소 삭제", description = "요약 저장소 ID에 해당하는 요약 저장소를 삭제합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "삭제 성공"),
            @ApiResponse(responseCode = "403", description = "삭제 권한 없음"),
            @ApiResponse(responseCode = "404", description = "요약 저장소를 찾을 수 없음")
    })
    public ResponseEntity<?> deleteArchive(@Parameter(description = "삭제할 요약 저장소 ID", example = "1") @PathVariable Long archiveId,
                                           Authentication authentication) {
        Long userId = getUserIdFromAuth(authentication);

        try {
            summaryArchiveService.deleteArchive(archiveId, userId);
            return ResponseEntity.ok(Map.of(
                    "code", 200,
                    "message", "요약 저장소 삭제 완료"
            ));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(Map.of(
                    "code", 404,
                    "message", "해당 요약 저장소를 찾을 수 없습니다."
            ));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of(
                    "code", 403,
                    "message", "해당 요약 저장소에 대한 권한이 없습니다."
            ));
        }
    }

    // 요약 저장소 검색
    @GetMapping("/search")
    @Operation(summary = "요약 저장소 검색", description = "제목 또는 태그로 요약 저장소를 검색합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "검색 성공"),
            @ApiResponse(responseCode = "404", description = "검색 결과 없음")
    })
    public ResponseEntity<?> searchArchive(Authentication auth,
                                           @Parameter(description = "검색할 제목", example = "AI 요약") @RequestParam(required = false) String title,
                                           @Parameter(description = "검색할 태그", example = "GPT, 요약") @RequestParam(required = false) String tags) {
        Long userId = getUserIdFromAuth(auth);
        var result = summaryArchiveService.search(userId, title, tags);
        if (result.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "code", 404,
                    "message", "error: 해당 조건에 맞는 요약 저장소가 없습니다.",
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
    @Operation(summary = "태그 통계 조회", description = "사용자의 요약 저장소에서 태그 통계를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "통계 조회 성공")
    })
    public ResponseEntity<?> getTagStatistics(Authentication auth) {
        Long userId = getUserIdFromAuth(auth);
        List<TagStatResponseDTO> tagStats = summaryArchiveService.getTagStatsByUser(userId);

        return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", "태그 통계 조회 성공",
                "data", tagStats
        ));
    }

    // 사용자 메모 업데이트
    @PatchMapping("/notes")
    @Operation(summary = "요약 저장소 메모 추가 및 수정", description = "해당 요약 저장소에 대한 사용자의 메모를 추가 및 수정합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "메모 수정 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "요약 저장소를 찾을 수 없음")
    })
    public ResponseEntity<?> updateUserNote(@RequestBody @Valid UserNoteUpdateRequestDTO requestDTO, Authentication auth) {
        log.info("Received memo update request: summaryArchiveId={}, note='{}'", 
                requestDTO.getSummaryArchiveId(), requestDTO.getNote());
        
        try {
            Long userId = getUserIdFromAuth(auth);
            log.info("User ID from auth: {}", userId);
            
            summaryArchiveService.updateUserNotes(userId, requestDTO);
            
            log.info("Memo update completed successfully");
            return ResponseEntity.ok(Map.of(
                    "code", 200,
                    "message", "메모가 성공적으로 업데이트되었습니다.",
                    "data", Map.of()
            ));
        } catch (IllegalArgumentException e) {
            log.error("IllegalArgumentException in memo update: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "code", 404,
                    "message", "error: " + e.getMessage(),
                    "data", Map.of()
            ));
        } catch (SecurityException e) {
            log.error("SecurityException in memo update: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "code", 403,
                    "message", "error: " + e.getMessage(),
                    "data", Map.of()
            ));
        } catch (Exception e) {
            log.error("Unexpected exception in memo update: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "code", 500,
                    "message", "메모 수정 중 오류가 발생했습니다: " + e.getMessage(),
                    "data", Map.of()
            ));
        }
    }

    // 공통: 인증 객체에서 userId 추출
    private Long getUserIdFromAuth(Authentication auth) {
        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        return userDetails.getUserId();
    }
}
