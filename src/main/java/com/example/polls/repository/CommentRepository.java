package com.example.polls.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.polls.model.Comment;

import jakarta.persistence.LockModeType;

public interface CommentRepository extends JpaRepository<Comment, Long> {

        // 获取根评论（包含直接回复）带分页, 嵌套评论查询优化 LEFT JOIN FETCH 预加载直接回复，避免多次查询数据库
        @Query("SELECT c FROM Comment c " +
                        "LEFT JOIN FETCH c.replies " +
                        "WHERE c.poll.id = :pollId " +
                        "AND c.parent IS NULL " +
                        "ORDER BY c.createdAt DESC")
        Page<Comment> findRootCommentsByPollId(@Param("pollId") Long pollId, Pageable pageable);

        // 获取评论的所有子回复（嵌套结构）
        @Query("SELECT c FROM Comment c " +
                        "WHERE c.parent.id = :parentId " +
                        "ORDER BY c.createdAt ASC")
        List<Comment> findRepliesByParentId(@Param("parentId") Long parentId);

        // 统计投票的评论总数
        @Query("SELECT COUNT(c) FROM Comment c WHERE c.poll.id = :pollId")
        long countByPollId(@Param("pollId") Long pollId);

        // 获取用户的所有评论（个人中心用）
        Page<Comment> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

        // 安全删除（验证用户所有权）
        @Modifying
        @Query("DELETE FROM Comment c WHERE c.id = :commentId AND c.user.id = :userId")
        int deleteByUserAndId(@Param("userId") Long userId, @Param("commentId") Long commentId);

        // 优化查询：使用 EntityGraph 避免 N+1 问题
        @EntityGraph(attributePaths = { "user", "mentions.mentionedUser" })
        @Query("SELECT c FROM Comment c WHERE c.id IN :ids")
        List<Comment> findCommentsWithUsersAndMentions(@Param("ids") List<Long> commentIds);

        // 使用派生查询
        Page<Comment> findByPollId(Long pollId, Pageable pageable);

        // 或使用自定义查询（推荐复杂查询时使用）
        @Query("SELECT c FROM Comment c WHERE c.poll.id = :pollId AND c.parent IS NULL")
        Page<Comment> findRootCommentsByPoll(
                        @Param("pollId") Long pollId,
                        Pageable pageable);

        // 使用悲观锁保证并发安全
        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("SELECT c FROM Comment c WHERE c.id = :id")
        Optional<Comment> findByIdWithLock(@Param("id") Long id);

        // 批量更新点赞计数
        @Modifying
        @Query("UPDATE Comment c SET c.likeCount = :count WHERE c.id = :id")
        void updateLikeCount(@Param("id") Long id, @Param("count") int count);
}