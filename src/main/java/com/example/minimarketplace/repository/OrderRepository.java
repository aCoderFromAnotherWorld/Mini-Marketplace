package com.example.minimarketplace.repository;

import com.example.minimarketplace.model.Order;
import com.example.minimarketplace.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByBuyer(User buyer);
    List<Order> findByBuyerId(Long buyerId);
    List<Order> findByProductSeller(User seller);
}
