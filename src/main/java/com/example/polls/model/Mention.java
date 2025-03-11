package com.example.polls.model;

import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "mentions")
public class Mention {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // -> id BIGSERIAL

    @Column(name = "read")
    private boolean isRead; // -> is_read BOOLEAN

    @Column(name = "notification_id")
    private Long notificationId; // -> notification_id BIGINT

    @ManyToOne
    @JoinColumn(name = "comment_id")
    private Comment comment; // -> comment_id BIGINT

    @ManyToOne
    @JoinColumn(name = "mentioned_user_id")
    private User mentionedUser; // -> mentioned_user_id BIGINT

    @CreatedDate
    @Column(name = "created_at")
    private Instant createdAt; // -> created_at TIMESTAMPTZ

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean isRead) {
        this.isRead = isRead;
    }

    public Long getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(Long notificationId) {
        this.notificationId = notificationId;
    }

    public Comment getComment() {
        return comment;
    }

    public void setComment(Comment comment) {
        this.comment = comment;
    }

    public User getMentionedUser() {
        return mentionedUser;
    }

    public void setMentionedUser(User mentionedUser) {
        this.mentionedUser = mentionedUser;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public void markAsRead() {
        this.isRead = true;
    }

}