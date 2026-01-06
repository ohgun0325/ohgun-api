package site.ohgun.api.oauth.naver;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;
import site.ohgun.api.oauth.jwt.JwtTokenProvider;
import site.ohgun.api.oauth.naver.dto.NaverTokenResponse;
import site.ohgun.api.oauth.naver.dto.NaverUserInfo;
import site.ohgun.api.oauth.naver.dto.OAuthLoginResponse;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/oauth/naver")
@RequiredArgsConstructor
public class NaverController {

    private final NaverService naverService;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${oauth.frontend.redirect-url:http://localhost:3000}")
    private String frontendRedirectUrl;

    @GetMapping("/login-url")
    public ResponseEntity<Map<String, String>> getLoginUrl() {
        String state = UUID.randomUUID().toString();
        String url = naverService.buildAuthorizeUrl(state);
        // TODO: state? Redis? ???? CSRF ?? ??
        return ResponseEntity.ok(Map.of("url", url, "state", state));
    }

    @GetMapping("/callback")
    public ResponseEntity<?> callback(
            @RequestParam String code,
            @RequestParam String state
    ) {
        try {
            // 1. ??? ?? ??
            NaverTokenResponse tokenResponse = naverService.exchangeToken(code, state);
            if (tokenResponse == null || tokenResponse.getAccessToken() == null) {
                throw new RuntimeException("??? ?? ?? ??");
            }

            // 2. ??? ?? ??
            NaverUserInfo userInfo = naverService.fetchUserInfo(tokenResponse.getAccessToken());
            if (userInfo == null || userInfo.getId() == null) {
                throw new RuntimeException("??? ?? ?? ??");
            }

            // 3. JWT ?? ??
            String accessToken = jwtTokenProvider.createAccessToken(
                    userInfo.getId(),
                    Map.of("email", userInfo.getEmail() != null ? userInfo.getEmail() : "",
                           "name", userInfo.getName() != null ? userInfo.getName() : "")
            );
            String refreshToken = jwtTokenProvider.createRefreshToken(
                    userInfo.getId(),
                    Map.of("email", userInfo.getEmail() != null ? userInfo.getEmail() : "",
                           "name", userInfo.getName() != null ? userInfo.getName() : "")
            );

            // 4. ??? ?? ??
            OAuthLoginResponse.UserInfo userInfoDto = OAuthLoginResponse.UserInfo.builder()
                    .id(userInfo.getId())
                    .email(userInfo.getEmail())
                    .nickname(userInfo.getNickname())
                    .name(userInfo.getName())
                    .profileImage(null) // ??? API?? ???? ?? ????? null ??
                    .build();

            OAuthLoginResponse response = OAuthLoginResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .user(userInfoDto)
                    .provider("naver")
                    .build();

            // ??? ?? ?? ??
            System.out.println("========================================");
            System.out.println("??? ??? ??!");
            System.out.println("??? ID: " + userInfo.getId());
            System.out.println("???: " + (userInfo.getEmail() != null ? userInfo.getEmail() : "N/A"));
            System.out.println("??: " + (userInfo.getName() != null ? userInfo.getName() : "N/A"));
            System.out.println("???: " + (userInfo.getNickname() != null ? userInfo.getNickname() : "N/A"));
            System.out.println("========================================");

            // 5. ?????? ????? (??? URL ?? ????? ??)
            String redirectUrl = UriComponentsBuilder.fromUriString(frontendRedirectUrl)
                    .path("/oauth/callback")
                    .queryParam("accessToken", accessToken)
                    .queryParam("refreshToken", refreshToken)
                    .queryParam("provider", "naver")
                    .queryParam("success", "true")
                    .build()
                    .toUriString();

            return ResponseEntity.status(HttpStatus.FOUND)
                    .header("Location", redirectUrl)
                    .build();
        } catch (Exception e) {
            // ?? ??
            System.err.println("OAuth callback error: " + e.getMessage());
            e.printStackTrace();

            // ?? ?? ? ?????? ?????
            String errorRedirectUrl = UriComponentsBuilder.fromUriString(frontendRedirectUrl)
                    .path("/oauth/error")
                    .queryParam("error", e.getMessage() != null ? e.getMessage() : "??? ?? ? ??? ??????.")
                    .queryParam("provider", "naver")
                    .build()
                    .toUriString();

            return ResponseEntity.status(HttpStatus.FOUND)
                    .header("Location", errorRedirectUrl)
                    .build();
        }
    }
}
