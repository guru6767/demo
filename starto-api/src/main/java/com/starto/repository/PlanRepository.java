package com.starto.repository;

import com.starto.model.PlanEntity;
import com.starto.enums.Plan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PlanRepository extends JpaRepository<PlanEntity, UUID> {
    Optional<PlanEntity> findByCode(Plan code);
}