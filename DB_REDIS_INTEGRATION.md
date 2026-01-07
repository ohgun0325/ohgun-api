# Upstash Redis & Neon PostgreSQL 통합 가이드

## 개요

이 문서는 Upstash Redis와 Neon PostgreSQL을 활용한 인증 시스템 구현을 설명합니다.

---

## 아키텍처

### 데이터 저장 전략

| 데이터 타입 | 저장 위치 | 용도 |
|------------|----------|------|
| **Refresh Token** | Upstash Redis | 토큰 저장/관리, 회전, 블랙리스트, 강제 로그아웃 |
| **User 정보** | Neon PostgreSQL | 유저/권한 정보 저장 |
| **Login History** | Neon PostgreSQL | 로그인 이력, 보안 감사 |

---

## 구현된 기능

### 1. User 엔티티 (Neon PostgreSQL)

**파일:** `api.ohgun.site/src/main/java/site/ohgun/api/user/entity/User.java`

**역할:**
- OAuth 제공자 정보 저장 (naver, kakao 등)
- 사용자 기본 정보 (이메일, 이름, 닉네임, 프로필 이미지)
- 권한 정보 (ROLE_USER, ROLE_ADMIN)
- 마지막 로그인 시간

**주요 필드:**
```java
- oauthProvider: OAuth 제공자 (naver, kakao 등)
- oauthProviderId: OAuth 제공자에서의 사용자 ID
- email: 이메일
- name: 이름
- nickname: 닉네임
- role: 권한 (기본값: ROLE_USER)
- enabled: 계정 활성화 여부
- lastLoginAt: 마지막 로그인 시간
```

---

### 2. LoginHistory 엔티티 (Neon PostgreSQL)

**파일:** `api.ohgun.site/src/main/java/site/ohgun/api/user/entity/LoginHistory.java`

**역할:**
- 로그인 시도 기록
- 로그인 성공/실패 기록
- 보안 감사(Audit) 용도

**주요 필드:**
```java
- user: 사용자 (외래키)
- oauthProvider: OAuth 제공자
- success: 로그인 성공 여부
- failureReason: 실패 사유
- ipAddress: IP 주소
- userAgent: User-Agent
- loginAt: 로그인 시간
```

---

### 3. RefreshTokenService (Upstash Redis)

**파일:** `api.ohgun.site/src/main/java/site/ohgun/api/oauth/redis/RefreshTokenService.java`

**역할:**
- Refresh Token 저장/조회/삭제
- Refresh Token 회전 (Rotation)
- Refresh Token 블랙리스트 관리
- 강제 로그아웃 처리

**주요 메서드:**

#### `saveRefreshToken(userId, refreshToken, expirationSeconds)`
- Refresh Token을 Redis에 저장
- 사용자별 토큰 목록에 추가

#### `getUserIdByRefreshToken(refreshToken)`
- Refresh Token으로 사용자 ID 조회

#### `isValidRefreshToken(refreshToken)`
- Refresh Token 유효성 검증 (블랙리스트 확인 포함)

#### `deleteRefreshToken(refreshToken)`
- Refresh Token 삭제 (로그아웃)

#### `deleteAllUserTokens(userId)`
- 사용자의 모든 Refresh Token 삭제 (강제 로그아웃)

#### `rotateRefreshToken(userId, oldRefreshToken, newRefreshToken, expirationSeconds)`
- Refresh Token 회전
- 이전 토큰을 블랙리스트에 추가
- 새 토큰 저장

#### `blacklistToken(refreshToken, expirationSeconds)`
- Refresh Token을 블랙리스트에 추가

#### `isBlacklisted(refreshToken)`
- Refresh Token이 블랙리스트에 있는지 확인

**Redis Key 구조:**
```
refresh_token:{refreshToken} -> userId (TTL: 만료 시간)
blacklist:{refreshToken} -> "blacklisted" (TTL: 만료 시간)
user_tokens:{userId} -> Set<refreshToken> (TTL: 만료 시간)
```

---

### 4. UserService (Neon PostgreSQL)

**파일:** `api.ohgun.site/src/main/java/site/ohgun/api/user/service/UserService.java`

**역할:**
- 사용자 생성/조회/업데이트
- 로그인 이력 저장

**주요 메서드:**

#### `findOrCreateUser(...)`
- OAuth 제공자와 제공자 ID로 사용자 찾기 또는 생성
- 기존 사용자면 정보 업데이트 (이름, 닉네임, 프로필 이미지)
- 새 사용자면 생성

#### `saveLoginHistory(...)`
- 로그인 이력 저장
- 성공/실패 여부, IP 주소, User-Agent 저장

---

### 5. NaverController 업데이트

**파일:** `api.ohgun.site/src/main/java/site/ohgun/api/oauth/naver/NaverController.java`

**변경 사항:**
1. 사용자 저장/조회 (UserService 사용)
2. Refresh Token을 Redis에 저장 (RefreshTokenService 사용)
3. 로그인 이력 저장 (UserService 사용)
4. IP 주소 및 User-Agent 추출

**로그인 플로우:**
```
1. 네이버 OAuth 토큰 교환
2. 네이버 사용자 정보 조회
3. 사용자 저장/업데이트 (Neon PostgreSQL)
4. JWT 토큰 생성
5. Refresh Token을 Redis에 저장 (Upstash Redis)
6. 로그인 이력 저장 (Neon PostgreSQL)
7. 프론트엔드로 리다이렉트
```

---

### 6. AuthController 구현

**파일:** `api.ohgun.site/src/main/java/site/ohgun/api/oauth/AuthController.java`

**엔드포인트:**

#### `POST /api/auth/refresh`
- Refresh Token으로 Access Token 재발급
- Refresh Token 회전 적용
- 새 Refresh Token을 HttpOnly 쿠키에 설정

**요청:**
- Cookie: `refreshToken` (HttpOnly)

**응답:**
```json
{
  "accessToken": "eyJ...",
  "message": "토큰이 재발급되었습니다."
}
```

#### `POST /api/auth/verify`
- 토큰 검증

**요청:**
```json
{
  "token": "eyJ..."
}
```

**응답:**
```json
{
  "valid": true,
  "userId": 1,
  "email": "user@example.com",
  "role": "ROLE_USER",
  "exp": 1234567890
}
```

#### `POST /api/auth/logout`
- 로그아웃 (서버 측 처리)
- Refresh Token 삭제 및 블랙리스트 추가
- HttpOnly 쿠키 삭제

**요청:**
- Cookie: `refreshToken` (HttpOnly)

**응답:**
```json
{
  "message": "로그아웃되었습니다."
}
```

---

## 데이터베이스 스키마

### users 테이블 (Neon PostgreSQL)

```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    oauth_provider VARCHAR(20) NOT NULL,
    oauth_provider_id VARCHAR(100) NOT NULL,
    email VARCHAR(255),
    name VARCHAR(100),
    nickname VARCHAR(100),
    profile_image_url VARCHAR(500),
    role VARCHAR(20) NOT NULL DEFAULT 'ROLE_USER',
    enabled BOOLEAN NOT NULL DEFAULT true,
    last_login_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_email ON users(email);
CREATE INDEX idx_oauth_provider_id ON users(oauth_provider, oauth_provider_id);
```

### login_history 테이블 (Neon PostgreSQL)

```sql
CREATE TABLE login_history (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    oauth_provider VARCHAR(20) NOT NULL,
    success BOOLEAN NOT NULL,
    failure_reason VARCHAR(500),
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    login_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_user_id ON login_history(user_id);
CREATE INDEX idx_login_at ON login_history(login_at);
CREATE INDEX idx_success ON login_history(success);
```

---

## 보안 기능

### 1. Refresh Token 회전 (Rotation)
- 새 Access Token 발급 시 Refresh Token도 함께 재발급
- 이전 Refresh Token을 블랙리스트에 추가
- 토큰 탈취 시 피해 최소화

### 2. 블랙리스트 관리
- 로그아웃한 Refresh Token을 블랙리스트에 추가
- 블랙리스트에 있는 토큰은 사용 불가

### 3. 강제 로그아웃
- 사용자의 모든 Refresh Token 삭제 가능
- 보안 사고 시 즉시 대응 가능

### 4. 로그인 이력 추적
- 모든 로그인 시도 기록
- IP 주소, User-Agent 저장
- 보안 감사 용도

---

## 사용 예시

### 로그인 플로우

1. **네이버 로그인 클릭**
   ```
   GET /oauth/naver/login-url
   ```

2. **네이버 인증 완료 후 콜백**
   ```
   GET /oauth/naver/callback?code=XXX&state=YYY
   ```
   - 사용자 저장/업데이트 (Neon PostgreSQL)
   - Refresh Token 저장 (Upstash Redis)
   - 로그인 이력 저장 (Neon PostgreSQL)
   - 프론트엔드로 리다이렉트

3. **Access Token 재발급**
   ```
   POST /api/auth/refresh
   Cookie: refreshToken=eyJ...
   ```
   - Refresh Token 검증 (Upstash Redis)
   - 새 Access Token 발급
   - Refresh Token 회전

4. **로그아웃**
   ```
   POST /api/auth/logout
   Cookie: refreshToken=eyJ...
   ```
   - Refresh Token 삭제 (Upstash Redis)
   - 블랙리스트 추가

---

## 환경 변수

### Neon PostgreSQL
```bash
NEON_DB_HOST=ep-dark-violet-a1dtvvt8-pooler.ap-southeast-1.aws.neon.tech
NEON_DB_NAME=neondb
NEON_DB_USER=neondb_owner
NEON_DB_PASSWORD=your_password
```

### Upstash Redis
```bash
UPSTASH_REDIS_HOST=ample-puma-6304.upstash.io
UPSTASH_REDIS_PORT=6379
UPSTASH_REDIS_PASSWORD=your_password
```

### JPA 설정
```bash
JPA_DDL_AUTO=update  # 개발 환경 (테이블 자동 생성)
JPA_DDL_AUTO=validate # 프로덕션 환경 (스키마 검증만)
```

---

## 다음 단계

1. **테이블 생성**
   - 개발 환경: `JPA_DDL_AUTO=update`로 자동 생성
   - 프로덕션 환경: SQL 스크립트로 수동 생성 권장

2. **프론트엔드 연동**
   - Refresh Token 재발급 API 호출
   - 로그아웃 API 호출

3. **모니터링**
   - Redis 메모리 사용량 모니터링
   - 로그인 이력 분석

4. **보안 강화**
   - IP 기반 이상 로그인 감지
   - 다중 디바이스 관리

---

## 참고사항

- **Refresh Token 만료 시간:** 30일 (기본값)
- **Access Token 만료 시간:** 24시간 (기본값)
- **블랙리스트 TTL:** Refresh Token 만료 시간과 동일
- **Redis Key TTL:** Refresh Token 만료 시간과 동일

