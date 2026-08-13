package com.potential.goodquestion.domain.parent.dto;

import com.potential.goodquestion.domain.parent.entity.Parent;
import lombok.Builder;
import lombok.Getter;

public class ParentResponseDto {

    @Getter
    @Builder
    public static class Me {

        private Long id;
        private String email;
        private String name;
        private String provider;

        public static Me from(Parent parent) {
            return Me.builder()
                    .id(parent.getId())
                    .email(parent.getEmail())
                    .name(parent.getName())
                    .provider(parent.getProvider().name())
                    .build();
        }
    }
}
