package site.ohgun.api.oauth.naver.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OAuthLoginResponse {
    private String accessToken;
    private String refreshToken;
    private UserInfo user;
    private String provider;

    @Data
    @Builder
    public static class UserInfo {
        private String id;
        private String email;
        private String nickname;
        private String name;
        private String profileImage;
    }
}

