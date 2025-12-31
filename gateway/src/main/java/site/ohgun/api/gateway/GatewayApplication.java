package site.ohgun.api.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import reactor.core.publisher.Mono;

@SpringBootApplication
public class GatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(GatewayApplication.class, args);
	}

	/**
	 * IP address-based Key Resolver for Rate Limiting
	 * Moved from RateLimiterKeyResolver.java to keep all configuration in one place
	 */
	@Bean
	public KeyResolver ipKeyResolver() {
		return exchange -> {
			String ipAddress = "unknown";
			var remoteAddr = exchange.getRequest().getRemoteAddress();
			if (remoteAddr != null && remoteAddr.getAddress() != null) {
				ipAddress = remoteAddr.getAddress().getHostAddress();
			}
			return Mono.just(ipAddress);
		};
	}

}
