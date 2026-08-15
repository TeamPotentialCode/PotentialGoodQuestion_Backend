package com.potential.goodquestion.domain.child.repository;

import com.potential.goodquestion.domain.child.entity.Child;
import com.potential.goodquestion.domain.parent.entity.Parent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
