package com.example.minimarketplace.repository;

import com.example.minimarketplace.entity.Product;
import com.example.minimarketplace.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
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

    @EntityGraph(attributePaths = "seller")
    List<Product> findTop12ByStockGreaterThanAndSellerNotOrderByCreatedAtDesc(Integer stock, User seller);

    @EntityGraph(attributePaths = "seller")
    List<Product> findTop12ByStockGreaterThanOrderByCreatedAtDesc(Integer stock);

    @EntityGraph(attributePaths = "seller")
    List<Product> findByStockGreaterThanOrderByCreatedAtDesc(Integer stock);

    @EntityGraph(attributePaths = "seller")
    List<Product> findByIdIn(List<Long> ids);

    @EntityGraph(attributePaths = "seller")
    List<Product> findAllByOrderByCreatedAtDesc();

    long countByStockGreaterThan(Integer stock);

    @Query("select count(distinct p.seller.id) from Product p where p.stock > 0")
    long countDistinctSellersWithStock();
}
