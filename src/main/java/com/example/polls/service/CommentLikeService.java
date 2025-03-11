package com.example.polls.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.polls.enums.LikeAction;
import com.example.polls.event.LikeEvent;
import com.example.polls.exception.ConflictException;
import com.example.polls.exception.ResourceNotFoundException;
import com.example.polls.model.Comment;
import com.example.polls.model.CommentLike;
import com.example.polls.model.CommentLike.CommentLikeId;
import com.example.polls.model.User;
import com.example.polls.payload.LikeStatusResponse;
import com.example.polls.payload.UserProfile;
import com.example.polls.repository.CommentLikeRepository;
import com.example.polls.repository.CommentRepository;
import com.example.polls.repository.UserRepository;
import com.example.polls.security.UserPrincipal;

@Service
public class CommentLikeService {

    private final CommentLikeRepository commentLikeRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final ApplicationEventPublisher eventPublisher;

    public CommentLikeService(CommentLikeRepository commentLikeRepository, CommentRepository commentRepository,
            UserRepository userRepository, UserService userService, ApplicationEventPublisher eventPublisher) {
        this.commentLikeRepository = commentLikeRepository;
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
        this.userService = userService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void likeComment(Long commentId, Long userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment with id" + commentId + " not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User with id " + userId + " not found"));

        if (commentLikeRepository.existsByUserAndComment(user, comment)) {
            throw new ConflictException("Already liked");
        }

        commentLikeRepository.save(new CommentLike(user, comment));
    }

    @Transactional(readOnly = true)
    public Page<UserProfile> getLikedUsers(Long commentId, Pageable pageable) {
        return commentLikeRepository.findUsersByCommentId(commentId, pageable)
                .map(user -> new UserProfile(
                        user.getId(),
                        user.getUsername()));
    }

    @Transactional
    public LikeStatusResponse toggleLike(UserPrincipal currentUser, Long commentId) {
        // 获取当前用户
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User with id " + currentUser.getId() + " not found"));
        // 1. 获取并验证评论存在性
        Comment comment = commentRepository.findByIdWithLock(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment with id" + commentId + " not found"));

        // 2. 检查现有点赞记录（使用复合主键快速查询）
        CommentLikeId likeId = new CommentLikeId(user.getId(), commentId);
        boolean alreadyLiked = commentLikeRepository.existsById(likeId);

        // 3. 执行点赞/取消操作
        if (alreadyLiked) {
            commentLikeRepository.deleteById(likeId);
            comment.decrementLikeCount(); // 内存计数
        } else {
            CommentLike like = new CommentLike(user, comment);
            commentLikeRepository.save(like);
            comment.incrementLikeCount(); // 内存计数
        }

        // 4. 批量更新计数到数据库（避免并发问题）
        commentRepository.updateLikeCount(commentId, comment.getLikeCount());

        // 5. 发布领域事件（用于通知、审计等）
        eventPublisher.publishEvent(new LikeEvent(
                commentId,
                user.getId(),
                alreadyLiked ? LikeAction.UNLIKE : LikeAction.LIKE));

        return new LikeStatusResponse(!alreadyLiked, comment.getLikeCount());
    }
}