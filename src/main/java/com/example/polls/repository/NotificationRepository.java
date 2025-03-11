package com.example.polls.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.polls.model.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByRecipientIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.recipient.id = :userId AND n.read = false")
    long countUnreadByUserId(Long userId);

    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.id IN :ids AND n.recipient.id = :userId")
    int markAsRead(Long userId, List<Long> ids);

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.recipient.id = :userId AND n.createdAt < :threshold")
    int deleteOldNotifications(Long userId, Instant threshold);

    /**
     * 批量更新通知为已读状态
     * 
     * @param notificationIds 通知ID列表
     */
    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.id IN :ids")
    void markNotificationsAsRead(@Param("ids") List<Long> notificationIds);
}