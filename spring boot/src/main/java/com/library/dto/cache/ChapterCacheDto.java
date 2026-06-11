package com.library.dto.cache;

import lombok.Data;

import java.time.LocalDateTime;

/** 章节缓存 DTO（无 Hibernate 代理） */
@Data
public class ChapterCacheDto {
    private Long id;
    private Long ebookId;
    private Integer chapterIndex;
    private String title;
    private Integer wordCount;
    private String content;
    private Boolean contentCached;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
