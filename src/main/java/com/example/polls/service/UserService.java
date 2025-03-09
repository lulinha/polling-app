package com.example.polls.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.polls.exception.ResourceNotFoundException;
import com.example.polls.model.User;
import com.example.polls.model.Vote;
import com.example.polls.payload.UserIdentityAvailability;
import com.example.polls.payload.UserProfile;
import com.example.polls.payload.UserSummary;
import com.example.polls.repository.PollRepository;
import com.example.polls.repository.UserRepository;
import com.example.polls.repository.VoteRepository;
import com.example.polls.security.UserPrincipal;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PollRepository pollRepository;

    @Autowired
    private VoteRepository voteRepository;

    @Autowired
    private NotificationService notificationService;

    public User banUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User with id " + userId + " not found"));
        user.setBanned(true);
        User savedUser = userRepository.save(user);

        // 通知用户被封禁
        notificationService.notifyUserAboutBan(userId);
        return savedUser;
    }

    public UserSummary getCurrentUser(UserPrincipal currentUser) {
        return new UserSummary(currentUser.getId(), currentUser.getUsername(), currentUser.getName());
    }

     // 核心方法：获取当前认证用户
    @Transactional(readOnly = true)
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AuthenticationCredentialsNotFoundException("用户未登录");
        }

        // 根据认证主体类型处理不同情况
        Object principal = authentication.getPrincipal();
        
        if (principal instanceof UserDetails) { // JWT 或 Spring Security UserDetails
            return (User) principal;
        } else if (principal instanceof String) { // 用户名作为 principal
            String username = (String) principal;
            return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在: " + username));
        }

        throw new IllegalStateException("无法识别的用户凭证类型");
    }

    public UserIdentityAvailability checkUsernameAvailability(String username) {
        Boolean isAvailable = !userRepository.existsByUsername(username);
        return new UserIdentityAvailability(isAvailable);
    }

    public UserIdentityAvailability checkEmailAvailability(String email) {
        Boolean isAvailable = !userRepository.existsByEmail(email);
        return new UserIdentityAvailability(isAvailable);
    }

    public UserProfile getUserProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User with username " + username + " not found"));

        long pollCount = pollRepository.countByCreatedBy(user.getId());
        long voteCount = voteRepository.countByUserId(user.getId());

        return new UserProfile(user.getId(), user.getUsername(), user.getName(), user.getCreatedAt(),
                pollCount, voteCount, user.getPoints(), user.getLevel());
    }

    public void addPoints(User user, int points) {
        user.setPoints(user.getPoints() + points);
        updateLevel(user);
        userRepository.saveAndFlush(user);
    }

    private void updateLevel(User user) {
        int points = user.getPoints();
        if (points < 100) {
            user.setLevel(1);
        } else if (points < 500) {
            user.setLevel(2);
        } else {
            user.setLevel(3);
        }
    }

    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User with username " + username + " not found"));
    }

    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User with id " + userId + " not found"));
    }

    public List<Vote> getUserVotes(Long userId) {
        return voteRepository.findByUserId(userId);
    }
}