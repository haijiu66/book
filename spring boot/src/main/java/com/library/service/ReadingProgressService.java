package com.library.service;

import com.library.cache.RedisCacheHelper;
import com.library.dto.RankingItem;
import com.library.entity.Chapter;
import com.library.entity.EBook;
import com.library.entity.ReadingProgress;
import com.library.repository.EBookRepository;
import com.library.repository.ReadingProgressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ReadingProgressService {

    private static final String CACHE_EBOOK = "ebook";
    private static final String CACHE_RANKING = "ranking";

    private final ReadingProgressRepository progressRepository;
    private final EBookService ebookService;
    private final EBookRepository ebookRepository;
    private final RedisCacheHelper cache;

    /** 查：阅读进度 — 先缓存后数据库 */
    public ReadingProgress getProgress(Long userId, String userType, Long ebookId) {
        String key = "progress_" + userId + "_" + ebookId;
        return cache.getOrLoad(CACHE_EBOOK, key, () ->
                progressRepository.findByUserIdAndUserTypeAndEbookId(userId, userType, ebookId).orElse(null));
    }

    public List<ReadingProgress> getUserProgress(Long userId, String userType) {
        String key = "progress_list_" + userId + "_" + userType;
        return cache.getOrLoad(CACHE_EBOOK, key, () ->
                progressRepository.findByUserIdAndUserTypeOrderByLastReadTimeDesc(userId, userType));
    }

    /** 增/改：保存阅读进度后同步写入缓存 */
    @Transactional
    public ReadingProgress saveProgress(Long userId, String userType, Long ebookId, Long chapterId, Integer chapterIndex, Integer scrollPosition) {
        Optional<ReadingProgress> existing = progressRepository.findByUserIdAndUserTypeAndEbookId(userId, userType, ebookId);

        ReadingProgress progress;
        if (existing.isPresent()) {
            progress = existing.get();
            progress.setChapterId(chapterId);
            progress.setChapterIndex(chapterIndex);
            progress.setScrollPosition(scrollPosition);
            progress.setReadCount((progress.getReadCount() != null ? progress.getReadCount() : 0) + 1);
        } else {
            progress = new ReadingProgress();
            progress.setUserId(userId);
            progress.setUserType(userType);
            progress.setEbookId(ebookId);
            progress.setChapterId(chapterId);
            progress.setChapterIndex(chapterIndex);
            progress.setScrollPosition(scrollPosition);
            progress.setReadCount(1L);
        }

        ReadingProgress saved = progressRepository.save(progress);
        cacheAfterSaveProgress(saved, userId, userType, ebookId);
        return saved;
    }

    /** 改：更新阅读时长后同步缓存 */
    @Transactional
    public ReadingProgress updateReadTime(Long userId, String userType, Long ebookId, Long additionalSeconds) {
        Optional<ReadingProgress> existing = progressRepository.findByUserIdAndUserTypeAndEbookId(userId, userType, ebookId);
        if (existing.isPresent()) {
            ReadingProgress progress = existing.get();
            progress.setTotalReadTime(progress.getTotalReadTime() + additionalSeconds);
            ReadingProgress saved = progressRepository.save(progress);
            cacheAfterSaveProgress(saved, userId, userType, ebookId);
            return saved;
        }
        return null;
    }

    public Chapter getStartChapter(Long userId, String userType, Long ebookId) {
        ReadingProgress progress = progressRepository
                .findByUserIdAndUserTypeAndEbookId(userId, userType, ebookId).orElse(null);
        if (progress != null) {
            return ebookService.getChapterById(progress.getChapterId());
        }
        return ebookService.getChapterByIndex(ebookId, 0);
    }

    /** 查：阅读排行 — 先缓存后数据库 */
    public List<RankingItem> getReadingRanking(int limit) {
        String key = "read_all_" + limit;
        return cache.getOrLoad(CACHE_RANKING, key, () -> {
            List<Object[]> rows = progressRepository.findReadingRanking();
            List<RankingItem> result = new ArrayList<>();
            for (int i = 0; i < rows.size() && i < limit; i++) {
                Object[] row = rows.get(i);
                Long ebookId = row[0] != null ? ((Number) row[0]).longValue() : null;
                Long count = row[1] != null ? ((Number) row[1]).longValue() : 0L;
                EBook ebook = ebookId != null ? ebookRepository.findById(ebookId).orElse(null) : null;
                result.add(new RankingItem(
                        ebookId,
                        ebook != null ? ebook.getTitle() : "未知电子书",
                        ebook != null ? ebook.getAuthor() : "",
                        count
                ));
            }
            return result;
        });
    }

    /** 删：删除阅读进度并清除对应缓存 */
    @Transactional
    public void deleteProgress(Long userId, String userType, Long ebookId) {
        progressRepository.deleteByUserIdAndUserTypeAndEbookId(userId, userType, ebookId);
        cacheAfterDeleteProgress(userId, userType, ebookId);
    }

    private void cacheAfterSaveProgress(ReadingProgress saved, Long userId, String userType, Long ebookId) {
        cache.put(CACHE_EBOOK, "progress_" + userId + "_" + ebookId, saved);
        cache.evict(CACHE_EBOOK, "progress_list_" + userId + "_" + userType);
        cache.evictByKeyPrefix(CACHE_RANKING, "read_");
    }

    private void cacheAfterDeleteProgress(Long userId, String userType, Long ebookId) {
        cache.evict(CACHE_EBOOK, "progress_" + userId + "_" + ebookId);
        cache.evict(CACHE_EBOOK, "progress_list_" + userId + "_" + userType);
        cache.evictByKeyPrefix(CACHE_RANKING, "read_");
    }
}
