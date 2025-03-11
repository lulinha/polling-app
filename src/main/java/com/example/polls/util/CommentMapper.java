package com.example.polls.util;

import java.util.List;

import org.springframework.stereotype.Component;

import com.example.polls.model.Comment;
import com.example.polls.payload.CommentResponse;

@Component
public class CommentMapper {

    public CommentResponse toResponse(Comment comment) {
        return new CommentResponse(
            comment.getId(),
            comment.getContent(),
            comment.getUser().getUsername(),
            comment.getCreatedAt(),
            comment.getLikeCount(),
            comment.getParent() != null ? comment.getParent().getId() : null,
            mapReplies(comment.getReplies())
        );
    }

    private List<CommentResponse> mapReplies(List<Comment> replies) {
        return replies.stream()
            .map(this::toResponse)
            .toList();
    }
}