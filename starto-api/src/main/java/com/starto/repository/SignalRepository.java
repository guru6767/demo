package com.starto.repository;

import com.starto.model.Signal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;
import java.time.OffsetDateTime;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.UUID;

public interface SignalRepository extends JpaRepository<Signal, UUID> {

    /**
     * Fix #7 N+1: single query with JOIN FETCH loads signal + user in one round-trip.
     * Use this instead of findAll() or findByStatusOrderByCreatedAtDesc() for the feed.
     */
    @Query("SELECT s FROM Signal s JOIN FETCH s.user ORDER BY s.createdAt DESC")
    List<Signal> findAllWithUser();

    /**
     * Fix #8 Pagination: paginated feed query, also JOIN FETCH to avoid N+1.
     * Controller passes PageRequest.of(page, 20, Sort.by("createdAt").descending()).
     *
     * NOTE: Spring Data cannot apply Pageable directly to a JOIN FETCH query in JPQL
     * (it performs in-memory pagination which defeats the purpose). Use countQuery to
     * let Spring issue a separate COUNT for the Page metadata.
     */
    @Query(
        value      = "SELECT s FROM Signal s JOIN FETCH s.user",
        countQuery = "SELECT COUNT(s) FROM Signal s"
    )
    Page<Signal> findAllWithUserPageable(Pageable pageable);

    /** Active signals only — paginated, JOIN FETCH to avoid N+1 */
    @Query(
        value      = "SELECT s FROM Signal s JOIN FETCH s.user WHERE s.status = 'open'",
        countQuery = "SELECT COUNT(s) FROM Signal s WHERE s.status = 'open'"
    )
    Page<Signal> findActiveWithUserPageable(Pageable pageable);

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
        @Param("seeking") String seeking
    );

    // keeping these in case used elsewhere
    List<Signal> findBySeekingAndStatus(String seeking, String status);
    List<Signal> findBySeekingAndCityAndStatus(String seeking, String city, String status);

// fuzzy search by username, limit 2 per user
@Query("""
    SELECT s FROM Signal s
    JOIN s.user u
    WHERE LOWER(u.username) LIKE LOWER(CONCAT('%', :username, '%'))
    AND s.status = 'open'
    ORDER BY s.user.id, s.createdAt DESC
    """)
List<Signal> findByUsernameLike(@Param("username") String username);

@Modifying
@Query("UPDATE Signal s SET s.status = 'expired' WHERE s.status = 'open' AND s.expiresAt < :now")
void expireOldSignals(@Param("now") OffsetDateTime now);

    // bounding-box search by geo coords
    List<Signal> findByStatusAndLatBetweenAndLngBetween(String status, java.math.BigDecimal latMin, java.math.BigDecimal latMax, java.math.BigDecimal lngMin, java.math.BigDecimal lngMax);

    List<Signal> findByUserIdAndCategoryIgnoreCase(UUID userId, String category);

    List<Signal> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<Signal> findByStatusOrderByCreatedAtDesc(String status);

    @Query("""
        SELECT s FROM Signal s
        JOIN s.user u
        WHERE LOWER(s.title) LIKE LOWER(CONCAT('%', :query, '%'))
           OR LOWER(s.description) LIKE LOWER(CONCAT('%', :query, '%'))
           OR LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%'))
        """)
    List<Signal> findByTitleDescriptionOrOwner(@Param("query") String query);
}

