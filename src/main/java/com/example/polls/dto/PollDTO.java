package com.example.polls.dto;

import java.time.Instant;
import java.util.List;

public class PollDTO {
    private Long id;
    private String question;
    private List<ChoiceDTO> choices;
    private Instant expirationDateTime;

    public PollDTO() {

    }

    public PollDTO(Long id, String question, List<ChoiceDTO> choices, Instant expirationDateTime) {
        this.id = id;
        this.question = question;
        this.choices = choices;
        this.expirationDateTime = expirationDateTime;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public List<ChoiceDTO> getChoices() {
        return choices;
    }

    public void setChoices(List<ChoiceDTO> choices) {
        this.choices = choices;
    }

    public Instant getExpirationDateTime() {
        return expirationDateTime;
    }

    public void setExpirationDateTime(Instant expirationDateTime) {
        this.expirationDateTime = expirationDateTime;
    }

}