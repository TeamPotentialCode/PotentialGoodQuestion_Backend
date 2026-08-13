package com.potential.goodquestion.domain.word.repository;

import com.potential.goodquestion.domain.word.entity.Word;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
