package com.starto.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;

/**
 * Fix #6: Firebase initialisation prefers base64-encoded JSON injected via env var,
 * making it container/cloud-safe (no file-system mounting required).
 *
 * Priority order:
 *   1. FIREBASE_CONFIG_BASE64   (alias accepted by this config)
 *   2. FIREBASE_SERVICE_ACCOUNT_B64  (original name, also accepted)
 *   3. FIREBASE_CONFIG_PATH     (local dev fallback — reads from filesystem)
 *
 * How to generate the base64 value:
 *   Linux/Mac:   base64 -w 0 firebase-service-account.json
 *   PowerShell:  [Convert]::ToBase64String([IO.File]::ReadAllBytes("firebase-service-account.json"))
 */
@Configuration
public class FirebaseConfig {

    /** Primary env var: FIREBASE_CONFIG_BASE64 */
    @Value("${firebase.config-base64:}")
    private String configBase64;

    /** Legacy alias: FIREBASE_SERVICE_ACCOUNT_B64 (kept for backwards compatibility) */
    @Value("${firebase.service-account-b64:}")
    private String serviceAccountB64;

    /** Local dev fallback: path to firebase-service-account.json */
    @Value("${firebase.config-path:src/main/resources/firebase-service-account.json}")
    private String configPath;

    @PostConstruct
    public void init() throws IOException {
        if (!FirebaseApp.getApps().isEmpty()) {
            return;
        }

        // Resolve effective base64 value — prefer config-base64, fall back to service-account-b64
        String effectiveB64 = StringUtils.hasText(configBase64) ? configBase64 : serviceAccountB64;

        InputStream serviceAccount;

        if (StringUtils.hasText(effectiveB64)) {
            // Container / production path: decode inline base64 JSON
            byte[] decoded = Base64.getDecoder().decode(effectiveB64.trim());
            serviceAccount = new ByteArrayInputStream(decoded);
        } else {
            // Local dev fallback: load from filesystem path
            serviceAccount = new FileInputStream(configPath);
        }

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .build();

        FirebaseApp.initializeApp(options);
        System.out.println("Firebase initialised: " + FirebaseApp.getApps().size() + " app(s)");
    }
}
