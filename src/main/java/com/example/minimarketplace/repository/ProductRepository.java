package com.example.minimarketplace.repository;

import com.example.minimarketplace.entity.Product;
import com.example.minimarketplace.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findAllByOrderByCreatedAtDesc();
    List<Product> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCaseOrderByCreatedAtDesc(
        String nameQuery,
        String descriptionQuery
    );
    List<Product> findBySellerOrderByCreatedAtDesc(User seller);
    Optional<Product> findByIdAndSeller(Long id, User seller);
    long countBySeller(User seller);
}
