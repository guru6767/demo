package com.starto.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

@Data
class RegisterRequest {
    private String username;
    private String name;
    private String role;
    private String industry;
    private String subIndustry;
    private String city;
    private String state;
    private BigDecimal lat;
    private BigDecimal lng;
    private String bio;
}

@Data
class SignalRequest {
    private String type;
    private String category;
    private String title;
    private String description;
    private String stage;
    private String city;
    private String state;
    private BigDecimal lat;
    private BigDecimal lng;
    private Integer timelineDays;
    private String compensation;
    private String visibility;
}