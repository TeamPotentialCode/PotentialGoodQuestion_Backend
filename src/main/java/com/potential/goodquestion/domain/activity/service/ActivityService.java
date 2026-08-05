package com.potential.goodquestion.domain.activity.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Activity 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ActivityService {
}
