package com.potential.goodquestion.domain.session.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Session 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SessionService {
}
