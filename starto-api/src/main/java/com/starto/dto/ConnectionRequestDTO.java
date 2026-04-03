package com.starto.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class ConnectionRequestDTO {
    private UUID signalId;
    private String message;
}