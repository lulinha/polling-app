package com.example.polls.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.polls.dto.PollDTO;

@Service
public class NotificationService {
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    public void notifyAdmins(PollDTO poll) {
        // 实际实现邮件/短信通知逻辑
        logger.info("Notifying admins about new poll: {}", poll.getQuestion());
    }
}