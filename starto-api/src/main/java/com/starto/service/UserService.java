package com.starto.service;

import com.starto.enums.Plan;
import com.starto.model.User;
import com.starto.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.core.Authentication;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import com.starto.service.PresenceService;
import com.starto.service.NotificationService;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PresenceService presenceService;
    private final NotificationService notificationService;


 public Optional<User> getUserByFirebaseUid(String firebaseUid) {
    Optional<User> userOpt = userRepository.findByFirebaseUid(firebaseUid);
    
    System.out.println("USER FOUND: " + userOpt.isPresent());
    System.out.println("FIREBASE UID SEARCHED: '" + firebaseUid + "'");
    
    userOpt.ifPresent(user -> {

        if (user.getPlan() != Plan.EXPLORER
            && user.getPlanExpiresAt() != null
            && user.getPlanExpiresAt().isBefore(OffsetDateTime.now())) {

        String expiredPlanName = user.getPlan().name();  // ← save before changing

        user.setPlan(Plan.EXPLORER);
        user.setPlanExpiresAt(null);
        userRepository.save(user);

        // notify with correct plan name
        notificationService.send(
            user.getId(),
            "PLAN_EXPIRED",
            "Plan Expired",
            "Your " + expiredPlanName + " plan has expired. Upgrade to continue.",
            Map.of("plan", "EXPLORER")
        );

        System.out.println("PLAN EXPIRED - downgraded to EXPLORER");
    }
        
        System.out.println("USER EMAIL: " + user.getEmail());
        System.out.println("USER PLAN: " + user.getPlan());
        user.setIsOnline(true);
        user.setLastSeen(OffsetDateTime.now());
        userRepository.save(user);
    });
    
    System.out.println("RETURNING: " + userOpt.isPresent());
    return userOpt;
}


@Cacheable(value = "userCache", key = "#firebaseUid")
public User getUserCached(String firebaseUid) {
    return userRepository.findByFirebaseUid(firebaseUid).orElse(null);
}


    @Transactional
public User createOrUpdateUser(String firebaseUid,
                                String email,
                                String name,
                                String phone,
                                String role,
                                String city,
                                String state,
                                String country) {

    return userRepository.findByFirebaseUid(firebaseUid)
            .map(user -> {

                user.setLastSeen(OffsetDateTime.now());
                user.setIsOnline(true);

                // only fill missing fields (DO NOT overwrite existing data)
                if (user.getCity() == null) user.setCity(city);
                if (user.getState() == null) user.setState(state);
                if (user.getCountry() == null) user.setCountry(country != null ? country : "India");
                if (user.getPhone() == null) user.setPhone(phone);

                return userRepository.save(user);
            })
            .orElseGet(() -> {

                String finalUsername = generateUniqueUsername(name, role);

                User newUser = User.builder()
                        .firebaseUid(firebaseUid)
                        .email(email)
                        .name(name)
                        .phone(phone)
                        .role(role)
                        .city(city)
                        .state(state)
                        .country(country != null ? country : "India")
                        .username(finalUsername)
                        .plan(Plan.EXPLORER)
                        .isOnline(true)
                        .lastSeen(OffsetDateTime.now())
                        .build();

                return userRepository.save(newUser);
            });
}

@CacheEvict(value = "userCache", key = "#user.firebaseUid")
    @Transactional
public User updateProfile(User user) {

    User existing = userRepository.findById(user.getId())
            .orElseThrow(() -> new RuntimeException("User not found"));

   // UserService.updateProfile — add missing fields
if (user.getUsername() != null) existing.setUsername(user.getUsername());
if (user.getSubIndustry() != null) existing.setSubIndustry(user.getSubIndustry());
if (user.getWebsiteUrl() != null) existing.setWebsiteUrl(user.getWebsiteUrl());
if (user.getLinkedinUrl() != null) existing.setLinkedinUrl(user.getLinkedinUrl());
if (user.getTwitterUrl() != null) existing.setTwitterUrl(user.getTwitterUrl());
if (user.getGithubUrl() != null) existing.setGithubUrl(user.getGithubUrl());
if (user.getLat() != null) existing.setLat(user.getLat());
if (user.getLng() != null) existing.setLng(user.getLng());
if (user.getIndustry() != null) existing.setIndustry(user.getIndustry());

    existing.setUpdatedAt(OffsetDateTime.now());

    return userRepository.save(existing);
}

   public boolean isUsernameAvailable(String baseUsername, String role) { ///////
    String finalUsername = baseUsername + "_" + role.toLowerCase();
    return !userRepository.existsByUsername(finalUsername);
}


@Cacheable(value = "userCache", key = "#username")
public Optional<User> getUserByUsername(String username) {
    return userRepository.findByUsername(username);
}

private String generateUniqueUsername(String name, String role) {

    String base = name.toLowerCase().trim().replaceAll("\\s+", "");
    String baseUsername = base + "_" + role.toLowerCase();

    String finalUsername = baseUsername;
    int i = 1;

    while (userRepository.existsByUsername(finalUsername)) {
        finalUsername = baseUsername + i;
        i++;
    }

    return finalUsername;
}


@CacheEvict(value = "userCache", key = "#firebaseUid")
@Transactional
public void updatePresence(String firebaseUid) {
    userRepository.findByFirebaseUid(firebaseUid).ifPresent(user -> {
        user.setIsOnline(true);
        user.setLastSeen(OffsetDateTime.now());
        userRepository.save(user);
    });
}

  @CacheEvict(value = "userCache", key = "#firebaseUid")
    @Transactional
    public void markOffline(String firebaseUid) {
        userRepository.findByFirebaseUid(firebaseUid).ifPresent(user -> {
            user.setIsOnline(false);
            user.setLastSeen(OffsetDateTime.now());
            userRepository.save(user);
        });
    }




}
