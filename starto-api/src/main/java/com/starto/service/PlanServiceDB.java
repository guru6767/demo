package com.starto.service;

import com.starto.model.PlanEntity;
import com.starto.repository.PlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PlanServiceDB {

    private final PlanRepository planRepository;

    public List<PlanEntity> getAllPlans() {
        return planRepository.findAll();
    }
}