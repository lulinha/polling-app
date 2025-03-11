package com.example.polls.service;

import java.util.Optional;

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
import com.example.polls.repository.UserRepository;
import com.example.polls.security.UserPrincipal;

@Service
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final PollRepository pollRepository;
    private final UserRepository userRepository;

    public FavoriteService(FavoriteRepository favoriteRepository, PollRepository pollRepository,
            UserRepository userRepository) {
        this.favoriteRepository = favoriteRepository;
        this.pollRepository = pollRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void toggleFavorite(UserPrincipal currentUser, Long pollId) {
        // 获取当前用户
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User with id " + currentUser.getId() + " not found"));

        Poll poll = pollRepository.findById(pollId)
                .orElseThrow(() -> new ResourceNotFoundException("Poll with id" + pollId + "not found"));

        Optional<Favorite> existing = favoriteRepository
                .findByUserIdAndPollId(user.getId(), pollId);

        if (existing.isPresent()) {
            favoriteRepository.delete(existing.get());
        } else {
            Favorite favorite = new Favorite();
            favorite.setUser(user);
            favorite.setPoll(poll);
            favoriteRepository.save(favorite);
        }
    }

    @Transactional(readOnly = true)
    public Page<PollResponse> getUserFavorites(UserPrincipal currentUser, Pageable pageable) {
        return favoriteRepository.findByUserId(currentUser.getId(), pageable)
                .map(f -> mapToPollResponse(f.getPoll()));
    }

    private PollResponse mapToPollResponse(Poll poll) {
        // 实现你的DTO转换逻辑
        return null;
    }
}