package com.library.controller;

import com.library.dto.ApiResponse;
import com.library.dto.RankingItem;
import com.library.service.RankingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rankings")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class RankingController {

    private final RankingService rankingService;

    private String toPeriod(String type) {
        switch (type) {
            case "yearly": return "YEARLY";
            case "monthly": return "MONTHLY";
            case "all": return "ALL";
            default: return "WEEKLY";
        }
    }

    /** 借阅排行 — 从 ranking 缓存表读取 */
    @GetMapping("/borrow")
    public ApiResponse<List<RankingItem>> getBorrowRanking(
            @RequestParam(defaultValue = "weekly") String type,
            @RequestParam(defaultValue = "10") int limit) {
        int clampedLimit = Math.min(Math.max(limit, 1), 50);
        return ApiResponse.success(rankingService.getRanking("BORROW", toPeriod(type), clampedLimit));
    }

    /** 阅读排行 — 从 ranking 缓存表读取 */
    @GetMapping("/reading")
    public ApiResponse<List<RankingItem>> getReadingRanking(
            @RequestParam(defaultValue = "weekly") String type,
            @RequestParam(defaultValue = "10") int limit) {
        int clampedLimit = Math.min(Math.max(limit, 1), 50);
        return ApiResponse.success(rankingService.getRanking("READING", toPeriod(type), clampedLimit));
    }
}
