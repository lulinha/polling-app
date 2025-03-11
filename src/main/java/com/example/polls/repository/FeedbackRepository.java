package com.example.polls.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.polls.model.Feedback;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    List<Feedback> findAll();
}