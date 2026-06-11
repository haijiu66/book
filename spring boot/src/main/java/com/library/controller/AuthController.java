package com.library.controller;

import com.library.dto.ApiResponse;
import com.library.dto.LoginRequest;
import com.library.dto.LoginResponse;
import com.library.dto.RegisterNormalUserRequest;
import com.library.entity.User;
import com.library.entity.user.NormalUser;
import com.library.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@RequestBody LoginRequest loginRequest, HttpServletRequest request) {
        LoginResponse response = authService.login(loginRequest, request);
        return ApiResponse.success("登录成功", response);
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestHeader(value = "Authorization", required = false) String token,
                                      @RequestParam(required = false) String username) {
        authService.logout(token, username);
        return ApiResponse.success("退出成功", null);
    }

    @PostMapping("/register")
    public ApiResponse<User> register(@RequestBody User user) {
        User registeredUser = authService.register(user);
        return ApiResponse.success("注册成功", registeredUser);
    }

    @PostMapping("/register-normal-user")
    public ApiResponse<?> registerNormalUser(@RequestBody RegisterNormalUserRequest request) {
        if (request.getPassword() == null || !request.getPassword().equals(request.getConfirmPassword())) {
            return ApiResponse.error(400, "两次输入的密码不一致");
        }
        if (request.getUsername() == null || request.getUsername().isBlank()) {
            return ApiResponse.error(400, "用户名不能为空");
        }
        if (request.getPassword() == null || request.getPassword().length() < 6) {
            return ApiResponse.error(400, "密码长度不能少于6位");
        }
        if (authService.existsNormalUserByUsername(request.getUsername())) {
            return ApiResponse.conflict("用户名已存在");
        }
        NormalUser normalUser = authService.registerNormalUser(request);
        return ApiResponse.success("注册成功", normalUser);
    }
}
