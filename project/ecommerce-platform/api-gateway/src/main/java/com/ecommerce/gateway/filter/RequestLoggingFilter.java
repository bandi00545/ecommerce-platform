package com.ecommerce.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class RequestLoggingFilter extends AbstractGatewayFilterFactory<RequestLoggingFilter.Config> {

    private static final String HEADER_REQUEST_ID = "X-Request-Id";
    private static final String HEADER_USER_ID    = "X-User-Id";

    public RequestLoggingFilter() {
        super(Config.class);
    }

    public static class Config {
        // No config parameters needed
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request    = exchange.getRequest();
            String requestId             = request.getHeaders().getFirst(HEADER_REQUEST_ID);
            String userId                = request.getHeaders().getFirst(HEADER_USER_ID);
            String method                = request.getMethod().name();
            String path                  = request.getPath().value();
            String queryString           = request.getURI().getQuery();
            long   startTime             = System.currentTimeMillis();

            // Log the incoming request
            log.info("→ REQUEST  | requestId={} | method={} | path={}{} | userId={} | ip={}",
                    requestId != null ? requestId : "N/A",
                    method,
                    path,
                    queryString != null ? "?" + queryString : "",
                    userId != null ? userId : "anonymous",
                    getClientIp(request)
            );

            // Process the request and log the response after completion
            return chain.filter(exchange).then(Mono.fromRunnable(() -> {
                ServerHttpResponse response = exchange.getResponse();
                long duration = System.currentTimeMillis() - startTime;

                int statusCode = response.getStatusCode() != null
                        ? response.getStatusCode().value()
                        : 0;

                // Use different log levels based on status code
                if (statusCode >= 500) {
                    log.error("← RESPONSE | requestId={} | method={} | path={} | status={} | duration={}ms",
                            requestId, method, path, statusCode, duration);
                } else if (statusCode >= 400) {
                    log.warn("← RESPONSE | requestId={} | method={} | path={} | status={} | duration={}ms",
                            requestId, method, path, statusCode, duration);
                } else {
                    log.info("← RESPONSE | requestId={} | method={} | path={} | status={} | duration={}ms",
                            requestId, method, path, statusCode, duration);
                }
            }));
        };
    }

    /**
     * Extracts client IP address, checking X-Forwarded-For first
     * (set by load balancers and proxies in front of the gateway).
     */
    private String getClientIp(ServerHttpRequest request) {
        String xForwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            // X-Forwarded-For can be a comma-separated list; first IP is the original client
            return xForwardedFor.split(",")[0].trim();
        }
        if (request.getRemoteAddress() != null) {
            return request.getRemoteAddress().getAddress().getHostAddress();
        }
        return "unknown";
    }
}
