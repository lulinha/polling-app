package com.example.polls.controller;


import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.polls.model.Message;
import com.example.polls.payload.MessageRequest;
import com.example.polls.security.CurrentUser;
import com.example.polls.security.UserPrincipal;
import com.example.polls.service.MessageService;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    @Autowired
    private MessageService messageService;

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Message> sendMessage(@CurrentUser UserPrincipal currentUser,
            @RequestBody MessageRequest messageRequest) {
        Message message = messageService.sendMessage(currentUser.getId(), messageRequest.getRecipientId(),
                messageRequest.getContent());
        return ResponseEntity.ok().body(message);
    }

    @GetMapping("/between/{senderId}/{recipientId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<Message>> getMessagesBetweenUsers(@PathVariable Long senderId,
            @PathVariable Long recipientId) {
        List<Message> messages = messageService.getMessagesBetweenUsers(senderId, recipientId);
        return ResponseEntity.ok().body(messages);
    }

    @GetMapping("/for/{recipientId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<Message>> getMessagesForUser(@PathVariable Long recipientId) {
        List<Message> messages = messageService.getMessagesForUser(recipientId);
        return ResponseEntity.ok().body(messages);
    }
}