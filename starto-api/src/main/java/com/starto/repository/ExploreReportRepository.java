package com.starto.repository;

import com.starto.model.ExploreReport;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ExploreReportRepository extends JpaRepository<ExploreReport, UUID> {
    List<ExploreReport> findByUserId(UUID userId);
}