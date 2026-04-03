package com.starto.repository;

import com.starto.model.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CommentRepository extends JpaRepository<Comment, UUID> {

    // get only top level comments (not replies)
    List<Comment> findBySignalIdAndParentIdIsNullOrderByCreatedAtDesc(UUID signalId);

    // get replies for a comment
    List<Comment> findByParentIdOrderByCreatedAtAsc(UUID parentId);

    int countBySignalId(UUID signalId);
}