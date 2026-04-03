package com.starto.config;

import com.starto.filter.FirebaseAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import java.util.List;

import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public FirebaseAuthFilter firebaseAuthFilter() {
        return new FirebaseAuthFilter();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            throw new UsernameNotFoundException("No local users");
        };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // Disable CSRF for Postman and APIs
                .cors(cors -> cors.configurationSource(request -> {
                    var config = new CorsConfiguration();
                    config.setAllowedOrigins(List.of("*")); // Allow all origins temporarily
                    config.setAllowedMethods(List.of("*")); // Allow all HTTP methods
                    config.setAllowedHeaders(List.of("*")); // Allow all headers
                    return config;
                }))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/public/**",
                                "/ws/**",
                                "/api/auth/register",
                                "/api/auth/forgot-password",
                                "/api/signals",
                                "/api/signals/**",
                                "/api/users/check-username",
                                "/api/subscriptions/webhook/razorpay",
                                "/api/subscriptions/create",
                                "/api/subscriptions/verify",
                                "/actuator/health")
                        .permitAll() // Public routes
                        .anyRequest().authenticated())
                // Add Firebase filter before UsernamePasswordAuthenticationFilter
                .addFilterBefore(firebaseAuthFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

}