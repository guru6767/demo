package com.starto.repository;

import com.starto.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.List;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByFirebaseUid(String firebaseUid);

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    @Modifying
    @Query("UPDATE User u SET u.isOnline = false WHERE u.isOnline = true AND u.lastSeen < :cutoff")
    void markInactiveUsersOffline(@Param("cutoff") OffsetDateTime cutoff);


    @Query("SELECT u FROM User u WHERE u.planExpiresAt BETWEEN :from AND :to AND u.plan != com.starto.enums.Plan.EXPLORER")
List<User> findUsersExpiringBetween(
    @Param("from") OffsetDateTime from,
    @Param("to") OffsetDateTime to
);

@Query("SELECT u FROM User u WHERE u.planExpiresAt < :now AND u.plan != com.starto.enums.Plan.EXPLORER")
List<User> findExpiredUsers(@Param("now") OffsetDateTime now);

List<User> findByLatBetweenAndLngBetween(
            BigDecimal latMin,
            BigDecimal latMax,
            BigDecimal lngMin,
            BigDecimal lngMax
    );

    List<User> findByUsernameContainingIgnoreCaseOrNameContainingIgnoreCase(
            String username,
            String name
    );

}
