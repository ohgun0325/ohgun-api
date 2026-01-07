package site.ohgun.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 설정 (Upstash Redis)
 *
 * 역할:
 * - Upstash Redis에 TLS로 연결되는 커스텀 LettuceConnectionFactory 구성
 * - Refresh Token 저장용 RedisTemplate 제공
 */
@Configuration
public class RedisConfig {

    /**
     * Upstash Redis에 연결하기 위한 커스텀 LettuceConnectionFactory.
     * Spring Boot 기본 spring.data.redis.* / spring.redis.* 설정과 무관하게
     * 직접 UPSTASH_* 환경 변수를 사용해 접속합니다.
     */
    @Bean
    public LettuceConnectionFactory redisConnectionFactory(
            @Value("${UPSTASH_REDIS_HOST}") String host,
            @Value("${UPSTASH_REDIS_PORT}") int port,
            @Value("${UPSTASH_REDIS_PASSWORD}") String password) {

        // Standalone Redis 설정 (호스트/포트/패스워드)
        RedisStandaloneConfiguration serverConfig = new RedisStandaloneConfiguration();
        serverConfig.setHostName(host);
        serverConfig.setPort(port);
        serverConfig.setPassword(RedisPassword.of(password));

        // Upstash 는 TLS(rediss) 필수이므로 SSL 활성화
        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                .useSsl()
                .build();

        return new LettuceConnectionFactory(serverConfig, clientConfig);
    }

    /**
     * String 기반 RedisTemplate.
     * Refresh Token 등을 문자열 형태로 저장/조회하는 데 사용됩니다.
     */
    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // String 직렬화 사용 (키/값/해시 모두 문자열 기반)
        template.setKeySerializer(new StringSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());

        template.afterPropertiesSet();
        return template;
    }
}
