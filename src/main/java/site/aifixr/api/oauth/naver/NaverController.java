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
        // TODO: state??Redis ?±Ï????Ä?•Ìï¥??CSRF Î∞©Ï?
        return ResponseEntity.ok(Map.of("url", url, "state", state));
    }

    @GetMapping("/callback")
    public ResponseEntity<?> callback(
            @RequestParam String code,
            @RequestParam String state
    ) {
        try {
            // 1. ?§Ïù¥Î≤??†ÌÅ∞ ÍµêÌôò
            NaverTokenResponse tokenResponse = naverService.exchangeToken(code, state);
            if (tokenResponse == null || tokenResponse.getAccessToken() == null) {
                throw new RuntimeException("?§Ïù¥Î≤??†ÌÅ∞ ÍµêÌôò ?§Ìå®");
            }

            // 2. ?¨Ïö©???ïÎ≥¥ Ï°∞Ìöå
            NaverUserInfo userInfo = naverService.fetchUserInfo(tokenResponse.getAccessToken());
            if (userInfo == null || userInfo.getId() == null) {
                throw new RuntimeException("?¨Ïö©???ïÎ≥¥ Ï°∞Ìöå ?§Ìå®");
            }

            // 3. JWT ?†ÌÅ∞ ?ùÏÑ±
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

            // 4. ?ëÎãµ Í∞ùÏ≤¥ ?ùÏÑ±
            OAuthLoginResponse.UserInfo userInfoDto = OAuthLoginResponse.UserInfo.builder()
                    .id(userInfo.getId())
                    .email(userInfo.getEmail())
                    .nickname(userInfo.getNickname())
                    .name(userInfo.getName())
                    .profileImage(null) // ?§Ïù¥Î≤?API?êÏÑú ?ÑÎ°ú???¥Î?ÏßÄ ?ïÎ≥¥Í∞Ä ?àÏúºÎ©?Ï∂îÍ?
                    .build();

            OAuthLoginResponse response = OAuthLoginResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .user(userInfoDto)
                    .provider("naver")
                    .build();

            // Î°úÍ∑∏???±Í≥µ Î©îÏãúÏßÄ Ï∂úÎ†•
            System.out.println("========================================");
            System.out.println("???§Ïù¥Î≤?Î°úÍ∑∏???±Í≥µ!");
            System.out.println("?¨Ïö©??ID: " + userInfo.getId());
            System.out.println("?¥Î©î?? " + (userInfo.getEmail() != null ? userInfo.getEmail() : "N/A"));
            System.out.println("?¥Î¶Ñ: " + (userInfo.getName() != null ? userInfo.getName() : "N/A"));
            System.out.println("?âÎÑ§?? " + (userInfo.getNickname() != null ? userInfo.getNickname() : "N/A"));
            System.out.println("========================================");

            // 5. ?ÑÎ°†?∏Ïóî?úÎ°ú Î¶¨Îã§?¥Î†â??(?†ÌÅ∞??URL ?åÎùºÎØ∏ÌÑ∞Î°??ÑÎã¨)
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
            // ?êÎü¨ Î°úÍπÖ
            System.err.println("OAuth callback error: " + e.getMessage());
            e.printStackTrace();
            
            // ?êÎü¨ Î∞úÏÉù ???ÑÎ°†?∏Ïóî?úÎ°ú Î¶¨Îã§?¥Î†â??
            String errorRedirectUrl = UriComponentsBuilder.fromUriString(frontendRedirectUrl)
                    .path("/oauth/error")
                    .queryParam("error", e.getMessage() != null ? e.getMessage() : "Î°úÍ∑∏??Ï≤òÎ¶¨ Ï§??§Î•òÍ∞Ä Î∞úÏÉù?àÏäµ?àÎã§.")
                    .queryParam("provider", "naver")
                    .build()
                    .toUriString();
            
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header("Location", errorRedirectUrl)
                    .build();
        }
    }
}

