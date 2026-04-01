package com.example.minimarketplace.service;

import com.example.minimarketplace.dto.RegisterRequest;
import com.example.minimarketplace.entity.Role;
import com.example.minimarketplace.entity.SellerRequest;
import com.example.minimarketplace.entity.User;
import com.example.minimarketplace.repository.RoleRepository;
import com.example.minimarketplace.repository.SellerRequestRepository;
import com.example.minimarketplace.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static com.example.minimarketplace.entity.SellerRequest.RequestStatus.APPROVED;
import static com.example.minimarketplace.entity.SellerRequest.RequestStatus.PENDING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private SellerRequestRepository sellerRequestRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void registerShouldEncodePasswordAssignBuyerRoleAndSaveUser() {
        RegisterRequest request = new RegisterRequest("buyer1", "buyer1@example.com", "secret123");
        Role buyerRole = new Role(1L, "ROLE_BUYER");

        when(userRepository.existsByUsername("buyer1")).thenReturn(false);
        when(userRepository.existsByEmail("buyer1@example.com")).thenReturn(false);
        when(roleRepository.findByName("ROLE_BUYER")).thenReturn(Optional.of(buyerRole));
        when(passwordEncoder.encode("secret123")).thenReturn("encoded-secret");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User saved = userService.register(request);

        assertThat(saved.getUsername()).isEqualTo("buyer1");
        assertThat(saved.getEmail()).isEqualTo("buyer1@example.com");
        assertThat(saved.getPassword()).isEqualTo("encoded-secret");
        assertThat(saved.isEnabled()).isTrue();
        assertThat(saved.getRoles()).containsExactly(buyerRole);
    }

    @Test
    void registerShouldRejectDuplicateUsername() {
        RegisterRequest request = new RegisterRequest("taken", "taken@example.com", "secret123");

        when(userRepository.existsByUsername("taken")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(request))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("taken");

        verifyNoInteractions(roleRepository, sellerRequestRepository, passwordEncoder);
    }

    @Test
    void requestSellerRoleShouldCreatePendingRequestForBuyer() {
        User buyer = User.builder()
            .username("buyer1")
            .roles(new HashSet<>(Set.of(new Role(1L, "ROLE_BUYER"))))
            .enabled(true)
            .build();

        when(userRepository.findByUsername("buyer1")).thenReturn(Optional.of(buyer));
        when(sellerRequestRepository.existsByUserAndStatus(buyer, PENDING)).thenReturn(false);
        when(sellerRequestRepository.save(any(SellerRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SellerRequest request = userService.requestSellerRole("buyer1", "Please promote me");

        assertThat(request.getUser()).isSameAs(buyer);
        assertThat(request.getStatus()).isEqualTo(PENDING);
        assertThat(request.getNote()).isEqualTo("Please promote me");
    }

    @Test
    void approveSellerRequestShouldMarkRequestApprovedAndGrantSellerRole() {
        User buyer = User.builder()
            .username("buyer1")
            .roles(new HashSet<>(Set.of(new Role(1L, "ROLE_BUYER"))))
            .enabled(true)
            .build();
        SellerRequest request = SellerRequest.builder()
            .id(15L)
            .user(buyer)
            .status(PENDING)
            .build();
        Role sellerRole = new Role(2L, "ROLE_SELLER");

        when(sellerRequestRepository.findById(15L)).thenReturn(Optional.of(request));
        when(roleRepository.findByName("ROLE_SELLER")).thenReturn(Optional.of(sellerRole));

        userService.approveSellerRequest(15L);

        assertThat(request.getStatus()).isEqualTo(APPROVED);
        assertThat(request.getReviewedAt()).isNotNull();
        assertThat(buyer.isSeller()).isTrue();
        verify(userRepository).save(buyer);
        verify(sellerRequestRepository).save(request);
    }
}
