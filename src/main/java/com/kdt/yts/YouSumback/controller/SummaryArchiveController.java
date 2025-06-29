package com.kdt.yts.YouSumback.controller;

import com.kdt.yts.YouSumback.model.dto.request.SummaryArchiveRequestDTO;
import com.kdt.yts.YouSumback.model.dto.request.UserNoteUpdateRequestDTO;
import com.kdt.yts.YouSumback.model.dto.response.SummaryArchiveResponseDTO;
import com.kdt.yts.YouSumback.model.dto.response.TagStatResponseDTO;
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

@Tag(name = "요약 아카이브", description = "요약 아카이브 관리 API")
@RestController
@RequestMapping("/api/summary-archives")
@RequiredArgsConstructor
@Slf4j
public class SummaryArchiveController {

    private final SummaryArchiveService summaryArchiveService;

    @PostMapping
    @Operation(summary = "요약 저장소 등록", description = "요청 정보를 기반으로 새로운 요약 저장소를 생성합니다.")
    public ResponseEntity<?> saveArchive(@RequestBody SummaryArchiveRequestDTO request, Authentication auth) {
        try {
            Long userId = getUserIdFromAuth(auth);
            summaryArchiveService.addSummaryToArchive(userId, request.getSummaryId());
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "code", 201, "message", "요약 저장소 등록 완료"
            ));
        } catch (IllegalArgumentException | NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("code", 404, "message", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("code", 409, "message", e.getMessage()));
        }
    }

    @GetMapping
    @Operation(summary = "사용자 요약 저장소 전체 조회", description = "인증된 사용자의 요약 저장소 목록을 조회합니다.")
    public ResponseEntity<?> getArchives(Authentication auth) {
        Long userId = getUserIdFromAuth(auth);
        var response = summaryArchiveService.getUserSummaryArchives(userId);
        return ResponseEntity.ok(Map.of("code", 200, "message", "요약 저장소 조회 완료", "data", response));
    }

    @GetMapping("/{archiveId}")
    @Operation(summary = "요약 저장소 상세 조회", description = "요약 저장소 ID를 통해 상세 정보를 조회합니다.")
    public ResponseEntity<?> getArchiveDetail(@PathVariable Long archiveId, Authentication authentication) {
        Long userId = getUserIdFromAuth(authentication);
        try {
            SummaryArchiveResponseDTO detail = summaryArchiveService.getArchiveDetail(archiveId, userId);
            return ResponseEntity.ok().body(Map.of("code", 200, "message", "요약 저장소 상세 조회 완료", "data", detail));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(Map.of("code", 404, "message", e.getMessage()));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("code", 403, "message", e.getMessage()));
        }
    }

    @DeleteMapping("/{archiveId}")
    @Operation(summary = "요약 저장소 삭제", description = "요약 저장소 ID에 해당하는 요약 저장소를 삭제합니다.")
    public ResponseEntity<?> deleteArchive(@PathVariable Long archiveId, Authentication authentication) {
        Long userId = getUserIdFromAuth(authentication);
        try {
            summaryArchiveService.deleteArchive(archiveId, userId);
            return ResponseEntity.ok(Map.of("code", 200, "message", "요약 저장소 삭제 완료"));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(Map.of("code", 404, "message", e.getMessage()));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("code", 403, "message", e.getMessage()));
        }
    }

    @GetMapping("/search")
    @Operation(summary = "요약 저장소 검색", description = "제목 또는 태그로 요약 저장소를 검색합니다.")
    public ResponseEntity<?> searchArchive(Authentication auth, @RequestParam(required = false) String keyword) {
        Long userId = getUserIdFromAuth(auth);
        List<SummaryArchiveResponseDTO> result = summaryArchiveService.searchArchives(userId, keyword);
        return ResponseEntity.ok(Map.of("code", 200, "message", "검색 성공", "data", result));
    }

    @GetMapping("/stat/tags")
    @Operation(summary = "태그 통계 조회", description = "사용자의 요약 저장소에서 태그 통계를 조회합니다.")
    public ResponseEntity<?> getTagStatistics(Authentication auth) {
        Long userId = getUserIdFromAuth(auth);
        List<TagStatResponseDTO> tagStats = summaryArchiveService.getTagStats(userId);
        return ResponseEntity.ok(Map.of("code", 200, "message", "태그 통계 조회 성공", "data", tagStats));
    }

    @PatchMapping("/notes")
    @Operation(summary = "요약 저장소 메모 추가 및 수정", description = "해당 요약 저장소에 대한 사용자의 메모를 추가 및 수정합니다.")
    public ResponseEntity<?> updateUserNote(@RequestBody @Valid UserNoteUpdateRequestDTO requestDTO, Authentication auth) {
        try {
            Long userId = getUserIdFromAuth(auth);
            summaryArchiveService.updateUserNote(userId, requestDTO.getSummaryArchiveId(), requestDTO.getNote());
            return ResponseEntity.ok(Map.of("code", 200, "message", "메모가 성공적으로 업데이트되었습니다."));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("code", 404, "message", e.getMessage()));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("code", 403, "message", e.getMessage()));
        }
    }

    private Long getUserIdFromAuth(Authentication auth) {
        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        return userDetails.getUserId();
    }
}
