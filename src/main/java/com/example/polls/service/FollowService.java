package com.example.polls.service;


import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.polls.model.Follow;
import com.example.polls.model.User;
import com.example.polls.repository.FollowRepository;

@Service
public class FollowService {

    @Autowired
    private FollowRepository followRepository;

    public Follow followUser(User follower, User followed) {
        Optional<Follow> existingFollow = followRepository.findByFollowerAndFollowed(follower, followed);
        if (existingFollow.isPresent()) {
            return existingFollow.get();
        }
        Follow follow = new Follow(follower, followed);
        return followRepository.save(follow);
    }

    public void unfollowUser(User follower, User followed) {
        Optional<Follow> follow = followRepository.findByFollowerAndFollowed(follower, followed);
        follow.ifPresent(followRepository::delete);
    }

    public List<Follow> getFollowingList(User user) {
        return followRepository.findByFollower(user);
    }

    public List<Follow> getFollowerList(User user) {
        return followRepository.findByFollowed(user);
    }
}