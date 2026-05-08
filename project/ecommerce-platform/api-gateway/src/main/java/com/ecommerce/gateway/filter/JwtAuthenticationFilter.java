package com.ecommerce.gateway.filter;

import com.ecommerce.gateway.util.JwtUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class JwtAuthenticationFilter
        extends AbstractGatewayFilterFactory<JwtAuthenticationFilter.Config> {

    private static final String HEADER_REQUEST_ID = "X-Request-Id";
    private static final String HEADER_USER_ID    = "X-User-Id";
    private static final String HEADER_USER_ROLE  = "X-User-Role";

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, ObjectMapper objectMapper) {
        super(Config.class);
        this.jwtUtil = jwtUtil;
        this.objectMapper = objectMapper;
    }

    /**
     * Config class: contains filter-specific configuration from application.yml.
     * requiredRole: if set, only users with this role can access the route.
     * null means any authenticated user can access.
     */
    @lombok.Data
    public static class Config {
        private String requiredRole;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String requestId = request.getHeaders().getFirst(HEADER_REQUEST_ID);
            if (requestId == null) requestId = "GATEWAY-" + System.nanoTime();

            // ================================================================
            // STEP 1: Extract Authorization header
            // ================================================================
            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("Missing or invalid Authorization header | requestId={} | path={}",
                        requestId, request.getPath());
                return writeErrorResponse(exchange, HttpStatus.UNAUTHORIZED,
                        "ERR_1007", "Authorization header is missing or invalid", requestId);
            }

            // ================================================================
            // STEP 2: Extract token
            // ================================================================
            String token = jwtUtil.extractTokenFromHeader(authHeader);
            if (token == null) {
                return writeErrorResponse(exchange, HttpStatus.UNAUTHORIZED,
                        "ERR_1007", "Bearer token is empty", requestId);
            }

            // ================================================================
            // STEP 3: Validate token
            // ================================================================
            if (!jwtUtil.isTokenValid(token)) {
                log.warn("Invalid or expired JWT token | requestId={} | path={}",
                        requestId, request.getPath());
                return writeErrorResponse(exchange, HttpStatus.UNAUTHORIZED,
                        "ERR_1005", "JWT token is invalid or expired", requestId);
            }

            // ================================================================
            // STEP 4: Extract claims
            // ================================================================
            String userId   = jwtUtil.extractUserId(token);
            String userRole = jwtUtil.extractRole(token);
            String username = jwtUtil.extractUsername(token);

            if (userId == null || userId.isBlank()) {
                log.warn("JWT token missing userId claim | requestId={}", requestId);
                return writeErrorResponse(exchange, HttpStatus.UNAUTHORIZED,
                        "ERR_1006", "JWT token is malformed: missing userId", requestId);
            }

            // ================================================================
            // STEP 5: Role check (if route has requiredRole configured)
            // ================================================================
            if (config.getRequiredRole() != null && !config.getRequiredRole().isBlank()) {
                if (!config.getRequiredRole().equalsIgnoreCase(userRole)) {
                    log.warn("Access forbidden | requestId={} | userId={} | required={} | actual={}",
                            requestId, userId, config.getRequiredRole(), userRole);
                    return writeErrorResponse(exchange, HttpStatus.FORBIDDEN,
                            "ERR_0006",
                            "Access forbidden: requires role " + config.getRequiredRole(),
                            requestId);
                }
            }

            // ================================================================
            // STEP 6: Inject user identity headers into the forwarded request
            // Downstream services read these headers instead of re-parsing JWT
            // ================================================================
            log.debug("JWT validated | requestId={} | userId={} | role={} | path={}",
                    requestId, userId, userRole, request.getPath());

            ServerHttpRequest mutatedRequest = request.mutate()
                    .header(HEADER_USER_ID,   userId)
                    .header(HEADER_USER_ROLE, userRole != null ? userRole : "")
                    // Remove original Authorization header to prevent JWT leaking downstream
                    // Comment this out if downstream services also need the JWT
                    // .headers(headers -> headers.remove(HttpHeaders.AUTHORIZATION))
                    .build();

            // ================================================================
            // STEP 7: Continue filter chain with mutated request
            // ================================================================
            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        };
    }

    private Mono<Void> writeErrorResponse(ServerWebExchange exchange,
                                           HttpStatus status,
                                           String errorCode,
                                           String message,
                                           String requestId) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        response.getHeaders().set(HEADER_REQUEST_ID, requestId);

        Map<String, Object> errorBody = new HashMap<>();
        errorBody.put("status", "FAILURE");
        errorBody.put("errorCode", errorCode);
        errorBody.put("message", message);
        errorBody.put("requestId", requestId);
        errorBody.put("timestamp", LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")));

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(errorBody);
            return response.writeWith(
                    Mono.just(response.bufferFactory().wrap(bytes))
            );
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize error response", e);
            return response.setComplete();
        }
    }
}
