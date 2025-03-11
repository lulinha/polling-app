package com.example.polls.payload;

import java.time.Instant;
import java.util.List;

public class CommentResponse {

    private Long id;
    private String content;
    private String author;
    private Instant createdAt;
    private int likeCount;
    private Long parentId;
    private List<CommentResponse> replies;

    public CommentResponse(Long id, String content, String author, Instant createdAt, int likeCount, Long parentId,
            List<CommentResponse> replies) {
        this.id = id;
        this.content = content;
        this.author = author;
        this.createdAt = createdAt;
        this.likeCount = likeCount;
        this.parentId = parentId;
        this.replies = replies;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public int getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(int likeCount) {
        this.likeCount = likeCount;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public List<CommentResponse> getReplies() {
        return replies;
    }

    public void setReplies(List<CommentResponse> replies) {
        this.replies = replies;
    }

}
