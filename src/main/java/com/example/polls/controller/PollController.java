package com.example.polls.controller;

import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.example.polls.constants.AppConstants;
import com.example.polls.model.Poll;
import com.example.polls.payload.ApiResponse;
import com.example.polls.payload.PagedResponse;
import com.example.polls.payload.PollRequest;
import com.example.polls.payload.PollResponse;
import com.example.polls.payload.VoteRequest;
import com.example.polls.security.CurrentUser;
import com.example.polls.security.UserPrincipal;
import com.example.polls.service.PollService;
import com.example.polls.service.VoteService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/polls")
public class PollController {

    @Autowired
    private PollService pollService;

    @Autowired
    private VoteService voteService;

    private static final Logger logger = LoggerFactory.getLogger(PollController.class);

    @GetMapping
    public PagedResponse<PollResponse> getPolls(@CurrentUser UserPrincipal currentUser,
            @RequestParam(value = "page", defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(value = "size", defaultValue = AppConstants.DEFAULT_PAGE_SIZE) int size) {
        return pollService.getAllPollsWithCache(currentUser, page, size);
    }

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Poll>> createPoll(@Valid @RequestBody PollRequest pollRequest,
            @CurrentUser UserPrincipal currentUser) {
        Poll poll = pollService.createPollWithCache(pollRequest, currentUser);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest().path("/api/polls/{pollId}")
                .buildAndExpand(poll.getId()).toUri();
        ApiResponse<Poll> response = new ApiResponse<>(true, "Poll Created Successfully", poll);
        return ResponseEntity.created(location)
                .body(response);
    }

    @GetMapping("/{pollId}")
    public PollResponse getPollById(@CurrentUser UserPrincipal currentUser,
            @PathVariable Long pollId) {
        return pollService.getPollByIdWithCache(pollId, currentUser);
    }

    @PostMapping("/{pollId}/votes")
    @PreAuthorize("hasRole('USER')")
    public PollResponse castVote(@CurrentUser UserPrincipal currentUser,
            @PathVariable Long pollId,
            @Valid @RequestBody VoteRequest voteRequest) {
        return voteService.castVoteAndGetUpdatedPollWithCache(pollId, voteRequest, currentUser);
    }

    @GetMapping("/hot")
    public PagedResponse<PollResponse> getHotPolls(
            @CurrentUser UserPrincipal currentUser) {
        return pollService.getHotPolls(currentUser);
    }

    @PutMapping("/{pollId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public Poll approvePoll(@PathVariable Long pollId, @RequestParam boolean approved) {
        return pollService.approvePoll(pollId, approved);
    }

    @DeleteMapping("/{pollId}")
    @PreAuthorize("hasRole('ADMIN')")
    public void deletePoll(@PathVariable Long pollId) {
        pollService.deletePoll(pollId);
    }

}
