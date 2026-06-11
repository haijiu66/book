package com.library.service;

import com.library.cache.RedisCacheHelper;
import com.library.cache.CacheDtoMapper;
import com.library.dto.cache.CategoryCacheDto;
import com.library.entity.Category;
import com.library.repository.BookRepository;
import com.library.repository.CategoryRepository;
import com.library.repository.EBookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private static final String CACHE_CATEGORIES = "categories";
    private static final String KEY_ALL = "all_v2";

    private final CategoryRepository categoryRepository;
    private final BookRepository bookRepository;
    private final EBookRepository ebookRepository;
    private final RedisCacheHelper cache;

    /** 查：全部分类（扁平列表）— 先缓存后数据库 */
    public List<Category> findAll() {
        List<CategoryCacheDto> dtos = cache.getOrLoad(CACHE_CATEGORIES, KEY_ALL, () ->
                categoryRepository.findAll().stream()
                        .map(CacheDtoMapper::toCategoryDto)
                        .collect(Collectors.toList()));
        return dtos.stream().map(CacheDtoMapper::toCategory).collect(Collectors.toList());
    }

    /** 查：全部分类（树形结构） */
    public List<Category> findTree() {
        List<CategoryCacheDto> dtos = cache.getOrLoad(CACHE_CATEGORIES, KEY_ALL, () ->
                categoryRepository.findAll().stream()
                        .map(CacheDtoMapper::toCategoryDto)
                        .collect(Collectors.toList()));
        List<CategoryCacheDto> tree = CacheDtoMapper.buildCategoryTree(dtos);
        return tree.stream().map(CacheDtoMapper::toCategory).collect(Collectors.toList());
    }

    /** 查：按 ID — 先缓存后数据库 */
    public Optional<Category> findById(Long id) {
        CategoryCacheDto cached = cache.get(CACHE_CATEGORIES, "id_" + id);
        if (cached != null) {
            return Optional.of(CacheDtoMapper.toCategory(cached));
        }
        Optional<Category> found = categoryRepository.findById(id);
        found.ifPresent(cat -> cache.put(CACHE_CATEGORIES, "id_" + id, CacheDtoMapper.toCategoryDto(cat)));
        return found;
    }

    /** 增：入库后同步写入缓存 */
    public Category create(Category category) {
        if (categoryRepository.existsByName(category.getName())) {
            throw new RuntimeException("分类名称已存在");
        }
        if (category.getParentId() != null && !categoryRepository.existsById(category.getParentId())) {
            throw new RuntimeException("父级分类不存在");
        }
        Category saved = categoryRepository.save(category);
        cacheAfterAddCategory(saved);
        return saved;
    }

    /** 改：更新数据库后同步缓存 */
    public Category update(Long id, Category category) {
        Category existing = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("分类不存在"));
        if (!existing.getName().equals(category.getName())
                && categoryRepository.existsByName(category.getName())) {
            throw new RuntimeException("分类名称已存在");
        }
        if (category.getParentId() != null && category.getParentId().equals(id)) {
            throw new RuntimeException("不能将自身设为父级分类");
        }
        if (category.getParentId() != null && !categoryRepository.existsById(category.getParentId())) {
            throw new RuntimeException("父级分类不存在");
        }
        existing.setName(category.getName());
        existing.setDescription(category.getDescription());
        existing.setParentId(category.getParentId());
        Category saved = categoryRepository.save(existing);
        cacheAfterUpdateCategory(saved);
        return saved;
    }

    /** 删：物理删除并清除对应缓存 */
    public void delete(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new RuntimeException("分类不存在");
        }
        if (!bookRepository.findByCategoryId(id).isEmpty()) {
            throw new RuntimeException("该分类下有图书正在使用，请先解除关联后再删除");
        }
        if (!ebookRepository.findByCategoryId(id).isEmpty()) {
            throw new RuntimeException("该分类下有电子书正在使用，请先解除关联后再删除");
        }
        if (categoryRepository.existsByParentId(id)) {
            throw new RuntimeException("该分类下有子分类，请先删除子分类");
        }
        categoryRepository.deleteById(id);
        cacheAfterDeleteCategory(id);
    }

    private void cacheAfterAddCategory(Category saved) {
        cache.put(CACHE_CATEGORIES, "id_" + saved.getId(), CacheDtoMapper.toCategoryDto(saved));
        cache.evict(CACHE_CATEGORIES, KEY_ALL);
    }

    private void cacheAfterUpdateCategory(Category saved) {
        cache.put(CACHE_CATEGORIES, "id_" + saved.getId(), CacheDtoMapper.toCategoryDto(saved));
        cache.evict(CACHE_CATEGORIES, KEY_ALL);
    }

    private void cacheAfterDeleteCategory(Long id) {
        cache.evict(CACHE_CATEGORIES, "id_" + id);
        cache.evict(CACHE_CATEGORIES, KEY_ALL);
    }
}
