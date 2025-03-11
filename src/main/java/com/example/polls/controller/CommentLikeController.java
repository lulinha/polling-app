package com.example.polls.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.polls.payload.LikeStatusResponse;
import com.example.polls.payload.UserProfile;
import com.example.polls.security.CurrentUser;
import com.example.polls.security.UserPrincipal;
import com.example.polls.service.CommentLikeService;
import com.example.polls.service.UserService;

@RestController
@RequestMapping("/api/comments/{commentId}/likes")
public class CommentLikeController {
    private final CommentLikeService commentLikeService;
    private final UserService userService;

    public CommentLikeController(CommentLikeService commentLikeService, UserService userService) {
        this.commentLikeService = commentLikeService;
        this.userService = userService;
    }

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<LikeStatusResponse> toggleLike(@CurrentUser UserPrincipal currentUser, @PathVariable Long commentId) {
        return ResponseEntity.ok(commentLikeService.toggleLike(currentUser, commentId));
    }

    @GetMapping("/users")
    public ResponseEntity<Page<UserProfile>> getLikedUsers(
        @PathVariable Long commentId,
        @PageableDefault(size = 10) Pageable pageable
    ) {
        return ResponseEntity.ok(commentLikeService.getLikedUsers(commentId, pageable));
    }
}