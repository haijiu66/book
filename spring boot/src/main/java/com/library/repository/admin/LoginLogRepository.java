package com.library.repository.admin;

import com.library.entity.admin.LoginLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface LoginLogRepository extends JpaRepository<LoginLog, Long> {
    List<LoginLog> findByUsername(String username);
    List<LoginLog> findByUserType(String userType);
    Page<LoginLog> findByUsername(String username, Pageable pageable);
    List<LoginLog> findByCreateTimeBetween(LocalDateTime startTime, LocalDateTime endTime);
    void deleteByCreateTimeBefore(LocalDateTime dateTime);

    Page<LoginLog> findByUserIdAndUserType(Long userId, String userType, Pageable pageable);
    Optional<LoginLog> findTopByUserIdAndUserTypeOrderByCreateTimeDesc(Long userId, String userType);
}
