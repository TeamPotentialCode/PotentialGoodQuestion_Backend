package com.potential.goodquestion.domain.child.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Child 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChildService {
}
