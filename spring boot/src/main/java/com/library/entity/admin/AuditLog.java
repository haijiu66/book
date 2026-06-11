package com.library.entity.admin;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "audit_log", indexes = {
    @Index(name = "idx_audit_log_create_time", columnList = "create_time"),
    @Index(name = "idx_audit_log_operator_type_create_time", columnList = "operator_type, create_time")
})
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "operator_type", nullable = false, length = 20)
    private String operatorType;

    @Column(name = "operator_id")
    private Long operatorId;

    @Column(name = "operator_name", nullable = false, length = 50)
    private String operatorName;

    @Column(name = "operation_type", nullable = false, length = 50)
    private String operationType;

    @Column(name = "operation_detail", nullable = false, columnDefinition = "TEXT")
    private String operationDetail;

    @Column(name = "target_type", length = 20)
    private String targetType;

    @Column(name = "target_id")
    private Long targetId;

    @Column(name = "operation_status", nullable = false, length = 20)
    private String operationStatus;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "request_method", length = 10)
    private String requestMethod;

    @Column(name = "request_uri", length = 500)
    private String requestUri;

    @Column(name = "execution_time")
    private Long executionTime;

    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
    }
}
