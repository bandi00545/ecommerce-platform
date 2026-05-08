package com.ecommerce.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

@Configuration
public class RateLimiterConfig {

    @Bean
    public RedisRateLimiter redisRateLimiter() {
        return new RedisRateLimiter(100, 200, 1);
    }

    @Bean
    @Primary
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            String ip = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
            if (ip == null || ip.isBlank()) {
                if (exchange.getRequest().getRemoteAddress() != null) {
                    ip = exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
                } else {
                    ip = "unknown";
                }
            }
            // Use first IP in chain (original client, not proxy)
            String clientIp = ip.split(",")[0].trim();
            return Mono.just(clientIp);
        };
    }

    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
            return Mono.just(userId != null ? userId : "anonymous");
        };
    }
}
