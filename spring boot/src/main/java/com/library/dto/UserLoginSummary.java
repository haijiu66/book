package com.library.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserLoginSummary {
    private Long userId;
    private String username;
    private String userType;
    private String name;
    private LocalDateTime lastLoginTime;
    private String lastLoginIp;
    private String lastLoginStatus;
    private String status;
}
