package com.starto.repository;

import com.starto.model.NearbySpace;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface NearbySpaceRepository extends JpaRepository<NearbySpace, UUID> {
    List<NearbySpace> findByCity(String city);

    List<NearbySpace> findByUser_Id(UUID userId);

    // bounding-box search by geo coords
    List<NearbySpace> findByLatBetweenAndLngBetween(java.math.BigDecimal latMin, java.math.BigDecimal latMax,
            java.math.BigDecimal lngMin, java.math.BigDecimal lngMax);
}