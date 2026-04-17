package com.starto.filter;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

public class FirebaseAuthFilter extends OncePerRequestFilter {

    // skip the authentication
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        String uri = request.getRequestURI();
        String method = request.getMethod();
        // Skip auth for public GET endpoints
        if ("GET".equalsIgnoreCase(method) && (path.equals("/api/signals") || path.startsWith("/api/signals/"))) {
            return true;
        }
        return path.equals("/api/auth/forgot-password") || 
               path.startsWith("/api/search") || 
               path.startsWith("/api/users") ||
               uri.contains("/api/search");
    }

    // do the filter for mapping
    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        System.out.println("=== FIREBASE FILTER ===");
        System.out.println("URI: " + request.getRequestURI());
        System.out.println("AUTH HEADER: "
                + (authHeader != null ? authHeader.substring(0, Math.min(30, authHeader.length())) : "NULL"));

        // no token — let Spring Security decide (public routes will pass)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String idToken = authHeader.substring(7);

        // DEV MODE: bypass Firebase for dev_ and local- prefixed tokens
        if (idToken.startsWith("dev_") || idToken.startsWith("local-")) {
            // Normalize: local-username → dev_username
            String uid = idToken.startsWith("local-") ? "dev_" + idToken.substring(6) : idToken;
            System.out.println("DEV AUTH BYPASS: Using UID " + uid);
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(uid, null, Collections.emptyList());
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
            return;
        }

        try {
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
            String uid = decodedToken.getUid();

            System.out.println("UID VERIFIED: " + uid);

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(uid, null,
                    Collections.emptyList());

            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (Exception e) {

            System.out.println("TOKEN ERROR: " + e.getMessage());

            // return 401 immediately — don't continue with broken token
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Invalid or expired token: " + e.getMessage() + "\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }
}