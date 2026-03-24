package com.example.minimarketplace.repository;

import com.example.minimarketplace.model.Product;
import com.example.minimarketplace.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findBySeller(User seller);
    List<Product> findBySellerId(Long sellerId);
}
