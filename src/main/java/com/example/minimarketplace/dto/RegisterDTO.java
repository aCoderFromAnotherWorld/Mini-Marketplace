package com.example.minimarketplace.dto;

import lombok.Data;

@Data
public class RegisterDTO {
    private String username;
    private String email;
    private String password;
    private String role; // SELLER or BUYER
}
