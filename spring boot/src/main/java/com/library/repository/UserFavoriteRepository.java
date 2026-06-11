package com.library.repository;

import com.library.entity.UserFavorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserFavoriteRepository extends JpaRepository<UserFavorite, Long> {

    Optional<UserFavorite> findByUserIdAndUserTypeAndTargetTypeAndTargetId(
            Long userId, String userType, String targetType, Long targetId);

    List<UserFavorite> findByUserIdAndUserTypeAndTargetType(Long userId, String userType, String targetType);

    long countByTargetTypeAndTargetId(String targetType, Long targetId);

    boolean existsByUserIdAndUserTypeAndTargetTypeAndTargetId(
            Long userId, String userType, String targetType, Long targetId);

    void deleteByUserIdAndUserTypeAndTargetTypeAndTargetId(
            Long userId, String userType, String targetType, Long targetId);

    @Query("SELECT f.targetId, COUNT(f) FROM UserFavorite f WHERE f.targetType = :targetType " +
           "GROUP BY f.targetId ORDER BY COUNT(f) DESC")
    List<Object[]> findFavoriteRanking(@Param("targetType") String targetType);
}
