package com.library.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordUtil {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String password = "123456";
        String hashedPassword = encoder.encode(password);
        System.out.println("原始密码: " + password);
        System.out.println("BCrypt哈希值: " + hashedPassword);
    }
}
