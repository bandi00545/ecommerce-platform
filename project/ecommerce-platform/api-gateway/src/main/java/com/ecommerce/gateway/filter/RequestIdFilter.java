package com.ecommerce.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@Component
public class RequestIdFilter extends AbstractGatewayFilterFactory<RequestIdFilter.Config> {

    private static final String HEADER_REQUEST_ID = "X-Request-Id";

    public RequestIdFilter() {
        super(Config.class);
    }

    public static class Config {
        // No config needed
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String requestId = exchange.getRequest().getHeaders().getFirst(HEADER_REQUEST_ID);

            // Validate incoming requestId or generate new one
            if (requestId == null || requestId.isBlank() || requestId.length() < 8) {
                requestId = UUID.randomUUID().toString();
            }

            final String finalRequestId = requestId;

            // Mutate request to add/overwrite the requestId header
            ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                    .header(HEADER_REQUEST_ID, finalRequestId)
                    .build();

            // Also add to response headers so client gets it back
            exchange.getResponse().getHeaders().set(HEADER_REQUEST_ID, finalRequestId);

            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        };
    }
}
