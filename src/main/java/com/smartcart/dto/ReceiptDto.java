package com.smartcart.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ReceiptDto {
    private Long id;
    private StoreDto store;
    private LocalDate purchaseDate;
    private BigDecimal totalAmount;
    private String currency;
    private String imagePath;
    private String rawText;
    private BigDecimal itemsTotal;
    private String validationStatus;
    private BigDecimal totalDifference;
    private LocalDateTime createdAt;
    private List<ReceiptItemDto> items;
}
