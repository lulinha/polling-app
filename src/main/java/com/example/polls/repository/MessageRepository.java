package com.example.polls.repository;


import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.polls.model.Message;

public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findBySenderIdAndRecipientId(Long senderId, Long recipientId);
    List<Message> findByRecipientId(Long recipientId);
}