package com.library.dto;

import lombok.Data;

@Data
public class RegisterNormalUserRequest {
    private String username;
    private String password;
    private String confirmPassword;
    private String name;
    private String phone;
    private String email;
}
