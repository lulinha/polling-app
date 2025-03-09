package com.example.polls.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.polls.exception.ResourceNotFoundException;
import com.example.polls.model.Favorite;
import com.example.polls.model.Poll;
import com.example.polls.model.User;
import com.example.polls.payload.PollResponse;
import com.example.polls.repository.FavoriteRepository;
import com.example.polls.repository.PollRepository;

@Service
public class FavoriteService {

    @Autowired
    private FavoriteRepository favoriteRepository;
    private PollRepository pollRepository;
    private UserService userService;

    @Transactional
    public void toggleFavorite(Long pollId) {
        User currentUser = userService.getCurrentUser();
        Poll poll = pollRepository.findById(pollId)
            .orElseThrow(() -> new ResourceNotFoundException("Poll with id" + pollId + "not found"));

        Optional<Favorite> existing = favoriteRepository
            .findByUserIdAndPollId(currentUser.getId(), pollId);

        if (existing.isPresent()) {
            favoriteRepository.delete(existing.get());
        } else {
            Favorite favorite = new Favorite();
            favorite.setUser(currentUser);
            favorite.setPoll(poll);
            favoriteRepository.save(favorite);
        }
    }

    @Transactional(readOnly = true)
    public Page<PollResponse> getUserFavorites(Pageable pageable) {
        User currentUser = userService.getCurrentUser();
        return favoriteRepository.findByUserId(currentUser.getId(), pageable)
            .map(f -> mapToPollResponse(f.getPoll()));
    }

    private PollResponse mapToPollResponse(Poll poll) {
        // 实现你的DTO转换逻辑
        return null;
    }
}