package com.example.polls.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.polls.model.KafkaOutbox;

public interface KafkaOutboxRepository extends JpaRepository<KafkaOutbox, Long> {
    List<KafkaOutbox> findByStatusAndRetryCountLessThan(String status, int maxRetries);
}
