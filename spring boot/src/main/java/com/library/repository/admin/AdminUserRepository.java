package com.library.repository.admin;

import com.library.entity.admin.AdminUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdminUserRepository extends JpaRepository<AdminUser, Long> {
    Optional<AdminUser> findByUsername(String username);
    boolean existsByUsername(String username);
    List<AdminUser> findByCreatedBy(Long createdBy);
    List<AdminUser> findByStatus(String status);
}
