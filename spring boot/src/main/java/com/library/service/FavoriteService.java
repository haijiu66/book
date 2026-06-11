package com.library.service;

import com.library.cache.RedisCacheHelper;
import com.library.cache.CacheDtoMapper;
import com.library.dto.cache.BookCacheDto;
import com.library.dto.cache.EBookCacheDto;
import com.library.dto.RankingItem;
import com.library.entity.Book;
import com.library.entity.EBook;
import com.library.entity.UserFavorite;
import com.library.repository.BookRepository;
import com.library.repository.EBookRepository;
import com.library.repository.UserFavoriteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FavoriteService {

    private static final String CACHE_FAVORITES = "favorites";
    private static final String CACHE_RANKING = "ranking";

    private final UserFavoriteRepository favoriteRepository;
    private final BookRepository bookRepository;
    private final EBookRepository ebookRepository;
    private final RedisCacheHelper cache;

    /** 增/删：切换收藏后同步更新缓存 */
    @Transactional
    public boolean toggleFavorite(Long userId, String userType, String targetType, Long targetId) {
        if (favoriteRepository.existsByUserIdAndUserTypeAndTargetTypeAndTargetId(
                userId, userType, targetType, targetId)) {
            favoriteRepository.deleteByUserIdAndUserTypeAndTargetTypeAndTargetId(
                    userId, userType, targetType, targetId);
            cacheAfterToggleFavorite(userId, userType, targetType, targetId);
            return false;
        }
        UserFavorite f = new UserFavorite();
        f.setUserId(userId);
        f.setUserType(userType);
        f.setTargetType(targetType);
        f.setTargetId(targetId);
        favoriteRepository.save(f);
        cacheAfterToggleFavorite(userId, userType, targetType, targetId);
        return true;
    }

    /** 查：是否已收藏 — 先缓存后数据库 */
    public boolean isFavorited(Long userId, String userType, String targetType, Long targetId) {
        String key = "status_" + userId + "_" + userType + "_" + targetType + "_" + targetId;
        Boolean cached = cache.get(CACHE_FAVORITES, key);
        if (cached != null) {
            return cached;
        }
        boolean result = favoriteRepository.existsByUserIdAndUserTypeAndTargetTypeAndTargetId(
                userId, userType, targetType, targetId);
        cache.put(CACHE_FAVORITES, key, result);
        return result;
    }

    /** 查：收藏数量 — 先缓存后数据库 */
    public long getFavoriteCount(String targetType, Long targetId) {
        String key = "count_" + targetType + "_" + targetId;
        Long cached = cache.get(CACHE_FAVORITES, key);
        if (cached != null) {
            return cached;
        }
        long count = favoriteRepository.countByTargetTypeAndTargetId(targetType, targetId);
        cache.put(CACHE_FAVORITES, key, count);
        return count;
    }

    public Map<String, Boolean> getFavoriteStatus(Long userId, String userType, String targetType, List<Long> targetIds) {
        Map<String, Boolean> result = new HashMap<>();
        for (Long id : targetIds) {
            result.put(id.toString(), isFavorited(userId, userType, targetType, id));
        }
        return result;
    }

    /** 查：用户收藏 ID 列表 — 先缓存后数据库 */
    public List<Long> getUserFavoriteIds(Long userId, String userType, String targetType) {
        String key = "ids_" + userId + "_" + userType + "_" + targetType;
        return cache.getOrLoad(CACHE_FAVORITES, key, () ->
                favoriteRepository.findByUserIdAndUserTypeAndTargetType(userId, userType, targetType)
                        .stream().map(UserFavorite::getTargetId).collect(Collectors.toList()));
    }

    /** 查：用户收藏详情 — 先缓存后数据库 */
    public List<?> getUserFavorites(Long userId, String userType, String targetType) {
        String key = "list_" + userId + "_" + userType + "_" + targetType;
        if ("BOOK".equals(targetType)) {
            List<BookCacheDto> dtos = cache.getOrLoad(CACHE_FAVORITES, key, () -> {
                List<UserFavorite> favs = favoriteRepository.findByUserIdAndUserTypeAndTargetType(userId, userType, targetType);
                List<Book> books = favs.stream()
                        .map(f -> bookRepository.findById(f.getTargetId()).orElse(null))
                        .filter(Objects::nonNull).collect(Collectors.toList());
                books.forEach(b -> b.getCategories().size());
                return CacheDtoMapper.toBookDtoList(books);
            });
            return CacheDtoMapper.toBookList(dtos);
        }
        List<EBookCacheDto> dtos = cache.getOrLoad(CACHE_FAVORITES, key, () -> {
            List<UserFavorite> favs = favoriteRepository.findByUserIdAndUserTypeAndTargetType(userId, userType, targetType);
            List<EBook> ebooks = favs.stream()
                    .map(f -> ebookRepository.findById(f.getTargetId()).orElse(null))
                    .filter(Objects::nonNull).collect(Collectors.toList());
            ebooks.forEach(e -> e.getCategories().size());
            return CacheDtoMapper.toEBookDtoList(ebooks);
        });
        return CacheDtoMapper.toEBookList(dtos);
    }

    /** 查：收藏排行 — 先缓存后数据库 */
    public List<RankingItem> getFavoriteRanking(String targetType, int limit) {
        String key = "fav_" + targetType + "_" + limit;
        return cache.getOrLoad(CACHE_RANKING, key, () -> {
            List<Object[]> rows = favoriteRepository.findFavoriteRanking(targetType);
            List<RankingItem> result = new ArrayList<>();
            for (int i = 0; i < rows.size() && i < limit; i++) {
                Object[] row = rows.get(i);
                Long targetId = row[0] != null ? ((Number) row[0]).longValue() : null;
                Long count = row[1] != null ? ((Number) row[1]).longValue() : 0L;
                String title = "未知";
                String author = "";
                if ("BOOK".equals(targetType) && targetId != null) {
                    Book book = bookRepository.findById(targetId).orElse(null);
                    if (book != null) {
                        title = book.getTitle();
                        author = book.getAuthor();
                    }
                } else if ("EBOOK".equals(targetType) && targetId != null) {
                    EBook ebook = ebookRepository.findById(targetId).orElse(null);
                    if (ebook != null) {
                        title = ebook.getTitle();
                        author = ebook.getAuthor();
                    }
                }
                result.add(new RankingItem(targetId, title, author, count));
            }
            return result;
        });
    }

    private void cacheAfterToggleFavorite(Long userId, String userType, String targetType, Long targetId) {
        cache.evict(CACHE_FAVORITES, "status_" + userId + "_" + userType + "_" + targetType + "_" + targetId);
        cache.evict(CACHE_FAVORITES, "count_" + targetType + "_" + targetId);
        cache.evict(CACHE_FAVORITES, "ids_" + userId + "_" + userType + "_" + targetType);
        cache.evict(CACHE_FAVORITES, "list_" + userId + "_" + userType + "_" + targetType);
        cache.evictByKeyPrefix(CACHE_RANKING, "fav_" + targetType + "_");
    }
}
