package com.ecommerce.common.filter;

import com.ecommerce.common.constants.AppConstants;
import com.ecommerce.common.context.RequestContext;
import com.ecommerce.common.util.RequestIdGenerator;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component("ecommerceRequestContextFilter")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestContextFilter extends OncePerRequestFilter {

    /** MDC key for requestId - used in logback pattern */
    private static final String MDC_REQUEST_ID = "requestId";

    /** MDC key for userId - used in logback pattern */
    private static final String MDC_USER_ID = "userId";

    /** MDC key for user role */
    private static final String MDC_USER_ROLE = "userRole";

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        try {
            // ================================================================
            // STEP 1: Resolve requestId
            // Either from X-Request-Id header or generate a new one
            // ================================================================
            String requestId = request.getHeader(AppConstants.HEADER_REQUEST_ID);
            requestId = RequestIdGenerator.ensureValid(requestId);

            // ================================================================
            // STEP 2: Read userId and userRole from headers
            // These headers are SET by API Gateway after JWT validation.
            // Internal service-to-service calls also propagate these headers.
            // ================================================================
            String userId   = request.getHeader(AppConstants.HEADER_USER_ID);
            String userRole = request.getHeader(AppConstants.HEADER_USER_ROLE);

            // ================================================================
            // STEP 3: Set into RequestContext (ThreadLocal)
            // Makes values accessible from ANY layer in this request's thread
            // ================================================================
            RequestContext.setRequestId(requestId);
            RequestContext.setUserId(userId);
            RequestContext.setUserRole(userRole);

            // ================================================================
            // STEP 4: Set into SLF4J MDC
            // Every log statement in this thread will now automatically include
            // requestId and userId without explicit parameter passing
            // ================================================================
            MDC.put(MDC_REQUEST_ID, requestId);
            if (userId != null && !userId.isBlank()) {
                MDC.put(MDC_USER_ID, userId);
            }
            if (userRole != null && !userRole.isBlank()) {
                MDC.put(MDC_USER_ROLE, userRole);
            }

            // ================================================================
            // STEP 5: Add X-Request-Id to response header
            // Client can log this to correlate their request with server logs
            // ================================================================
            response.setHeader(AppConstants.HEADER_REQUEST_ID, requestId);

            // ================================================================
            // STEP 6: Log incoming request (debug level - don't log in prod INFO)
            // ================================================================
            log.debug("Incoming request | method={} | uri={} | requestId={} | userId={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    requestId,
                    userId != null ? userId : "anonymous"
            );

            // ================================================================
            // STEP 7: Continue the filter chain (actual request processing)
            // ================================================================
            filterChain.doFilter(request, response);

        } finally {
            // ================================================================
            // CRITICAL: ALWAYS clear ThreadLocal and MDC after request completes
            // Even if an exception was thrown!
            //
            // WHY: Thread pools reuse threads. Without clearing, the NEXT request
            // served by this thread sees the PREVIOUS request's userId and requestId.
            // This is a security and data integrity bug.
            // ================================================================
            RequestContext.clear();
            MDC.remove(MDC_REQUEST_ID);
            MDC.remove(MDC_USER_ID);
            MDC.remove(MDC_USER_ROLE);
        }
    }
}
