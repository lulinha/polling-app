package com.example.polls.service;

import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.polls.dto.PollDTO;
import com.example.polls.exception.ResourceNotFoundException;
import com.example.polls.model.Comment;
import com.example.polls.model.Notification;
import com.example.polls.model.User;
import com.example.polls.repository.CommentRepository;
import com.example.polls.repository.NotificationRepository;
import com.example.polls.repository.UserRepository;

@Service
public class NotificationService {
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    @Autowired
    private MessageService messageService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private CommentRepository commentRepository;

    // 假设系统管理员的用户 ID 为 1
    private static final Long ADMIN_SENDER_ID = 1L;

    // 获取管理员列表
    private List<User> getAdmins() {
        // 这里需要根据实际情况实现获取管理员列表的逻辑
        // 例如，假设管理员角色为 ROLE_ADMIN
        // return userRepository.findByRoleName("ROLE_ADMIN");
        return null;
    }

    // 通知管理员新投票创建
    public void notifyAdmins(PollDTO poll) {
        List<User> admins = getAdmins();
        String messageContent = "新投票已创建: " + poll.getQuestion();
        admins.forEach(admin -> {
            messageService.sendMessage(ADMIN_SENDER_ID, admin.getId(), messageContent);
            logger.info("Notifying admin {} about new poll: {}", admin.getUsername(), poll.getQuestion());
        });
    }

    // 通知用户投票被拒绝
    public void notifyUserAboutPollRejection(Long pollId) {
        User user = getUserByPollId(pollId);
        if (user != null) {
            String messageContent = "您的投票 ID " + pollId + " 已被拒绝。";
            messageService.sendMessage(ADMIN_SENDER_ID, user.getId(), messageContent);
            logger.info("Notifying user {} about poll {} rejection", user.getUsername(), pollId);
        }
    }

    // 通知用户投票被删除
    public void notifyUserAboutPollDeletion(Long pollId) {
        User user = getUserByPollId(pollId);
        if (user != null) {
            String messageContent = "您的投票 ID " + pollId + " 已被删除。";
            messageService.sendMessage(ADMIN_SENDER_ID, user.getId(), messageContent);
            logger.info("Notifying user {} about poll {} deletion", user.getUsername(), pollId);
        }
    }

    // 通知用户被封禁
    public void notifyUserAboutBan(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User with id " + userId + " not found"));
        String messageContent = "您的账户已被封禁。";
        messageService.sendMessage(ADMIN_SENDER_ID, user.getId(), messageContent);
        logger.info("Notifying user {} about account ban", user.getUsername());
    }

    // 根据 pollId 获取用户
    private User getUserByPollId(Long pollId) {
        // 实际实现中需要从数据库中根据 pollId 获取用户
        // 这里简单假设可以通过 pollRepository 找到创建该投票的用户
        // 你需要根据实际情况实现该方法
        // 例如：
        // Poll poll = pollRepository.findById(pollId).orElse(null);
        // return poll != null ?
        // userRepository.findById(poll.getCreatedBy()).orElse(null) : null;
        return null;
    }

    @Transactional
    public void createMentionNotification(User mentionedUser, Comment comment) {
        Notification notification = new Notification();
        notification.setType(Notification.NotificationType.MENTION);
        notification.setRecipient(mentionedUser);
        notification.setSourceId(comment.getId());
        notification.setContent(buildMentionContent(comment));
        notification.setCreatedAt(Instant.now());

        notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public Page<Notification> getUserNotifications(Long userId, Pageable pageable) {
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(userId, pageable);
    }

    @Transactional
    public void markNotificationsAsRead(Long userId, List<Long> notificationIds) {
        notificationRepository.markAsRead(userId, notificationIds);
    }

    private String buildMentionContent(Comment comment) {
        String username = comment.getUser().getUsername();
        String contentPreview = comment.getContent().length() > 100
                ? comment.getContent().substring(0, 100) + "..."
                : comment.getContent();

        return String.format("%s mentioned you in a comment: %s", username, contentPreview);
    }

    @Async // 异步处理
    @Transactional
    public void sendMentionNotification(User mentionedUser, Comment comment) {
    }

    /**
     * 批量标记通知为已读（通过通知ID列表）
     * 
     * @param notificationIds 要标记的通知ID集合
     */
    public void markAsRead(List<Long> notificationIds) {
        if (notificationIds == null || notificationIds.isEmpty()) {
            return; // 无操作
        }

        // 批量更新数据库（使用JPQL批量操作）
        notificationRepository.markNotificationsAsRead(notificationIds);
    }
}
