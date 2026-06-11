package com.library.repository;

import com.library.entity.Book;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {

    /** 悲观写锁查询 — 用于借阅/归还/取消等库存操作，防止并发超卖 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Book b WHERE b.id = :id")
    Optional<Book> findByIdWithLock(@Param("id") Long id);

    Optional<Book> findByIsbn(String isbn);

    @Query("SELECT DISTINCT b FROM Book b JOIN b.categories c WHERE c.id = :categoryId")
    List<Book> findByCategoryId(@Param("categoryId") Long categoryId);

    @Query("SELECT DISTINCT b FROM Book b LEFT JOIN b.categories c WHERE " +
           "b.title LIKE %:keyword% OR " +
           "b.author LIKE %:keyword% OR " +
           "b.isbn LIKE %:keyword% OR " +
           "c.name LIKE %:keyword%")
    List<Book> searchBooks(@Param("keyword") String keyword);

    boolean existsByTitleAndAuthor(String title, String author);

    @Query("SELECT b FROM Book b WHERE b.available <= :threshold AND b.available > 0 ORDER BY b.available ASC")
    List<Book> findLowStockBooks(@Param("threshold") Integer threshold);

    @Query("SELECT b FROM Book b WHERE b.available = 0 ORDER BY b.title ASC")
    List<Book> findOutOfStockBooks();
}
