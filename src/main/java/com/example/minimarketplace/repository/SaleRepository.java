package com.example.minimarketplace.repository;

import com.example.minimarketplace.entity.Sale;
import com.example.minimarketplace.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SaleRepository extends JpaRepository<Sale, Long> {

    @EntityGraph(attributePaths = {"product", "seller"})
    List<Sale> findTop10ByBuyerOrderBySoldAtDesc(User buyer);

    @EntityGraph(attributePaths = {"product", "seller"})
    List<Sale> findByBuyerOrderBySoldAtDesc(User buyer);

    Optional<Sale> findByIdAndBuyer(Long id, User buyer);

    List<Sale> findTop10BySellerOrderBySoldAtDesc(User seller);
    List<Sale> findBySellerAndSoldAtGreaterThanEqualOrderBySoldAtAsc(User seller, LocalDateTime soldAt);
    long countBySellerAndProductId(User seller, Long productId);

    long countBySeller(User seller);
    long countByBuyer(User buyer);

    @Query("select coalesce(sum(s.totalAmount), 0) from Sale s where s.seller = :seller")
    BigDecimal totalRevenueBySeller(@Param("seller") User seller);

    @Query("select coalesce(sum(s.totalAmount), 0) from Sale s where s.buyer = :buyer")
    BigDecimal totalSpentByBuyer(@Param("buyer") User buyer);

    @Query("select coalesce(sum(s.quantity), 0) from Sale s where s.seller = :seller")
    Long totalUnitsSoldBySeller(@Param("seller") User seller);

    @Query("select coalesce(sum(s.quantity), 0) from Sale s where s.buyer = :buyer")
    Long totalUnitsPurchasedByBuyer(@Param("buyer") User buyer);

    @Query("select count(distinct s.seller.id) from Sale s where s.buyer = :buyer")
    long countDistinctSellersByBuyer(@Param("buyer") User buyer);

    @Query("select count(s) from Sale s where s.buyer = :buyer and s.product.assetFilename is not null")
    long countDownloadableOrdersByBuyer(@Param("buyer") User buyer);

    @Query("""
        select s.product.name as productName, sum(s.quantity) as units
        from Sale s
        where s.seller = :seller
        group by s.product.id, s.product.name
        order by sum(s.quantity) desc
        """)
    List<TopProductProjection> findTopProductsBySeller(@Param("seller") User seller, Pageable pageable);

    interface TopProductProjection {
        String getProductName();
        Long getUnits();
    }
}
