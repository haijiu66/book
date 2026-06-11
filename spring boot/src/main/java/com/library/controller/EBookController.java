package com.library.controller;

import com.library.dto.ApiResponse;
import com.library.entity.Chapter;
import com.library.entity.EBook;
import com.library.entity.ReadingProgress;
import com.library.entity.admin.AdminUser;
import com.library.entity.admin.SuperAdmin;
import com.library.entity.user.NormalUser;
import com.library.security.annotation.AuditLog;
import com.library.security.annotation.RequireAdmin;
import com.library.service.EBookService;
import org.springframework.security.access.prepost.PreAuthorize;
import com.library.service.ReadingProgressService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ebooks")
@RequiredArgsConstructor
public class EBookController {

    private final EBookService ebookService;
    private final ReadingProgressService progressService;

    /**
     * 从 Authentication 中获取用户ID
     * 支持三种用户类型：SuperAdmin、AdminUser、NormalUser
     */
    private Long getUserIdFromAuthentication(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof SuperAdmin) {
            return ((SuperAdmin) principal).getId();
        } else if (principal instanceof AdminUser) {
            return ((AdminUser) principal).getId();
        } else if (principal instanceof NormalUser) {
            return ((NormalUser) principal).getId();
        } else if (principal instanceof UserDetails) {
            // 兜底处理，如果 principal 是 UserDetails 但不是上述三种类型
            throw new RuntimeException("无法识别的用户类型: " + principal.getClass().getName());
        }
        throw new RuntimeException("用户未认证或认证信息无效");
    }

    /**
     * 从 Authentication 中获取用户类型
     */
    private String getUserTypeFromAuthentication(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof SuperAdmin) {
            return "SUPER_ADMIN";
        } else if (principal instanceof AdminUser) {
            return "ADMIN";
        } else if (principal instanceof NormalUser) {
            return "READER";
        } else if (principal instanceof UserDetails) {
            throw new RuntimeException("无法识别的用户类型: " + principal.getClass().getName());
        }
        throw new RuntimeException("用户未认证或认证信息无效");
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<EBook>>> getAllEBooks() {
        List<EBook> ebooks = ebookService.getAllEBooks();
        return ResponseEntity.ok(ApiResponse.success("获取成功", ebooks));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EBook>> getEBookById(@PathVariable Long id) {
        EBook ebook = ebookService.getEBookById(id);
        if (ebook == null) {
            return ResponseEntity.ok(ApiResponse.notFound("电子书不存在"));
        }
        return ResponseEntity.ok(ApiResponse.success("获取成功", ebook));
    }

    @GetMapping("/{id}/chapters")
    public ResponseEntity<ApiResponse<List<Chapter>>> getChapters(@PathVariable Long id) {
        List<Chapter> chapters = ebookService.getChaptersByEBookId(id);
        return ResponseEntity.ok(ApiResponse.success("获取成功", chapters));
    }

    @GetMapping("/{ebookId}/chapters/{index}")
    public ResponseEntity<ApiResponse<Chapter>> getChapterByIndex(@PathVariable Long ebookId, @PathVariable int index) {
        Chapter chapter = ebookService.getChapterByIndex(ebookId, index);
        if (chapter == null) {
            return ResponseEntity.ok(ApiResponse.notFound("章节不存在"));
        }
        return ResponseEntity.ok(ApiResponse.success("获取成功", chapter));
    }

    @GetMapping("/{ebookId}/start")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStartReading(
            @PathVariable Long ebookId,
            Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        String userType = getUserTypeFromAuthentication(authentication);
        Chapter chapter = progressService.getStartChapter(userId, userType, ebookId);
        ReadingProgress progress = progressService.getProgress(userId, userType, ebookId);

        Map<String, Object> result = new HashMap<>();
        result.put("chapter", chapter);
        result.put("progress", progress);

        return ResponseEntity.ok(ApiResponse.success("获取成功", result));
    }

    @GetMapping("/progress")
    public ResponseEntity<ApiResponse<List<ReadingProgress>>> getUserProgress(Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        String userType = getUserTypeFromAuthentication(authentication);
        List<ReadingProgress> progressList = progressService.getUserProgress(userId, userType);
        return ResponseEntity.ok(ApiResponse.success("获取成功", progressList));
    }

    @GetMapping("/progress/{ebookId}")
    public ResponseEntity<ApiResponse<ReadingProgress>> getProgress(
            @PathVariable Long ebookId,
            Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        String userType = getUserTypeFromAuthentication(authentication);
        ReadingProgress progress = progressService.getProgress(userId, userType, ebookId);
        return ResponseEntity.ok(ApiResponse.success("获取成功", progress));
    }

    @PostMapping("/progress")
    @AuditLog(operationType = "SAVE", targetType = "READING_PROGRESS", description = "保存阅读进度")
    public ResponseEntity<ApiResponse<ReadingProgress>> saveProgress(
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        String userType = getUserTypeFromAuthentication(authentication);

        Object ebookIdObj = request.get("ebookId");
        Object chapterIdObj = request.get("chapterId");
        Object chapterIndexObj = request.get("chapterIndex");
        Object scrollPosObj = request.get("scrollPosition");

        if (ebookIdObj == null || chapterIdObj == null || chapterIndexObj == null || scrollPosObj == null) {
            return ResponseEntity.ok(ApiResponse.badRequest("缺少必填字段（ebookId/chapterId/chapterIndex/scrollPosition）"));
        }

        Long ebookId = Long.valueOf(ebookIdObj.toString());
        Long chapterId = Long.valueOf(chapterIdObj.toString());
        Integer chapterIndex = Integer.valueOf(chapterIndexObj.toString());
        Integer scrollPosition = Math.round(Float.parseFloat(scrollPosObj.toString()));

        ReadingProgress progress = progressService.saveProgress(
                userId, userType, ebookId, chapterId, chapterIndex, scrollPosition);
        return ResponseEntity.ok(ApiResponse.success("保存成功", progress));
    }

    @PostMapping("/upload")
    @RequireAdmin
    @AuditLog(operationType = "UPLOAD", targetType = "EBOOK", description = "上传电子书")
    public ResponseEntity<ApiResponse<EBook>> uploadEBook(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "author", required = false) String author,
            @RequestParam(value = "categoryIds", required = false) List<Long> categoryIds,
            @RequestParam(value = "description", required = false) String description,
            Authentication authentication) throws IOException {
        Long userId = getUserIdFromAuthentication(authentication);
        EBook ebook = ebookService.uploadEBook(file, title, author, categoryIds, description, userId);
        return ResponseEntity.ok(ApiResponse.success("上传成功", ebook));
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<ApiResponse<List<EBook>>> getByCategory(@PathVariable Long categoryId) {
        List<EBook> ebooks = ebookService.getEBooksByCategoryId(categoryId);
        return ResponseEntity.ok(ApiResponse.success("获取成功", ebooks));
    }

    @PutMapping("/{id}")
    @RequireAdmin
    @AuditLog(operationType = "UPDATE_EBOOK", targetType = "EBOOK", description = "更新电子书信息")
    public ResponseEntity<ApiResponse<EBook>> updateEBook(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String title = (String) body.get("title");
        String author = (String) body.get("author");
        String description = (String) body.get("description");
        @SuppressWarnings("unchecked")
        List<Long> categoryIds = body.get("categoryIds") != null
                ? ((List<?>) body.get("categoryIds")).stream()
                    .map(o -> Long.valueOf(o.toString())).toList()
                : null;
        EBook ebook = ebookService.updateEBook(id, title, author, categoryIds, description);
        return ResponseEntity.ok(ApiResponse.success("更新成功", ebook));
    }

    @PutMapping("/{id}/categories")
    @PreAuthorize("@permissionChecker.hasPermission('CATEGORY_ADD')")
    @AuditLog(operationType = "UPDATE_EBOOK_CATEGORIES", targetType = "EBOOK", description = "更新电子书分类")
    public ResponseEntity<ApiResponse<EBook>> updateCategories(@PathVariable Long id, @RequestBody List<Long> categoryIds) {
        return ResponseEntity.ok(ApiResponse.success("更新成功", ebookService.updateEBookCategories(id, categoryIds)));
    }

    @PostMapping("/{id}/cover")
    @RequireAdmin
    @AuditLog(operationType = "UPDATE_EBOOK_COVER", targetType = "EBOOK", description = "更新电子书封面")
    public ResponseEntity<ApiResponse<String>> uploadCover(@PathVariable Long id, @RequestParam("file") MultipartFile file) throws Exception {
        String coverPath = ebookService.updateEBookCover(id, file);
        return ResponseEntity.ok(ApiResponse.success("封面上传成功", coverPath));
    }

    @DeleteMapping("/{id}")
    @RequireAdmin
    @AuditLog(operationType = "DELETE", targetType = "EBOOK", description = "删除电子书")
    public ResponseEntity<ApiResponse<Void>> deleteEBook(@PathVariable Long id) {
        ebookService.deleteEBook(id);
        return ResponseEntity.ok(ApiResponse.success("删除成功", null));
    }

    // ========== 下载功能已禁用 ==========
    /**
     * 缓存电子书所有章节到DB并以TXT格式下载到本地
     */
    // @GetMapping("/{id}/cache-progress")
    // public ResponseEntity<ApiResponse<Integer>> getCacheProgress(@PathVariable Long id) {
    //     return ResponseEntity.ok(ApiResponse.success("获取成功", ebookService.getCacheProgress(id)));
    // }

    // @PostMapping("/{id}/cache")
    // public ResponseEntity<ApiResponse<String>> cacheEBook(@PathVariable Long id) throws IOException {
    //     String txtName = ebookService.cacheAndExportEBook(id);
    //     return ResponseEntity.ok(ApiResponse.success("缓存成功",
    //             "/ebooks/" + id + "/download/" + java.net.URLEncoder.encode(txtName, "UTF-8")));
    // }

    // @GetMapping("/{id}/download/{filename}")
    // public ResponseEntity<Resource> downloadEBook(@PathVariable Long id, @PathVariable String filename) {
    //     try {
    //         String decoded = java.net.URLDecoder.decode(filename, "UTF-8");
    //         Path projectRoot = Paths.get(System.getProperty("user.dir"));
    //         Path bookDir = projectRoot.resolve("src/main/resources/Book").toAbsolutePath().normalize();
    //         Path txtPath = bookDir.resolve(decoded).toAbsolutePath().normalize();
    //         // 防止路径穿越攻击
    //         if (!txtPath.startsWith(bookDir) || !Files.exists(txtPath)) {
    //             return ResponseEntity.notFound().build();
    //         }
    //         Resource resource = new InputStreamResource(new FileInputStream(txtPath.toFile()));
    //         String safeName = java.net.URLEncoder.encode(decoded, "UTF-8").replace("+", "%20");
    //         return ResponseEntity.ok()
    //                 .header(HttpHeaders.CONTENT_DISPOSITION,
    //                         "attachment; filename*=UTF-8''" + safeName)
    //                 .contentType(MediaType.TEXT_PLAIN)
    //                 .body(resource);
    //     } catch (Exception e) {
    //         return ResponseEntity.notFound().build();
    //     }
    // }

    // @PostMapping("/{id}/cache-and-download")
    // public ResponseEntity<?> cacheAndDownload(@PathVariable Long id) throws Exception {
    //     String txtName = ebookService.cacheAndExportEBook(id);
    //     Path projectRoot = Paths.get(System.getProperty("user.dir"));
    //     Path bookDir = projectRoot.resolve("src/main/resources/Book").toAbsolutePath().normalize();
    //     Path txtPath = bookDir.resolve(txtName).toAbsolutePath().normalize();
    //     if (!txtPath.startsWith(bookDir)) {
    //         return ResponseEntity.badRequest().build();
    //     }
    //     Resource resource = new InputStreamResource(new FileInputStream(txtPath.toFile()));
    //     return ResponseEntity.ok()
    //             .header(HttpHeaders.CONTENT_DISPOSITION,
    //                     "attachment; filename=\"" + new String(txtName.getBytes("UTF-8"), "ISO-8859-1") + "\"")
    //             .contentType(MediaType.TEXT_PLAIN)
    //             .body(resource);
    // }
}