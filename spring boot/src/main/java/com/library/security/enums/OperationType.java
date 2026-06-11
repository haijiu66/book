package com.library.security.enums;

/**
 * 操作类型枚举
 * 定义审计日志中的所有操作类型
 */
public enum OperationType {
    // 用户相关操作
    LOGIN("LOGIN", "登录"),
    LOGOUT("LOGOUT", "登出"),
    REGISTER("REGISTER", "注册"),
    
    // 管理员相关操作
    CREATE_ADMIN("CREATE_ADMIN", "创建管理员"),
    UPDATE_ADMIN("UPDATE_ADMIN", "更新管理员"),
    DELETE_ADMIN("DELETE_ADMIN", "删除管理员"),
    
    // 用户管理相关操作
    CREATE_USER("CREATE_USER", "创建用户"),
    UPDATE_USER("UPDATE_USER", "更新用户"),
    DELETE_USER("DELETE_USER", "删除用户"),
    
    // 图书相关操作
    CREATE_BOOK("CREATE_BOOK", "添加图书"),
    UPDATE_BOOK("UPDATE_BOOK", "更新图书"),
    DELETE_BOOK("DELETE_BOOK", "删除图书"),
    IMPORT_BOOKS("IMPORT_BOOKS", "导入图书"),
    EXPORT_BOOKS("EXPORT_BOOKS", "导出图书"),
    
    // 借阅相关操作
    BORROW_BOOK("BORROW_BOOK", "借书"),
    RETURN_BOOK("RETURN_BOOK", "还书"),
    UPDATE_OVERDUE("UPDATE_OVERDUE", "更新逾期状态"),
    DELETE_BORROW("DELETE_BORROW", "删除借阅记录"),
    
    // 电子书相关操作
    UPLOAD_EBOOK("UPLOAD", "上传电子书"),
    DELETE_EBOOK("DELETE", "删除电子书"),
    READ_EBOOK("READ", "阅读电子书"),
    SAVE_PROGRESS("SAVE", "保存阅读进度"),
    
    // 查询操作
    QUERY("QUERY", "查询");
    
    private final String code;
    private final String description;
    
    OperationType(String code, String description) {
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
    public static OperationType fromCode(String code) {
        for (OperationType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的操作类型代码: " + code);
    }
}