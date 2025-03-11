package com.example.polls.payload;

import java.time.Instant;
import java.util.Objects;

public class MentionResponse {
    private Long id;
    private String commentContent;
    private String pollQuestion;
    private Instant createdAt;
    private boolean read;

    // 全参构造器
    public MentionResponse(
            Long id,
            String commentContent,
            String pollQuestion,
            Instant createdAt,
            boolean read) {
        this.id = id;
        this.commentContent = commentContent;
        this.pollQuestion = pollQuestion;
        this.createdAt = createdAt;
        this.read = read;
    }

    // 无参构造器
    public MentionResponse() {
    }

    // Getter 和 Setter
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCommentContent() {
        return commentContent;
    }

    public void setCommentContent(String commentContent) {
        this.commentContent = commentContent;
    }

    public String getPollQuestion() {
        return pollQuestion;
    }

    public void setPollQuestion(String pollQuestion) {
        this.pollQuestion = pollQuestion;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    // equals() 和 hashCode()
    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        MentionResponse that = (MentionResponse) o;
        return read == that.read &&
                Objects.equals(id, that.id) &&
                Objects.equals(commentContent, that.commentContent) &&
                Objects.equals(pollQuestion, that.pollQuestion) &&
                Objects.equals(createdAt, that.createdAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, commentContent, pollQuestion, createdAt, read);
    }

    // toString()
    @Override
    public String toString() {
        return "MentionResponse{" +
                "id=" + id +
                ", commentContent='" + commentContent + '\'' +
                ", pollQuestion='" + pollQuestion + '\'' +
                ", createdAt=" + createdAt +
                ", read=" + read +
                '}';
    }
}