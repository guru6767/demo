package com.starto.repository;

import com.starto.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {
    List<Message> findBySenderIdAndReceiverIdOrReceiverIdAndSenderIdOrderByCreatedAtAsc(
            UUID s1, UUID r1, UUID s2, UUID r2);
}
