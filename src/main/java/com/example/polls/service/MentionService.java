package com.example.polls.service;

import java.util.Collections;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.polls.exception.ResourceNotFoundException;
import com.example.polls.model.Comment;
import com.example.polls.model.Mention;
import com.example.polls.payload.MentionResponse;
import com.example.polls.repository.MentionRepository;
import com.example.polls.security.UserPrincipal;

@Service
public class MentionService {
    private final MentionRepository mentionRepository;
    private final NotificationService notificationService;

    

    public MentionService(MentionRepository mentionRepository, NotificationService notificationService) {
        this.mentionRepository = mentionRepository;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public Page<MentionResponse> getUserMentions(UserPrincipal currentUser, Pageable pageable) {
        Long userId = currentUser.getId();
        return mentionRepository.findByMentionedUserId(userId, pageable)
            .map(mention -> {
                Comment comment = mention.getComment();
                return new MentionResponse(
                    mention.getId(),
                    comment.getContent(),
                    comment.getPoll().getQuestion(),
                    mention.getCreatedAt(),
                    mention.isRead()
                );
            });
    }

    @Transactional
    public void markAsRead(Long mentionId) {
        Mention mention = mentionRepository.findById(mentionId)
            .orElseThrow(() -> new ResourceNotFoundException("Mention with id" + mentionId + " not found"));
        
        mention.markAsRead();
        mentionRepository.save(mention);
        
        notificationService.markAsRead(
            Collections.singletonList(mention.getNotificationId())
        );
    }
}
