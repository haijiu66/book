package com.library.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "reading_progress", uniqueConstraints = {
    @UniqueConstraint(name = "uk_user_type_ebook", columnNames = {"user_id", "user_type", "ebook_id"})
})
public class ReadingProgress {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "user_type", nullable = false, length = 20, columnDefinition = "VARCHAR(20) DEFAULT 'READER'")
    private String userType;

    @Column(name = "ebook_id", nullable = false)
    private Long ebookId;

    @Column(name = "chapter_id", nullable = false)
    private Long chapterId;

    @Column(name = "chapter_index", nullable = false)
    private Integer chapterIndex;

    @Column(name = "scroll_position")
    private Integer scrollPosition = 0;

    @Column(name = "last_read_time")
    private LocalDateTime lastReadTime;

    @Column(name = "total_read_time")
    private Long totalReadTime = 0L;

    @Column(name = "read_count")
    private Long readCount = 0L;

    @PrePersist
    protected void onCreate() {
        lastReadTime = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        lastReadTime = LocalDateTime.now();
    }
}
