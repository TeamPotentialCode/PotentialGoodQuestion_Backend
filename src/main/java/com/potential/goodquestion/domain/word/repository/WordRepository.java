package com.potential.goodquestion.domain.word.repository;

import com.potential.goodquestion.domain.word.entity.Word;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 단어장 레포지토리
 */
public interface WordRepository extends JpaRepository<Word, Long> {

    /**
     * 아이의 단어장 전체 조회 (최근 저장 순)
     */
    List<Word> findByChildIdOrderByCreatedAtDesc(Long childId);

    /**
     * 아이가 해당 단어를 이미 저장했는지 확인 (중복 저장 방지)
     */
    boolean existsByChildIdAndWord(Long childId, String word);

    /**
     * 보호자의 모든 단어장 삭제 (회원 탈퇴 시 연관 데이터 정리용)
     */
    @Modifying
    @Query("DELETE FROM Word w WHERE w.child.parent.id = :parentId")
    void deleteByParentId(@Param("parentId") Long parentId);
}
