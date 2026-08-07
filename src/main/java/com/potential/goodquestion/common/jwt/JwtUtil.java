package com.potential.goodquestion.common.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import java.security.Key;
import java.util.Base64;
import java.util.Date;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * JWT 토큰 생성 / 검증 / 파싱 유틸리티
 */
@Slf4j
@Component
public class JwtUtil {

    private static final String BEARER_PREFIX = "Bearer ";

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.access-expiration}")
    private long accessExpiration;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    private Key key;
    private final SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;

    /**
     * 빈 초기화 시 Base64 디코딩된 비밀키로 HMAC Key 생성
     */
    @PostConstruct
    public void init() {
        byte[] bytes = Base64.getDecoder().decode(secretKey);
        key = Keys.hmacShaKeyFor(bytes);
        log.info("JwtUtil 초기화 완료");
    }

    /**
     * Access Token 생성 (만료: jwt.access-expiration)
     */
    public String generateAccessToken(Long parentId) {
        return buildToken(parentId, "ACCESS", accessExpiration);
    }

    /**
     * Refresh Token 생성 (만료: jwt.refresh-expiration)
     */
    public String generateRefreshToken(Long parentId) {
        return buildToken(parentId, "REFRESH", refreshExpiration);
    }

    /**
     * HTTP Authorization 헤더에서 "Bearer " 접두사 제거 후 토큰 반환
     */
    public String resolveToken(String bearerToken) {
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(7);
        }
        return null;
    }

    /**
     * 토큰 유효성 검증 (서명 + 만료 확인)
     */
    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("유효하지 않은 JWT 토큰: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 토큰에서 보호자 ID 추출
     */
    public Long getParentId(String token) {
        return Long.parseLong(getClaims(token).getSubject());
    }

    /**
     * 토큰 유형 확인 (ACCESS / REFRESH)
     */
    public String getTokenType(String token) {
        return getClaims(token).get("tokenType", String.class);
    }

    /**
     * 토큰 남은 만료 시간 (ms)
     */
    public long getExpiration(String token) {
        Date expiration = getClaims(token).getExpiration();
        return expiration.getTime() - new Date().getTime();
    }

    // ─────────── private ────────────────

    private String buildToken(Long parentId, String tokenType, long expirationMs) {
        Date now = new Date();
        return Jwts.builder()
                .setSubject(String.valueOf(parentId))
                .claim("tokenType", tokenType)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + expirationMs))
                .signWith(key, signatureAlgorithm)
                .compact();
    }

    private Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
