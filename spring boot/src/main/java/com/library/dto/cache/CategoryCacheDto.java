package com.library.dto.cache;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/** 分类缓存 DTO（无 Hibernate 代理） */
@Data
public class CategoryCacheDto {
    private Long id;
    private String name;
    private String description;
    private Long parentId;
    private List<CategoryCacheDto> children;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
