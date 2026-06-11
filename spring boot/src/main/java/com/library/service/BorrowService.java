package com.library.service;

import com.library.cache.RedisCacheHelper;
import com.library.cache.CacheDtoMapper;
import com.library.dto.RankingItem;
import com.library.entity.Book;
import com.library.entity.Borrow;
import com.library.repository.BookRepository;
import com.library.repository.BorrowRepository;
import com.library.security.annotation.AuditLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 借阅管理业务逻辑层
 * 处理图书借阅、归还、查询等核心业务
 */
@Slf4j
@Service
@Transactional
public class BorrowService {

    private static final String CACHE_BORROWS = "borrows";
    private static final String CACHE_RANKING = "ranking";
    private static final String CACHE_BOOK = "book";
    private static final String CACHE_BOOKS = "books";

    @Autowired
    private BorrowRepository borrowRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private RedisCacheHelper cache;

    /** 增：借书入库后同步借阅缓存，并更新图书库存缓存 */
    @AuditLog(operationType = "BORROW_BOOK_SERVICE", targetType = "BORROW", description = "借书（业务层）")
    public Borrow borrowBook(Long userId, Long bookId, Integer borrowDays, String role) {
        log.info("用户借阅图书: userId={}, bookId={}", userId, bookId);

        // 悲观写锁：SELECT ... FOR UPDATE，防止并发超卖
        Book book = bookRepository.findByIdWithLock(bookId).orElse(null);
        if (book == null) {
            throw new RuntimeException("图书不存在");
        }
        if (book.getAvailable() == null || book.getAvailable() <= 0) {
            throw new RuntimeException("图书库存不足");
        }

        Borrow existingBorrow = borrowRepository.findByUserIdAndBookIdAndStatus(userId, bookId, "BORROWED").orElse(null);
        if (existingBorrow != null) {
            throw new RuntimeException("您已经借阅了这本书且尚未归还");
        }

        Borrow borrow = new Borrow();
        borrow.setUserId(userId);
        borrow.setBookId(bookId);
        borrow.setBorrowDate(LocalDate.now());

        if (borrowDays != null && borrowDays > 0) {
            if (borrowDays > 120) {
                throw new RuntimeException("借阅天数不能超过 120 天");
            }
            borrow.setDueDate(LocalDate.now().plusDays(borrowDays));
        } else {
            borrow.setDueDate(LocalDate.now().plusDays(30));
        }

        borrow.setStatus("BORROWED");

        book.setAvailable(book.getAvailable() - 1);
        Book savedBook = bookRepository.save(book);
        book.getCategories().size();

        Borrow savedBorrow = borrowRepository.save(borrow);
        log.info("借阅成功: borrowId={}", savedBorrow.getId());

        // 库存预警：剩余库存 ≤ 3 时携带警告信息
        if (book.getAvailable() <= 3) {
            if ("ADMIN".equals(role) || "SUPER_ADMIN".equals(role)) {
                savedBorrow.setStockWarning("库存预警：《" + book.getTitle() + "》仅剩 " + book.getAvailable() + " 本，请及时补货");
            } else {
                savedBorrow.setStockWarning("《" + book.getTitle() + "》剩余较少，仅剩 " + book.getAvailable() + " 本");
            }
        }

        cacheAfterAddBorrow(savedBorrow, userId);
        cacheAfterBookStockChange(savedBook);
        cache.evictByKeyPrefix(CACHE_RANKING, "borrow_");
        return savedBorrow;
    }

    /** 改：还书后同步更新借阅缓存与图书库存缓存 */
    @AuditLog(operationType = "RETURN_BOOK_SERVICE", targetType = "BORROW", description = "还书（业务层）")
    public Borrow returnBook(Long borrowId) {
        log.info("归还图书: borrowId={}", borrowId);

        Borrow borrow = borrowRepository.findById(borrowId).orElse(null);
        if (borrow == null) {
            throw new RuntimeException("借阅记录不存在");
        }
        if ("RETURNED".equals(borrow.getStatus())) {
            throw new RuntimeException("该图书已经归还");
        }

        borrow.setReturnDate(LocalDate.now());
        borrow.setStatus("RETURNED");

        Book book = bookRepository.findByIdWithLock(borrow.getBookId()).orElse(null);
        if (book != null) {
            book.setAvailable(book.getAvailable() + 1);
            Book savedBook = bookRepository.save(book);
            book.getCategories().size();
            cacheAfterBookStockChange(savedBook);
        }

        Borrow savedBorrow = borrowRepository.save(borrow);
        log.info("归还成功: borrowId={}", savedBorrow.getId());

        cacheAfterUpdateBorrow(savedBorrow);
        cache.evictByKeyPrefix(CACHE_RANKING, "borrow_");
        return savedBorrow;
    }

    /** 查：按 ID — 先缓存后数据库 */
    public Borrow getBorrowById(Long id) {
        return cache.getOrLoad(CACHE_BORROWS, "id_" + id, () ->
                borrowRepository.findById(id).orElse(null));
    }

    /** 查：全部借阅 — 先缓存后数据库 */
    public List<Borrow> getAllBorrows() {
        return cache.getOrLoad(CACHE_BORROWS, "all", () ->
                borrowRepository.findAll(org.springframework.data.domain.Sort.by(
                        org.springframework.data.domain.Sort.Direction.DESC, "id")));
    }

    /** 查：用户借阅 — 先缓存后数据库 */
    public List<Borrow> getBorrowsByUserId(Long userId) {
        return cache.getOrLoad(CACHE_BORROWS, "user_" + userId, () ->
                borrowRepository.findByUserIdOrderByIdDesc(userId));
    }

    /** 查：用户借阅（按状态）— 先缓存后数据库 */
    public List<Borrow> getBorrowsByUserIdAndStatus(Long userId, String status) {
        String key = "user_" + userId + "_" + status;
        return cache.getOrLoad(CACHE_BORROWS, key, () ->
                borrowRepository.findByUserIdAndStatusOrderByIdDesc(userId, status));
    }

    public List<Borrow> getBorrowsByBookId(Long bookId) {
        return borrowRepository.findByBookId(bookId);
    }

    public List<Borrow> getBorrowsByStatus(String status) {
        return borrowRepository.findByStatus(status);
    }

    public List<Borrow> getOverdueBorrows() {
        return borrowRepository.findOverdueBorrows(LocalDate.now());
    }

    @AuditLog(operationType = "UPDATE_OVERDUE_SERVICE", targetType = "BORROW", description = "更新逾期状态（业务层）")
    public void updateOverdueStatus() {
        List<Borrow> overdueBorrows = getOverdueBorrows();
        for (Borrow borrow : overdueBorrows) {
            borrow.setStatus("OVERDUE");
            Borrow saved = borrowRepository.save(borrow);
            cacheAfterUpdateBorrow(saved);
        }
        log.info("已更新 {} 条逾期记录", overdueBorrows.size());
    }

    /** 改：续借后同步更新借阅缓存 */
    @AuditLog(operationType = "RENEW_BOOK_SERVICE", targetType = "BORROW", description = "续借（业务层）")
    public Borrow renewBook(Long borrowId, LocalDate newDueDate) {
        log.info("续借图书: borrowId={}, newDueDate={}", borrowId, newDueDate);

        Borrow borrow = borrowRepository.findById(borrowId).orElse(null);
        if (borrow == null) {
            throw new RuntimeException("借阅记录不存在");
        }
        if (!"BORROWED".equals(borrow.getStatus())) {
            throw new RuntimeException("只能续借借阅中的图书");
        }

        // 绝对上限：从借阅日起最长 365 天，防止无限续借
        LocalDate absoluteMax = borrow.getBorrowDate().plusDays(365);
        // 单次续借上限：当前到期日 + 120 天
        LocalDate singleMax = borrow.getDueDate().plusDays(120);
        // 取两者中较早的作为最终上限
        LocalDate effectiveMax = absoluteMax.isBefore(singleMax) ? absoluteMax : singleMax;

        if (newDueDate != null) {
            if (newDueDate.isBefore(borrow.getDueDate().plusDays(1))) {
                throw new RuntimeException("续借日期必须晚于当前应还日期");
            }
            if (newDueDate.isAfter(effectiveMax)) {
                throw new RuntimeException("续借后最晚不能超过借阅日起 365 天，且单次不超过当前到期日 + 120 天");
            }
            borrow.setDueDate(newDueDate);
        } else {
            LocalDate defaultDue = borrow.getDueDate().plusDays(7);
            if (defaultDue.isAfter(effectiveMax)) {
                throw new RuntimeException("已接近最大借阅期限（借阅日起 365 天），无法继续续借");
            }
            borrow.setDueDate(defaultDue);
        }

        Borrow savedBorrow = borrowRepository.save(borrow);
        log.info("续借成功: borrowId={}", savedBorrow.getId());

        cacheAfterUpdateBorrow(savedBorrow);
        return savedBorrow;
    }

    /** 改：取消借阅后同步更新借阅缓存与图书库存缓存 */
    @AuditLog(operationType = "CANCEL_BORROW_SERVICE", targetType = "BORROW", description = "取消借阅（业务层）")
    public Borrow cancelBorrow(Long borrowId) {
        log.info("取消借阅: borrowId={}", borrowId);

        Borrow borrow = borrowRepository.findById(borrowId).orElse(null);
        if (borrow == null) {
            throw new RuntimeException("借阅记录不存在");
        }
        if (!"BORROWED".equals(borrow.getStatus())) {
            throw new RuntimeException("只能取消借阅中的记录");
        }

        borrow.setStatus("CANCELLED");
        borrow.setReturnDate(LocalDate.now());

        Book book = bookRepository.findByIdWithLock(borrow.getBookId()).orElse(null);
        if (book != null) {
            book.setAvailable(book.getAvailable() + 1);
            Book savedBook = bookRepository.save(book);
            book.getCategories().size();
            cacheAfterBookStockChange(savedBook);
        }

        Borrow savedBorrow = borrowRepository.save(borrow);
        log.info("取消借阅成功: borrowId={}", savedBorrow.getId());

        cacheAfterUpdateBorrow(savedBorrow);
        cache.evictByKeyPrefix(CACHE_RANKING, "borrow_");
        return savedBorrow;
    }

    /** 查：借阅排行 — 先缓存后数据库 */
    public List<RankingItem> getBorrowRanking(LocalDate since, int limit) {
        String key = "borrow_" + since + "_" + limit;
        return cache.getOrLoad(CACHE_RANKING, key, () -> {
            List<Object[]> rows = borrowRepository.findBorrowRanking(since);
            List<RankingItem> result = new ArrayList<>();
            for (int i = 0; i < rows.size() && i < limit; i++) {
                Object[] row = rows.get(i);
                Long bookId = row[0] != null ? ((Number) row[0]).longValue() : null;
                Long count = row[1] != null ? ((Number) row[1]).longValue() : 0L;
                Book book = bookId != null ? bookRepository.findById(bookId).orElse(null) : null;
                result.add(new RankingItem(
                        bookId,
                        book != null ? book.getTitle() : "未知图书",
                        book != null ? book.getAuthor() : "",
                        count
                ));
            }
            return result;
        });
    }

    /** 删：删除借阅记录并清除对应缓存 */
    @AuditLog(operationType = "DELETE_BORROW_SERVICE", targetType = "BORROW", description = "删除借阅记录（业务层）")
    public boolean deleteBorrow(Long id) {
        return borrowRepository.findById(id).map(borrow -> {
            borrowRepository.deleteById(id);
            cacheAfterDeleteBorrow(id, borrow.getUserId(), borrow.getStatus());
            cache.evictByKeyPrefix(CACHE_RANKING, "borrow_");
            return true;
        }).orElse(false);
    }

    private void cacheAfterAddBorrow(Borrow saved, Long userId) {
        cache.put(CACHE_BORROWS, "id_" + saved.getId(), saved);
        cache.evict(CACHE_BORROWS, "all");
        cache.evict(CACHE_BORROWS, "user_" + userId);
        cache.evict(CACHE_BORROWS, "user_" + userId + "_BORROWED");
    }

    private void cacheAfterUpdateBorrow(Borrow saved) {
        cache.put(CACHE_BORROWS, "id_" + saved.getId(), saved);
        cache.evict(CACHE_BORROWS, "all");
        cache.evict(CACHE_BORROWS, "user_" + saved.getUserId());
        cache.evict(CACHE_BORROWS, "user_" + saved.getUserId() + "_" + saved.getStatus());
        cache.evict(CACHE_BORROWS, "user_" + saved.getUserId() + "_BORROWED");
        cache.evict(CACHE_BORROWS, "user_" + saved.getUserId() + "_RETURNED");
        cache.evict(CACHE_BORROWS, "user_" + saved.getUserId() + "_OVERDUE");
        cache.evict(CACHE_BORROWS, "user_" + saved.getUserId() + "_CANCELLED");
    }

    private void cacheAfterDeleteBorrow(Long id, Long userId, String status) {
        cache.evict(CACHE_BORROWS, "id_" + id);
        cache.evict(CACHE_BORROWS, "all");
        cache.evict(CACHE_BORROWS, "user_" + userId);
        cache.evict(CACHE_BORROWS, "user_" + userId + "_" + status);
    }

    private void cacheAfterBookStockChange(Book savedBook) {
        cache.put(CACHE_BOOK, savedBook.getId(), CacheDtoMapper.toBookDto(savedBook));
        cache.evict(CACHE_BOOKS, "all_v2");
        cache.evictByKeyPrefix(CACHE_BOOKS, "search_v2_");
        cache.evictByKeyPrefix(CACHE_BOOKS, "cat_v2_");
    }
}
