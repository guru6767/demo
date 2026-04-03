package com.starto.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class OfferRequestDTO {
    private UUID signalId;
    private String organizationName;
    private String portfolioLink;
    private String message;
}