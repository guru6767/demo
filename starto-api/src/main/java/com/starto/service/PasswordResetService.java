package com.starto.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    

    //firebase admin SDK sends the email automatically
    public void sendPasswordResetEmail(String email) {
        try {
            // Check if user exists with this email first
            UserRecord userRecord = FirebaseAuth.getInstance().getUserByEmail(email);

            if (userRecord == null) {
                // Don't reveal if email exists or not (security best practice)
                log.warn("Password reset requested for non-existent email: {}", email);
                return;
            }

            // Generate password reset link — Firebase sends the email automatically
            String resetLink = FirebaseAuth.getInstance().generatePasswordResetLink(email);
            log.info("Password reset link generated for: {}", email);

            // Optional: If you want to send a custom email instead of Firebase's default,
            // pass the link to your own email service here
            // emailService.sendCustomResetEmail(email, resetLink);

        } catch (FirebaseAuthException e) {
            log.error("Failed to generate password reset link for {}: {}", email, e.getMessage());
            throw new RuntimeException("Failed to send password reset email", e);
        }
    }



    //directly updates the password in firebase
    public void updatePassword(String firebaseUid, String newPassword) {
        try {
            UserRecord.UpdateRequest request = new UserRecord.UpdateRequest(firebaseUid)
                    .setPassword(newPassword);

            FirebaseAuth.getInstance().updateUser(request);
            log.info("Password updated successfully for UID: {}", firebaseUid);

        } catch (FirebaseAuthException e) {
            log.error("Failed to update password for UID {}: {}", firebaseUid, e.getMessage());
            throw new RuntimeException("Failed to update password", e);
        }
    }
}