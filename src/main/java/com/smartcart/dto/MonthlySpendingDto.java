package com.smartcart.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class MonthlySpendingDto {
    private int year;
    private int month;
    private String monthLabel;
    private BigDecimal total;
    private long receiptCount;
}
