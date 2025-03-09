package com.example.polls.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.polls.model.Tag;

public interface TagRepository extends JpaRepository<Tag, Long> {
    Optional<Tag> findByName(String name);
    boolean existsByName(String name);
}