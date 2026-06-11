package com.library.cache;

import com.library.dto.cache.*;
import com.library.entity.Book;
import com.library.entity.Category;
import com.library.entity.Chapter;
import com.library.entity.EBook;

import java.util.*;
import java.util.stream.Collectors;

/**
 * JPA 实体 ↔ 缓存 DTO 转换器。
 * Redis 只存 DTO，避免 PersistentSet 等 Hibernate 类型污染缓存。
 */
public final class CacheDtoMapper {

    private CacheDtoMapper() {}

    // ── Category ──

    public static CategoryCacheDto toCategoryDto(Category cat) {
        if (cat == null) return null;
        CategoryCacheDto dto = new CategoryCacheDto();
        dto.setId(cat.getId());
        dto.setName(cat.getName());
        dto.setDescription(cat.getDescription());
        dto.setParentId(cat.getParentId());
        dto.setCreateTime(cat.getCreateTime());
        dto.setUpdateTime(cat.getUpdateTime());
        return dto;
    }

    public static Category toCategory(CategoryCacheDto dto) {
        if (dto == null) return null;
        Category cat = new Category();
        cat.setId(dto.getId());
        cat.setName(dto.getName());
        cat.setDescription(dto.getDescription());
        cat.setParentId(dto.getParentId());
        cat.setCreateTime(dto.getCreateTime());
        cat.setUpdateTime(dto.getUpdateTime());
        return cat;
    }

    /** 扁平列表 → 树形结构 */
    public static List<CategoryCacheDto> buildCategoryTree(List<CategoryCacheDto> flatList) {
        Map<Long, CategoryCacheDto> map = new LinkedHashMap<>();
        List<CategoryCacheDto> roots = new ArrayList<>();
        for (CategoryCacheDto c : flatList) {
            c.setChildren(new ArrayList<>());
            map.put(c.getId(), c);
        }
        for (CategoryCacheDto c : flatList) {
            if (c.getParentId() != null && map.containsKey(c.getParentId())) {
                map.get(c.getParentId()).getChildren().add(c);
            } else {
                roots.add(c);
            }
        }
        return roots;
    }

    public static Set<CategoryCacheDto> toCategoryDtoSet(Collection<Category> categories) {
        if (categories == null || categories.isEmpty()) return new HashSet<>();
        return categories.stream()
                .map(CacheDtoMapper::toCategoryDto)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(HashSet::new));
    }

    public static Set<Category> toCategorySet(Collection<CategoryCacheDto> dtos) {
        if (dtos == null || dtos.isEmpty()) return new HashSet<>();
        return dtos.stream()
                .map(CacheDtoMapper::toCategory)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(HashSet::new));
    }

    // ── Book ──

    public static BookCacheDto toBookDto(Book book) {
        if (book == null) return null;
        BookCacheDto dto = new BookCacheDto();
        dto.setId(book.getId());
        dto.setIsbn(book.getIsbn());
        dto.setTitle(book.getTitle());
        dto.setAuthor(book.getAuthor());
        dto.setPublisher(book.getPublisher());
        dto.setPublishDate(book.getPublishDate());
        dto.setCoverPath(book.getCoverPath());
        dto.setCategories(toCategoryDtoSet(book.getCategories()));
        dto.setPrice(book.getPrice());
        dto.setStock(book.getStock());
        dto.setAvailable(book.getAvailable());
        dto.setCreateTime(book.getCreateTime());
        dto.setUpdateTime(book.getUpdateTime());
        dto.setVisible(book.getVisible());
        return dto;
    }

    public static Book toBook(BookCacheDto dto) {
        if (dto == null) return null;
        Book book = new Book();
        book.setId(dto.getId());
        book.setIsbn(dto.getIsbn());
        book.setTitle(dto.getTitle());
        book.setAuthor(dto.getAuthor());
        book.setPublisher(dto.getPublisher());
        book.setPublishDate(dto.getPublishDate());
        book.setCoverPath(dto.getCoverPath());
        book.setCategories(toCategorySet(dto.getCategories()));
        book.setPrice(dto.getPrice());
        book.setStock(dto.getStock());
        book.setAvailable(dto.getAvailable());
        book.setCreateTime(dto.getCreateTime());
        book.setUpdateTime(dto.getUpdateTime());
        book.setVisible(dto.getVisible());
        return book;
    }

    public static List<BookCacheDto> toBookDtoList(List<Book> books) {
        if (books == null) return List.of();
        return books.stream().map(CacheDtoMapper::toBookDto).collect(Collectors.toList());
    }

    public static List<Book> toBookList(List<BookCacheDto> dtos) {
        if (dtos == null) return List.of();
        return dtos.stream().map(CacheDtoMapper::toBook).collect(Collectors.toList());
    }

    // ── EBook ──

    public static EBookCacheDto toEBookDto(EBook ebook) {
        if (ebook == null) return null;
        EBookCacheDto dto = new EBookCacheDto();
        dto.setId(ebook.getId());
        dto.setBookId(ebook.getBookId());
        dto.setTitle(ebook.getTitle());
        dto.setAuthor(ebook.getAuthor());
        dto.setResourceName(ebook.getResourceName());
        dto.setFileType(ebook.getFileType());
        dto.setFileSize(ebook.getFileSize());
        dto.setCoverPath(ebook.getCoverPath());
        dto.setCategories(toCategoryDtoSet(ebook.getCategories()));
        dto.setDescription(ebook.getDescription());
        dto.setChapterCount(ebook.getChapterCount());
        dto.setStatus(ebook.getStatus());
        dto.setUploadUserId(ebook.getUploadUserId());
        dto.setCreateTime(ebook.getCreateTime());
        dto.setUpdateTime(ebook.getUpdateTime());
        dto.setVisible(ebook.getVisible());
        return dto;
    }

    public static EBook toEBook(EBookCacheDto dto) {
        if (dto == null) return null;
        EBook ebook = new EBook();
        ebook.setId(dto.getId());
        ebook.setBookId(dto.getBookId());
        ebook.setTitle(dto.getTitle());
        ebook.setAuthor(dto.getAuthor());
        ebook.setResourceName(dto.getResourceName());
        ebook.setFileType(dto.getFileType());
        ebook.setFileSize(dto.getFileSize());
        ebook.setCoverPath(dto.getCoverPath());
        ebook.setCategories(toCategorySet(dto.getCategories()));
        ebook.setDescription(dto.getDescription());
        ebook.setChapterCount(dto.getChapterCount());
        ebook.setStatus(dto.getStatus());
        ebook.setUploadUserId(dto.getUploadUserId());
        ebook.setCreateTime(dto.getCreateTime());
        ebook.setUpdateTime(dto.getUpdateTime());
        ebook.setVisible(dto.getVisible());
        return ebook;
    }

    public static List<EBookCacheDto> toEBookDtoList(List<EBook> ebooks) {
        if (ebooks == null) return List.of();
        return ebooks.stream().map(CacheDtoMapper::toEBookDto).collect(Collectors.toList());
    }

    public static List<EBook> toEBookList(List<EBookCacheDto> dtos) {
        if (dtos == null) return List.of();
        return dtos.stream().map(CacheDtoMapper::toEBook).collect(Collectors.toList());
    }

    // ── Chapter ──

    public static ChapterCacheDto toChapterDto(Chapter chapter) {
        if (chapter == null) return null;
        ChapterCacheDto dto = new ChapterCacheDto();
        dto.setId(chapter.getId());
        dto.setEbookId(chapter.getEbookId());
        dto.setChapterIndex(chapter.getChapterIndex());
        dto.setTitle(chapter.getTitle());
        dto.setWordCount(chapter.getWordCount());
        dto.setContent(chapter.getContent());
        dto.setContentCached(chapter.getContentCached());
        dto.setCreateTime(chapter.getCreateTime());
        dto.setUpdateTime(chapter.getUpdateTime());
        return dto;
    }

    public static Chapter toChapter(ChapterCacheDto dto) {
        if (dto == null) return null;
        Chapter chapter = new Chapter();
        chapter.setId(dto.getId());
        chapter.setEbookId(dto.getEbookId());
        chapter.setChapterIndex(dto.getChapterIndex());
        chapter.setTitle(dto.getTitle());
        chapter.setWordCount(dto.getWordCount());
        chapter.setContent(dto.getContent());
        chapter.setContentCached(dto.getContentCached());
        chapter.setCreateTime(dto.getCreateTime());
        chapter.setUpdateTime(dto.getUpdateTime());
        return chapter;
    }

    public static List<ChapterCacheDto> toChapterDtoList(List<Chapter> chapters) {
        if (chapters == null) return List.of();
        return chapters.stream().map(CacheDtoMapper::toChapterDto).collect(Collectors.toList());
    }

    public static List<Chapter> toChapterList(List<ChapterCacheDto> dtos) {
        if (dtos == null) return List.of();
        return dtos.stream().map(CacheDtoMapper::toChapter).collect(Collectors.toList());
    }
}
