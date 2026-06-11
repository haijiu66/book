package com.library.dto.cache;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/** 电子书缓存 DTO（无 Hibernate 代理） */
@Data
public class EBookCacheDto {
    private Long id;
    private Long bookId;
    private String title;
    private String author;
    private String resourceName;
    private String fileType;
    private Long fileSize;
    private String coverPath;
    private Set<CategoryCacheDto> categories = new HashSet<>();
    private String description;
    private Integer chapterCount;
    private String status;
    private Long uploadUserId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer visible;
}
