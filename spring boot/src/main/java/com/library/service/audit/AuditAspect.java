package com.library.service.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.entity.admin.AuditLog;
import com.library.repository.admin.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;

/**
 * 审计切面
 * 用于记录用户操作日志，包括操作人、操作类型、操作详情、IP地址等信息
 */
@Slf4j
@Aspect
@Component
public class AuditAspect {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 审计方法执行
     * @param joinPoint 连接点
     * @param auditLogAnnotation 审计注解
     * @return 方法执行结果
     * @throws Throwable 异常
     */
    @Around("@annotation(auditLogAnnotation)")
    public Object audit(ProceedingJoinPoint joinPoint, com.library.security.annotation.AuditLog auditLogAnnotation) throws Throwable {
        Instant startTime = Instant.now();
        Object result = null;
        Throwable throwable = null;

        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable t) {
            throwable = t;
            throw t;
        } finally {
            long executionTime = Duration.between(startTime, Instant.now()).toMillis();
            saveAuditLog(joinPoint, auditLogAnnotation, executionTime, throwable, result);
        }
    }

    /**
     * 获取操作类型代码
     * 优先使用枚举方式，如果枚举为默认值则使用字符串方式
     */
    private String getOperationTypeCode(com.library.security.annotation.AuditLog auditLogAnnotation) {
        if (auditLogAnnotation.operationType() != null && !auditLogAnnotation.operationType().isEmpty()) {
            return auditLogAnnotation.operationType();
        }
        return auditLogAnnotation.operation().getCode();
    }

    /**
     * 获取目标类型代码
     * 优先使用枚举方式，如果枚举为默认值则使用字符串方式
     */
    private String getTargetTypeCode(com.library.security.annotation.AuditLog auditLogAnnotation) {
        if (auditLogAnnotation.targetType() != null && !auditLogAnnotation.targetType().isEmpty()) {
            return auditLogAnnotation.targetType();
        }
        return auditLogAnnotation.target().getCode();
    }

    /**
     * 保存审计日志
     * @param joinPoint 连接点
     * @param auditLogAnnotation 审计注解
     * @param executionTime 执行时间
     * @param throwable 异常（如果有）
     * @param result 执行结果
     */
    private void saveAuditLog(ProceedingJoinPoint joinPoint, com.library.security.annotation.AuditLog auditLogAnnotation, 
                             long executionTime, Throwable throwable, Object result) {
        AuditLog auditLog = null;
        try {
            auditLog = new AuditLog();

            // 设置操作人信息
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                Object principal = authentication.getPrincipal();
                if (principal instanceof com.library.entity.admin.SuperAdmin) {
                    com.library.entity.admin.SuperAdmin admin = (com.library.entity.admin.SuperAdmin) principal;
                    auditLog.setOperatorType("SUPER_ADMIN");
                    auditLog.setOperatorId(admin.getId());
                    auditLog.setOperatorName(admin.getName());
                } else if (principal instanceof com.library.entity.admin.AdminUser) {
                    com.library.entity.admin.AdminUser admin = (com.library.entity.admin.AdminUser) principal;
                    auditLog.setOperatorType("ADMIN");
                    auditLog.setOperatorId(admin.getId());
                    auditLog.setOperatorName(admin.getName());
                } else if (principal instanceof com.library.entity.user.NormalUser) {
                    com.library.entity.user.NormalUser user = (com.library.entity.user.NormalUser) principal;
                    auditLog.setOperatorType("READER");
                    auditLog.setOperatorId(user.getId());
                    auditLog.setOperatorName(user.getName());
                } else if (principal instanceof com.library.entity.User) {
                    com.library.entity.User user = (com.library.entity.User) principal;
                    auditLog.setOperatorType("USER");
                    auditLog.setOperatorId(user.getId());
                    auditLog.setOperatorName(user.getUsername());
                }
            }

            // 设置操作类型和目标类型
            auditLog.setOperationType(getOperationTypeCode(auditLogAnnotation));
            auditLog.setTargetType(getTargetTypeCode(auditLogAnnotation));

            // 提取目标ID
            Long targetId = extractTargetId(joinPoint, result, getTargetTypeCode(auditLogAnnotation));
            auditLog.setTargetId(targetId);

            // 设置操作详情
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            String operationDetail = buildOperationDetail(joinPoint, signature, auditLogAnnotation.description(), result);
            auditLog.setOperationDetail(operationDetail);

            // 设置操作状态
            auditLog.setOperationStatus(throwable == null ? "SUCCESS" : "FAILURE");
            if (throwable != null) {
                auditLog.setErrorMessage(throwable.getMessage());
            }

            // 设置请求信息
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                auditLog.setIpAddress(getClientIpAddress(request));
                auditLog.setUserAgent(request.getHeader("User-Agent"));
                auditLog.setRequestMethod(request.getMethod());
                auditLog.setRequestUri(request.getRequestURI());
            }

            auditLog.setExecutionTime(executionTime);
            auditLogRepository.save(auditLog);

        } catch (Exception e) {
            log.error("保存审计日志失败，尝试写入本地回退文件", e);
            writeFallbackLog(auditLog, e);
        }
    }

    /**
     * 当数据库写入失败时，将审计日志以 JSON 格式写入本地文件作为回退。
     */
    private void writeFallbackLog(AuditLog auditLog, Exception dbException) {
        try {
            java.nio.file.Path dir = java.nio.file.Paths.get("logs/audit_fallback");
            java.nio.file.Files.createDirectories(dir);
            java.nio.file.Path file = dir.resolve("audit_" + System.currentTimeMillis() + ".json");
            String json = objectMapper.writeValueAsString(auditLog);
            java.nio.file.Files.writeString(file, json);
            log.info("审计日志已写入本地回退文件: {}", file);
        } catch (Exception ioException) {
            log.error("审计日志本地回退写入也失败", ioException);
        }
    }

    /**
     * 提取目标ID
     * @param joinPoint 连接点
     * @param result 执行结果
     * @param targetType 目标类型
     * @return 目标ID
     */
    private Long extractTargetId(ProceedingJoinPoint joinPoint, Object result, String targetType) {
        try {
            // 首先尝试从方法参数中提取ID
            Object[] args = joinPoint.getArgs();
            for (Object arg : args) {
                if (arg instanceof Long) {
                    return (Long) arg;
                }
                if (arg instanceof Integer) {
                    return ((Integer) arg).longValue();
                }
            }
            
            // 然后尝试从返回结果中提取ID
            if (result != null) {
                try {
                    Method getIdMethod = result.getClass().getMethod("getId");
                    Object idObj = getIdMethod.invoke(result);
                    if (idObj instanceof Long) {
                        return (Long) idObj;
                    }
                    if (idObj instanceof Integer) {
                        return ((Integer) idObj).longValue();
                    }
                } catch (Exception e) {
                    // 忽略异常
                }
            }
        } catch (Exception e) {
            log.debug("提取目标ID失败", e);
        }
        return null;
    }

    /**
     * 构建操作详情
     * @param joinPoint 连接点
     * @param signature 方法签名
     * @param description 描述
     * @param result 执行结果
     * @return 操作详情
     */
    private String buildOperationDetail(ProceedingJoinPoint joinPoint, MethodSignature signature, String description, Object result) {
        StringBuilder detail = new StringBuilder();
        
        // 添加描述
        if (description != null && !description.isEmpty()) {
            detail.append(description).append(" | ");
        }
        
        // 添加方法信息
        detail.append("方法: ").append(signature.getDeclaringType().getSimpleName())
              .append(".").append(signature.getName()).append(" | ");
        
        // 添加参数信息
        String[] paramNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();
        if (paramNames != null && paramNames.length > 0) {
            detail.append("参数: ");
            for (int i = 0; i < paramNames.length; i++) {
                if (i > 0) {
                    detail.append(", ");
                }
                detail.append(paramNames[i]).append("=");
                try {
                    if (args[i] != null) {
                        if (args[i] instanceof String || args[i] instanceof Number || args[i] instanceof Boolean) {
                            detail.append(args[i]);
                        } else {
                            detail.append(objectMapper.writeValueAsString(args[i]));
                        }
                    } else {
                        detail.append("null");
                    }
                } catch (Exception e) {
                    detail.append("[无法序列化]");
                }
            }
        }
        
        return detail.toString();
    }

    /**
     * 获取客户端IP地址
     * @param request HTTP请求
     * @return IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String[] headers = {
                "X-Forwarded-For",
                "X-Real-IP",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_X_FORWARDED_FOR",
                "HTTP_X_FORWARDED",
                "HTTP_X_CLUSTER_CLIENT_IP",
                "HTTP_CLIENT_IP",
                "HTTP_FORWARDED_FOR",
                "HTTP_FORWARDED"
        };

        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                int index = ip.indexOf(',');
                if (index != -1) {
                    return ip.substring(0, index).trim();
                }
                return ip;
            }
        }
        return request.getRemoteAddr();
    }
}
