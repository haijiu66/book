package com.library.controller;

import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/simple-auth")
@CrossOrigin(origins = "*")
public class SimpleAuthController {

    @PostMapping("/login")
    public Map<String, Object> simpleLogin(@RequestBody Map<String, String> request) {
        System.out.println("收到登录请求: " + request);
        
        String username = request.get("username");
        String password = request.get("password");
        String role = request.get("role");
        
        Map<String, Object> result = new HashMap<>();
        
        if ("123456".equals(password)) {
            result.put("code", 200);
            result.put("message", "登录成功");
            
            Map<String, Object> data = new HashMap<>();
            data.put("token", "test-token-" + System.currentTimeMillis());
            data.put("username", username);
            data.put("name", getNameByUsername(username));
            data.put("role", role != null ? role : "READER");
            data.put("userId", 1L);
            data.put("userType", role != null ? role : "READER");
            
            result.put("data", data);
        } else {
            result.put("code", 401);
            result.put("message", "密码错误");
        }
        
        System.out.println("返回: " + result);
        return result;
    }
    
    private String getNameByUsername(String username) {
        if ("superadmin".equals(username)) return "系统超级管理员";
        if ("admin".equals(username)) return "系统管理员";
        if ("reader".equals(username)) return "普通读者";
        return username;
    }
}
