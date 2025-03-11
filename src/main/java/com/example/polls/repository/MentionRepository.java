package com.example.polls.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.polls.model.Mention;


public interface MentionRepository extends JpaRepository<Mention, Long> {
    
    // 根据被提及用户ID分页查询
    Page<Mention> findByMentionedUserId(Long mentionedUserId, Pageable pageable);
    
    // 标准findById方法（使用Long主键）
    Optional<Mention> findById(Long id);

    @Modifying
    @Query("DELETE FROM Mention m WHERE m.comment.id = :commentId")
    void deleteByCommentId(@Param("commentId") Long commentId);
}