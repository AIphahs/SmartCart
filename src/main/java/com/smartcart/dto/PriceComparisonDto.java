package com.smartcart.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PriceComparisonDto {
    private String storeName;
    private BigDecimal averagePrice;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private long occurrences;
}
