package com.potential.goodquestion.domain.message.repository;

import com.potential.goodquestion.common.enums.SpeakerType;
import com.potential.goodquestion.domain.message.entity.Message;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findBySessionIdAndSceneIdOrderByCreatedAtAsc(Long sessionId, Long sceneId);

    Optional<Message> findTopBySessionIdAndSceneIdAndSpeakerTypeOrderByCreatedAtDesc(
            Long sessionId, Long sceneId, SpeakerType speakerType);

    List<Message> findBySessionIdAndSpeakerTypeOrderByCreatedAtAsc(
            Long sessionId, SpeakerType speakerType);

    long countBySessionId(Long sessionId);
}
