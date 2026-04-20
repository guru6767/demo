package com.starto.service;

import com.starto.model.Comment;
import com.starto.model.Signal;
import com.starto.model.User;
import com.starto.model.NearbySpace;
import com.starto.repository.CommentRepository;
import com.starto.repository.SignalRepository;
import com.starto.repository.NearbySpaceRepository;

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
    private final NearbySpaceRepository nearbySpaceRepository; 
    private final NotificationService notificationService;

    //  add top level comment
    @Transactional
    public Comment addComment(User user, UUID postId, String content) {

        //  Step 1: check Signal
        Signal signal = signalRepository.findById(postId).orElse(null);

        if (signal != null) {
            Comment comment = Comment.builder()
                    .signal(signal)
                    .user(user)
                    .username(user.getUsername())
                    .content(content)
                    .parentId(null)
                    .build();

            commentRepository.save(comment);

            //  increment only for signal
            signal.setResponseCount(signal.getResponseCount() + 1);
            signalRepository.save(signal);

            notificationService.send(
                    signal.getUser().getId(),
                    "NEW_COMMENT",
                    "New Comment",
                    user.getName() + " commented on your signal",
                    null
            );

            return comment;
        }

        //  Step 2: check NearbySpace
        NearbySpace space = nearbySpaceRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        //  TEMP: attach NULL signal (since your model requires it)
        Comment comment = Comment.builder()
                .signal(null) 
                .user(user)
                .username(user.getUsername())
                .content(content)
                .parentId(null)
                .build();

        return commentRepository.save(comment);
    }

    //  reply to a comment
    @Transactional
    public Comment addReply(User user, UUID postId, UUID parentId, String content) {

        //  Step 1: check Signal
        Signal signal = signalRepository.findById(postId).orElse(null);

        Comment parentComment = commentRepository.findById(parentId)
                .orElseThrow(() -> new RuntimeException("Comment not found"));

        if (signal != null) {
            Comment reply = Comment.builder()
                    .signal(signal)
                    .user(user)
                    .username(user.getUsername())
                    .content(content)
                    .parentId(parentId)
                    .build();

            commentRepository.save(reply);

            signal.setResponseCount(signal.getResponseCount() + 1);
            signalRepository.save(signal);

            return reply;
        }

        //  Step 2: check NearbySpace
        nearbySpaceRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        Comment reply = Comment.builder()
                .signal(null)
                .user(user)
                .username(user.getUsername())
                .content(content)
                .parentId(parentId)
                .build();

        return commentRepository.save(reply);
    }

    //  delete comment
    @Transactional
    public void deleteComment(User user, UUID commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found"));

        if (!comment.getUserId().equals(user.getId())) {
            throw new RuntimeException("Forbidden: not your comment");
        }

        Signal signal = comment.getSignal();

        int totalDeleted = countAllReplies(comment) + 1;

        commentRepository.delete(comment);

        //  only update if signal exists
        if (signal != null) {
            signal.setResponseCount(Math.max(0, signal.getResponseCount() - totalDeleted));
            signalRepository.save(signal);
        }
    }

    //  DTO conversion
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

    private int countAllReplies(Comment comment) {
        if (comment.getReplies() == null || comment.getReplies().isEmpty()) return 0;
        int count = comment.getReplies().size();
        for (Comment reply : comment.getReplies()) {
            count += countAllReplies(reply);
        }
        return count;
    }
}