package com.starto.repository;

import com.starto.model.Signal;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.UUID;

public interface SignalRepository extends JpaRepository<Signal, UUID> {
    List<Signal> findByType(String type);

    List<Signal> findByStatus(String status);

    List<Signal> findByUserId(UUID userId);

    // city — partial, case-insensitive
    @Query("SELECT s FROM Signal s WHERE LOWER(s.city) LIKE LOWER(CONCAT('%', :city, '%'))")
    List<Signal> findByCity(@Param("city") String city);

    // seeking — partial, case-insensitive
    @Query("SELECT s FROM Signal s WHERE LOWER(s.seeking) LIKE LOWER(CONCAT('%', :seeking, '%'))")
    List<Signal> findBySeeking(@Param("seeking") String seeking);

    long countByUserId(UUID userId);

    // seeking + city — both partial, case-insensitive
    @Query("""
            SELECT s FROM Signal s
            WHERE LOWER(s.seeking) LIKE LOWER(CONCAT('%', :seeking, '%'))
            AND LOWER(s.city) LIKE LOWER(CONCAT('%', :city, '%'))
            """)
    List<Signal> findBySeekingAndCity(@Param("seeking") String seeking, @Param("city") String city);

    // username — partial, case-insensitive
    @Query("""
            SELECT s FROM Signal s
            JOIN s.user u
            WHERE LOWER(u.username) LIKE LOWER(CONCAT('%', :username, '%'))
            AND s.status = 'open'
            """)
    List<Signal> findByUsername(@Param("username") String username);

    // username + seeking — both partial, case-insensitive
    @Query("""
            SELECT s FROM Signal s
            JOIN s.user u
            WHERE LOWER(u.username) LIKE LOWER(CONCAT('%', :username, '%'))
            AND LOWER(s.seeking) LIKE LOWER(CONCAT('%', :seeking, '%'))
            AND s.status = 'open'
            """)
    List<Signal> findByUsernameAndSeeking(
            @Param("username") String username,
            @Param("seeking") String seeking);

    // keeping these in case used elsewhere
    List<Signal> findBySeekingAndStatus(String seeking, String status);

    List<Signal> findBySeekingAndCityAndStatus(String seeking, String city, String status);

    // bounding-box search by geo coords
    List<Signal> findByStatusAndLatBetweenAndLngBetween(String status, java.math.BigDecimal latMin,
            java.math.BigDecimal latMax, java.math.BigDecimal lngMin, java.math.BigDecimal lngMax);
}