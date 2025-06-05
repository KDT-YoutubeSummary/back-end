package com.kdt.yts.YouSumback.controller;

import com.kdt.yts.YouSumback.model.dto.request.SummaryRequestDTO;
import com.kdt.yts.YouSumback.model.dto.response.SummaryResponseDTO;
import com.kdt.yts.YouSumback.service.SummaryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/summary")
@RequiredArgsConstructor
public class SummaryController {

    private final SummaryService summaryService;

    // 요약 요청을 처리하는 API 엔드포인트
    @PostMapping
    public SummaryResponseDTO summarize(@Valid @RequestBody SummaryRequestDTO request) {
        return summaryService.summarize(request);
    }


}
