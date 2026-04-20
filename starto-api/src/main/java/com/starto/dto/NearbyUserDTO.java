package com.starto.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.util.UUID;

@Data
@AllArgsConstructor
public class NearbyUserDTO {
    private UUID id;
    private String username;
    private BigDecimal lat;
    private BigDecimal lng;
}

