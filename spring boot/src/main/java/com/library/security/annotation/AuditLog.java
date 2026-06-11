package com.library.security.annotation;

import com.library.security.enums.OperationType;
import com.library.security.enums.TargetType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 审计日志注解
 * 用于标记需要记录审计日志的方法
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditLog {
    /**
     * 操作类型（枚举方式，推荐使用）
     */
    OperationType operation() default OperationType.QUERY;
    
    /**
     * 操作类型（字符串方式，保持向后兼容）
     */
    String operationType() default "";
    
    /**
     * 目标类型（枚举方式，推荐使用）
     */
    TargetType target() default TargetType.BOOK;
    
    /**
     * 目标类型（字符串方式，保持向后兼容）
     */
    String targetType() default "";
    
    /**
     * 操作描述
     */
    String description() default "";
}
