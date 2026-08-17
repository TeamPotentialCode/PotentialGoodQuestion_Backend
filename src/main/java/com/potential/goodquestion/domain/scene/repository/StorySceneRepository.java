package com.potential.goodquestion.domain.scene.repository;

import com.potential.goodquestion.domain.scene.entity.StoryScene;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StorySceneRepository extends JpaRepository<StoryScene, Long> {

    @Override
    Optional<StoryScene> findById(Long id);

    Optional<StoryScene> findByStoryIdAndSceneOrder(Long storyId, Integer sceneOrder);

    List<StoryScene> findByStoryIdOrderBySceneOrder(Long storyId);
}
