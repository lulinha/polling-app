package com.example.polls.service;

import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.polls.exception.ResourceNotFoundException;
import com.example.polls.model.Message;
import com.example.polls.model.User;
import com.example.polls.repository.MessageRepository;
import com.example.polls.repository.UserRepository;

@Service
public class MessageService {

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private UserRepository userRepository;

    public Message sendMessage(Long senderId, Long recipientId, String content) {
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new ResourceNotFoundException("User with id " + senderId + " not found"));
        User recipient = userRepository.findById(recipientId)
                .orElseThrow(() -> new ResourceNotFoundException("User with id " + recipientId + " not found"));

        Message message = new Message();
        message.setSender(sender);
        message.setRecipient(recipient);
        message.setContent(content);
        message.setSentAt(Instant.now());

        return messageRepository.save(message);
    }

    public List<Message> getMessagesBetweenUsers(Long senderId, Long recipientId) {
        return messageRepository.findBySenderIdAndRecipientId(senderId, recipientId);
    }

    public List<Message> getMessagesForUser(Long recipientId) {
        return messageRepository.findByRecipientId(recipientId);
    }
}