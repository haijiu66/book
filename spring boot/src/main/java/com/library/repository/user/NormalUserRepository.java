package com.library.repository.user;

import com.library.entity.user.NormalUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NormalUserRepository extends JpaRepository<NormalUser, Long> {
    Optional<NormalUser> findByUsername(String username);
    boolean existsByUsername(String username);
    List<NormalUser> findByCreatedBy(Long createdBy);
    List<NormalUser> findByStatus(String status);
}
