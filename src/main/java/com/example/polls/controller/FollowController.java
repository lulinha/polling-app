package com.example.polls.controller;


import com.example.polls.model.User;
import com.example.polls.security.CurrentUser;
import com.example.polls.security.UserPrincipal;
import com.example.polls.service.FollowService;
import com.example.polls.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/follows")
public class FollowController {

    @Autowired
    private FollowService followService;

    @Autowired
    private UserService userService;

    @PostMapping("/{username}/follow")
    public ResponseEntity<?> followUser(@CurrentUser UserPrincipal currentUser, @PathVariable String username) {
        User follower = userService.getUserByUsername(currentUser.getUsername());
        User followed = userService.getUserByUsername(username);
        followService.followUser(follower, followed);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{username}/unfollow")
    public ResponseEntity<?> unfollowUser(@CurrentUser UserPrincipal currentUser, @PathVariable String username) {
        User follower = userService.getUserByUsername(currentUser.getUsername());
        User followed = userService.getUserByUsername(username);
        followService.unfollowUser(follower, followed);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/following")
    public List<?> getFollowingList(@CurrentUser UserPrincipal currentUser) {
        User user = userService.getUserByUsername(currentUser.getUsername());
        return followService.getFollowingList(user);
    }

    @GetMapping("/followers")
    public List<?> getFollowerList(@CurrentUser UserPrincipal currentUser) {
        User user = userService.getUserByUsername(currentUser.getUsername());
        return followService.getFollowerList(user);
    }
}