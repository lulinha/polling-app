package com.example.polls.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.polls.exception.AccessDeniedException;
import com.example.polls.exception.ResourceNotFoundException;
import com.example.polls.model.Comment;
import com.example.polls.model.Mention;
import com.example.polls.model.RoleName;
import com.example.polls.model.User;
import com.example.polls.payload.CommentRequest;
import com.example.polls.payload.CommentResponse;
import com.example.polls.repository.CommentLikeRepository;
import com.example.polls.repository.CommentRepository;
import com.example.polls.repository.MentionRepository;
import com.example.polls.repository.PollRepository;
import com.example.polls.repository.UserRepository;
import com.example.polls.security.UserPrincipal;
import com.example.polls.util.CommentMapper;

@Service
@Transactional
public class CommentService {

    private final PollRepository pollRepository;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final MentionRepository mentionRepository;
    private final UserService userService;
    private final NotificationService notificationService;
    private final CommentMapper commentMapper;

    public CommentService(PollRepository pollRepository, UserRepository userRepository,
            CommentRepository commentRepository, CommentLikeRepository commentLikeRepository,
            MentionRepository mentionRepository, UserService userService, NotificationService notificationService,
            CommentMapper commentMapper) {
        this.pollRepository = pollRepository;
        this.userRepository = userRepository;
        this.commentRepository = commentRepository;
        this.commentLikeRepository = commentLikeRepository;
        this.mentionRepository = mentionRepository;
        this.userService = userService;
        this.notificationService = notificationService;
        this.commentMapper = commentMapper;
    }

    public CommentResponse createComment(UserPrincipal currentUser, CommentRequest request) {
        // 获取当前用户
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User with id " + currentUser.getId() + " not found"));

        String content = request.getContent();
        Long pollId = request.getPollId();
        Long parentId = request.getParentId();
        // 解析@提及
        Set<String> mentionedUsernames = parseMentions(content);

        Comment comment = new Comment();
        comment.setContent(content);
        comment.setPoll(pollRepository.getReferenceById(pollId));
        comment.setUser(user);

        if (parentId != null) {
            Comment parentComment = commentRepository.findById(parentId)
                   .orElseThrow(() -> new ResourceNotFoundException("Parent comment with id" + parentId + " not found"));
            comment.setParent(parentComment);
        }

        Comment savedComment = commentRepository.save(comment);

        // 处理提及
        mentionedUsernames.forEach(username -> {
            User mentionedUser = userRepository.findByUsername(username)
                    .orElseThrow(() -> new ResourceNotFoundException("User with username" + username + " not found"));
            Mention mention = new Mention();
            mention.setComment(savedComment);
            mention.setMentionedUser(mentionedUser);
            mentionRepository.save(mention);

            // 发送通知
            notificationService.sendMentionNotification(mentionedUser, savedComment);
        });

        return commentMapper.toResponse(savedComment);
    }

    private Set<String> parseMentions(String content) {
        Pattern pattern = Pattern.compile("@([a-zA-Z0-9_]+)");
        Matcher matcher = pattern.matcher(content);
        Set<String> mentions = new HashSet<>();
        while (matcher.find()) {
            mentions.add(matcher.group(1));
        }
        return mentions;
    }

    public Page<CommentResponse> getCommentsByPoll(Long pollId, Pageable pageable) {
        return commentRepository.findByPollId(pollId, pageable)
                .map(commentMapper::toResponse);
    }

    @Transactional
    public void deleteComment(UserPrincipal currentUser, Long commentId) {
        // 获取当前用户
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User with id " + currentUser.getId() + " not found"));
        // 1. 获取评论并验证存在性
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment with id" + commentId + " not found"));

        // 2. 验证权限：评论作者或管理员可删除
        if (!isCommentOwnerOrAdmin(comment, user)) {
            throw new AccessDeniedException("无权删除该评论");
        }

        // 3. 级联删除关联数据
        deleteRelatedData(comment);

        // 4. 删除评论及其子评论
        deleteCommentAndReplies(comment);
    }

    private boolean isCommentOwnerOrAdmin(Comment comment, User user) {
        return comment.getUser().equals(user)
                || user.getRoles().stream()
                        .anyMatch(role -> role.getName() == RoleName.ROLE_ADMIN);
    }

    private void deleteRelatedData(Comment comment) {
        // 删除所有点赞记录
        commentLikeRepository.deleteByCommentId(comment.getId());
        // 删除所有提及记录
        mentionRepository.deleteByCommentId(comment.getId());
    }

    private void deleteCommentAndReplies(Comment comment) {
        // 递归删除子评论
        List<Comment> replies = comment.getReplies();
        if (replies != null && !replies.isEmpty()) {
            replies.forEach(reply -> deleteCommentAndReplies(reply));
        }
        // 删除当前评论
        commentRepository.delete(comment);
    }
}