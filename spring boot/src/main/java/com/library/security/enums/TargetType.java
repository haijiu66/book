package com.library.security.enums;

/**
 * 目标类型枚举
 * 定义审计日志中的操作目标类型
 */
public enum TargetType {
    ADMIN("ADMIN", "管理员"),
    USER("USER", "用户"),
    BOOK("BOOK", "图书"),
    BORROW("BORROW", "借阅记录"),
    EBOOK("EBOOK", "电子书"),
    CHAPTER("CHAPTER", "电子书章节"),
    READING_PROGRESS("READING_PROGRESS", "阅读进度"),
    AUDIT_LOG("AUDIT_LOG", "审计日志");
    
    private final String code;
    private final String description;
    
    TargetType(String code, String description) {
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
    public static TargetType fromCode(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        for (TargetType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的目标类型代码: " + code);
    }
}