package site.ohgun.api.oauth.jwt;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private String secret = "default-secret-key-change-in-production-min-256-bits";
    private long accessTokenValidityInSeconds = 86400; // 24시간
    private long refreshTokenValidityInSeconds = 2592000; // 30일
}

