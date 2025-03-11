package com.example.polls.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.polls.model.User;
import com.example.polls.payload.MentionResponse;
import com.example.polls.security.CurrentUser;
import com.example.polls.security.UserPrincipal;
import com.example.polls.service.MentionService;
import com.example.polls.service.UserService;

@RestController
@RequestMapping("/api/mentions")
public class MentionController {
    private final MentionService mentionService;
    private final UserService userService;

    public MentionController(MentionService mentionService, UserService userService) {
        this.mentionService = mentionService;
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Page<MentionResponse>> getUserMentions(@CurrentUser UserPrincipal currentUser, 
        @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(mentionService.getUserMentions(currentUser, pageable));
    }

    @PutMapping("/{mentionId}/read")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Void> markAsRead(@PathVariable Long mentionId) {
        mentionService.markAsRead(mentionId);
        return ResponseEntity.ok().build();
    }
}