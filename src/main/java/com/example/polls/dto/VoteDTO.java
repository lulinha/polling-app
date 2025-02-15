package com.example.polls.dto;

public class VoteDTO {
    private Long id;
    private Long pollId;
    private Long choiceId;
    private Long userId;

    public VoteDTO() {
    }

    public VoteDTO(Long id, Long pollId, Long choiceId, Long userId) {
        this.id = id;
        this.pollId = pollId;
        this.choiceId = choiceId;
        this.userId = userId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPollId() {
        return pollId;
    }

    public void setPollId(Long pollId) {
        this.pollId = pollId;
    }

    public Long getChoiceId() {
        return choiceId;
    }

    public void setChoiceId(Long choiceId) {
        this.choiceId = choiceId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

}