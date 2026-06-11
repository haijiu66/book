package com.library.service;

import com.library.dto.RankingItem;
import com.library.entity.Ranking;
import com.library.repository.BookRepository;
import com.library.repository.BorrowRepository;
import com.library.repository.EBookRepository;
import com.library.repository.RankingRepository;
import com.library.repository.ReadingProgressRepository;
import com.library.repository.UserFavoriteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@EnableScheduling
public class RankingService {

    private final RankingRepository rankingRepository;
    private final BorrowRepository borrowRepository;
    private final ReadingProgressRepository progressRepository;
    private final UserFavoriteRepository favoriteRepository;
    private final BookRepository bookRepository;
    private final EBookRepository ebookRepository;

    /** 每 30 分钟全量刷新所有排行 */
    @Scheduled(fixedRate = 30 * 60 * 1000)
    @Transactional
    public void refreshAllRankings() {
        log.info("开始刷新排行数据...");
        try {
            refreshBorrowRankings();
            refreshReadingRankings();
            refreshFavoriteRankings("BOOK", "FAVORITE_BOOK");
            refreshFavoriteRankings("EBOOK", "FAVORITE_EBOOK");
            log.info("排行数据刷新完成");
        } catch (Exception e) {
            log.error("排行数据刷新失败", e);
        }
    }

    private void refreshBorrowRankings() {
        refreshBorrowPeriod("WEEKLY", LocalDate.now().minusDays(7));
        refreshBorrowPeriod("MONTHLY", LocalDate.now().minusDays(30));
        refreshBorrowPeriod("YEARLY", LocalDate.now().minusDays(365));
    }

    private void refreshBorrowPeriod(String period, LocalDate since) {
        List<Object[]> rows = borrowRepository.findBorrowRanking(since);
        saveRankingRows("BORROW", period, rows, true);
    }

    private void refreshReadingRankings() {
        List<Object[]> rows = progressRepository.findReadingRanking();
        saveRankingRows("READING", "ALL", rows, false);
    }

    private void refreshFavoriteRankings(String targetType, String rankingType) {
        List<Object[]> rows = favoriteRepository.findFavoriteRanking(targetType);
        saveRankingRowsWithTitleLookup(rankingType, "ALL", rows, targetType);
    }

    private void saveRankingRows(String type, String period, List<Object[]> rows, boolean isBook) {
        rankingRepository.deleteByTypeAndPeriod(type, period);
        List<Ranking> rankings = new ArrayList<>();
        int rank = 1;
        for (int i = 0; i < rows.size() && i < 50; i++) {
            Object[] row = rows.get(i);
            Long targetId = row[0] != null ? ((Number) row[0]).longValue() : null;
            Long count = row[1] != null ? ((Number) row[1]).longValue() : 0L;
            if (targetId == null) continue;

            String title = "未知";
            String author = "";
            if (isBook) {
                var book = bookRepository.findById(targetId).orElse(null);
                if (book != null) { title = book.getTitle(); author = book.getAuthor(); }
            } else {
                var ebook = ebookRepository.findById(targetId).orElse(null);
                if (ebook != null) { title = ebook.getTitle(); author = ebook.getAuthor(); }
            }

            Ranking r = new Ranking();
            r.setType(type); r.setPeriod(period); r.setTargetId(targetId);
            r.setTitle(title); r.setAuthor(author); r.setCount(count); r.setRankNum(rank++);
            rankings.add(r);
        }
        rankingRepository.saveAll(rankings);
    }

    private void saveRankingRowsWithTitleLookup(String type, String period, List<Object[]> rows, String targetType) {
        rankingRepository.deleteByTypeAndPeriod(type, period);
        List<Ranking> rankings = new ArrayList<>();
        int rank = 1;
        for (int i = 0; i < rows.size() && i < 50; i++) {
            Object[] row = rows.get(i);
            Long targetId = row[0] != null ? ((Number) row[0]).longValue() : null;
            Long count = row[1] != null ? ((Number) row[1]).longValue() : 0L;
            if (targetId == null) continue;

            String title = "未知"; String author = "";
            if ("BOOK".equals(targetType)) {
                var book = bookRepository.findById(targetId).orElse(null);
                if (book != null) { title = book.getTitle(); author = book.getAuthor(); }
            } else if ("EBOOK".equals(targetType)) {
                var ebook = ebookRepository.findById(targetId).orElse(null);
                if (ebook != null) { title = ebook.getTitle(); author = ebook.getAuthor(); }
            }

            Ranking r = new Ranking();
            r.setType(type); r.setPeriod(period); r.setTargetId(targetId);
            r.setTitle(title); r.setAuthor(author); r.setCount(count); r.setRankNum(rank++);
            rankings.add(r);
        }
        rankingRepository.saveAll(rankings);
    }

    /** 查询排行（从缓存表读取，不查源表） */
    public List<RankingItem> getRanking(String type, String period, int limit) {
        List<Ranking> list = rankingRepository.findByTypeAndPeriodOrderByRankNumAsc(type, period);
        List<RankingItem> result = new ArrayList<>();
        for (int i = 0; i < list.size() && i < limit; i++) {
            Ranking r = list.get(i);
            result.add(new RankingItem(r.getTargetId(), r.getTitle(), r.getAuthor(), r.getCount()));
        }
        return result;
    }
}
