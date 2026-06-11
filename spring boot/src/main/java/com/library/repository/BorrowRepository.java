package com.library.repository;

import com.library.entity.Borrow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 借阅记录数据访问层
 */
@Repository
public interface BorrowRepository extends JpaRepository<Borrow, Long> {
    
    /**
     * 根据用户ID查询所有借阅记录
     */
    List<Borrow> findByUserId(Long userId);
    
    /**
     * 根据用户ID和状态查询借阅记录
     */
    List<Borrow> findByUserIdAndStatus(Long userId, String status);
    
    /**
     * 根据图书ID查询所有借阅记录
     */
    List<Borrow> findByBookId(Long bookId);
    
    /**
     * 根据图书ID和状态查询借阅记录
     */
    List<Borrow> findByBookIdAndStatus(Long bookId, String status);
    
    /**
     * 查询用户是否正在借阅某本书
     */
    Optional<Borrow> findByUserIdAndBookIdAndStatus(Long userId, Long bookId, String status);
    
    /**
     * 查询所有借阅中的记录
     */
    List<Borrow> findByStatus(String status);
    
    /**
     * 查询逾期的借阅记录
     */
    @Query("SELECT b FROM Borrow b WHERE b.status = 'BORROWED' AND b.dueDate < :today ORDER BY b.id DESC")
    List<Borrow> findOverdueBorrows(@Param("today") LocalDate today);

    List<Borrow> findByUserIdOrderByIdDesc(Long userId);
    List<Borrow> findByUserIdAndStatusOrderByIdDesc(Long userId, String status);
    
    /**
     * 查询指定日期范围内的借阅记录
     */
    @Query("SELECT b FROM Borrow b WHERE b.borrowDate BETWEEN :startDate AND :endDate")
    List<Borrow> findByBorrowDateBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT b.bookId, COUNT(b) FROM Borrow b WHERE b.borrowDate >= :since GROUP BY b.bookId ORDER BY COUNT(b) DESC")
    List<Object[]> findBorrowRanking(@Param("since") LocalDate since);
}
