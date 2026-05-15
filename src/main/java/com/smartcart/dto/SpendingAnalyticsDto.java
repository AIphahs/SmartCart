package com.smartcart.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class SpendingAnalyticsDto {
    private BigDecimal totalSpending;
    private long receiptCount;
    private BigDecimal averagePerReceipt;
    private LocalDate firstReceiptDate;
    private LocalDate lastReceiptDate;
}
