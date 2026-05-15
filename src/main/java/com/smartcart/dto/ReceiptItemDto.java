package com.smartcart.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ReceiptItemDto {
    private Long id;
    private String productName;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private String category;
}
