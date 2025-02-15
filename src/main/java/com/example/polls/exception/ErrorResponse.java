package com.example.polls.exception;

import java.time.LocalDateTime;

public class ErrorResponse {
    private final LocalDateTime timestamp;
    private final int status;
    private final String error;
    private final String code;
    private final String message;
    private final String path;
    private final Object details;

    // 构造函数
    public ErrorResponse(LocalDateTime timestamp, int status, String error,
            String code, String message, String path, Object details) {
        this.timestamp = timestamp;
        this.status = status;
        this.error = error;
        this.code = code;
        this.message = message;
        this.path = path;
        this.details = details;
    }

    // Getter 方法
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public int getStatus() {
        return status;
    }

    public String getError() {
        return error;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public String getPath() {
        return path;
    }

    public Object getDetails() {
        return details;
    }

    // Builder 模式（手动实现）
    public static class Builder {
        private LocalDateTime timestamp;
        private int status;
        private String error;
        private String code;
        private String message;
        private String path;
        private Object details;

        public Builder timestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder status(int status) {
            this.status = status;
            return this;
        }

        public Builder error(String error) {
            this.error = error;
            return this;
        }

        public Builder code(String code) {
            this.code = code;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder path(String path) {
            this.path = path;
            return this;
        }

        public Builder details(Object details) {
            this.details = details;
            return this;
        }

        public ErrorResponse build() {
            return new ErrorResponse(timestamp, status, error, code, message, path, details);
        }
    }
}