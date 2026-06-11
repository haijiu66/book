package com.library.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component("authz")
public class AuthzChecker {

    /**
     * 检查当前用户是否拥有指定角色中的任意一个。
     * 接收 String[] 而非 varargs，避免 SpEL 中 String[] → String... 的展开问题。
     *
     * @param roleCodes 角色编码数组，如 {"SUPER_ADMIN", "READER"}
     */
    public boolean hasAnyRole(String[] roleCodes) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        for (String code : roleCodes) {
            String authority = code.startsWith("ROLE_") ? code : "ROLE_" + code;
            for (GrantedAuthority ga : authentication.getAuthorities()) {
                if (ga.getAuthority().equals(authority)) {
                    return true;
                }
            }
        }
        return false;
    }
}
