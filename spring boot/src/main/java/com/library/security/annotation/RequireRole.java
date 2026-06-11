package com.library.security.annotation;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 要求指定角色权限
 * 支持多个角色，只要满足其中一个即可访问
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("@authz.hasAnyRole(#roles)")
public @interface RequireRole {
    /**
     * 允许访问的角色列表
     */
    String[] roles();
}
