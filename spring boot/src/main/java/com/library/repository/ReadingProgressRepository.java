package com.library.repository;

import com.library.entity.ReadingProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;

@Repository
public interface ReadingProgressRepository extends JpaRepository<ReadingProgress, Long> {
    Optional<ReadingProgress> findByUserIdAndUserTypeAndEbookId(Long userId, String userType, Long ebookId);
    List<ReadingProgress> findByUserIdAndUserTypeOrderByLastReadTimeDesc(Long userId, String userType);
    void deleteByUserIdAndUserTypeAndEbookId(Long userId, String userType, Long ebookId);

    @Query("SELECT rp.ebookId, SUM(rp.readCount) FROM ReadingProgress rp GROUP BY rp.ebookId ORDER BY SUM(rp.readCount) DESC")
    List<Object[]> findReadingRanking();
}
