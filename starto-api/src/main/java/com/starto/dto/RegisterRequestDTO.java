package com.starto.dto;

import lombok.Data;

@Data
public class RegisterRequestDTO {
    private String name;
    private String email;
    private String phone;
    private String role;
    private String city;
    private String state;
    private String country;
}