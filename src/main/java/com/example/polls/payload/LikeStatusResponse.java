package com.example.polls.payload;

public class LikeStatusResponse {
    private boolean liked;
    private int likeCount;

    public LikeStatusResponse() {
    }

    public LikeStatusResponse(boolean liked, int likeCount) {
        this.liked = liked;
        this.likeCount = likeCount;
    }

    public boolean isLiked() {
        return liked;
    }

    public void setLiked(boolean liked) {
        this.liked = liked;
    }

    public int getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(int likeCount) {
        this.likeCount = likeCount;
    }

}