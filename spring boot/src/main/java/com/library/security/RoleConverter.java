package com.library.security;

import com.library.security.enums.Role;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 角色转换器
 * 用于将Role枚举数组转换为Spring Security需要的字符串数组
 */
@Component("roleConverter")
public class RoleConverter {
    
    /**
     * 将Role枚举数组转换为字符串数组
     */
    public String[] convert(Role[] roles) {
        if (roles == null || roles.length == 0) {
            return new String[0];
        }
        return Arrays.stream(roles)
                .map(Role::getCode)
                .toArray(String[]::new);
    }
    
    /**
     * 将Role枚举列表转换为字符串列表
     */
    public List<String> convertToList(Role[] roles) {
        if (roles == null || roles.length == 0) {
            return List.of();
        }
        return Arrays.stream(roles)
                .map(Role::getCode)
                .collect(Collectors.toList());
    }
}