package site.ohgun.api.oauth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.http.HttpMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        // OPTIONS 요청은 모든 경로에서 허용 (CORS Preflight)
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // 공개 엔드포인트 (인증 불필요)
                        .requestMatchers(
                                "/",
                                "/actuator/**",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/docs",
                                "/docs/**",
                                "/oauth/**",              // OAuth 로그인 플로우 (로그인 전이므로 인증 불필요)
                                "/api/auth/refresh",       // Refresh Token으로 Access Token 재발급 (Refresh Token으로 인증)
                                "/api/auth/verify",        // 토큰 검증 (토큰을 검증하는 것이므로 인증 불필요)
                                "/api/auth/logout"        // 로그아웃 (Refresh Token 쿠키로 처리)
                        ).permitAll()
                        // 나머지 API는 JWT 토큰 인증 필요
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().authenticated()
                )
                .httpBasic(httpBasic -> httpBasic.disable())
                .formLogin(formLogin -> formLogin.disable());

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // 허용할 Origin 목록
        configuration.setAllowedOrigins(List.of(
                "http://localhost:3000",
                "http://localhost:3002",
                "http://127.0.0.1:3000",
                "http://127.0.0.1:3002",
                "https://ohgun.kr",            // Vercel 프로덕션 도메인 (www 없이)
                "https://www.ohgun.kr",        // Vercel 프로덕션 도메인 (www 포함)
                "https://admin.ohgun.kr"       // Admin 사이트 (필요시)
                // Vercel 프리뷰 도메인은 배포 후 실제 URL을 추가해야 함
                // 예: "https://your-project-git-branch.vercel.app"
        ));
        
        // 허용할 HTTP 메서드 (OPTIONS는 Preflight 요청에 필수)
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        
        // 허용할 헤더 (모든 헤더 허용)
        configuration.setAllowedHeaders(List.of("*"));
        
        // 인증 정보(쿠키, Authorization 헤더) 허용
        configuration.setAllowCredentials(true);
        
        // 클라이언트에서 접근 가능한 응답 헤더
        configuration.setExposedHeaders(Arrays.asList(
                "Authorization", 
                "Content-Type",
                "Access-Control-Allow-Origin",
                "Access-Control-Allow-Credentials"
        ));
        
        // Preflight 요청 캐시 시간 (1시간)
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}

