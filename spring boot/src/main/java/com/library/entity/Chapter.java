package com.library.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 章节实体 - 存储章节元数据和缓存内容
 * 首次阅读时从文件加载并缓存，后续直接从数据库读取
 */
@Data
@Entity
@Table(name = "chapter")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Chapter {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ebook_id", nullable = false)
    private Long ebookId;

    @Column(name = "chapter_index", nullable = false)
    private Integer chapterIndex;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "word_count")
    private Integer wordCount;

    /**
     * 章节内容缓存 - 首次从文件加载后存储到数据库
     * 后续直接从数据库读取，无需再次解析文件
     */
    @Column(columnDefinition = "LONGTEXT")
    private String content;

    /**
     * 内容是否已缓存
     * true: content字段有数据，可直接读取
     * false: content为空，需要从文件加载
     */
    @Column(name = "content_cached")
    private Boolean contentCached = false;

    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
        updateTime = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
    }
}