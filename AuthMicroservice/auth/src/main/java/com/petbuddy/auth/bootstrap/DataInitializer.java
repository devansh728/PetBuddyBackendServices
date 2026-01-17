package com.petbuddy.auth.bootstrap;

import com.petbuddy.auth.model.enums.ERole;
import com.petbuddy.auth.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleService roleService;

    @Override
    public void run(String... args) throws Exception {
        // Ensure roles exist
        for (ERole eRole : ERole.values()) {
            roleService.createIfNotExists(eRole);
        }
    }
}
