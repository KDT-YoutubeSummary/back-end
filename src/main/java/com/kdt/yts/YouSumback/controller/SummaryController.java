package com.kdt.yts.YouSumback.controller;

import com.kdt.yts.YouSumback.model.dto.request.SummaryRequest;
import com.kdt.yts.YouSumback.model.dto.response.SummaryResponse;
import com.kdt.yts.YouSumback.service.SummaryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/summary")
@RequiredArgsConstructor
public class SummaryController {

    private final SummaryService summaryService;

    @PostMapping
    public SummaryResponse summarize(@Valid @RequestBody SummaryRequest request) {
        return summaryService.summarize(request);
    }


}
