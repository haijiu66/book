package com.library.controller;

import com.library.dto.ApiResponse;
import com.library.entity.Category;
import com.library.security.annotation.AuditLog;
import com.library.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ApiResponse<List<Category>> getAll(@RequestParam(required = false, defaultValue = "false") boolean tree) {
        if (tree) {
            return ApiResponse.success(categoryService.findTree());
        }
        return ApiResponse.success(categoryService.findAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<Category> getById(@PathVariable Long id) {
        return categoryService.findById(id)
                .map(c -> ApiResponse.success(c))
                .orElse(ApiResponse.notFound("分类不存在"));
    }

    @PostMapping
    @PreAuthorize("@permissionChecker.hasPermission('CATEGORY_ADD')")
    @AuditLog(operationType = "CREATE_CATEGORY", targetType = "CATEGORY", description = "创建分类")
    public ApiResponse<Category> create(@RequestBody Category category) {
        return ApiResponse.success("创建成功", categoryService.create(category));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@permissionChecker.hasPermission('CATEGORY_ADD')")
    @AuditLog(operationType = "UPDATE_CATEGORY", targetType = "CATEGORY", description = "更新分类")
    public ApiResponse<Category> update(@PathVariable Long id, @RequestBody Category category) {
        return ApiResponse.success("更新成功", categoryService.update(id, category));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@permissionChecker.hasPermission('CATEGORY_DELETE')")
    @AuditLog(operationType = "DELETE_CATEGORY", targetType = "CATEGORY", description = "删除分类")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return ApiResponse.success("删除成功", null);
    }
}
