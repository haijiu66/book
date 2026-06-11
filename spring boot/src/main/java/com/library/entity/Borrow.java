package com.library.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 借阅记录实体类
 */
@Data
@Entity
@Table(name = "borrow", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_book_id", columnList = "book_id"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_user_id_status", columnList = "user_id, status")
})
@JsonIgnoreProperties(ignoreUnknown = true)
public class Borrow {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 用户ID
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    /**
     * 图书ID
     */
    @Column(name = "book_id", nullable = false)
    private Long bookId;
    
    /**
     * 借阅日期
     */
    @Column(name = "borrow_date", nullable = false)
    private LocalDate borrowDate;
    
    /**
     * 应还日期
     */
    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;
    
    /**
     * 归还日期
     */
    @Column(name = "return_date")
    private LocalDate returnDate;
    
    /**
     * 借阅状态：BORROWED-借阅中，RETURNED-已归还，OVERDUE-已逾期
     */
    @Column(nullable = false, length = 20)
    private String status = "BORROWED";
    
    @Transient
    private String bookTitle;
    @Transient
    private String isbn;
    @Transient
    private String username;

    /** 库存预警信息：借书后剩余库存 ≤ 3 时填充 */
    @Transient
    private String stockWarning;

    /**
     * 创建时间
     */
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    @Column(name = "update_time")
    private LocalDateTime updateTime;
    
    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
        updateTime = LocalDateTime.now();
        if (borrowDate == null) {
            borrowDate = LocalDate.now();
        }
        if (dueDate == null) {
            dueDate = borrowDate.plusDays(30); // 默认借阅30天
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
    }
}
