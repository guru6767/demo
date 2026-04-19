package com.starto.filter;

import com.starto.repository.UserRepository;
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
 
    private final UserRepository userRepository;
 
    public FirebaseAuthFilter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.equals("/api/auth/forgot-password");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String idToken = authHeader.substring(7);
 
        // ✅ DEV TOKEN HANDLER
        if (idToken.startsWith("dev_") || idToken.startsWith("local-")) {
            String username = idToken.startsWith("dev_") ? idToken.substring(4) : idToken.substring(6);
            userRepository.findByUsername(username).ifPresent(user -> {
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        user.getFirebaseUid(), null, Collections.emptyList());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            });
            filterChain.doFilter(request, response);
            return;
        }
 
        try {
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
            String uid = decodedToken.getUid();

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(uid, null,
                    Collections.emptyList());

            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (Exception e) {
            // If token is invalid, we just don't set the authentication.
            System.err.println("Firebase Auth Error: " + e.getMessage());
            logger.warn("Firebase token verification failed: " + e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}