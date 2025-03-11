package com.example.polls.event;

import java.util.Objects;

import com.example.polls.enums.LikeAction;

public final class LikeEvent {
    private final Long commentId;
    private final Long userId;
    private final LikeAction action;

    // 全参构造函数
    public LikeEvent(Long commentId, Long userId, LikeAction action) {
        this.commentId = commentId;
        this.userId = userId;
        this.action = action;
    }

    // 字段访问方法（保持record风格的方法名）
    public Long commentId() {
        return commentId;
    }

    public Long userId() {
        return userId;
    }

    public LikeAction action() {
        return action;
    }

    // 自动生成的equals方法
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LikeEvent likeEvent = (LikeEvent) o;
        return Objects.equals(commentId, likeEvent.commentId)
                && Objects.equals(userId, likeEvent.userId)
                && action == likeEvent.action;
    }

    // 自动生成的hashCode方法
    @Override
    public int hashCode() {
        return Objects.hash(commentId, userId, action);
    }

    // 自动生成的toString方法
    @Override
    public String toString() {
        return "LikeEvent[" +
                "commentId=" + commentId +
                ", userId=" + userId +
                ", action=" + action +
                ']';
    }
}