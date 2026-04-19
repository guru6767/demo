package com.starto.dto;

import lombok.Data;

@Data
public class ExploreRequest {
    private String location;
    private String industry;
    private Long budget;
    private String stage;
    private String targetCustomer;
}