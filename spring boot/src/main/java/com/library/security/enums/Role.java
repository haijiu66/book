package com.library.security.enums;

/**
 * 角色枚举
 * 定义系统中所有可用的角色
 */
public enum Role {
    /**
     * 超级管理员 - 拥有所有权限
     */
    SUPER_ADMIN("SUPER_ADMIN", "超级管理员"),
    
    /**
     * 管理员 - 拥有管理权限
     */
    ADMIN("ADMIN", "管理员"),
    
    /**
     * 读者 - 普通用户
     */
    READER("READER", "读者");
    
    private final String code;
    private final String description;
    
    Role(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * 根据code获取枚举
     */
    public static Role fromCode(String code) {
        for (Role role : values()) {
            if (role.code.equals(code)) {
                return role;
            }
        }
        throw new IllegalArgumentException("未知的角色代码: " + code);
    }
}