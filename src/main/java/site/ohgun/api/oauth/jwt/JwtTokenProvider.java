package site.ohgun.api.oauth.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;

    public String createAccessToken(String subject, Map<String, Object> claims) {
        return createToken(subject, claims, jwtProperties.getAccessTokenValidityInSeconds());
    }

    public String createRefreshToken(String subject, Map<String, Object> claims) {
        return createToken(subject, claims, jwtProperties.getRefreshTokenValidityInSeconds());
    }

    private String createToken(String subject, Map<String, Object> claims, long validitySeconds) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(validitySeconds);

        Map<String, Object> mutableClaims = new HashMap<>(claims);

        return Jwts.builder()
                .setSubject(subject)
                .setClaims(mutableClaims)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiry))
                .signWith(SignatureAlgorithm.HS256, jwtProperties.getSecret())
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .setSigningKey(jwtProperties.getSecret())
                .parseClaimsJws(token)
                .getBody();
    }
}

