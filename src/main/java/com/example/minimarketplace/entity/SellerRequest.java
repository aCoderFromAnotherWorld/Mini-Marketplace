package com.example.minimarketplace.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "seller_requests")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class SellerRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private RequestStatus status = RequestStatus.PENDING;

    @Column(length = 500)
    private String note;

    @Column(updatable = false)
    @CreationTimestamp
    private LocalDateTime requestedAt;

    private LocalDateTime reviewedAt;

    public enum RequestStatus { PENDING, APPROVED, REJECTED }
}
