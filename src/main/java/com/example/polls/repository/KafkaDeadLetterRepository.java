package com.example.polls.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.polls.model.KafkaDeadLetter;

public interface KafkaDeadLetterRepository extends JpaRepository<KafkaDeadLetter, Long> {
    List<KafkaDeadLetter> findByRetryCountLessThan(int maxRetries);
}