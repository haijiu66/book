package com.library.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 排行缓存表 — 定时任务定期刷新，避免实时 GROUP BY 全表扫描
 */
@Data
@Entity
@Table(name = "ranking", indexes = {
    @Index(name = "idx_ranking_type_period", columnList = "type, period")
})
public class Ranking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 排行类型: BORROW / READING / FAVORITE_BOOK / FAVORITE_EBOOK */
    @Column(nullable = false, length = 20)
    private String type;

    /** 时间周期: WEEKLY / MONTHLY / YEARLY / ALL */
    @Column(nullable = false, length = 10)
    private String period;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 100)
    private String author;

    @Column(nullable = false)
    private Long count;

    @Column(name = "rank_num")
    private Integer rankNum;

    @Column(name = "update_time")
    private LocalDateTime updateTime;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
    }
}
