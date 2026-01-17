package com.petbuddy.auth.repository;

import com.petbuddy.auth.model.entity.Role;
import com.petbuddy.auth.model.enums.ERole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Integer> {
    Optional<Role> findByName(ERole name);
}