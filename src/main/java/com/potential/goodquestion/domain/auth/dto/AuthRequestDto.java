package com.potential.goodquestion.domain.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 인증 요청 DTO 모음 (내부 static 클래스로 관리)
 */
public class AuthRequestDto {

    /**
     * 일반 회원가입 요청
     */
    @Getter
    @NoArgsConstructor
    public static class Signup {

        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        private String email;

        @NotBlank(message = "이름은 필수입니다.")
        @Size(max = 50, message = "이름은 50자 이하여야 합니다.")
        private String name;

        @NotBlank(message = "비밀번호는 필수입니다.")
        @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다.")
        private String password;
    }

    /**
     * 일반 로그인 요청
     */
    @Getter
    @NoArgsConstructor
    public static class Login {

        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        private String email;

        @NotBlank(message = "비밀번호는 필수입니다.")
        private String password;
    }

    /**
     * 토큰 재발급 요청
     */
    @Getter
    @NoArgsConstructor
    public static class Refresh {

        @NotBlank(message = "Refresh Token은 필수입니다.")
        private String refreshToken;
    }
}
