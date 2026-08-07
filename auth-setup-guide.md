# 인증·세션·인프라 구현 가이드

> 담당: 김현정  
> 작성일: 2026-08-07  
> 대상: 백엔드 팀원, 프론트엔드 팀

---

## 1. 담당 API 목록

| 메서드 | 경로 | 설명 | 인증 |
|--------|------|------|------|
| POST | `/api/auth/signup` | 일반 회원가입 | 불필요 |
| POST | `/api/auth/login` | 일반 로그인 | 불필요 |
| POST | `/api/auth/refresh` | Access Token 재발급 | 불필요 |
| GET | `/oauth2/authorization/google` | 구글 소셜 로그인 시작 | 불필요 |
| GET | `/oauth2/authorization/naver` | 네이버 소셜 로그인 시작 | 불필요 |
| GET | `/api/children` | 아이 목록 조회 | JWT 필요 |
| POST | `/api/children` | 아이 프로필 등록 | JWT 필요 |
| PATCH | `/api/children/{childId}` | 아이 프로필 수정 | JWT 필요 |
| POST | `/api/stories/{storyId}/sessions` | 새 학습 세션 시작 | JWT 필요 |
| GET | `/api/sessions/{sessionId}` | 세션 정보 조회 | JWT 필요 |

---

## 2. 기술 스택

- **Spring Boot** 4.1.0 / **Java** 17
- **Spring Security** + **JWT** (io.jsonwebtoken 0.11.5)
- **OAuth2** 소셜 로그인 (Google, Naver)
- **PostgreSQL** (Railway) + **JPA/Hibernate** (ddl-auto: update)
- **Lombok**, **Jakarta Validation**

---

## 3. DB 연결 (Railway PostgreSQL)

### Public URL (로컬 개발용)
```
Host:     zephyr.proxy.rlwy.net
Port:     57437
Database: railway
Username: postgres
Password: (팀 공유 채널 확인)
```

### JDBC URL 형식
```
jdbc:postgresql://zephyr.proxy.rlwy.net:57437/railway
```

> ⚠️ `postgres.railway.internal`은 Railway 내부 네트워크 전용 → 로컬에서 접속 불가  
> 반드시 Public URL (`zephyr.proxy.rlwy.net:57437`) 사용

---

## 4. 환경변수 목록 (.env)

프로젝트 루트에 `.env` 파일 생성 후 아래 값 입력:

```env
# ────────────────────────────────────
# Railway PostgreSQL
# ────────────────────────────────────
DB_URL=jdbc:postgresql://zephyr.proxy.rlwy.net:57437/railway
DB_USERNAME=postgres
DB_PASSWORD=<팀 공유 채널 확인>

# ────────────────────────────────────
# JPA / HikariCP
# ────────────────────────────────────
JPA_SHOW_SQL=true
HIKARI_MAX_POOL_SIZE=5
HIKARI_MIN_IDLE=2

# ────────────────────────────────────
# JWT
# ────────────────────────────────────
JWT_SECRET_KEY=<Base64 인코딩된 256bit 이상 키>
JWT_EXPIRATION_TIME=86400000        # Access Token 만료: 24시간 (ms)
JWT_REFRESH_EXPIRATION_TIME=604800000  # Refresh Token 만료: 7일 (ms)

# ────────────────────────────────────
# OAuth2 - Google
# https://console.cloud.google.com
# ────────────────────────────────────
GOOGLE_CLIENT_ID=<구글 클라이언트 ID>
GOOGLE_CLIENT_SECRET=<구글 클라이언트 시크릿>

# ────────────────────────────────────
# OAuth2 - Naver
# https://developers.naver.com
# ────────────────────────────────────
NAVER_CLIENT_ID=<네이버 클라이언트 ID>
NAVER_CLIENT_SECRET=<네이버 클라이언트 시크릿>

# ────────────────────────────────────
# OAuth2 Redirect (프론트엔드 콜백 URL)
# ────────────────────────────────────
OAUTH2_REDIRECT_URI=http://localhost:3000/oauth/callback
OAUTH2_FAILURE_REDIRECT_URI=http://localhost:3000/login?error=true

# ────────────────────────────────────
# CORS
# ────────────────────────────────────
CORS_ALLOWED_ORIGINS=http://localhost:3000
```

### IntelliJ에서 .env 파일 로드하는 방법
1. 상단 메뉴 → **실행/디버그 구성 편집**
2. 해당 Spring Boot 구성 클릭
3. **환경 변수** 필드 우측 📁 아이콘 클릭
4. `.env` 파일 선택 (숨김 파일 안 보이면 `Cmd + Shift + .`)
5. **적용** → **확인**

---

## 5. application.yaml 구조

```yaml
spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  jpa:
    hibernate.ddl-auto: update    # 로컬/개발용, 운영 시 validate로 변경
    show-sql: ${JPA_SHOW_SQL:false}
  security.oauth2.client:
    registration:
      google:
        client-id: ${GOOGLE_CLIENT_ID:disabled}   # :disabled → 미설정 시 비활성화
        client-secret: ${GOOGLE_CLIENT_SECRET:disabled}
        scope: [email, profile]
      naver:
        client-id: ${NAVER_CLIENT_ID:disabled}
        client-secret: ${NAVER_CLIENT_SECRET:disabled}
        # Spring이 네이버를 기본 지원 안 해서 provider 별도 설정
    provider:
      naver:
        authorization-uri: https://nid.naver.com/oauth2.0/authorize
        token-uri: https://nid.naver.com/oauth2.0/token
        user-info-uri: https://openapi.naver.com/v1/nid/me
        user-name-attribute: response

jwt:
  secret: ${JWT_SECRET_KEY}
  access-expiration: ${JWT_EXPIRATION_TIME:3600000}
  refresh-expiration: ${JWT_REFRESH_EXPIRATION_TIME:604800000}

oauth2:
  redirect-uri: ${OAUTH2_REDIRECT_URI:http://localhost:3000/oauth/callback}
  failure-redirect-uri: ${OAUTH2_FAILURE_REDIRECT_URI:http://localhost:3000/login?error=true}

cors:
  allowed-origins: ${CORS_ALLOWED_ORIGINS:http://localhost:3000}

server:
  port: ${PORT:8080}
```

---

## 6. JWT 방식

### 토큰 구조
- **Access Token**: 24시간 만료, 매 API 요청 헤더에 포함
- **Refresh Token**: 7일 만료, DB(`auth_tokens` 테이블)에 저장

### 토큰 페이로드
```json
{
  "sub": "1",           // parentId (보호자 ID)
  "tokenType": "ACCESS" // or "REFRESH"
}
```

### 사용 방법 (프론트엔드)
```
Authorization: Bearer <access_token>
```

### 토큰 재발급 흐름
```
1. API 호출 → 401 응답
2. POST /api/auth/refresh { "refreshToken": "..." } 호출
3. 새 accessToken + refreshToken 발급 (Refresh Token Rotation)
4. 재시도
```

### DB 저장 테이블: auth_tokens
| 컬럼 | 설명 |
|------|------|
| parent_id | 보호자 ID (FK) |
| refresh_token | 저장된 Refresh Token |
| expires_at | 만료 시각 |

---

## 7. 일반 로그인/회원가입 API

### 회원가입
```
POST /api/auth/signup
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123",
  "name": "홍길동"
}
```

**응답 (201)**
```json
{
  "success": true,
  "message": "회원가입이 완료되었습니다.",
  "data": {
    "accessToken": "eyJhbGci...",
    "refreshToken": "eyJhbGci...",
    "parentId": 1,
    "name": "홍길동"
  }
}
```

### 로그인
```
POST /api/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123"
}
```

**응답 (200)** — 회원가입과 동일한 구조

### 토큰 재발급
```
POST /api/auth/refresh
Content-Type: application/json

{
  "refreshToken": "eyJhbGci..."
}
```

### 에러 코드
| 코드 | HTTP | 설명 |
|------|------|------|
| AUTH_001 | 409 | 이미 사용 중인 이메일 |
| AUTH_002 | 401 | 이메일 또는 비밀번호 불일치 |
| AUTH_003 | 409 | 소셜 로그인으로 가입된 계정 (일반 로그인 시도 시) |
| AUTH_004 | 401 | 유효하지 않은 토큰 |
| AUTH_005 | 401 | 만료된 토큰 |
| AUTH_006 | 401 | Refresh Token 불일치 |

---

## 8. 소셜 로그인 (Google / Naver)

### 흐름
```
1. 프론트: GET /oauth2/authorization/google (또는 /naver)
2. 소셜 제공자 로그인 페이지로 리다이렉트
3. 로그인 성공 → Spring이 /login/oauth2/code/{provider} 콜백 처리
4. CustomOAuth2UserService → 이메일로 기존 회원 조회 or 신규 가입
5. OAuth2SuccessHandler → Access/Refresh Token 발급 후 프론트 콜백 URL로 리다이렉트
```

### 콜백 URL (프론트엔드)
```
성공: {OAUTH2_REDIRECT_URI}?accessToken=...&refreshToken=...&parentId=...&name=...
실패: {OAUTH2_FAILURE_REDIRECT_URI}
```
예시:
```
http://localhost:3000/oauth/callback?accessToken=eyJ...&refreshToken=eyJ...&parentId=1&name=홍길동
```

### 소셜 로그인 계정 구조
- 소셜 로그인은 비밀번호 없음 (`password = null`)
- `OAuthProvider`: LOCAL / GOOGLE / NAVER
- `providerId`: 각 소셜 제공자의 고유 사용자 ID

### 구글 OAuth2 콘솔 설정
- 승인된 리다이렉트 URI: `http://localhost:8080/login/oauth2/code/google`
- Railway 배포 후: `https://<railway-domain>/login/oauth2/code/google`

### 네이버 개발자 센터 설정
- 서비스 URL: `http://localhost:8080`
- Callback URL: `http://localhost:8080/login/oauth2/code/naver`

---

## 9. Security 설정

### 인증 없이 접근 가능한 엔드포인트 (permitAll)
```
POST /api/auth/signup
POST /api/auth/login
POST /api/auth/refresh
GET  /oauth2/**
GET  /login/oauth2/**
```

### 나머지 모든 엔드포인트 → JWT 인증 필수

### 인증 실패 시 응답
```json
{
  "success": false,
  "message": "인증이 필요합니다.",
  "data": null
}
```

### JWT 에러 응답 형식 (JwtFilter)
| 상황 | HTTP | message |
|------|------|---------|
| 서명 불일치 | 401 | 유효하지 않은 JWT 서명입니다. |
| 형식 오류 | 401 | 잘못된 JWT 형식입니다. |
| 만료 | 401 | 만료된 JWT 토큰입니다. |
| 미지원 | 401 | 지원하지 않는 JWT 토큰입니다. |

---

## 10. 주요 파일 목록

```
src/main/java/com/potential/goodquestion/
│
├── common/
│   ├── base/BaseEntity.java                    # createdAt, updatedAt 자동 관리
│   ├── code/
│   │   ├── ErrorCode.java                      # 에러 코드 인터페이스
│   │   ├── AuthErrorCode.java                  # AUTH_001~006
│   │   ├── ChildErrorCode.java                 # CHILD_001~003
│   │   └── SessionErrorCode.java               # SESSION_001~005
│   ├── config/
│   │   ├── SecurityConfig.java                 # JWT + OAuth2 + CORS 설정
│   │   └── JacksonConfig.java                  # ObjectMapper Bean (JavaTimeModule 포함)
│   ├── exception/CustomException.java          # 공통 예외
│   ├── jwt/
│   │   ├── JwtUtil.java                        # Access/Refresh Token 생성·검증
│   │   └── JwtFilter.java                      # 요청마다 JWT 검증 필터
│   ├── response/ApiResponse.java               # 공통 응답 형식
│   └── security/
│       ├── CustomUserPrincipal.java            # UserDetails 구현 (일반 로그인)
│       ├── CustomOAuth2User.java               # OAuth2User 구현 (소셜 로그인)
│       └── CustomUserDetailsService.java       # DB에서 사용자 로드
│
└── domain/
    ├── auth/
    │   ├── controller/AuthController.java      # /api/auth/*
    │   ├── service/AuthService.java            # signup, login, refresh
    │   ├── dto/AuthRequestDto.java             # Signup, Login, Refresh
    │   ├── dto/AuthResponseDto.java            # TokenResponse
    │   └── oauth/
    │       ├── OAuth2UserInfo.java             # 소셜 제공자 정보 인터페이스
    │       ├── GoogleUserInfo.java             # 구글 응답 파싱
    │       ├── NaverUserInfo.java              # 네이버 응답 파싱
    │       ├── OAuth2UserInfoFactory.java      # 제공자별 파싱 팩토리
    │       ├── CustomOAuth2UserService.java    # 소셜 로그인 처리
    │       ├── OAuth2SuccessHandler.java       # 성공 시 토큰 발급 + 리다이렉트
    │       └── OAuth2FailureHandler.java       # 실패 시 에러 리다이렉트
    ├── parent/
    │   ├── entity/Parent.java                  # 보호자 엔티티 (id, email, password, name, provider)
    │   ├── enums/OAuthProvider.java            # LOCAL, GOOGLE, NAVER
    │   └── repository/ParentRepository.java
    ├── child/
    │   ├── entity/Child.java                   # 아이 엔티티 (id, parent, name, age)
    │   ├── controller/ChildController.java     # /api/children
    │   ├── service/ChildService.java           # 아이 목록·등록·수정 (MVP: 1명 제한)
    │   ├── dto/ChildRequestDto.java            # Create, Update
    │   ├── dto/ChildResponseDto.java           # ChildInfo
    │   └── repository/ChildRepository.java
    └── session/
        ├── entity/StorySession.java            # 세션 엔티티 (story_sessions 테이블)
        ├── controller/SessionController.java   # /api/stories/{id}/sessions, /api/sessions/{id}
        ├── service/SessionService.java         # 세션 생성·조회
        ├── dto/SessionRequestDto.java          # Create
        ├── dto/SessionResponseDto.java         # SessionInfo
        └── repository/StorySessionRepository.java
```

---

## 11. 공통 응답 형식

**성공**
```json
{
  "success": true,
  "message": "요청이 성공했습니다.",
  "data": { ... }
}
```

**실패**
```json
{
  "success": false,
  "message": "에러 메시지",
  "data": null
}
```

---

## 12. CORS 설정

- 허용 Origin: `.env`의 `CORS_ALLOWED_ORIGINS` 값 (여러 개면 쉼표 구분)
- 허용 메서드: GET, POST, PUT, DELETE, PATCH, OPTIONS
- 허용 헤더: `*`
- `credentials: true` (쿠키/인증 헤더 포함 허용)
- `Authorization` 헤더 노출

---

## 13. 아이 수 제한 (MVP)

현재 MVP에서는 **보호자 계정당 아이 1명**만 등록 가능.
정식 서비스에서는 최대 3명으로 확장 예정.

제거 또는 변경 위치: `ChildService.java` → `createChild()` 메서드 내 "MVP 아이 수 제한" 블록
```java
// 이 블록 삭제 또는 MAX_CHILDREN = 3 으로 변경
final int MAX_CHILDREN = 1;
int childCount = childRepository.findAllByParent(parent).size();
if (childCount >= MAX_CHILDREN) {
    throw new CustomException(ChildErrorCode.CHILD_LIMIT_EXCEEDED);
}
```

---

## 14. 전우선 담당 API 연동 시 참고

전우선 UtteranceService는 아래 방식으로 세션을 조회함:
```java
sessionRepository.findByIdAndStatus(sessionId, "IN_PROGRESS")
```

→ `StorySessionRepository.findByIdAndStatus(Long id, String status)` 메서드 사용

전우선이 StoryScene 엔티티 생성 후 교체해야 할 필드:
- `StorySession.currentSceneId (Long)` → `StorySession.currentScene (StoryScene)`
- `StorySession.advanceToNextScene(Long nextSceneId)` → `advanceScene(StoryScene nextScene)`
