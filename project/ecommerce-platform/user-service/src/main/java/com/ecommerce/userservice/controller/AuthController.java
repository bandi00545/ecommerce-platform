package com.ecommerce.userservice.controller;

import com.ecommerce.common.constants.AppConstants;
import com.ecommerce.common.context.RequestContext;
import com.ecommerce.common.dto.request.RequestEnvelope;
import com.ecommerce.common.dto.response.ResponseEnvelope;
import com.ecommerce.userservice.dto.request.LoginRequest;
import com.ecommerce.userservice.dto.request.RegisterRequest;
import com.ecommerce.userservice.dto.response.AuthResponse;
import com.ecommerce.userservice.facade.UserFacade;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserFacade userFacade;

    @PostMapping("/register")
    public ResponseEntity<ResponseEnvelope<AuthResponse>> register(
            @Valid @RequestBody RequestEnvelope<RegisterRequest> envelope) {

        String requestId = RequestContext.getRequestIdSafe();
        log.info("Register endpoint | requestId={}", requestId);

        AuthResponse authResponse = userFacade.register(envelope.getPayload());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ResponseEnvelope.success(
                        authResponse,
                        "Registration successful. Welcome to the platform!",
                        requestId
                ));
    }

    @PostMapping("/login")
    public ResponseEntity<ResponseEnvelope<AuthResponse>> login(
            @Valid @RequestBody RequestEnvelope<LoginRequest> envelope) {

        String requestId = RequestContext.getRequestIdSafe();
        log.info("Login endpoint | requestId={}", requestId);

        AuthResponse authResponse = userFacade.login(envelope.getPayload());

        return ResponseEntity.ok(ResponseEnvelope.success(
                authResponse,
                "Login successful",
                requestId
        ));
    }

    /**
     * GET /api/v1/auth/health
     * Simple health check for the auth endpoints.
     */
    @GetMapping("/health")
    public ResponseEntity<ResponseEnvelope<String>> health() {
        return ResponseEntity.ok(
                ResponseEnvelope.success("User Service is running", RequestContext.getRequestIdSafe())
        );
    }
}
