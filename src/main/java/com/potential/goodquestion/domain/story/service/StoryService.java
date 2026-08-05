package com.potential.goodquestion.domain.story.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Story 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StoryService {
}
