package com.potential.goodquestion.domain.report.controller;

import com.potential.goodquestion.common.response.ApiResponse;
import com.potential.goodquestion.domain.report.dto.ReportResponse;
import com.potential.goodquestion.domain.report.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Report", description = "보호자 리포트 API")
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @Operation(
        summary = "보호자 리포트 조회",
        description = "세션 종료 후 아이의 말하기 역량 분석, 대표 발화, 가정 연계 학습 가이드를 반환합니다."
    )
    @GetMapping("/{sessionId}")
    public ResponseEntity<ApiResponse<ReportResponse>> getReport(
            @Parameter(description = "세션 ID", required = true) @PathVariable Long sessionId) {
        return ResponseEntity.ok(ApiResponse.success(reportService.getReport(sessionId)));
    }
}
