package com.ecommerce.eurekaserver.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class EurekaSecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF: Eureka client registration uses POST without CSRF token
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/eureka/**")
            )
            // All requests require authentication
            .authorizeHttpRequests(auth -> auth
                .anyRequest().authenticated()
            )
            // Enable HTTP Basic Auth for Eureka client registration
            .httpBasic(basic -> {})
            // Enable form login for dashboard browser access
            .formLogin(form -> {});

        return http.build();
    }
}
