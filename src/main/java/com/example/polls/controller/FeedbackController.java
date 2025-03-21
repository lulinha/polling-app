package com.example.polls.controller;

import com.example.polls.model.Feedback;
import com.example.polls.payload.FeedbackRequest;
import com.example.polls.payload.FeedbackResponse;
import com.example.polls.service.FeedbackService;
import com.example.polls.security.CurrentUser;
import com.example.polls.security.UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/feedback")
public class FeedbackController {

    @Autowired
    private FeedbackService userFeedbackService;

    @PostMapping
    public ResponseEntity<?> submitFeedback(@CurrentUser UserPrincipal currentUser,
            @Valid @RequestBody FeedbackRequest feedbackRequest) {
        userFeedbackService.submitFeedback(currentUser.getId(), feedbackRequest.getFeedbackText());
        return ResponseEntity.ok("Feedback submitted successfully");
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<Feedback> getAllFeedbacks() {
        return userFeedbackService.getAllFeedbacks();
    }

    @PostMapping("/{feedbackId}/response")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> respondToFeedback(@PathVariable Long feedbackId,
            @Valid @RequestBody FeedbackResponse response) {
        userFeedbackService.respondToFeedback(feedbackId, response.getResponseText());
        return ResponseEntity.ok("Response submitted successfully");
    }
}