package com.library.service;

import com.library.cache.RedisCacheHelper;
import com.library.cache.CacheDtoMapper;
import com.library.dto.cache.BookCacheDto;
import com.library.entity.Book;
import com.library.entity.Category;
import com.library.repository.BookRepository;
import com.library.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class BookService {

    private static final String CACHE_BOOKS = "books";
    private static final String CACHE_BOOK = "book";
    private static final String KEY_ALL = "all_v2";

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private RedisCacheHelper cache;

    /** 低库存阈值（可配置） */
    private static final int LOW_STOCK_THRESHOLD = 3;

    /** 查：全部图书 — 先缓存后数据库 */
    public List<Book> getAllBooks() {
        List<BookCacheDto> dtos = cache.getOrLoad(CACHE_BOOKS, KEY_ALL, () -> {
            List<Book> books = bookRepository.findAll();
            books.forEach(b -> b.getCategories().size());
            return CacheDtoMapper.toBookDtoList(books);
        });
        return CacheDtoMapper.toBookList(dtos);
    }

    /** 查：按 ID — 先缓存后数据库 */
    public Book getBookById(Long id) {
        BookCacheDto dto = cache.getOrLoad(CACHE_BOOK, id, () -> {
            Book book = bookRepository.findById(id).orElse(null);
            if (book == null) {
                return null;
            }
            book.getCategories().size();
            return CacheDtoMapper.toBookDto(book);
        });
        return CacheDtoMapper.toBook(dto);
    }

    /** 增：入库后写入单本缓存，并失效相关列表/搜索缓存 */
    public Book addBook(Book book) {
        if (book.getTitle() != null && book.getAuthor() != null
                && bookRepository.existsByTitleAndAuthor(book.getTitle(), book.getAuthor())) {
            throw new RuntimeException("已存在同名同作者的图书，请使用更新功能");
        }
        book.setAvailable(book.getStock());
        Book saved = bookRepository.save(book);
        saved.getCategories().size();
        cacheAfterAddBook(saved);
        return saved;
    }

    /** 改：更新数据库后同步单本缓存，并失效相关列表/搜索缓存 */
    public Book updateBook(Long id, Book book) {
        Book existingBook = bookRepository.findById(id).orElse(null);
        if (existingBook != null) {
            Set<Long> oldCategoryIds = existingBook.getCategories().stream()
                    .map(Category::getId).collect(Collectors.toSet());

            Integer oldStock = existingBook.getStock();
            Integer oldAvailable = existingBook.getAvailable();

            existingBook.setIsbn(book.getIsbn());
            existingBook.setTitle(book.getTitle());
            existingBook.setAuthor(book.getAuthor());
            existingBook.setPublisher(book.getPublisher());
            existingBook.setPublishDate(book.getPublishDate());
            existingBook.setPrice(book.getPrice());
            existingBook.setStock(book.getStock());

            if (book.getCategories() != null) {
                Set<Category> synced = new HashSet<>();
                for (Category cat : book.getCategories()) {
                    if (cat.getId() != null) {
                        categoryRepository.findById(cat.getId()).ifPresent(synced::add);
                    }
                }
                existingBook.setCategories(synced);
            }

            // 防止库存减少导致 available 为负数
            int safeOldStock = oldStock != null ? oldStock : 0;
            int safeOldAvailable = oldAvailable != null ? oldAvailable : 0;
            int booksOutOnLoan = Math.max(0, safeOldStock - safeOldAvailable);
            int newStock = book.getStock() != null ? book.getStock() : 0;
            if (newStock < booksOutOnLoan) {
                throw new RuntimeException("库存不能低于当前借出数量（" + booksOutOnLoan + " 本）");
            }
            existingBook.setAvailable(newStock - booksOutOnLoan);

            Book saved = bookRepository.save(existingBook);
            saved.getCategories().size();
            cacheAfterUpdateBook(saved, oldCategoryIds);
            return saved;
        }
        return null;
    }

    /** 改：更新图书分类后同步缓存 */
    public Book updateBookCategories(Long bookId, List<Long> categoryIds) {
        Book book = bookRepository.findById(bookId).orElse(null);
        if (book == null) {
            throw new RuntimeException("图书不存在");
        }
        Set<Long> oldCategoryIds = book.getCategories().stream()
                .map(Category::getId).collect(Collectors.toSet());
        Set<Category> categories = new HashSet<>();
        if (categoryIds != null) {
            for (Long catId : categoryIds) {
                categoryRepository.findById(catId).ifPresent(categories::add);
            }
        }
        book.setCategories(categories);
        Book saved = bookRepository.save(book);
        saved.getCategories().size();
        cacheAfterUpdateBook(saved, oldCategoryIds);
        return saved;
    }

    /** 改：更新封面后同步缓存 */
    public String updateBookCover(Long bookId, MultipartFile file) throws Exception {
        Book book = bookRepository.findById(bookId).orElse(null);
        if (book == null) {
            throw new RuntimeException("图书不存在");
        }

        com.library.util.ImageFileValidator.validate(file);

        String fn = file.getOriginalFilename();
        String ext = (fn != null && fn.contains("."))
                ? fn.substring(fn.lastIndexOf('.')).toLowerCase() : ".jpg";
        String name = "book_" + bookId + "_" + System.currentTimeMillis() + ext;

        Path dir = Paths.get(System.getProperty("user.dir"),
                "src", "main", "resources", "static", "covers");
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }

        String oldPath = book.getCoverPath();
        if (oldPath != null && oldPath.startsWith("/covers/")) {
            try {
                Files.deleteIfExists(dir.resolve(oldPath.substring(8)));
            } catch (Exception ignored) {}
        }

        Files.copy(file.getInputStream(), dir.resolve(name), StandardCopyOption.REPLACE_EXISTING);

        String path = "/covers/" + name;
        book.setCoverPath(path);
        Book saved = bookRepository.save(book);
        saved.getCategories().size();
        cacheAfterUpdateBook(saved, saved.getCategories().stream()
                .map(Category::getId).collect(Collectors.toSet()));
        return path;
    }

    /** 删：软删除 visible=0，并删除对应缓存 */
    public boolean deleteBook(Long id) {
        Book book = bookRepository.findById(id).orElse(null);
        if (book != null) {
            Set<Long> categoryIds = book.getCategories().stream()
                    .map(Category::getId).collect(Collectors.toSet());
            book.setVisible(0);
            bookRepository.save(book);
            cacheAfterDeleteBook(id, categoryIds);
            return true;
        }
        return false;
    }

    /** 查：按分类 — 先缓存后数据库 */
    public List<Book> getBooksByCategoryId(Long categoryId) {
        String key = "cat_v2_" + categoryId;
        List<BookCacheDto> dtos = cache.getOrLoad(CACHE_BOOKS, key, () -> {
            List<Book> books;
            if (categoryId == null) {
                books = bookRepository.findAll();
            } else {
                books = bookRepository.findByCategoryId(categoryId);
            }
            books.forEach(b -> b.getCategories().size());
            return CacheDtoMapper.toBookDtoList(books);
        });
        return CacheDtoMapper.toBookList(dtos);
    }

    /** 查：搜索 — 直查数据库，搜索属低频操作不宜缓存 */
    public List<Book> searchBooks(String keyword) {
        List<Book> books;
        if (keyword == null || keyword.trim().isEmpty()) {
            books = bookRepository.findAll();
        } else {
            books = bookRepository.searchBooks(keyword);
        }
        books.forEach(b -> b.getCategories().size());
        return books;
    }

    /** 增：批量导入 — 每条写入后同步缓存 */
    public List<Book> importFromTxt(MultipartFile file) throws Exception {
        List<Book> books = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), "UTF-8"))) {
            String line;
            boolean isFirstLine = true;
            while ((line = reader.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }

                String[] parts = line.split("\\|");
                if (parts.length >= 8) {
                    Book book = new Book();
                    book.setIsbn(parts[0].trim());
                    book.setTitle(parts[1].trim());
                    book.setAuthor(parts[2].trim());
                    book.setPublisher(parts[3].trim());
                    if (!parts[4].trim().isEmpty()) {
                        book.setPublishDate(LocalDate.parse(parts[4].trim(), DateTimeFormatter.ofPattern("yyyy-MM-dd")));
                    }
                    String catName = parts[5].trim();
                    if (!catName.isEmpty()) {
                        Category cat = categoryRepository.findByName(catName).orElse(null);
                        if (cat != null) {
                            book.getCategories().add(cat);
                        }
                    }
                    if (!parts[6].trim().isEmpty()) {
                        book.setPrice(new BigDecimal(parts[6].trim()));
                    }
                    book.setStock(Integer.parseInt(parts[7].trim()));
                    book.setAvailable(book.getStock());

                    Book existing = bookRepository.findByIsbn(book.getIsbn()).orElse(null);
                    if (existing == null) {
                        Book saved = bookRepository.save(book);
                        saved.getCategories().size();
                        cacheAfterAddBook(saved);
                        books.add(saved);
                    }
                }
            }
        }
        return books;
    }

    public void exportToTxt(PrintWriter writer) {
        List<Book> books = bookRepository.findAll();
        writer.println("ISBN|书名|作者|出版社|出版日期|分类|价格|库存");
        for (Book book : books) {
            writer.printf("%s|%s|%s|%s|%s|%s|%s|%d%n",
                    book.getIsbn() != null ? book.getIsbn() : "",
                    book.getTitle() != null ? book.getTitle() : "",
                    book.getAuthor() != null ? book.getAuthor() : "",
                    book.getPublisher() != null ? book.getPublisher() : "",
                    book.getPublishDate() != null ? book.getPublishDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) : "",
                    book.getCategories() != null ? book.getCategories().stream().map(Category::getName).collect(Collectors.joining(",")) : "",
                    book.getPrice() != null ? book.getPrice().toString() : "",
                    book.getStock() != null ? book.getStock() : 0);
        }
    }

    // ── 图书缓存：增 ──
    private void cacheAfterAddBook(Book saved) {
        cache.put(CACHE_BOOK, saved.getId(), CacheDtoMapper.toBookDto(saved));
        cache.evict(CACHE_BOOKS, KEY_ALL);
        cache.evictByKeyPrefix(CACHE_BOOKS, "search_v2_");
        saved.getCategories().forEach(cat -> cache.evict(CACHE_BOOKS, "cat_v2_" + cat.getId()));
    }

    // ── 图书缓存：改 ──
    private void cacheAfterUpdateBook(Book saved, Set<Long> oldCategoryIds) {
        cache.put(CACHE_BOOK, saved.getId(), CacheDtoMapper.toBookDto(saved));
        cache.evict(CACHE_BOOKS, KEY_ALL);
        cache.evictByKeyPrefix(CACHE_BOOKS, "search_v2_");
        Set<Long> allCategoryIds = new HashSet<>(oldCategoryIds);
        saved.getCategories().forEach(cat -> allCategoryIds.add(cat.getId()));
        allCategoryIds.forEach(catId -> cache.evict(CACHE_BOOKS, "cat_v2_" + catId));
    }

    // ── 图书缓存：删 ──
    private void cacheAfterDeleteBook(Long id, Set<Long> categoryIds) {
        cache.evict(CACHE_BOOK, id);
        cache.evict(CACHE_BOOKS, KEY_ALL);
        cache.evictByKeyPrefix(CACHE_BOOKS, "search_v2_");
        categoryIds.forEach(catId -> cache.evict(CACHE_BOOKS, "cat_v2_" + catId));
    }

    // ── 库存预警 ──
    public List<Book> getLowStockBooks() {
        return bookRepository.findLowStockBooks(LOW_STOCK_THRESHOLD);
    }

    public List<Book> getOutOfStockBooks() {
        return bookRepository.findOutOfStockBooks();
    }

    public Map<String, Object> getInventoryWarning() {
        Map<String, Object> warning = new HashMap<>();
        List<Book> lowStock = getLowStockBooks();
        List<Book> outOfStock = getOutOfStockBooks();
        warning.put("lowStockBooks", lowStock);
        warning.put("outOfStockBooks", outOfStock);
        warning.put("lowStockCount", lowStock.size());
        warning.put("outOfStockCount", outOfStock.size());
        warning.put("hasWarning", !lowStock.isEmpty() || !outOfStock.isEmpty());
        return warning;
    }
}
