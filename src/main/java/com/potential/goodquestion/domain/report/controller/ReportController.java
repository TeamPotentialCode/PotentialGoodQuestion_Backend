package com.potential.goodquestion.domain.report.controller;

import com.potential.goodquestion.common.response.ApiResponse;
import com.potential.goodquestion.domain.report.dto.ReportResponse;
import com.potential.goodquestion.domain.report.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/{sessionId}")
    public ResponseEntity<ApiResponse<ReportResponse>> getReport(@PathVariable Long sessionId) {
        return ResponseEntity.ok(ApiResponse.success(reportService.getReport(sessionId)));
    }
}
