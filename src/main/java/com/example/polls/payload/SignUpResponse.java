package com.example.polls.payload;

public class SignUpResponse {
    private final Long userId;
    private final String username;
    private final String email;

    // 构造函数
    public SignUpResponse(Long userId, String username, String email) {
        this.userId = userId;
        this.username = username;
        this.email = email;
    }

    // Getter 方法
    public Long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

}
