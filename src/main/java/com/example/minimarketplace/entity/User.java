package com.example.minimarketplace.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String username;

    @Column(unique = true, nullable = false, length = 100)
    private String email;

    @Column(nullable = false)
    private String password;  // BCrypt hash — never plain text

    @Column(length = 400)
    private String shippingAddress;

    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = true;

    /**
     * EAGER required: Spring Security loads authorities during authentication;
     * the session may be closed by the time roles are accessed later.
     */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_roles",
        joinColumns        = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    @Column(updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    // ── Convenience helpers ──────────────────────────────────────────
    public boolean hasRole(String name) {
        return roles.stream().anyMatch(r -> r.getName().equals(name));
    }
    public boolean isAdmin()  { return hasRole("ROLE_ADMIN");  }
    public boolean isSeller() { return hasRole("ROLE_SELLER"); }
    public boolean isBuyer()  { return hasRole("ROLE_BUYER");  }
}
