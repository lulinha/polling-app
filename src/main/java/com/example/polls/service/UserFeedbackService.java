package com.example.polls.service;

import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.polls.model.User;
import com.example.polls.model.UserFeedback;
import com.example.polls.repository.UserFeedbackRepository;
import com.example.polls.repository.UserRepository;

@Service
public class UserFeedbackService {

    @Autowired
    private UserFeedbackRepository userFeedbackRepository;

    @Autowired
    private UserRepository userRepository;

    public void submitFeedback(Long userId, String feedbackText) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        UserFeedback feedback = new UserFeedback();
        feedback.setUser(user);
        feedback.setFeedbackText(feedbackText);
        feedback.setCreatedAt(Instant.now());
        userFeedbackRepository.save(feedback);
    }

    public List<UserFeedback> getAllFeedbacks() {
        return userFeedbackRepository.findAll();
    }

    public void respondToFeedback(Long feedbackId, String responseText) {
        UserFeedback feedback = userFeedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new RuntimeException("Feedback not found"));
        feedback.setResponseText(responseText);
        feedback.setRespondedAt(Instant.now());
        userFeedbackRepository.save(feedback);
    }
}