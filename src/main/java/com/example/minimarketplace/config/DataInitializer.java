package com.example.minimarketplace.config;

import com.example.minimarketplace.entity.Role;
import com.example.minimarketplace.entity.User;
import com.example.minimarketplace.repository.RoleRepository;
import com.example.minimarketplace.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

/**
 * Runs on every boot. Idempotent — safe to re-run.
 *
 * 1. Creates ROLE_ADMIN, ROLE_SELLER, ROLE_BUYER if absent.
 * 2. Creates the hardcoded admin user if absent.
 *
 * Admin credentials (override via ADMIN_USERNAME / ADMIN_PASSWORD env vars):
 *   username: admin
 *   password: admin123
 *
 * IMPORTANT: Change the password via env var before any production deployment.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository  roleRepository;
    private final UserRepository  userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("=== DataInitializer starting ===");

        Role adminRole  = createRoleIfAbsent("ROLE_ADMIN");
        createRoleIfAbsent("ROLE_SELLER");
        createRoleIfAbsent("ROLE_BUYER");

        String adminUsername = System.getenv().getOrDefault("ADMIN_USERNAME", "admin");
        String adminPassword = System.getenv().getOrDefault("ADMIN_PASSWORD", "admin123");

        if (!userRepository.existsByUsername(adminUsername)) {
            var admin = User.builder()
                .username(adminUsername)
                .email("admin@minimarket.com")
                .password(passwordEncoder.encode(adminPassword))
                .enabled(true)
                .roles(new HashSet<>(Set.of(adminRole)))
                .build();
            userRepository.save(admin);
            log.info("Admin user '{}' created.", adminUsername);
        } else {
            log.info("Admin user '{}' already exists — skipped.", adminUsername);
        }

        log.info("=== DataInitializer done ===");
    }

    private Role createRoleIfAbsent(String name) {
        return roleRepository.findByName(name).orElseGet(() -> {
            var r = roleRepository.save(new Role(null, name));
            log.info("Role '{}' created.", name);
            return r;
        });
    }
}
