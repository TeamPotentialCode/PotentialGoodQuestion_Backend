package com.potential.goodquestion.domain.child.dto;

import com.potential.goodquestion.domain.child.entity.ChildConsent;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

/**
 * 아동 개인정보 처리 동의 응답 DTO
 */
public class ChildConsentResponseDto {

    /**
     * 동의 단건 응답
     */
    @Getter
    @Builder
    public static class ConsentInfo {

        private Long consentId;
        private Long childId;
        private String consentVersion;
        private String verificationMethod;
        private LocalDateTime consentedAt;
        private boolean active;   // withdrawnAt == null 여부

        /**
         * ChildConsent 엔티티 → ConsentInfo 변환
         */
        public static ConsentInfo from(ChildConsent consent) {
            return ConsentInfo.builder()
                    .consentId(consent.getId())
                    .childId(consent.getChild().getId())
                    .consentVersion(consent.getConsentVersion())
                    .verificationMethod(consent.getVerificationMethod())
                    .consentedAt(consent.getConsentedAt())
                    .active(consent.isActive())
                    .build();
        }
    }
}
