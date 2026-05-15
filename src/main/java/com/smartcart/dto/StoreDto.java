package com.smartcart.dto;

import lombok.Data;

@Data
public class StoreDto {
    private Long id;
    private String name;
    private String address;
    private int receiptCount;
}
