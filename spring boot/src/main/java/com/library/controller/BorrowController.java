package com.library.controller;

import com.library.dto.ApiResponse;
import com.library.entity.Book;
import com.library.entity.Borrow;
import com.library.repository.BookRepository;
import com.library.repository.user.NormalUserRepository;
import com.library.security.annotation.AuditLog;
import com.library.security.annotation.RequireAdmin;
import com.library.service.BorrowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 借阅管理控制器
 * 提供图书借阅、归还、查询等接口
 */
@RestController
@RequestMapping("/api/borrows")
@CrossOrigin(origins = "*")
public class BorrowController {
    
    @Autowired
    private BorrowService borrowService;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private NormalUserRepository normalUserRepository;

    private void enrichBorrow(Borrow b) {
        if (b.getBookId() != null) {
            bookRepository.findById(b.getBookId()).ifPresent(book -> {
                b.setBookTitle(book.getTitle());
                b.setIsbn(book.getIsbn());
            });
        }
        if (b.getUserId() != null) {
            normalUserRepository.findById(b.getUserId()).ifPresent(u -> b.setUsername(u.getUsername()));
        }
    }

    /**
     * 借书
     * @param bookId 图书ID
     * @param borrowDays 借阅天数（可选，默认30天）
     * @param authentication 认证信息
     * @return 借阅记录
     */
    @PostMapping("/borrow")
    @AuditLog(operationType = "BORROW_BOOK", targetType = "BORROW", description = "借书")
    public ApiResponse<Borrow> borrowBook(@RequestParam Long bookId,
                                          @RequestParam(required = false) Integer borrowDays,
                                          Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        String role = getRoleFromAuthentication(authentication);
        Borrow borrow = borrowService.borrowBook(userId, bookId, borrowDays, role);
        return ApiResponse.success("借阅成功", borrow);
    }
    
    /**
     * 还书
     * @param id 借阅记录ID
     * @param authentication 认证信息
     * @return 借阅记录
     */
    @PostMapping("/return/{id}")
    @AuditLog(operationType = "RETURN_BOOK", targetType = "BORROW", description = "还书")
    public ApiResponse<Borrow> returnBook(@PathVariable Long id, Authentication authentication) {
        if (!isOwnerOrAdmin(id, authentication)) {
            return ApiResponse.forbidden("无权操作他人的借阅记录");
        }
        Borrow borrow = borrowService.returnBook(id);
        return ApiResponse.success("归还成功", borrow);
    }
    
    /**
     * 获取所有借阅记录（管理员）
     * @return 所有借阅记录列表
     */
    @GetMapping
    @RequireAdmin
    public ApiResponse<List<Borrow>> getAllBorrows() {
        List<Borrow> list = borrowService.getAllBorrows();
        list.forEach(this::enrichBorrow);
        return ApiResponse.success(list);
    }
    
    /**
     * 根据ID查询借阅记录（仅本人或管理员可查看）
     * @param id 借阅记录ID
     * @param authentication 认证信息
     * @return 借阅记录
     */
    @GetMapping("/{id}")
    public ApiResponse<Borrow> getBorrowById(@PathVariable Long id, Authentication authentication) {
        Borrow borrow = borrowService.getBorrowById(id);
        if (borrow == null) {
            return ApiResponse.notFound("借阅记录不存在");
        }
        if (!isOwnerOrAdminBorrow(borrow, authentication)) {
            return ApiResponse.forbidden("无权查看他人的借阅记录");
        }
        return ApiResponse.success(borrow);
    }
    
    /**
     * 获取当前用户的借阅记录
     * @param status 可选的状态筛选
     * @param authentication 认证信息
     * @return 当前用户的借阅记录列表
     */
    @GetMapping("/my")
    public ApiResponse<List<Borrow>> getMyBorrows(
            @RequestParam(required = false) String status,
            Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        List<Borrow> borrows;
        if (status != null && !status.isEmpty()) {
            borrows = borrowService.getBorrowsByUserIdAndStatus(userId, status);
        } else {
            borrows = borrowService.getBorrowsByUserId(userId);
        }
        borrows.forEach(this::enrichBorrow);
        return ApiResponse.success(borrows);
    }

    @GetMapping("/user/{userId}")
    @RequireAdmin
    public ApiResponse<List<Borrow>> getBorrowsByUserId(@PathVariable Long userId) {
        List<Borrow> list = borrowService.getBorrowsByUserId(userId);
        list.forEach(this::enrichBorrow);
        return ApiResponse.success(list);
    }

    @GetMapping("/book/{bookId}")
    @RequireAdmin
    public ApiResponse<List<Borrow>> getBorrowsByBookId(@PathVariable Long bookId) {
        List<Borrow> list = borrowService.getBorrowsByBookId(bookId);
        list.forEach(this::enrichBorrow);
        return ApiResponse.success(list);
    }

    @GetMapping("/status/{status}")
    @RequireAdmin
    public ApiResponse<List<Borrow>> getBorrowsByStatus(@PathVariable String status) {
        List<Borrow> list = borrowService.getBorrowsByStatus(status);
        list.forEach(this::enrichBorrow);
        return ApiResponse.success(list);
    }
    
    /**
     * 续借
     * @param id 借阅记录ID
     * @param newDueDate 用户选择的新归还日期（可选）
     * @param authentication 认证信息
     * @return 借阅记录
     */
    @PostMapping("/renew/{id}")
    @AuditLog(operationType = "RENEW_BOOK", targetType = "BORROW", description = "续借")
    public ApiResponse<Borrow> renewBook(@PathVariable Long id,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate newDueDate,
                                         Authentication authentication) {
        if (!isOwnerOrAdmin(id, authentication)) {
            return ApiResponse.forbidden("无权操作他人的借阅记录");
        }
        Borrow borrow = borrowService.renewBook(id, newDueDate);
        return ApiResponse.success("续借成功", borrow);
    }

    /**
     * 取消借阅
     * @param id 借阅记录ID
     * @param authentication 认证信息
     * @return 借阅记录
     */
    @PostMapping("/cancel/{id}")
    @AuditLog(operationType = "CANCEL_BORROW", targetType = "BORROW", description = "取消借阅")
    public ApiResponse<Borrow> cancelBorrow(@PathVariable Long id, Authentication authentication) {
        if (!isOwnerOrAdmin(id, authentication)) {
            return ApiResponse.forbidden("无权操作他人的借阅记录");
        }
        Borrow borrow = borrowService.cancelBorrow(id);
        return ApiResponse.success("取消借阅成功", borrow);
    }

    /**
     * 获取逾期的借阅记录（管理员）
     * @return 逾期借阅记录列表
     */
    @GetMapping("/overdue")
    @RequireAdmin
    public ApiResponse<List<Borrow>> getOverdueBorrows() {
        return ApiResponse.success(borrowService.getOverdueBorrows());
    }
    
    /**
     * 更新逾期状态（管理员）
     * @return 操作结果
     */
    @PostMapping("/update-overdue")
    @RequireAdmin
    @AuditLog(operationType = "UPDATE_OVERDUE", targetType = "BORROW", description = "更新逾期状态")
    public ApiResponse<Void> updateOverdueStatus() {
        borrowService.updateOverdueStatus();
        return ApiResponse.success("逾期状态更新成功", null);
    }
    
    /**
     * 删除借阅记录（管理员）
     * @param id 借阅记录ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    @RequireAdmin
    @AuditLog(operationType = "DELETE_BORROW", targetType = "BORROW", description = "删除借阅记录")
    public ApiResponse<Void> deleteBorrow(@PathVariable Long id) {
        boolean deleted = borrowService.deleteBorrow(id);
        if (deleted) {
            return ApiResponse.success("删除成功", null);
        }
        return ApiResponse.notFound("借阅记录不存在");
    }
    
    /**
     * 从认证信息中获取用户ID
     */
    private Long getUserIdFromAuthentication(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof com.library.entity.user.NormalUser) {
            return ((com.library.entity.user.NormalUser) principal).getId();
        } else if (principal instanceof com.library.entity.admin.AdminUser) {
            return ((com.library.entity.admin.AdminUser) principal).getId();
        } else if (principal instanceof com.library.entity.admin.SuperAdmin) {
            return ((com.library.entity.admin.SuperAdmin) principal).getId();
        } else if (principal instanceof com.library.entity.User) {
            return ((com.library.entity.User) principal).getId();
        }
        throw new RuntimeException("无法获取用户信息");
    }

    /**
     * 从认证信息中获取用户角色
     */
    private String getRoleFromAuthentication(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof com.library.entity.admin.AdminUser) {
            return "ADMIN";
        } else if (principal instanceof com.library.entity.admin.SuperAdmin) {
            return "SUPER_ADMIN";
        } else if (principal instanceof com.library.entity.User) {
            return ((com.library.entity.User) principal).getRole();
        }
        return "READER";
    }

    /**
     * 检查当前用户是否为管理员
     */
    private boolean isAdmin(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        return principal instanceof com.library.entity.admin.AdminUser
            || principal instanceof com.library.entity.admin.SuperAdmin;
    }

    /**
     * 校验借阅记录所有权：本人或管理员可操作
     */
    private boolean isOwnerOrAdmin(Long borrowId, Authentication authentication) {
        Borrow borrow = borrowService.getBorrowById(borrowId);
        if (borrow == null) return false;
        if (isAdmin(authentication)) return true;
        Long currentUserId = getUserIdFromAuthentication(authentication);
        return currentUserId.equals(borrow.getUserId());
    }

    /**
     * 校验借阅记录所有权（已有 borrow 对象时使用）
     */
    private boolean isOwnerOrAdminBorrow(Borrow borrow, Authentication authentication) {
        if (borrow == null) return false;
        if (isAdmin(authentication)) return true;
        Long currentUserId = getUserIdFromAuthentication(authentication);
        return currentUserId.equals(borrow.getUserId());
    }
}
