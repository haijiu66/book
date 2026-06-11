package com.library.service;

import com.library.dto.LoginRequest;
import com.library.dto.LoginResponse;
import com.library.dto.RegisterNormalUserRequest;
import com.library.entity.User;
import com.library.entity.admin.AdminUser;
import com.library.entity.admin.SuperAdmin;
import com.library.entity.user.NormalUser;
import com.library.repository.admin.AdminUserRepository;
import com.library.repository.admin.SuperAdminRepository;
import com.library.repository.user.NormalUserRepository;
import com.library.security.JwtUtil;
import com.library.security.LoginRoleContext;
import com.library.service.admin.AdminUserService;
import com.library.service.admin.SuperAdminService;
import com.library.service.isolation.TransactionalIsolationService;
import com.library.service.user.NormalUserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private LoginLogService loginLogService;

    @Autowired
    private SuperAdminRepository superAdminRepository;

    @Autowired
    private AdminUserRepository adminUserRepository;

    @Autowired
    private NormalUserRepository normalUserRepository;

    @Autowired
    private TransactionalIsolationService transactionalIsolationService;

    @Autowired
    private SuperAdminService superAdminService;

    @Autowired
    private AdminUserService adminUserService;

    @Autowired
    private NormalUserService normalUserService;

    public LoginResponse login(LoginRequest loginRequest, HttpServletRequest request) {
        String username = loginRequest.getUsername();
        String selectedRole = loginRequest.getRole();
        String ipAddress = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");

        try {
            // 将选定角色传递给 UserDetailsService，使同名账号可按角色查不同表
            LoginRoleContext.set(selectedRole);
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, loginRequest.getPassword())
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();

            String token = jwtUtil.generateToken(userDetails);

            String userType = determineUserType(userDetails);
            if (selectedRole != null && !selectedRole.isBlank() && !selectedRole.equals(userType)) {
                throw new BadCredentialsException("所选角色与账号实际角色不匹配，该账号在所选角色表中不存在");
            }
            String name = extractUserName(userDetails);
            Long userId = extractUserId(userDetails);

            loginLogService.recordLoginSuccess(userId, username, userType, ipAddress, userAgent);

            updateUserLoginInfo(userDetails, ipAddress);

            LoginResponse response = new LoginResponse();
            response.setToken(token);
            response.setUsername(username);
            response.setName(name);
            response.setRole(userType);
            response.setUserId(userId);
            response.setUserType(userType);
            response.setPermissions(extractPermissions(userDetails));

            return response;

        } catch (BadCredentialsException e) {
            String reason = (e.getMessage() == null || e.getMessage().isBlank()) ? "用户名或密码错误" : e.getMessage();
            loginLogService.recordLoginFail(null, username, null, ipAddress, userAgent, reason);
            throw e;
        } catch (LockedException e) {
            loginLogService.recordLoginFail(null, username, null, ipAddress, userAgent, "账号已锁定");
            throw e;
        } catch (DisabledException e) {
            loginLogService.recordLoginFail(null, username, null, ipAddress, userAgent, "账号已禁用");
            throw e;
        } catch (Exception e) {
            loginLogService.recordLoginFail(null, username, null, ipAddress, userAgent, e.getMessage());
            throw e;
        } finally {
            LoginRoleContext.clear();
        }
    }

    public void logout(String token, String username) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        jwtUtil.addToBlacklist(token, username);
    }

    public User register(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userService.save(user);
    }

    public NormalUser registerNormalUser(RegisterNormalUserRequest request) {
        NormalUser normalUser = new NormalUser();
        normalUser.setUsername(request.getUsername());
        normalUser.setPassword(request.getPassword());
        normalUser.setName(request.getName());
        normalUser.setPhone(request.getPhone());
        normalUser.setEmail(request.getEmail());

        return transactionalIsolationService.createUserWithTables(normalUser, null, "SELF");
    }

    public boolean existsNormalUserByUsername(String username) {
        return superAdminRepository.existsByUsername(username)
                || adminUserRepository.existsByUsername(username)
                || normalUserRepository.existsByUsername(username);
    }

    private String determineUserType(UserDetails userDetails) {
        if (userDetails instanceof SuperAdmin) {
            return "SUPER_ADMIN";
        } else if (userDetails instanceof AdminUser) {
            return "ADMIN";
        } else if (userDetails instanceof NormalUser) {
            return "READER";
        }
        return "READER";
    }

    private String extractUserName(UserDetails userDetails) {
        if (userDetails instanceof SuperAdmin) {
            return ((SuperAdmin) userDetails).getName();
        } else if (userDetails instanceof AdminUser) {
            return ((AdminUser) userDetails).getName();
        } else if (userDetails instanceof NormalUser) {
            return ((NormalUser) userDetails).getName();
        }
        return userDetails.getUsername();
    }

    private Long extractUserId(UserDetails userDetails) {
        if (userDetails instanceof SuperAdmin) {
            return ((SuperAdmin) userDetails).getId();
        } else if (userDetails instanceof AdminUser) {
            return ((AdminUser) userDetails).getId();
        } else if (userDetails instanceof NormalUser) {
            return ((NormalUser) userDetails).getId();
        }
        return null;
    }

    private String extractPermissions(UserDetails userDetails) {
        if (userDetails instanceof AdminUser) {
            return ((AdminUser) userDetails).getPermissions();
        }
        return null;
    }

    private void updateUserLoginInfo(UserDetails userDetails, String ipAddress) {
        Long userId = extractUserId(userDetails);
        if (userId == null) {
            return;
        }
        if (userDetails instanceof SuperAdmin) {
            superAdminService.updateLastLoginInfo(userId, ipAddress);
        } else if (userDetails instanceof AdminUser) {
            adminUserService.updateLastLoginInfo(userId, ipAddress);
        } else if (userDetails instanceof NormalUser) {
            normalUserService.updateLastLoginInfo(userId, ipAddress);
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        }
        if (ipAddress != null && ipAddress.contains(",")) {
            ipAddress = ipAddress.split(",")[0].trim();
        }
        return ipAddress;
    }
}
