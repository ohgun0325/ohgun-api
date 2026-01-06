package site.ohgun.api.oauth.config;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import site.ohgun.api.oauth.jwt.JwtTokenProvider;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * JWT 토큰을 검증하고 인증 정보를 설정하는 필터
 * 
 * 동작 방식:
 * 1. 요청 헤더에서 "Authorization: Bearer <token>" 형식의 토큰 추출
 * 2. 토큰 검증 (JwtTokenProvider 사용)
 * 3. 검증 성공 시 SecurityContext에 인증 정보 설정
 * 4. 검증 실패 시 다음 필터로 진행 (인증 실패는 SecurityConfig에서 처리)
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            // 1. 요청 헤더에서 JWT 토큰 추출
            String token = extractTokenFromRequest(request);

            // 2. 토큰이 있고 유효한 경우 인증 정보 설정
            if (StringUtils.hasText(token) && validateToken(token)) {
                Claims claims = jwtTokenProvider.parseToken(token);
                String userId = claims.getSubject();

                // 3. 인증 정보 생성 (권한은 필요시 claims에서 추출)
                List<SimpleGrantedAuthority> authorities = Collections.emptyList(); // TODO: 필요시 권한 추가
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userId,
                                null,
                                authorities
                        );
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // 4. SecurityContext에 인증 정보 설정
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            // 토큰 검증 실패 시 로그만 남기고 계속 진행
            // 인증 실패는 SecurityConfig의 authenticated()에서 처리
            logger.debug("JWT token validation failed: " + e.getMessage());
        }

        // 5. 다음 필터로 진행
        filterChain.doFilter(request, response);
    }

    /**
     * 요청 헤더에서 JWT 토큰 추출
     * 형식: "Authorization: Bearer <token>"
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    /**
     * JWT 토큰 유효성 검증
     */
    private boolean validateToken(String token) {
        try {
            jwtTokenProvider.parseToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}

