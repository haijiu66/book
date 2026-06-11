package com.library.repository;

import com.library.entity.EBook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface EBookRepository extends JpaRepository<EBook, Long> {
    List<EBook> findByStatus(String status);
    Optional<EBook> findByBookId(Long bookId);
    List<EBook> findByUploadUserId(Long uploadUserId);
    List<EBook> findByTitleContainingIgnoreCase(String title);

    @Query("SELECT DISTINCT e FROM EBook e JOIN e.categories c WHERE c.id = :categoryId")
    List<EBook> findByCategoryId(@Param("categoryId") Long categoryId);

    @Query("SELECT DISTINCT e FROM EBook e JOIN e.categories c WHERE e.status = :status AND c.id = :categoryId")
    List<EBook> findByStatusAndCategoryId(@Param("status") String status, @Param("categoryId") Long categoryId);

    boolean existsByTitleAndAuthor(String title, String author);
}
