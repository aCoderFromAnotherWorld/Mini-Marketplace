package com.example.minimarketplace.service;

import com.example.minimarketplace.entity.Role;
import com.example.minimarketplace.entity.User;
import com.example.minimarketplace.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void loadUserByUsernameShouldMapRolesToGrantedAuthorities() {
        User user = User.builder()
            .username("alice")
            .password("hashed-password")
            .enabled(true)
            .roles(new HashSet<>(Set.of(
                new Role(1L, "ROLE_BUYER"),
                new Role(2L, "ROLE_SELLER")
            )))
            .build();

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        var details = customUserDetailsService.loadUserByUsername("alice");

        assertThat(details.getUsername()).isEqualTo("alice");
        assertThat(details.getPassword()).isEqualTo("hashed-password");
        assertThat(details.isEnabled()).isTrue();
        assertThat(details.getAuthorities())
            .extracting("authority")
            .containsExactlyInAnyOrder("ROLE_BUYER", "ROLE_SELLER");
    }

    @Test
    void loadUserByUsernameShouldThrowWhenUserDoesNotExist() {
        when(userRepository.findByUsername("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername("missing"))
            .isInstanceOf(UsernameNotFoundException.class)
            .hasMessage("No account: missing");
    }
}
