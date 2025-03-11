package com.example.polls.model;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

import org.springframework.data.annotation.CreatedDate;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

@Entity
@Table(name = "comment_likes")
public class CommentLike {
    @EmbeddedId
    private CommentLikeId id;

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @MapsId("commentId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id", nullable = false)
    private Comment comment;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    public CommentLike() {
        
    }
    public CommentLike(User user, Comment comment) {
        this.user = user;
        this.comment = comment;
        this.id = new CommentLikeId(user.getId(), comment.getId()); 
    }

    public CommentLikeId getId() {
        return id;
    }

    public void setId(CommentLikeId id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Comment getComment() {
        return comment;
    }

    public void setComment(Comment comment) {
        this.comment = comment;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    // 静态嵌套主键类
    @Embeddable
    public static class CommentLikeId implements Serializable {
        @Column(name = "user_id")
        private Long userId;

        @Column(name = "comment_id")
        private Long commentId;

        public CommentLikeId() {
        }

        public CommentLikeId(Long userId, Long commentId) {
            this.userId = userId;
            this.commentId = commentId;
        }

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public Long getCommentId() {
            return commentId;
        }

        public void setCommentId(Long commentId) {
            this.commentId = commentId;
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 13 * hash + Objects.hashCode(this.userId);
            hash = 13 * hash + Objects.hashCode(this.commentId);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final CommentLikeId other = (CommentLikeId) obj;
            if (!Objects.equals(this.userId, other.userId)) {
                return false;
            }
            return Objects.equals(this.commentId, other.commentId);
        }

    }
}