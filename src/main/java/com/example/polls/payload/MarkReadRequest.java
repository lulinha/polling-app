package com.example.polls.payload;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;

public class MarkReadRequest {
    @NotEmpty
    private List<Long> notificationIds;

    public List<Long> getNotificationIds() {
        return notificationIds;
    }

    public void setNotificationIds(List<Long> notificationIds) {
        this.notificationIds = notificationIds;
    }

}