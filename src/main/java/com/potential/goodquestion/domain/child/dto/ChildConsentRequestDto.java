package com.potential.goodquestion.domain.child.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 아동 개인정보 처리 동의 요청 DTO
 */
public class ChildConsentRequestDto {

    /**
     * 동의 등록 요청
     * POST /api/children/{childId}/consent
     */
    @Getter
    @NoArgsConstructor
    public static class Create {

        @NotBlank(message = "동의 버전은 필수입니다.")
        private String consentVersion;

        @NotBlank(message = "보호자 확인 방식은 필수입니다.")
        private String verificationMethod;
    }
}
