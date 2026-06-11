package com.library.repository;

import com.library.entity.Chapter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChapterRepository extends JpaRepository<Chapter, Long> {
    List<Chapter> findByEbookIdOrderByChapterIndexAsc(Long ebookId);
    Optional<Chapter> findByEbookIdAndChapterIndex(Long ebookId, Integer chapterIndex);
    void deleteByEbookId(Long ebookId);
    int countByEbookId(Long ebookId);
}
