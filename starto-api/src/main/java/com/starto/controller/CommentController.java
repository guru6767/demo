package com.starto.controller;

import com.starto.dto.CommentRequestDTO;
import com.starto.dto.CommentResponseDTO;
import com.starto.service.CommentService;
import com.starto.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/signals/{signalId}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;
    private final UserService userService;

    // saving the comment into DB
    @PostMapping
    public ResponseEntity<?> addComment(
            Authentication authentication,
            @PathVariable UUID signalId,
            @RequestBody CommentRequestDTO dto) {

        if (authentication == null)
            return ResponseEntity.status(401).build();

        return userService.getUserByFirebaseUid(
                authentication.getPrincipal().toString())
                .map(user -> {
                    if (dto.getContent() == null || dto.getContent().isBlank()) {
                        return ResponseEntity.status(400).body(
                                Map.of("error", "Comment cannot be empty"));
                    }
                    return ResponseEntity.ok(
                            commentService.addComment(user, signalId, dto.getContent()));
                })
                .orElse(ResponseEntity.status(401).build());
    }

    // saving the reply comment into DB
    @PostMapping("/{parentId}/reply")
    public ResponseEntity<?> addReply(
            Authentication authentication,
            @PathVariable UUID signalId,
            @PathVariable UUID parentId,
            @RequestBody CommentRequestDTO dto) {

        if (authentication == null)
            return ResponseEntity.status(401).build();

        return userService.getUserByFirebaseUid(
                authentication.getPrincipal().toString())
                .map(user -> {
                    if (dto.getContent() == null || dto.getContent().isBlank()) {
                        return ResponseEntity.status(400).body(
                                Map.of("error", "Reply cannot be empty"));
                    }
                    return ResponseEntity.ok(
                            commentService.addReply(user, signalId, parentId, dto.getContent()));
                })
                .orElse(ResponseEntity.status(401).build());
    }

    // Get all comments on single signal
    @GetMapping
    public ResponseEntity<List<CommentResponseDTO>> getComments(
            @PathVariable UUID signalId) {
        return ResponseEntity.ok(commentService.getComments(signalId));
    }

    // delete the comment
    @DeleteMapping("/{commentId}")
    public ResponseEntity<?> deleteComment(
            Authentication authentication,
            @PathVariable UUID signalId,
            @PathVariable UUID commentId) {

        if (authentication == null)
            return ResponseEntity.status(401).build();

        return userService.getUserByFirebaseUid(
                authentication.getPrincipal().toString())
                .map(user -> {
                    commentService.deleteComment(user, commentId);
                    return ResponseEntity.ok(Map.of("message", "Deleted successfully"));
                })
                .orElse(ResponseEntity.status(401).build());
    }
}