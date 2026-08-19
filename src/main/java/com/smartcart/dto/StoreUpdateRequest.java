package com.smartcart.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class StoreUpdateRequest {

    @Size(max = 500)
    private String address;

    @Size(max = 50)
    private String phone;

    @Size(max = 255)
    private String website;

    @Valid
    private List<StoreHourDto> hours;
}
