package com.potential.goodquestion.domain.child.repository;

import com.potential.goodquestion.domain.child.entity.Child;
import com.potential.goodquestion.domain.parent.entity.Parent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Child 레포지토리
 */
public interface ChildRepository extends JpaRepository<Child, Long> {

    /**
     * 보호자에 속한 아이 목록 조회
     */
    List<Child> findAllByParent(Parent parent);

    /**
     * 보호자 ID로 아이 목록 조회 (관리자 전용)
     */
    List<Child> findByParentId(Long parentId);

    /**
     * 보호자 ID로 아이 수 집계 (회원 목록 childCount 표시용)
     */
    long countByParentId(Long parentId);

    /**
     * 보호자의 모든 아이 삭제 (회원 탈퇴 시 연관 데이터 정리용)
     * 아이를 참조하는 세션, 단어장, 동의 기록을 먼저 삭제한 뒤 호출해야 한다.
     */
    @Modifying
    @Query("DELETE FROM Child c WHERE c.parent.id = :parentId")
    void deleteByParentId(@Param("parentId") Long parentId);
}
