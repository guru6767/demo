package com.starto.repository;

import com.starto.model.SignalView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface SignalViewRepository extends JpaRepository<SignalView, UUID> {

    long countBySignalId(UUID signalId);

    // views grouped by day
    @Query("""
            SELECT DATE(sv.viewedAt), COUNT(sv)
            FROM SignalView sv
            WHERE sv.signalId = :signalId
            GROUP BY DATE(sv.viewedAt)
            ORDER BY DATE(sv.viewedAt)
            """)
    List<Object[]> findViewsGroupedByDay(UUID signalId);

    boolean existsBySignalIdAndViewerUserId(UUID signalId, UUID viewerUserId);

    long countBySignalIdAndIsFollower(UUID signalId, Boolean isFollower);

}