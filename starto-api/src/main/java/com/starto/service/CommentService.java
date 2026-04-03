package com.starto.service;

import com.starto.model.Comment;
import com.starto.model.Signal;
import com.starto.model.User;
import com.starto.repository.CommentRepository;
import com.starto.repository.SignalRepository;
import lombok.RequiredArgsConstructor;
import com.starto.dto.CommentResponseDTO;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final SignalRepository signalRepository;

    // add top level comment
    @Transactional
    public Comment addComment(User user, UUID signalId, String content) {
        Signal signal = signalRepository.findById(signalId)
                .orElseThrow(() -> new RuntimeException("Signal not found"));

        Comment comment = Comment.builder()
                .signal(signal)
                .user(user)
                .username(user.getUsername())
                .content(content)
                .parentId(null) // top level
                .build();

        commentRepository.save(comment);

        // increment responseCount
        signal.setResponseCount(signal.getResponseCount() + 1);
        signalRepository.save(signal);

        return comment;
    }

    // reply to a comment
    @Transactional
    public Comment addReply(User user, UUID signalId, UUID parentId, String content) {
        Signal signal = signalRepository.findById(signalId)
                .orElseThrow(() -> new RuntimeException("Signal not found"));

        // check parent comment exists
        Comment parentComment = commentRepository.findById(parentId)
                .orElseThrow(() -> new RuntimeException("Comment not found"));

        // block replying to a reply (1 level only)
        if (parentComment.getParentId() != null) {
            throw new RuntimeException("Cannot reply to a reply");
        }

        Comment reply = Comment.builder()
                .signal(signal)
                .user(user)
                .username(user.getUsername())
                .content(content)
                .parentId(parentId) // ← links to parent comment
                .build();

        commentRepository.save(reply);

        // also increment responseCount for replies
        signal.setResponseCount(signal.getResponseCount() + 1);
        signalRepository.save(signal);

        return reply;
    }

    // delete comment or reply
    @Transactional
    public void deleteComment(User user, UUID commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found"));

        if (!comment.getUserId().equals(user.getId())) {
            throw new RuntimeException("Forbidden: not your comment");
        }

        // decrement responseCount
        Signal signal = comment.getSignal();
        signal.setResponseCount(Math.max(0, signal.getResponseCount() - 1));
        signalRepository.save(signal);

        commentRepository.delete(comment);
    }

    // convert Comment to DTO
    private CommentResponseDTO toDTO(Comment comment) {
        return CommentResponseDTO.builder()
                .id(comment.getId())
                .signalId(comment.getSignalId())
                .userId(comment.getUserId())
                .username(comment.getUsername())
                .content(comment.getContent())
                .parentId(comment.getParentId())
                .createdAt(comment.getCreatedAt())
                .replies(comment.getReplies() != null
                        ? comment.getReplies().stream().map(this::toDTO).toList()
                        : List.of())
                .build();
    }

    public List<CommentResponseDTO> getComments(UUID signalId) {
        return commentRepository
                .findBySignalIdAndParentIdIsNullOrderByCreatedAtDesc(signalId)
                .stream()
                .map(this::toDTO)
                .toList();
    }
}