package com.potential.goodquestion.common.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.potential.goodquestion.common.security.CustomUserDetailsService;
import com.potential.goodquestion.common.security.CustomUserPrincipal;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * JWT 토큰을 검증하고 사용자 인증을 처리하는 필터
 *
 * 역할:
 * 1. 모든 HTTP 요청을 가로채 Authorization 헤더의 JWT 토큰 검사
 * 2. 유효한 Access Token이면 SecurityContext에 인증 정보 저장
 * 3. Refresh Token으로 API 접근 시 차단
 * 4. JWT 예외 종류별로 구체적인 에러 응답 반환
 *
 * OncePerRequestFilter 상속:
 * - 같은 요청에서 필터가 중복 실행되는 것을 방지
 *
 * 인증 흐름:
 * 요청 → JwtFilter → 토큰 검증 → CustomUserDetailsService → SecurityContext 저장 → Controller
 */
@Slf4j
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;
    private final ObjectMapper objectMapper;

    /**
     * 모든 HTTP 요청에 실행되는 필터 핵심 메서드
     *
     * 실행 순서:
     * 1. CORS preflight(OPTIONS) 요청은 바로 통과
     * 2. Authorization 헤더에서 Bearer 토큰 추출
     * 3. 토큰 없으면 다음 필터로 전달 (Security 설정에서 인증 여부 판단)
     * 4. 토큰 있으면 유효성 검증 후 SecurityContext에 인증 정보 저장
     * 5. JWT 예외 발생 시 종류별로 에러 응답 반환
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // CORS preflight 요청은 인증 없이 통과
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String bearerToken = request.getHeader("Authorization");
        String token = jwtUtil.resolveToken(bearerToken);

        // 토큰이 없으면 다음 필터로 전달 (SecurityConfig에서 인증 여부 판단)
        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // Refresh Token을 API 인증에 사용하는 것 방지
            String tokenType = jwtUtil.getTokenType(token);
            if (!"ACCESS".equals(tokenType)) {
                log.warn("ACCESS 토큰이 아닌 토큰으로 API 접근 시도: tokenType={}", tokenType);
                sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Access Token이 아닙니다.");
                return;
            }

            // 관리자 토큰: role: ADMIN 클레임이 있으면 DB 조회 없이 ROLE_ADMIN 권한 부여
            String role = jwtUtil.getRole(token);
            if ("ADMIN".equals(role)) {
                UsernamePasswordAuthenticationToken adminAuth = new UsernamePasswordAuthenticationToken(
                        "ADMIN", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
                adminAuth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(adminAuth);
                filterChain.doFilter(request, response);
                return;
            }

            // 일반 사용자 토큰: 보호자 ID 추출 후 DB 조회
            Long parentId = jwtUtil.getParentId(token);
            CustomUserPrincipal userDetails = userDetailsService.loadUserById(parentId);

            // Spring Security 인증 객체 생성 후 SecurityContext에 저장
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);

            filterChain.doFilter(request, response);

        } catch (SignatureException | MalformedJwtException e) {
            // 서명이 잘못되었거나 토큰 형식이 올바르지 않은 경우
            log.warn("잘못된 JWT 서명 또는 형식: {}", e.getMessage());
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "유효하지 않은 토큰입니다.");
        } catch (ExpiredJwtException e) {
            // 토큰 만료 — 클라이언트가 Refresh Token으로 재발급 필요
            log.warn("만료된 JWT 토큰: {}", e.getMessage());
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "만료된 토큰입니다. 재발급이 필요합니다.");
        } catch (UnsupportedJwtException e) {
            // 지원하지 않는 JWT 형식
            log.warn("지원하지 않는 JWT 토큰: {}", e.getMessage());
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "지원하지 않는 토큰 형식입니다.");
        } catch (IllegalArgumentException e) {
            // 토큰이 비어있거나 null
            log.warn("JWT 토큰이 비어있음: {}", e.getMessage());
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "토큰이 비어있습니다.");
        } catch (UsernameNotFoundException e) {
            // 회원 탈퇴로 계정이 삭제됐지만 액세스 토큰이 아직 만료되지 않은 경우
            // 서버 오류가 아니라 인증 실패이므로 401을 반환한다.
            log.warn("토큰에 해당하는 계정 없음: {}", e.getMessage());
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "유효하지 않은 토큰입니다.");
        } catch (Exception e) {
            log.error("JWT 처리 중 예외 발생: {}", e.getMessage());
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "인증 처리 중 오류가 발생했습니다.");
        }
    }

    /**
     * JWT 인증 실패 시 JSON 에러 응답 반환
     *
     * @param response   HTTP 응답 객체
     * @param statusCode HTTP 상태 코드 (401, 500 등)
     * @param message    클라이언트에게 전달할 에러 메시지
     */
    private void sendErrorResponse(HttpServletResponse response, int statusCode, String message) throws IOException {
        response.setStatus(statusCode);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> errorBody = Map.of(
                "success", false,
                "message", message,
                "data", ""
        );
        response.getWriter().write(objectMapper.writeValueAsString(errorBody));
    }
}
