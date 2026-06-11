package com.library.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuditUserSummary {
    private String operatorType;
    private Long operatorId;
    private String operatorName;
    private LocalDateTime lastAuditTime;
    private Long auditCount;
}
