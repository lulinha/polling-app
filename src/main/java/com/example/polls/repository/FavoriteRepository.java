package com.example.polls.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.polls.model.Favorite;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    
    Optional<Favorite> findByUserIdAndPollId(Long userId, Long pollId);
    
    Page<Favorite> findByUserId(Long userId, Pageable pageable);
    
    boolean existsByUserIdAndPollId(Long userId, Long pollId);
    
    @Modifying
    @Query("DELETE FROM Favorite f WHERE f.user.id = :userId AND f.poll.id = :pollId")
    void deleteByUserAndPoll(@Param("userId") Long userId, @Param("pollId") Long pollId);
}