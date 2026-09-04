package com.inventory.repository;

import com.inventory.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    boolean existsByProductSkuAndPurchaseDate(String productSku, LocalDate purchaseDate);

    @Query("SELECT CONCAT(p.productSku, '|', p.purchaseDate) FROM Product p " +
            "WHERE CONCAT(p.productSku, '|', p.purchaseDate) IN :keys")
    Set<String> findExistingSkuDateKeys(@Param("keys") Collection<String> keys);

    @Query("SELECT COALESCE(SUM(p.unitPrice * p.quantity), 0) FROM Product p")
    java.math.BigDecimal calculateTotalInventoryValue();

    @Query("SELECT p.purchaseDate FROM Product p")
    List<LocalDate> findAllPurchaseDates();
}
