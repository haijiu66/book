package com.library.repository.admin;

import com.library.entity.admin.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findByOperatorTypeAndOperatorId(String operatorType, Long operatorId, Pageable pageable);

    Page<AuditLog> findByOperationType(String operationType, Pageable pageable);

    Page<AuditLog> findByOperationStatus(String operationStatus, Pageable pageable);

    List<AuditLog> findByCreateTimeBefore(LocalDateTime dateTime);

    @Query("SELECT a FROM AuditLog a WHERE a.createTime BETWEEN :startTime AND :endTime")
    Page<AuditLog> findByCreateTimeBetween(@Param("startTime") LocalDateTime startTime,
                                            @Param("endTime") LocalDateTime endTime,
                                            Pageable pageable);

    @Query("SELECT a FROM AuditLog a WHERE " +
           "(:operatorType IS NULL OR a.operatorType = :operatorType) AND " +
           "(:operationType IS NULL OR a.operationType = :operationType) AND " +
           "(:operationStatus IS NULL OR a.operationStatus = :operationStatus)")
    Page<AuditLog> findByConditions(@Param("operatorType") String operatorType,
                                     @Param("operationType") String operationType,
                                     @Param("operationStatus") String operationStatus,
                                     Pageable pageable);

    @Query("SELECT a.operatorType, a.operatorId, a.operatorName, MAX(a.createTime), COUNT(a) " +
           "FROM AuditLog a GROUP BY a.operatorType, a.operatorId, a.operatorName " +
           "ORDER BY MAX(a.createTime) DESC")
    List<Object[]> findOperatorSummaries();

    Page<AuditLog> findByOperatorTypeAndOperatorIdOrderByCreateTimeDesc(
            String operatorType, Long operatorId, Pageable pageable);
}
