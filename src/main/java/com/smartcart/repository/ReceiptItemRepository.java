package com.smartcart.repository;

import com.smartcart.model.ReceiptItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ReceiptItemRepository extends JpaRepository<ReceiptItem, Long> {

    @Query("SELECT ri.category, SUM(ri.totalPrice), COUNT(ri) " +
           "FROM ReceiptItem ri JOIN ri.receipt r " +
           "WHERE (:from IS NULL OR r.purchaseDate >= :from) " +
           "AND (:to IS NULL OR r.purchaseDate <= :to) " +
           "AND ri.category IS NOT NULL " +
           "GROUP BY ri.category ORDER BY SUM(ri.totalPrice) DESC")
    List<Object[]> findCategorySpending(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT r.store.name, AVG(ri.totalPrice), MIN(ri.totalPrice), MAX(ri.totalPrice), COUNT(ri) " +
           "FROM ReceiptItem ri JOIN ri.receipt r " +
           "WHERE LOWER(ri.productName) LIKE LOWER(CONCAT('%', :product, '%')) " +
           "AND r.store IS NOT NULL " +
           "GROUP BY r.store.name ORDER BY AVG(ri.totalPrice)")
    List<Object[]> findPriceComparisonByProduct(@Param("product") String product);
}
