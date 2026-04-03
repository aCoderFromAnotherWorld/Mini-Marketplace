package com.example.minimarketplace.service;

import com.example.minimarketplace.dto.RegisterRequest;
import com.example.minimarketplace.entity.SellerRequest;
import com.example.minimarketplace.entity.SellerRequest.RequestStatus;
import com.example.minimarketplace.entity.User;
import com.example.minimarketplace.repository.RoleRepository;
import com.example.minimarketplace.repository.SellerRequestRepository;
import com.example.minimarketplace.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository          userRepository;
    private final RoleRepository          roleRepository;
    private final SellerRequestRepository sellerRequestRepository;
    private final PasswordEncoder         passwordEncoder;

    // ── Registration ─────────────────────────────────────────────────────

    @Transactional
    public User register(RegisterRequest req) {
        if (userRepository.existsByUsername(req.getUsername()))
            throw new RuntimeException("Username '" + req.getUsername() + "' is already taken.");
        if (userRepository.existsByEmail(req.getEmail()))
            throw new RuntimeException("An account with that email already exists.");

        var buyerRole = roleRepository.findByName("ROLE_BUYER")
            .orElseThrow(() -> new IllegalStateException("ROLE_BUYER missing — check DataInitializer."));

        var user = User.builder()
            .username(req.getUsername())
            .email(req.getEmail())
            .password(passwordEncoder.encode(req.getPassword()))
            .enabled(true)
            .roles(new HashSet<>(Set.of(buyerRole)))
            .build();

        log.info("Registered new user: {}", req.getUsername());
        return userRepository.save(user);
    }

    // ── Seller request flow ───────────────────────────────────────────────

    @Transactional
    public SellerRequest requestSellerRole(String username, String note) {
        var user = findByUsername(username);
        if (user.isSeller())
            throw new RuntimeException("You are already a seller.");
        if (sellerRequestRepository.existsByUserAndStatus(user, RequestStatus.PENDING))
            throw new RuntimeException("You already have a pending request.");

        var req = SellerRequest.builder().user(user).note(note).build();
        log.info("Seller request submitted by: {}", username);
        return sellerRequestRepository.save(req);
    }

    @Transactional
    public void approveSellerRequest(Long requestId) {
        var req = sellerRequestRepository.findById(requestId)
            .orElseThrow(() -> new RuntimeException("Request not found: " + requestId));
        var sellerRole = roleRepository.findByName("ROLE_SELLER")
            .orElseThrow(() -> new IllegalStateException("ROLE_SELLER missing."));

        req.setStatus(RequestStatus.APPROVED);
        req.setReviewedAt(LocalDateTime.now());
        req.getUser().getRoles().add(sellerRole);

        userRepository.save(req.getUser());
        sellerRequestRepository.save(req);
        log.info("Approved seller request {} for user {}", requestId, req.getUser().getUsername());
    }

    @Transactional
    public void rejectSellerRequest(Long requestId) {
        var req = sellerRequestRepository.findById(requestId)
            .orElseThrow(() -> new RuntimeException("Request not found: " + requestId));
        req.setStatus(RequestStatus.REJECTED);
        req.setReviewedAt(LocalDateTime.now());
        sellerRequestRepository.save(req);
        log.info("Rejected seller request {}", requestId);
    }

    @Transactional(readOnly = true)
    public List<SellerRequest> getPendingRequests() {
        return sellerRequestRepository.findByStatus(RequestStatus.PENDING);
    }

    @Transactional(readOnly = true)
    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found: " + username));
    }

    @Transactional
    public User updateShippingAddress(String username, String shippingAddress) {
        User user = findByUsername(username);
        String normalized = shippingAddress == null ? "" : shippingAddress.trim();
        if (normalized.length() > 400) {
            throw new RuntimeException("Shipping address must be 400 characters or fewer.");
        }
        user.setShippingAddress(normalized.isBlank() ? null : normalized);
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public List<User> findAll() {
        return userRepository.findAll();
    }
}
