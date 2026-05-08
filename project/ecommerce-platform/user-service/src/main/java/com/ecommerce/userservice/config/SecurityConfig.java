package com.ecommerce.userservice.config;

import com.ecommerce.common.context.RequestContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import com.ecommerce.userservice.repository.UserRepository;

import java.util.Optional;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    /**
     * Loads UserDetails by email for Spring Security's internal auth mechanisms.
     * Used by DaoAuthenticationProvider during the manual login flow.
     */
    @Bean
    public UserDetailsService userDetailsService(UserRepository userRepository) {
        return email -> {
            // Explicit block lambda — avoids Eclipse type-inference failure on builder chain
            com.ecommerce.userservice.entity.UserEntity found = userRepository
                    .findByEmail(email)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

            return org.springframework.security.core.userdetails.User
                    .withUsername(found.getEmail())
                    .password(found.getPassword())
                    .roles(found.getRole().name())
                    .accountExpired(false)
                    .accountLocked(!found.isEnabled())
                    .credentialsExpired(false)
                    .disabled(!found.isEnabled())
                    .build();
        };
    }

    /**
     * AuthenticationManager - used by UserServiceImpl to authenticate credentials.
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Security filter chain defining access rules.
     *
     * STATELESS: No sessions created (JWT-based auth handled at Gateway).
     * CSRF disabled: REST API with JWT doesn't need CSRF protection.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(auth -> auth
                // Public endpoints - no authentication needed
                .requestMatchers(
                    "/api/v1/auth/**",
                    "/actuator/health",
                    "/actuator/info",
                    "/api/v1/users/*/exists",  // internal service-to-service
                    // Swagger / OpenAPI
                    "/swagger-ui.html",
                    "/swagger-ui/**",
                    "/v3/api-docs",
                    "/v3/api-docs/**",
                    "/swagger-resources/**",
                    "/webjars/**"
                ).permitAll()
                // All other requests require authentication
                .anyRequest().authenticated()
            );

        return http.build();
    }
}
