package com.potential.goodquestion.domain.parent.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Parent 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ParentService {
}
