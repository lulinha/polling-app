package com.example.polls.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.polls.model.Comment;
import com.example.polls.model.CommentLike;
import com.example.polls.model.User;

public interface CommentLikeRepository extends JpaRepository<CommentLike, CommentLike.CommentLikeId> {

    boolean existsByUserAndComment(User user, Comment comment);

    // 自定义复合主键查询
    Optional<CommentLike> findByUserAndComment(User user, Comment comment);

    // 根据评论统计点赞数
    @Query("SELECT COUNT(cl) FROM CommentLike cl WHERE cl.comment.id = :commentId")
    long countByCommentId(@Param("commentId") Long commentId);

    // 安全删除（验证用户所有权）
    @Modifying
    @Query("DELETE FROM CommentLike cl WHERE cl.user.id = :userId AND cl.comment.id = :commentId")
    void deleteByUserAndComment(@Param("userId") Long userId,
            @Param("commentId") Long commentId);

    @Modifying
    @Query("DELETE FROM CommentLike cl WHERE cl.comment.id = :commentId")
    void deleteByCommentId(@Param("commentId") Long commentId);

    @Query("SELECT u FROM User u " +
            "JOIN CommentLike cl ON cl.user.id = u.id " +
            "WHERE cl.comment.id = :commentId")
    Page<User> findUsersByCommentId(@Param("commentId") Long commentId, Pageable pageable);
}