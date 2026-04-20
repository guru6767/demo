package com.starto.repository;

import com.starto.model.Response;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ResponseRepository extends JpaRepository<Response, UUID> {
    List<Response> findBySignalId(UUID signalId);
}
