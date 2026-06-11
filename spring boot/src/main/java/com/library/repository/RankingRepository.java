package com.library.repository;

import com.library.entity.Ranking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface RankingRepository extends JpaRepository<Ranking, Long> {

    List<Ranking> findByTypeAndPeriodOrderByRankNumAsc(String type, String period);

    @Modifying
    @Transactional
    @Query("DELETE FROM Ranking r WHERE r.type = :type AND r.period = :period")
    void deleteByTypeAndPeriod(@Param("type") String type, @Param("period") String period);
}
