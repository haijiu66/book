package com.library.controller;

import com.library.dto.ApiResponse;
import com.library.entity.Book;
import com.library.security.annotation.AuditLog;
import com.library.security.annotation.RequireAdmin;
import com.library.service.BookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/books")
@CrossOrigin(origins = "*")
public class BookController {
    @Autowired
    private BookService bookService;

    @GetMapping
    public ApiResponse<List<Book>> getAllBooks() {
        return ApiResponse.success(bookService.getAllBooks());
    }

    @GetMapping("/{id}")
    public ApiResponse<Book> getBookById(@PathVariable Long id) {
        Book book = bookService.getBookById(id);
        if (book != null) {
            return ApiResponse.success(book);
        }
        return ApiResponse.notFound("图书不存在");
    }

    @PostMapping
    @RequireAdmin
    @AuditLog(operationType = "CREATE_BOOK", targetType = "BOOK", description = "添加图书")
    public ApiResponse<Book> addBook(@RequestBody Book book) {
        Book saved = bookService.addBook(book);
        return ApiResponse.success("添加成功", saved);
    }

    @PutMapping("/{id}")
    @RequireAdmin
    @AuditLog(operationType = "UPDATE_BOOK", targetType = "BOOK", description = "更新图书")
    public ApiResponse<Book> updateBook(@PathVariable Long id, @RequestBody Book book) {
        Book updated = bookService.updateBook(id, book);
        if (updated != null) {
            return ApiResponse.success("更新成功", updated);
        }
        return ApiResponse.notFound("图书不存在");
    }

    @PutMapping("/{id}/categories")
    @PreAuthorize("@permissionChecker.hasPermission('CATEGORY_ADD')")
    @AuditLog(operationType = "UPDATE_BOOK_CATEGORIES", targetType = "BOOK", description = "更新图书分类")
    public ApiResponse<Book> updateCategories(@PathVariable Long id, @RequestBody List<Long> categoryIds) {
        return ApiResponse.success("更新成功", bookService.updateBookCategories(id, categoryIds));
    }

    @DeleteMapping("/{id}")
    @RequireAdmin
    @AuditLog(operationType = "DELETE_BOOK", targetType = "BOOK", description = "删除图书")
    public ApiResponse<Void> deleteBook(@PathVariable Long id) {
        boolean deleted = bookService.deleteBook(id);
        if (deleted) {
            return ApiResponse.success("删除成功", null);
        }
        return ApiResponse.notFound("图书不存在");
    }

    @GetMapping("/search")
    public ApiResponse<List<Book>> searchBooks(@RequestParam String keyword) {
        return ApiResponse.success(bookService.searchBooks(keyword));
    }

    @GetMapping("/category/{categoryId}")
    public ApiResponse<List<Book>> getByCategory(@PathVariable Long categoryId) {
        return ApiResponse.success(bookService.getBooksByCategoryId(categoryId));
    }

    @PostMapping("/import")
    @RequireAdmin
    @AuditLog(operationType = "IMPORT_BOOKS", targetType = "BOOK", description = "导入图书")
    public ApiResponse<List<Book>> importBooks(@RequestParam("file") MultipartFile file) throws Exception {
        List<Book> books = bookService.importFromTxt(file);
        return ApiResponse.success("成功导入 " + books.size() + " 本图书", books);
    }

    @PostMapping("/{id}/cover")
    @RequireAdmin
    @AuditLog(operationType = "UPDATE_BOOK_COVER", targetType = "BOOK", description = "更新图书封面")
    public ApiResponse<String> uploadCover(@PathVariable Long id, @RequestParam("file") MultipartFile file) throws Exception {
        String coverPath = bookService.updateBookCover(id, file);
        return ApiResponse.success("封面上传成功", coverPath);
    }

    @GetMapping("/export")
    @RequireAdmin
    @AuditLog(operationType = "EXPORT_BOOKS", targetType = "BOOK", description = "导出图书")
    public ResponseEntity<String> exportBooks() {
        try {
            StringWriter stringWriter = new StringWriter();
            PrintWriter writer = new PrintWriter(stringWriter);
            bookService.exportToTxt(writer);
            writer.flush();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(new MediaType("text", "plain"));
            headers.setContentDispositionFormData("attachment", "books.txt");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(stringWriter.toString());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("导出失败: " + e.getMessage());
        }
    }

    @GetMapping("/warning")
    @RequireAdmin
    public ApiResponse<Map<String, Object>> getInventoryWarning() {
        return ApiResponse.success("获取成功", bookService.getInventoryWarning());
    }
}
