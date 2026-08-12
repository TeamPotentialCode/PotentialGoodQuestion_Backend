package com.potential.goodquestion.domain.child.service;

import com.potential.goodquestion.common.code.ChildErrorCode;
import com.potential.goodquestion.common.exception.CustomException;
import com.potential.goodquestion.domain.child.dto.ChildRequestDto;
import com.potential.goodquestion.domain.child.dto.ChildResponseDto;
import com.potential.goodquestion.domain.child.entity.Child;
import com.potential.goodquestion.domain.child.repository.ChildRepository;
import com.potential.goodquestion.domain.parent.entity.Parent;
import com.potential.goodquestion.domain.parent.repository.ParentRepository;
import java.time.Year;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 아이 프로필 서비스
 *
 * 담당 API:
 * - GET  /api/children         : 로그인한 보호자의 아이 목록 전체 조회
 * - POST /api/children         : 아이 프로필 등록
 * - PATCH /api/children/{id}   : 아이 프로필 수정 (이름, 나이)
 *
 * 보안:
 * - 모든 메서드에서 parentId를 기준으로 소유권 검증
 * - 다른 보호자의 아이에 접근하면 403 반환
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChildService {

    private final ChildRepository childRepository;
    private final ParentRepository parentRepository;

    /**
     * 로그인한 보호자의 아이 목록 전체 조회
     *
     * @param parentId JWT에서 추출한 보호자 ID
     * @return 아이 프로필 목록
     */
    public List<ChildResponseDto.ChildInfo> getChildren(Long parentId) {
        Parent parent = getParent(parentId);
        return childRepository.findAllByParent(parent).stream()
                .map(ChildResponseDto.ChildInfo::from)
                .toList();
    }

    /**
     * 아이 프로필 등록
     *
     * MVP 제한: 보호자 계정당 아이 1명만 등록 가능
     * → 정식 서비스에서는 최대 3명으로 확장 예정
     *   제거 방법: 아래 아이 수 제한 블록(childCount 변수 포함) 삭제 후 maxChildren 상수 제거
     *
     * @param parentId JWT에서 추출한 보호자 ID
     * @param request  아이 이름, 나이
     * @return 등록된 아이 프로필
     */
    @Transactional
    public ChildResponseDto.ChildInfo createChild(Long parentId, ChildRequestDto.Create request) {
        Parent parent = getParent(parentId);

        // ── MVP 아이 수 제한 (추후 제거 또는 maxChildren 값 변경) ──────────────
        // 정식 서비스에서는 계정당 최대 3명 지원 예정 (MAX_CHILDREN = 3)
        final int MAX_CHILDREN = 1;
        int childCount = childRepository.findAllByParent(parent).size();
        if (childCount >= MAX_CHILDREN) {
            throw new CustomException(ChildErrorCode.CHILD_LIMIT_EXCEEDED);
        }
        // ─────────────────────────────────────────────────────────────────────

        Child child = Child.create(parent, request.getName(), request.getBirthYear());
        return ChildResponseDto.ChildInfo.from(childRepository.save(child));
    }

    /**
     * 아이 프로필 수정 (이름, 나이)
     *
     * @param parentId JWT에서 추출한 보호자 ID (소유권 검증용)
     * @param childId  수정할 아이 ID
     * @param request  변경할 이름, 나이
     * @return 수정된 아이 프로필
     */
    @Transactional
    public ChildResponseDto.ChildInfo updateChild(Long parentId, Long childId, ChildRequestDto.Update request) {
        Child child = getChildWithOwnerCheck(parentId, childId);
        child.updateProfile(request.getName(), request.getBirthYear());
        return ChildResponseDto.ChildInfo.from(child);
    }

    // ─────────── private ────────────────

    /**
     * 입력받은 나이를 저장용 출생연도로 변환 (현재연도 - 나이)
     * DB에는 출생연도만 저장하고, 조회 시 연령을 계산한다.
     */
    private int toBirthYear(int age) {
        return Year.now().getValue() - age;
    }

    /**
     * 보호자 조회 (없으면 예외)
     */
    private Parent getParent(Long parentId) {
        return parentRepository.findById(parentId)
                .orElseThrow(() -> new UsernameNotFoundException("보호자를 찾을 수 없습니다: " + parentId));
    }

    /**
     * 아이 조회 + 소유권 검증
     * 아이가 없거나 다른 보호자의 아이이면 예외 발생
     */
    private Child getChildWithOwnerCheck(Long parentId, Long childId) {
        Child child = childRepository.findById(childId)
                .orElseThrow(() -> new CustomException(ChildErrorCode.CHILD_NOT_FOUND));

        // 로그인한 보호자의 아이가 아니면 403
        if (!child.getParent().getId().equals(parentId)) {
            throw new CustomException(ChildErrorCode.CHILD_ACCESS_DENIED);
        }
        return child;
    }
}
