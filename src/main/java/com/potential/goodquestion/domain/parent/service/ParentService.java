package com.potential.goodquestion.domain.parent.service;

import com.potential.goodquestion.common.code.AuthErrorCode;
import com.potential.goodquestion.common.exception.CustomException;
import com.potential.goodquestion.domain.parent.dto.ParentRequestDto;
import com.potential.goodquestion.domain.parent.dto.ParentResponseDto;
import com.potential.goodquestion.domain.parent.entity.Parent;
import com.potential.goodquestion.domain.parent.repository.ParentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ParentService {

    private final ParentRepository parentRepository;

    public ParentResponseDto.Me getMe(Long parentId) {
        Parent parent = parentRepository.findById(parentId)
                .orElseThrow(() -> new CustomException(AuthErrorCode.PARENT_NOT_FOUND));
        return ParentResponseDto.Me.from(parent);
    }

    @Transactional
    public ParentResponseDto.Me updateMe(Long parentId, ParentRequestDto.Update request) {
        Parent parent = parentRepository.findById(parentId)
                .orElseThrow(() -> new CustomException(AuthErrorCode.PARENT_NOT_FOUND));
        parent.updateName(request.getName());
        return ParentResponseDto.Me.from(parent);
    }
}
