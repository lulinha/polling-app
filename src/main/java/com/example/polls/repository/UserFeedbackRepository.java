package com.example.polls.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.polls.model.UserFeedback;

public interface UserFeedbackRepository extends JpaRepository<UserFeedback, Long> {
    List<UserFeedback> findAll();
}