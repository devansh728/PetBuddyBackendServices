package com.petbuddy.auth.service;

import com.petbuddy.auth.model.entity.Role;
import com.petbuddy.auth.model.enums.ERole;
import com.petbuddy.auth.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RoleService {
    private final RoleRepository roleRepository;

    public Role getRole(ERole eRole) {
        Optional<Role> role = roleRepository.findByName(eRole);
        return role.orElseThrow(() -> new IllegalStateException("Role not found: " + eRole));
    }

    public Role createIfNotExists(ERole eRole) {
        return roleRepository.findByName(eRole).orElseGet(() -> roleRepository.save(new Role(null, eRole)));
    }

    public Role getUserRole() {
        return getRole(ERole.ROLE_USER);
    }
}
