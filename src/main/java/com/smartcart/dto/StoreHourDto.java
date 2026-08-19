package com.smartcart.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.time.LocalTime;

@Data
public class StoreHourDto {

    @Min(1)
    @Max(7)
    private int dayOfWeek;

    private LocalTime openTime;
    private LocalTime closeTime;
    private boolean closed;
}
