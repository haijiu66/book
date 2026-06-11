package com.library.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.SQLRestriction;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@Entity
@Table(name = "ebook")
@JsonIgnoreProperties(ignoreUnknown = true)
@SQLRestriction("visible IS NULL OR visible = 1")
public class EBook {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "book_id")
    private Long bookId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 100)
    private String author;

    @Column(name = "resource_name", nullable = false, length = 200)
    private String resourceName;

    @Column(name = "file_type", nullable = false, length = 20)
    private String fileType;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "cover_path", length = 500)
    private String coverPath;

    @JsonSerialize(as = HashSet.class)
    @JsonDeserialize(as = HashSet.class)
    @JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
    @ManyToMany
    @JoinTable(name = "ebook_categories",
        joinColumns = @JoinColumn(name = "ebook_id"),
        inverseJoinColumns = @JoinColumn(name = "category_id"))
    private Set<Category> categories = new HashSet<>();

    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * 章节总数
     */
    @Column(name = "chapter_count")
    private Integer chapterCount;

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(name = "upload_user_id")
    private Long uploadUserId;

    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;

    @Column(nullable = false)
    private Integer visible = 1;

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
