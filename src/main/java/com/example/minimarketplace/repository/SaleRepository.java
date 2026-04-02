package com.example.minimarketplace.repository;

import com.example.minimarketplace.entity.Sale;
import com.example.minimarketplace.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SaleRepository extends JpaRepository<Sale, Long> {

    List<Sale> findTop10BySellerOrderBySoldAtDesc(User seller);
    List<Sale> findBySellerAndSoldAtGreaterThanEqualOrderBySoldAtAsc(User seller, LocalDateTime soldAt);
    long countBySellerAndProductId(User seller, Long productId);

    long countBySeller(User seller);

    @Query("select coalesce(sum(s.totalAmount), 0) from Sale s where s.seller = :seller")
    BigDecimal totalRevenueBySeller(@Param("seller") User seller);

    @Query("select coalesce(sum(s.quantity), 0) from Sale s where s.seller = :seller")
    Long totalUnitsSoldBySeller(@Param("seller") User seller);

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
