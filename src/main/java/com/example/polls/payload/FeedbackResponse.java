package com.example.polls.payload;

import jakarta.validation.constraints.NotBlank;

public class FeedbackResponse {
    @NotBlank
    private String responseText;

    public String getResponseText() {
        return responseText;
    }

    public void setResponseText(String responseText) {
        this.responseText = responseText;
    }
}