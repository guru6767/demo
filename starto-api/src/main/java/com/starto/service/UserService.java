package com.starto.service;

import com.starto.model.User;
import com.starto.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public Optional<User> getUserByFirebaseUid(String firebaseUid) {
        return userRepository.findByFirebaseUid(firebaseUid);
    }

    @Transactional
    public User createOrUpdateUser(String firebaseUid, String email, String name, String baseUsername, String role) {

        return userRepository.findByFirebaseUid(firebaseUid)
                .map(user -> {
                    user.setLastSeen(OffsetDateTime.now());
                    user.setIsOnline(true);
                    // Update metadata if provided
                    if (email != null) user.setEmail(email);
                    if (name != null) user.setName(name);
                    return userRepository.save(user);
                })
                .orElseGet(() -> {
                    // Check for dev token - use the baseUsername as is if possible
                    String finalUsername = baseUsername;
                    if (firebaseUid.startsWith("dev_") && !finalUsername.toLowerCase().contains(role.toLowerCase())) {
                        finalUsername = baseUsername + "_" + role.toLowerCase();
                    } else if (!firebaseUid.startsWith("dev_")) {
                        finalUsername = baseUsername + "_" + role.toLowerCase();
                    }

                    // If username collision, append UID suffix for dev users
                    if (userRepository.existsByUsername(finalUsername)) {
                        if (firebaseUid.startsWith("dev_")) {
                             finalUsername = finalUsername + "_" + firebaseUid.substring(Math.max(0, firebaseUid.length() - 4));
                        } else {
                             throw new RuntimeException("Username already exists");
                        }
                    }

                    User newUser = User.builder()
                            .firebaseUid(firebaseUid)
                            .email(email != null ? email : firebaseUid + "@dev.starto")
                            .name(name != null ? name : baseUsername)
                            .username(finalUsername)
                            .role(role)
                            .plan("free")
                            .lastSeen(OffsetDateTime.now())
                            .isOnline(true)
                            .build();

                    return userRepository.save(newUser);
                });
    }

    @Transactional
    public User updateProfile(User user) {
        user.setUpdatedAt(OffsetDateTime.now());
        return userRepository.save(user);
    }

    public boolean isUsernameAvailable(String baseUsername, String role) {
        String finalUsername = baseUsername + "_" + role.toLowerCase();
        return !userRepository.existsByUsername(finalUsername);
    }

    // called on every authenticated request (heartbeat)
    @Transactional
    public void markOnline(String firebaseUid) {
        userRepository.findByFirebaseUid(firebaseUid).ifPresent(user -> {
            user.setIsOnline(true);
            user.setLastSeen(OffsetDateTime.now());
            userRepository.save(user);
        });
    }

    // called on logout
    @Transactional
    public void markOffline(String firebaseUid) {
        userRepository.findByFirebaseUid(firebaseUid).ifPresent(user -> {
            user.setIsOnline(false);
            user.setLastSeen(OffsetDateTime.now());
            userRepository.save(user);
        });
    }

    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

}