package com.library.controller;

import com.library.dto.ApiResponse;
import com.library.dto.RankingItem;
import com.library.service.FavoriteService;
import com.library.service.RankingService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/favorites")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;
    private final RankingService rankingService;

    @PostMapping("/toggle")
    public ApiResponse<Map<String, Object>> toggle(
            @RequestBody Map<String, Object> body,
            Authentication authentication) {
        String userType = getUserType(authentication);
        Long userId = getUserId(authentication);
        String targetType = (String) body.get("targetType");
        Long targetId = Long.valueOf(body.get("targetId").toString());
        boolean favorited = favoriteService.toggleFavorite(userId, userType, targetType, targetId);
        long count = favoriteService.getFavoriteCount(targetType, targetId);
        return ApiResponse.success(Map.of("favorited", favorited, "count", count));
    }

    @GetMapping("/status")
    public ApiResponse<Map<String, Boolean>> getStatus(
            @RequestParam String targetType,
            @RequestParam List<Long> ids,
            Authentication authentication) {
        return ApiResponse.success(
                favoriteService.getFavoriteStatus(getUserId(authentication), getUserType(authentication), targetType, ids));
    }

    @GetMapping("/my/ids")
    public ApiResponse<List<Long>> getMyFavoriteIds(
            @RequestParam String targetType, Authentication authentication) {
        return ApiResponse.success(favoriteService.getUserFavoriteIds(
                getUserId(authentication), getUserType(authentication), targetType));
    }

    @GetMapping("/my/list")
    public ApiResponse<List<?>> getMyFavorites(
            @RequestParam String targetType, Authentication authentication) {
        return ApiResponse.success(favoriteService.getUserFavorites(
                getUserId(authentication), getUserType(authentication), targetType));
    }

    @GetMapping("/ranking/{targetType}")
    public ApiResponse<List<RankingItem>> getRanking(
            @PathVariable String targetType,
            @RequestParam(defaultValue = "10") int limit) {
        String type = targetType.equalsIgnoreCase("BOOK") ? "FAVORITE_BOOK" : "FAVORITE_EBOOK";
        return ApiResponse.success(rankingService.getRanking(type, "ALL", Math.min(limit, 50)));
    }

    private Long getUserId(Authentication auth) {
        Object p = auth.getPrincipal();
        if (p instanceof com.library.entity.user.NormalUser) return ((com.library.entity.user.NormalUser) p).getId();
        if (p instanceof com.library.entity.admin.AdminUser) return ((com.library.entity.admin.AdminUser) p).getId();
        if (p instanceof com.library.entity.admin.SuperAdmin) return ((com.library.entity.admin.SuperAdmin) p).getId();
        throw new RuntimeException("无法获取用户信息");
    }

    private String getUserType(Authentication auth) {
        Object p = auth.getPrincipal();
        if (p instanceof com.library.entity.user.NormalUser) return "READER";
        if (p instanceof com.library.entity.admin.AdminUser) return "ADMIN";
        if (p instanceof com.library.entity.admin.SuperAdmin) return "SUPER_ADMIN";
        throw new RuntimeException("无法获取用户类型");
    }
}
