package com.example.minimarketplace.repository;

import com.example.minimarketplace.entity.SellerRequest;
import com.example.minimarketplace.entity.SellerRequest.RequestStatus;
import com.example.minimarketplace.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface SellerRequestRepository extends JpaRepository<SellerRequest, Long> {
    List<SellerRequest>     findByStatus(RequestStatus status);
    Optional<SellerRequest> findByUserAndStatus(User user, RequestStatus status);
    Optional<SellerRequest> findTopByUserOrderByRequestedAtDesc(User user);
    boolean                 existsByUserAndStatus(User user, RequestStatus status);
    List<SellerRequest>     findByUserOrderByRequestedAtDesc(User user);
}
