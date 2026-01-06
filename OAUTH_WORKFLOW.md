# 네이버 OAuth 로그인 워크플로우

## 전체 흐름도

```
[프론트엔드]                [백엔드 API]                    [네이버]
    |                          |                              |
    | 1. 로그인 버튼 클릭       |                              |
    |------------------------->|                              |
    |  GET /oauth/naver/login-url                            |
    |                          |                              |
    |<-------------------------|                              |
    | 2. 네이버 로그인 URL 응답 |                              |
    | (state 포함)             |                              |
    |                          |                              |
    | 3. 네이버 로그인 페이지로 리다이렉트                     |
    |--------------------------------------------------------->|
    |                          |                    4. 사용자 로그인
    |                          |                              |
    |<---------------------------------------------------------|
    | 5. 네이버 콜백 리다이렉트                                |
    | (code, state 포함)       |                              |
    |                          |                              |
    | 6. 자동으로 백엔드 콜백으로 전달                         |
    |------------------------->|                              |
    |  GET /oauth/naver/callback?code=XXX&state=YYY          |
    |                          |                              |
    |                          | 7. 네이버에 토큰 요청         |
    |                          |----------------------------->|
    |                          |  (code로 access_token 교환)  |
    |                          |                              |
    |                          |<-----------------------------|
    |                          | 8. 네이버 access_token 응답   |
    |                          |                              |
    |                          | 9. 네이버에 사용자 정보 요청  |
    |                          |----------------------------->|
    |                          |                              |
    |                          |<-----------------------------|
    |                          | 10. 사용자 정보 응답          |
    |                          |                              |
    |                          | 11. JWT 토큰 생성             |
    |                          |  (accessToken, refreshToken) |
    |                          |                              |
    |<-------------------------|                              |
    | 12. 프론트엔드로 리다이렉트                              |
    | (JWT 토큰 포함)          |                              |
    |                          |                              |
    | 13. JWT 저장             |                              |
    | - accessToken → Zustand  |                              |
    | - refreshToken → Cookie  |                              |
```

---

## 상세 단계별 설명

### 1단계: 로그인 URL 요청 (프론트엔드 → 백엔드)

**프론트엔드 (www.ohgun.site)**:
```typescript
// mainservice.ts
const response = await fetch(`${baseUrl}/oauth/naver/login-url`, {
  method: 'GET',
});
const data = await response.json(); // { url: "...", state: "..." }
window.location.href = data.url;
```

**백엔드 (api.ohgun.site)**:
```java
// NaverController.java
@GetMapping("/login-url")
public ResponseEntity<Map<String, String>> getLoginUrl() {
    String state = UUID.randomUUID().toString();
    String url = naverService.buildAuthorizeUrl(state);
    // TODO: state를 Redis에 저장하여 CSRF 공격 방지
    return ResponseEntity.ok(Map.of("url", url, "state", state));
}
```

**응답 예시**:
```json
{
  "url": "https://nid.naver.com/oauth2.0/authorize?client_id=XXX&redirect_uri=http://api.ohgun.kr/oauth/naver/callback&response_type=code&state=abc123",
  "state": "abc123"
}
```

---

### 2단계: 네이버 로그인 페이지로 리다이렉트

**프론트엔드**:
- 사용자를 네이버 로그인 페이지로 리다이렉트
- URL: `https://nid.naver.com/oauth2.0/authorize?...`

---

### 3단계: 네이버 로그인 후 콜백 (네이버 → 백엔드)

**네이버**:
- 사용자가 로그인 후 백엔드 콜백 URL로 리다이렉트
- URL: `http://api.ohgun.kr/oauth/naver/callback?code=XXX&state=YYY`

---

### 4단계: 백엔드에서 JWT 토큰 생성

**백엔드 (NaverController.java)**:

#### 4-1. 네이버 토큰 교환
```java
// 1. 네이버 access_token 받기
NaverTokenResponse tokenResponse = naverService.exchangeToken(code, state);
```

#### 4-2. 사용자 정보 조회
```java
// 2. 네이버 사용자 정보 조회
NaverUserInfo userInfo = naverService.fetchUserInfo(tokenResponse.getAccessToken());
```

#### 4-3. JWT 토큰 생성 ⭐
```java
// 3. JWT 토큰 생성 (JwtTokenProvider 사용)
String accessToken = jwtTokenProvider.createAccessToken(
    userInfo.getId(),
    Map.of("email", userInfo.getEmail(), "name", userInfo.getName())
);
String refreshToken = jwtTokenProvider.createRefreshToken(
    userInfo.getId(),
    Map.of("email", userInfo.getEmail(), "name", userInfo.getName())
);
```

#### 4-4. 프론트엔드로 리다이렉트
```java
// 4. 프론트엔드로 리다이렉트 (JWT 토큰 전달)
String redirectUrl = "http://localhost:3000/oauth/callback"
    + "?accessToken=" + accessToken
    + "&refreshToken=" + refreshToken
    + "&provider=naver"
    + "&success=true";

return ResponseEntity.status(HttpStatus.FOUND)
    .header("Location", redirectUrl)
    .build();
```

---

### 5단계: 프론트엔드에서 JWT 저장

**프론트엔드 (www.ohgun.site/app/oauth/callback/page.tsx)**:

```typescript
// 1. URL에서 JWT 토큰 추출
const accessToken = searchParams.get('accessToken');
const refreshToken = searchParams.get('refreshToken');

// 2. Refresh Token을 HttpOnly 쿠키에 저장
if (refreshToken) {
  await storeRefreshTokenInCookie(refreshToken);
}

// 3. Access Token은 Zustand(메모리)에 저장
const payload = JSON.parse(atob(accessToken.split('.')[1]));
login(accessToken, null, {
  email: payload.email,
  name: payload.name,
});
```

---

## JWT 토큰 역할 분리

### JwtTokenProvider (토큰 생성 도구)

**역할**: JWT 토큰을 생성하고 파싱하는 **유틸리티 클래스**

```java
@Component
public class JwtTokenProvider {
    // 토큰 생성 메서드
    public String createAccessToken(String subject, Map<String, Object> claims) { }
    public String createRefreshToken(String subject, Map<String, Object> claims) { }
    
    // 토큰 파싱 메서드
    public Claims parseToken(String token) { }
}
```

**사용 위치**:
- `NaverController` - 로그인 시 토큰 발급
- `AuthController` - Refresh Token으로 Access Token 재발급 (TODO)
- `JwtAuthenticationFilter` - 요청마다 토큰 검증

**비유**: `JwtTokenProvider`는 "토큰 제조 공장"이고, `NaverController`는 "토큰을 주문하는 고객"입니다.

---

## Refresh Token 보안 설정

### 질문: `/api/auth/refresh`를 `permitAll()`에 두어도 괜찮은가?

**답변: 괜찮습니다. 하지만 조건부로 안전합니다.**

### 보안 구조

#### 1. Refresh Token의 위치
```
프론트엔드
├─ Access Token → Zustand (메모리)
└─ Refresh Token → HttpOnly Cookie
```

#### 2. `/api/auth/refresh` 엔드포인트 동작

```java
// TODO: 구현 예정
@PostMapping("/api/auth/refresh")
public ResponseEntity<?> refreshToken(HttpServletRequest request) {
    // 1. HttpOnly 쿠키에서 Refresh Token 추출
    String refreshToken = extractRefreshTokenFromCookie(request);
    
    // 2. Refresh Token 검증
    if (!jwtTokenProvider.validateToken(refreshToken)) {
        return ResponseEntity.status(401).body("Invalid refresh token");
    }
    
    // 3. 새 Access Token 생성
    Claims claims = jwtTokenProvider.parseToken(refreshToken);
    String newAccessToken = jwtTokenProvider.createAccessToken(
        claims.getSubject(),
        Map.of("email", claims.get("email"), "name", claims.get("name"))
    );
    
    // 4. 새 Access Token 반환
    return ResponseEntity.ok(Map.of("accessToken", newAccessToken));
}
```

### 왜 안전한가?

#### 보안 계층:

1. **Refresh Token은 HttpOnly Cookie에 저장**
   - JavaScript로 접근 불가 (XSS 방지)
   - 자동으로 요청에 포함됨

2. **Refresh Token 검증**
   - 서명 검증 (위조 불가)
   - 만료 시간 검증
   - 필요시 Redis에 블랙리스트 관리

3. **Refresh Token 탈취 시**
   - Access Token만 재발급됨 (짧은 만료 시간: 5-15분)
   - Refresh Token 자체는 재발급되지 않음
   - 의심스러운 활동 감지 시 Refresh Token 무효화 가능

### 위험 요소와 대응

| 위험 | 대응 |
|------|------|
| Refresh Token 탈취 | - HttpOnly Cookie로 저장<br>- HTTPS 전용 (Secure)<br>- SameSite=Strict (CSRF 방지) |
| Refresh Token 재사용 공격 | - Redis에 사용된 Refresh Token 저장<br>- Rotation (한 번 사용 후 새 Refresh Token 발급) |
| 장기간 유효한 Refresh Token | - 만료 시간 제한 (7일~30일)<br>- 의심스러운 활동 감지 시 즉시 무효화 |

---

## 보안 엔드포인트 구분

### 공개 엔드포인트 (permitAll)

| 엔드포인트 | 이유 | 보안 메커니즘 |
|-----------|------|---------------|
| `/oauth/**` | 로그인 전이므로 토큰 없음 | - CSRF state 검증<br>- 네이버 OAuth 인증 |
| `/api/auth/refresh` | Refresh Token으로 인증 | - HttpOnly Cookie<br>- 토큰 검증<br>- Redis 블랙리스트 |
| `/api/auth/verify` | 토큰 검증 자체가 목적 | - 토큰 검증 |
| `/api/auth/logout` | Refresh Token 쿠키로 인증 | - HttpOnly Cookie 삭제 |

### 보호된 엔드포인트 (authenticated)

| 엔드포인트 | JWT 필요 | 예시 |
|-----------|---------|------|
| `/api/users/**` | ✅ Access Token | 사용자 정보 조회/수정 |
| `/api/posts/**` | ✅ Access Token | 게시글 CRUD |
| `/api/admin/**` | ✅ Access Token + 관리자 권한 | 관리자 기능 |

---

## 토큰 재발급 플로우

```
[프론트엔드]                [백엔드 API]
    |                          |
    | 1. API 요청 (Access Token 만료)
    |------------------------->|
    | GET /api/users/me        |
    | Authorization: Bearer expired_token
    |                          |
    |<-------------------------|
    | 2. 401 Unauthorized      |
    |                          |
    | 3. Refresh Token으로 재발급 요청
    |------------------------->|
    | POST /api/auth/refresh   |
    | Cookie: refreshToken=XXX |
    |                          |
    |                          | 4. Refresh Token 검증
    |                          | 5. 새 Access Token 생성
    |                          |
    |<-------------------------|
    | 6. 새 Access Token 응답   |
    | { accessToken: "new_token" }
    |                          |
    | 7. Zustand에 새 Access Token 저장
    | 8. 원래 API 재요청       |
    |------------------------->|
    | GET /api/users/me        |
    | Authorization: Bearer new_token
    |                          |
    |<-------------------------|
    | 9. 정상 응답             |
```

---

## 요약

### 1. JWT 토큰 생성 위치
- **JwtTokenProvider**: 토큰 생성 도구 (유틸리티)
- **NaverController**: 로그인 시 토큰 발급 (JwtTokenProvider 사용)

### 2. Refresh Token 보안
- `/api/auth/refresh`는 `permitAll()`이지만 안전함
- HttpOnly Cookie로 Refresh Token 보호
- 토큰 검증 + Redis 블랙리스트로 이중 보안

### 3. 보안 원칙
- **Access Token**: 짧은 만료 시간 (5-15분), 메모리 저장
- **Refresh Token**: 긴 만료 시간 (7-30일), HttpOnly Cookie
- **인증 불필요 엔드포인트**: OAuth 로그인, 토큰 재발급, 토큰 검증
- **인증 필요 엔드포인트**: 일반 API (사용자 정보, 게시글 등)

이제 워크플로우가 명확해졌나요?

