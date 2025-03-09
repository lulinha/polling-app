package com.example.polls.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.polls.model.Role;
import com.example.polls.model.RoleName;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(RoleName roleName);
}
