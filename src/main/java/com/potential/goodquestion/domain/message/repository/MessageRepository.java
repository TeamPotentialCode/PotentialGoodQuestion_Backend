package com.potential.goodquestion.domain.message.repository;

import com.potential.goodquestion.common.enums.SpeakerType;
import com.potential.goodquestion.domain.message.entity.Message;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findBySessionIdAndSceneIdOrderByCreatedAtAsc(Long sessionId, Long sceneId);

    Optional<Message> findTopBySessionIdAndSceneIdAndSpeakerTypeOrderByCreatedAtDesc(
            Long sessionId, Long sceneId, SpeakerType speakerType);

    List<Message> findBySessionIdAndSpeakerTypeOrderByCreatedAtAsc(
            Long sessionId, SpeakerType speakerType);

    long countBySessionId(Long sessionId);

    /**
     * 세션 전체 메시지를 발화 순서 오름차순으로 조회
     * GET /api/sessions/{sessionId} 대화 내역 포함 시 사용
     */
    List<Message> findBySessionIdOrderByTurnOrderAsc(Long sessionId);

    /**
     * 보호자의 모든 메시지 삭제 (회원 탈퇴 시 연관 데이터 정리용)
     * 참조하는 utterance_analyses 를 먼저 삭제한 뒤 호출해야 한다.
     */
    @Modifying
    @Query("DELETE FROM Message m WHERE m.session.child.parent.id = :parentId")
    void deleteByParentId(@Param("parentId") Long parentId);

    /**
     * 아이의 모든 메시지 삭제 (아이 프로필 삭제 시 연관 데이터 정리용)
     * 참조하는 utterance_analyses 를 먼저 삭제한 뒤 호출해야 한다.
     */
    @Modifying
    @Query("DELETE FROM Message m WHERE m.session.child.id = :childId")
    void deleteByChildId(@Param("childId") Long childId);
}
