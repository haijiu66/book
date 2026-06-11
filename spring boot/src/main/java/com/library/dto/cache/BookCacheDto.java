package com.library.dto.cache;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/** 图书缓存 DTO（无 Hibernate 代理） */
@Data
public class BookCacheDto {
    private Long id;
    private String isbn;
    private String title;
    private String author;
    private String publisher;
    private LocalDate publishDate;
    private String coverPath;
    private Set<CategoryCacheDto> categories = new HashSet<>();
    private BigDecimal price;
    private Integer stock;
    private Integer available;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer visible;
}
