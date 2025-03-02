package com.example.polls.payload;


public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private long timestamp;

    // 构造方法
    public ApiResponse(boolean success, String message) {
        this(success, message, null);
    }

    public ApiResponse(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    // Getters（不需要 Setters，因为响应对象应该是不可变的）
    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }

    public long getTimestamp() {
        return timestamp;
    }
}