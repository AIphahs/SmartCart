package com.smartcart.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CategorySpendingDto {
    private String category;
    private BigDecimal total;
    private double percentage;
    private long itemCount;
}
