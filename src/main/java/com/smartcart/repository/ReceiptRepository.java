package com.smartcart.repository;

import com.smartcart.model.Receipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReceiptRepository extends JpaRepository<Receipt, Long> {

    List<Receipt> findByStoreId(Long storeId);

    List<Receipt> findByPurchaseDateBetween(LocalDate start, LocalDate end);

    @Query("SELECT DISTINCT r FROM Receipt r LEFT JOIN FETCH r.store LEFT JOIN FETCH r.items WHERE r.id = :id")
    Optional<Receipt> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT EXTRACT(YEAR FROM r.purchaseDate) as yr, EXTRACT(MONTH FROM r.purchaseDate) as mo, " +
           "SUM(r.totalAmount) as total, COUNT(r) as cnt " +
           "FROM Receipt r WHERE r.purchaseDate >= :from " +
           "GROUP BY EXTRACT(YEAR FROM r.purchaseDate), EXTRACT(MONTH FROM r.purchaseDate) " +
           "ORDER BY yr DESC, mo DESC")
    List<Object[]> findMonthlySpending(@Param("from") LocalDate from);

    @Query("SELECT SUM(r.totalAmount), COUNT(r), MIN(r.purchaseDate), MAX(r.purchaseDate) " +
           "FROM Receipt r " +
           "WHERE (:from IS NULL OR r.purchaseDate >= :from) AND (:to IS NULL OR r.purchaseDate <= :to)")
    Object[] findSpendingStats(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
