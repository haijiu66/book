package com.library.dto;

import lombok.Data;

@Data
public class LoginResponse {
    private String token;
    private String username;
    private String role;
    private String name;
    private Long userId;
    private String userType;
    private String permissions;
}
