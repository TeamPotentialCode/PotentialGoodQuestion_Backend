package com.potential.goodquestion.domain.child.service;

import com.potential.goodquestion.common.code.ChildErrorCode;
import com.potential.goodquestion.common.exception.CustomException;
import com.potential.goodquestion.domain.child.dto.ChildConsentRequestDto;
import com.potential.goodquestion.domain.child.dto.ChildConsentResponseDto;
import com.potential.goodquestion.domain.child.entity.Child;
import com.potential.goodquestion.domain.child.entity.ChildConsent;
import com.potential.goodquestion.domain.child.repository.ChildConsentRepository;
import com.potential.goodquestion.domain.child.repository.ChildRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 아동 개인정보 처리 동의 서비스
 *
 * MVP 규칙:
 * - 동의가 없거나 철회된 아이는 새 세션 시작 불가
 * - 세션 시작 시 ChildConsentRepository.existsActiveConsentByChild() 로 검증
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChildConsentService {

    private final ChildConsentRepository childConsentRepository;
    private final ChildRepository childRepository;

    /**
     * 아동 개인정보 처리 동의 등록
     * POST /api/children/{childId}/consent
     *
     * @param childId  동의 대상 아이 ID
     * @param parentId 소유권 검증용 보호자 ID
     * @param request  동의 버전, 확인 방식
     * @return 생성된 동의 정보
     */
    @Transactional
    public ChildConsentResponseDto.ConsentInfo createConsent(
            Long childId, Long parentId, ChildConsentRequestDto.Create request) {

        Child child = getChildWithOwnerCheck(childId, parentId);

        ChildConsent consent = ChildConsent.builder()
                .child(child)
                .consentVersion(request.getConsentVersion())
                .verificationMethod(request.getVerificationMethod())
                .build();

        return ChildConsentResponseDto.ConsentInfo.from(childConsentRepository.save(consent));
    }

    /**
     * 아동 개인정보 처리 동의 조회
     * GET /api/children/{childId}/consent
     *
     * @param childId  조회 대상 아이 ID
     * @param parentId 소유권 검증용 보호자 ID
     * @return 유효한 동의 정보
     */
    public ChildConsentResponseDto.ConsentInfo getActiveConsent(Long childId, Long parentId) {
        Child child = getChildWithOwnerCheck(childId, parentId);

        ChildConsent consent = childConsentRepository.findActiveConsentByChild(child)
                .orElseThrow(() -> new CustomException(ChildErrorCode.CONSENT_NOT_FOUND));

        return ChildConsentResponseDto.ConsentInfo.from(consent);
    }

    /**
     * 아동 개인정보 처리 동의 철회
     * DELETE /api/children/{childId}/consent
     *
     * @param childId  철회 대상 아이 ID
     * @param parentId 소유권 검증용 보호자 ID
     */
    @Transactional
    public void withdrawConsent(Long childId, Long parentId) {
        Child child = getChildWithOwnerCheck(childId, parentId);

        ChildConsent consent = childConsentRepository.findActiveConsentByChild(child)
                .orElseThrow(() -> new CustomException(ChildErrorCode.CONSENT_NOT_FOUND));

        consent.withdraw();
    }

    // ─────────── private ────────────────

    /**
     * 아이 조회 + 보호자 소유권 검증
     */
    private Child getChildWithOwnerCheck(Long childId, Long parentId) {
        Child child = childRepository.findById(childId)
                .orElseThrow(() -> new CustomException(ChildErrorCode.CHILD_NOT_FOUND));

        if (!child.getParent().getId().equals(parentId)) {
            throw new CustomException(ChildErrorCode.CHILD_ACCESS_DENIED);
        }
        return child;
    }
}
