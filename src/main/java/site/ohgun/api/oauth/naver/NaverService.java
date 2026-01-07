package site.ohgun.api.oauth.naver;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import site.ohgun.api.oauth.naver.dto.NaverTokenResponse;
import site.ohgun.api.oauth.naver.dto.NaverUserInfo;

@Service
@RequiredArgsConstructor
public class NaverService {

    private final RestTemplate restTemplate;

    @Value("${oauth.naver.client-id:}")
    private String clientId;

    @Value("${oauth.naver.client-secret:}")
    private String clientSecret;

    @Value("${oauth.naver.redirect-uri:}")
    private String redirectUri;

    @Value("${oauth.naver.authorize-url:https://nid.naver.com/oauth2.0/authorize}")
    private String authorizeUrl;

    @Value("${oauth.naver.token-url:https://nid.naver.com/oauth2.0/token}")
    private String tokenUrl;

    @Value("${oauth.naver.user-info-url:https://openapi.naver.com/v1/nid/me}")
    private String userInfoUrl;

    public String buildAuthorizeUrl(String state) {
        String url = UriComponentsBuilder.fromHttpUrl(authorizeUrl)
                .queryParam("response_type", "code")
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("state", state)
                .build()
                .toUriString();

        return url;
    }

    public NaverTokenResponse exchangeToken(String code, String state) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("code", code);
        params.add("state", state);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        return restTemplate.exchange(
                tokenUrl,
                HttpMethod.POST,
                request,
                NaverTokenResponse.class
        ).getBody();
    }

    public NaverUserInfo fetchUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        return restTemplate.exchange(
                userInfoUrl,
                HttpMethod.GET,
                request,
                NaverUserInfo.class
        ).getBody();
    }
}

