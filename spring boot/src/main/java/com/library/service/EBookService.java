package com.library.service;

import com.library.entity.Category;
import com.library.entity.Chapter;
import com.library.entity.EBook;
import com.library.repository.CategoryRepository;
import com.library.repository.EBookRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import com.library.cache.RedisCacheHelper;
import com.library.cache.CacheDtoMapper;
import com.library.dto.cache.ChapterCacheDto;
import com.library.dto.cache.EBookCacheDto;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.ResultSet;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
public class EBookService {

    private static final String CACHE_EBOOKS = "ebooks";
    private static final String CACHE_EBOOK = "ebook";
    private static final String KEY_ALL = "all_v2";

    private final EBookRepository ebookRepository;
    private final CategoryRepository categoryRepository;
    private final RedisCacheHelper cache;
    private final JdbcTemplate jdbc;

    public EBookService(EBookRepository ebookRepository, CategoryRepository categoryRepository,
                        RedisCacheHelper cache, @Qualifier("primaryJdbcTemplate") JdbcTemplate jdbc) {
        this.ebookRepository = ebookRepository;
        this.categoryRepository = categoryRepository;
        this.cache = cache;
        this.jdbc = jdbc;
    }

    private String chapterTable(Long ebookId) {
        return "ebook_chapters_" + ebookId;
    }

    /** 缓存下载进度: ebookId → 百分比(0-100) */
    private final Map<Long, Integer> cacheProgressMap = new ConcurrentHashMap<>();

    public Integer getCacheProgress(Long ebookId) {
        return cacheProgressMap.getOrDefault(ebookId, 100);
    }

    private static final String BOOK_RESOURCE_DIR = "Book";

    private static final Pattern CHAPTER_PATTERN = Pattern.compile(
        "^(第\\s*[0-9零一二三四五六七八九十百千两]+\\s*[章节卷集篇部回]|chapter\\s*\\d+|section\\s*\\d+).*$",
        Pattern.CASE_INSENSITIVE
    );

    private static final Charset GBK_CHARSET = Charset.forName("GBK");

    private Path getResourceBookPath() {
        try {
            Path projectRoot = Paths.get(System.getProperty("user.dir"));
            Path resourcePath = projectRoot.resolve("src").resolve("main").resolve("resources").resolve(BOOK_RESOURCE_DIR);

            if (!Files.exists(resourcePath)) {
                Files.createDirectories(resourcePath);
                log.info("创建资源目录: {}", resourcePath);
            }

            return resourcePath;
        } catch (Exception e) {
            log.error("创建资源目录失败", e);
            throw new RuntimeException("创建资源目录失败: " + e.getMessage());
        }
    }

    /** 查：全部电子书 — 先缓存后数据库 */
    public List<EBook> getAllEBooks() {
        List<EBookCacheDto> dtos = cache.getOrLoad(CACHE_EBOOKS, KEY_ALL, () -> {
            List<EBook> ebooks = ebookRepository.findAll();
            ebooks.forEach(e -> e.getCategories().size());
            return CacheDtoMapper.toEBookDtoList(ebooks);
        });
        return CacheDtoMapper.toEBookList(dtos);
    }

    /** 查：按 ID — 先缓存后数据库 */
    public EBook getEBookById(Long id) {
        EBookCacheDto dto = cache.getOrLoad(CACHE_EBOOK, id, () -> {
            EBook ebook = ebookRepository.findById(id).orElse(null);
            if (ebook == null) {
                return null;
            }
            ebook.getCategories().size();
            return CacheDtoMapper.toEBookDto(ebook);
        });
        return CacheDtoMapper.toEBook(dto);
    }

    private Charset detectCharset(File file) throws IOException {
        byte[] buffer = new byte[4096];
        FileInputStream fis = new FileInputStream(file);
        int bytesRead = fis.read(buffer);
        fis.close();

        if (bytesRead <= 0) {
            return StandardCharsets.UTF_8;
        }

        if (bytesRead >= 3 && buffer[0] == 0xEF && buffer[1] == 0xBB && buffer[2] == 0xBF) {
            return StandardCharsets.UTF_8;
        }
        if (bytesRead >= 2 && buffer[0] == 0xFF && buffer[1] == 0xFE) {
            return StandardCharsets.UTF_16LE;
        }
        if (bytesRead >= 2 && buffer[0] == 0xFE && buffer[1] == 0xFF) {
            return StandardCharsets.UTF_16BE;
        }

        boolean isUtf8 = true;
        int i = 0;
        while (i < bytesRead) {
            byte b = buffer[i];
            if ((b & 0x80) == 0) {
                i++;
            } else if ((b & 0xE0) == 0xC0) {
                if (i + 1 >= bytesRead || (buffer[i + 1] & 0xC0) != 0x80) {
                    isUtf8 = false;
                    break;
                }
                i += 2;
            } else if ((b & 0xF0) == 0xE0) {
                if (i + 2 >= bytesRead || (buffer[i + 1] & 0xC0) != 0x80 || (buffer[i + 2] & 0xC0) != 0x80) {
                    isUtf8 = false;
                    break;
                }
                i += 3;
            } else if ((b & 0xF8) == 0xF0) {
                if (i + 3 >= bytesRead || (buffer[i + 1] & 0xC0) != 0x80 || (buffer[i + 2] & 0xC0) != 0x80 || (buffer[i + 3] & 0xC0) != 0x80) {
                    isUtf8 = false;
                    break;
                }
                i += 4;
            } else {
                isUtf8 = false;
                break;
            }
        }

        if (isUtf8) {
            log.info("检测到文件编码: UTF-8");
            return StandardCharsets.UTF_8;
        }

        log.info("检测到文件编码: GBK (ANSI)");
        return GBK_CHARSET;
    }

    private BufferedReader createReader(File file) throws IOException {
        Charset charset = detectCharset(file);
        return new BufferedReader(new InputStreamReader(new FileInputStream(file), charset));
    }

    @Transactional
    public EBook uploadEBook(MultipartFile file, String title, String author, List<Long> categoryIds, String description, Long userId) throws IOException {
        Path bookPath = getResourceBookPath();

        String originalFilename = file.getOriginalFilename();
        String fileType = getFileExtension(originalFilename);

        // 仅支持 TXT 和 DOCX 格式
        String lowerType = fileType != null ? fileType.toLowerCase() : "";
        if (!"txt".equals(lowerType) && !"docx".equals(lowerType)) {
            throw new RuntimeException("仅支持 TXT 和 DOCX 格式的电子书上传");
        }

        String tempFilename = "temp_" + System.currentTimeMillis() + "." + fileType;
        Path tempFilePath = bookPath.resolve(tempFilename);
        Files.copy(file.getInputStream(), tempFilePath, StandardCopyOption.REPLACE_EXISTING);

        List<ParsedChapter> parsedChapters = parseFile(tempFilePath.toFile(), fileType.toLowerCase());
        String detectedTitle = detectTitle(tempFilePath.toFile(), fileType.toLowerCase());

        String resolvedTitle = resolveTitle(title, detectedTitle, originalFilename);

        if (author != null && !author.isBlank()
                && ebookRepository.existsByTitleAndAuthor(resolvedTitle, author)) {
            throw new RuntimeException("已存在同名同作者的电子书，请使用更新功能");
        }

        String safeFilename = sanitizeFilename(resolvedTitle) + "." + fileType;
        Path finalFilePath = bookPath.resolve(safeFilename);

        Files.move(tempFilePath, finalFilePath, StandardCopyOption.REPLACE_EXISTING);
        log.info("电子书文件已保存: {}", finalFilePath);

        EBook ebook = new EBook();
        ebook.setTitle(resolvedTitle);
        ebook.setAuthor(author);
        if (categoryIds != null) {
            for (Long catId : categoryIds) {
                EBook finalEbook = ebook;
                categoryRepository.findById(catId).ifPresent(c -> finalEbook.getCategories().add(c));
            }
        }
        ebook.setDescription(description);
        ebook.setResourceName(safeFilename);
        ebook.setFileType(fileType.toLowerCase());
        ebook.setFileSize(file.getSize());
        ebook.setChapterCount(parsedChapters.size());
        ebook.setUploadUserId(userId);
        ebook.setStatus("ACTIVE");
        ebook = ebookRepository.save(ebook);

        // 创建独立章节表
        String table = chapterTable(ebook.getId());
        jdbc.execute("DROP TABLE IF EXISTS " + table);
        jdbc.execute("CREATE TABLE " + table + " (" +
                "id BIGINT PRIMARY KEY, " +
                "chapter_index INT NOT NULL, " +
                "title VARCHAR(500), " +
                "content LONGTEXT, " +
                "content_cached BOOLEAN DEFAULT TRUE, " +
                "word_count INT DEFAULT 0)");

        for (int i = 0; i < parsedChapters.size(); i++) {
            ParsedChapter pc = parsedChapters.get(i);
            long chapterId = ebook.getId() * 1000000L + i;
            jdbc.update("INSERT INTO " + table + " (id, chapter_index, title, content, content_cached, word_count) VALUES (?,?,?,?,?,?)",
                    chapterId, i, pc.title, pc.content, true, pc.content.length());
        }

        ebook.getCategories().size();
        cacheAfterAddEBook(ebook);
        log.info("电子书上传成功: id={}, title={}, file={}, chapters={}", ebook.getId(), ebook.getTitle(), safeFilename, parsedChapters.size());
        return ebook;
    }

    public Chapter loadChapterContent(Long ebookId, int chapterIndex) {
        String table = chapterTable(ebookId);
        List<Chapter> rows = jdbc.query(
                "SELECT id, chapter_index, title, content, content_cached, word_count FROM " + table + " WHERE chapter_index = ?",
                (ResultSet rs, int rowNum) -> {
                    Chapter ch = new Chapter();
                    ch.setId(rs.getLong("id"));
                    ch.setEbookId(ebookId);
                    ch.setChapterIndex(rs.getInt("chapter_index"));
                    ch.setTitle(rs.getString("title"));
                    ch.setContent(rs.getString("content"));
                    ch.setContentCached(rs.getBoolean("content_cached"));
                    ch.setWordCount(rs.getInt("word_count"));
                    return ch;
                }, chapterIndex);

        if (rows.isEmpty()) {
            // 表可能不存在，从文件读取
            return readChapterFromFileDirect(ebookId, chapterIndex);
        }

        Chapter chapter = rows.get(0);
        if (chapter.getContentCached() != null && chapter.getContentCached()
                && chapter.getContent() != null && !chapter.getContent().isEmpty()) {
            return chapter;
        }

        // 从文件回填
        EBook ebook = ebookRepository.findById(ebookId).orElse(null);
        if (ebook != null) {
            Path filePath = getResourceBookPath().resolve(ebook.getResourceName());
            if (Files.exists(filePath)) {
                try {
                    String content = readChapterFromFile(filePath, ebook.getFileType(), chapterIndex);
                    chapter.setContent(content);
                    chapter.setContentCached(true);
                    chapter.setWordCount(content != null ? content.length() : 0);
                    jdbc.update("UPDATE " + table + " SET content=?, content_cached=TRUE, word_count=? WHERE chapter_index=?",
                            content, chapter.getWordCount(), chapterIndex);
                } catch (Exception e) {
                    log.error("回填章节失败: ebookId={}, chapterIndex={}", ebookId, chapterIndex, e);
                }
            }
        }
        return chapter;
    }

    private Chapter readChapterFromFileDirect(Long ebookId, int chapterIndex) {
        Chapter ch = new Chapter();
        ch.setEbookId(ebookId);
        ch.setChapterIndex(chapterIndex);
        ch.setTitle("第" + (chapterIndex + 1) + "章");
        try {
            EBook ebook = ebookRepository.findById(ebookId).orElse(null);
            if (ebook != null) {
                Path filePath = getResourceBookPath().resolve(ebook.getResourceName());
                if (Files.exists(filePath)) {
                    ch.setContent(readChapterFromFile(filePath, ebook.getFileType(), chapterIndex));
                }
            }
        } catch (Exception e) {
            ch.setContent("加载失败: " + e.getMessage());
        }
        return ch;
    }

    private String readChapterFromFile(Path filePath, String fileType, int targetIndex) throws IOException {
        if ("txt".equals(fileType)) {
            return readTxtChapter(filePath.toFile(), targetIndex);
        } else if ("docx".equals(fileType)) {
            return readDocxChapter(filePath, targetIndex);
        }
        throw new IllegalArgumentException("不支持的文件格式: " + fileType);
    }

    private String readTxtChapter(File file, int targetIndex) throws IOException {
        List<String> chapterLines = new ArrayList<>();
        int currentIndex = 0;

        try (BufferedReader reader = createReader(file)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                Matcher matcher = CHAPTER_PATTERN.matcher(trimmed);

                if (matcher.matches()) {
                    if (currentIndex == targetIndex && !chapterLines.isEmpty()) {
                        break;
                    }
                    currentIndex++;
                } else if (currentIndex == targetIndex) {
                    chapterLines.add(trimmed);
                }
            }
        }

        if (currentIndex < targetIndex) {
            return "";
        }

        return chapterLines.stream().collect(Collectors.joining("\n"));
    }

    private String readDocxChapter(Path filePath, int targetIndex) throws IOException {
        List<String> paragraphs = new ArrayList<>();
        int currentIndex = 0;

        try (FileInputStream fis = new FileInputStream(filePath.toFile());
             XWPFDocument document = new XWPFDocument(fis)) {

            for (XWPFParagraph paragraph : document.getParagraphs()) {
                String text = paragraph.getText() == null ? "" : paragraph.getText().trim();
                if (text.isEmpty()) continue;

                if (isDocxHeading(paragraph, text)) {
                    if (currentIndex == targetIndex && !paragraphs.isEmpty()) {
                        break;
                    }
                    currentIndex++;
                } else if (currentIndex == targetIndex) {
                    paragraphs.add(text);
                }
            }
        }

        return paragraphs.stream().collect(Collectors.joining("\n"));
    }

    private List<ParsedChapter> parseFile(File file, String fileType) throws IOException {
        if ("txt".equals(fileType)) {
            return parseTxtFile(file);
        } else if ("docx".equals(fileType)) {
            return parseDocxFile(file);
        }
        throw new IllegalArgumentException("不支持的文件格式: " + fileType);
    }

    private List<ParsedChapter> parseTxtFile(File file) throws IOException {
        List<ParsedChapter> chapters = new ArrayList<>();
        String currentTitle = "正文";
        List<String> currentLines = new ArrayList<>();

        try (BufferedReader reader = createReader(file)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                Matcher matcher = CHAPTER_PATTERN.matcher(trimmed);

                if (matcher.matches()) {
                    if (!currentLines.isEmpty()) {
                        chapters.add(new ParsedChapter(currentTitle,
                            currentLines.stream().collect(Collectors.joining("\n"))));
                    }
                    currentTitle = trimmed;
                    currentLines = new ArrayList<>();
                } else {
                    currentLines.add(trimmed);
                }
            }
        }

        if (!currentLines.isEmpty() || chapters.isEmpty()) {
            chapters.add(new ParsedChapter(currentTitle,
                currentLines.stream().collect(Collectors.joining("\n"))));
        }

        log.info("TXT文件解析完成: chapters={}", chapters.size());
        return chapters;
    }

    private List<ParsedChapter> parseDocxFile(File file) throws IOException {
        List<ParsedChapter> chapters = new ArrayList<>();
        String currentTitle = "正文";
        List<String> currentParagraphs = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(file);
             XWPFDocument document = new XWPFDocument(fis)) {

            for (XWPFParagraph paragraph : document.getParagraphs()) {
                String text = paragraph.getText() == null ? "" : paragraph.getText().trim();
                if (text.isEmpty()) continue;

                if (isDocxHeading(paragraph, text)) {
                    if (!currentParagraphs.isEmpty()) {
                        chapters.add(new ParsedChapter(currentTitle,
                            currentParagraphs.stream().collect(Collectors.joining("\n"))));
                    }
                    currentTitle = text;
                    currentParagraphs = new ArrayList<>();
                } else {
                    currentParagraphs.add(text);
                }
            }
        }

        if (!currentParagraphs.isEmpty() || chapters.isEmpty()) {
            chapters.add(new ParsedChapter(currentTitle,
                currentParagraphs.stream().collect(Collectors.joining("\n"))));
        }

        log.info("DOCX文件解析完成: chapters={}", chapters.size());
        return chapters;
    }

    private String detectTitle(File file, String fileType) throws IOException {
        if ("txt".equals(fileType)) {
            try (BufferedReader reader = createReader(file)) {
                String line = reader.readLine();
                if (line != null && looksLikeBookTitle(line.trim())) {
                    return line.trim();
                }
            }
        } else if ("docx".equals(fileType)) {
            try (FileInputStream fis = new FileInputStream(file);
                 XWPFDocument document = new XWPFDocument(fis)) {
                for (XWPFParagraph paragraph : document.getParagraphs()) {
                    String text = paragraph.getText() == null ? "" : paragraph.getText().trim();
                    if (!text.isEmpty() && looksLikeBookTitle(text)) {
                        return text;
                    }
                }
            }
        }
        return null;
    }

    /** 查：章节列表 — 先缓存后数据库 */
    public List<Chapter> getChaptersByEBookId(Long ebookId) {
        String key = "chapters_" + ebookId;
        List<ChapterCacheDto> dtos = cache.getOrLoad(CACHE_EBOOK, key, () -> {
            String table = chapterTable(ebookId);
            try {
                List<Chapter> chapters = jdbc.query(
                        "SELECT id, chapter_index, title, content_cached, word_count FROM " + table + " ORDER BY chapter_index",
                        (ResultSet rs, int rowNum) -> {
                            Chapter ch = new Chapter();
                            ch.setId(rs.getLong("id"));
                            ch.setEbookId(ebookId);
                            ch.setChapterIndex(rs.getInt("chapter_index"));
                            ch.setTitle(rs.getString("title"));
                            ch.setContentCached(rs.getBoolean("content_cached"));
                            ch.setWordCount(rs.getInt("word_count"));
                            return ch;
                        });
                return CacheDtoMapper.toChapterDtoList(chapters);
            } catch (Exception e) {
                log.warn("章节表不存在: {}", table);
                return List.of();
            }
        });
        return CacheDtoMapper.toChapterList(dtos);
    }

    /** 查：按章节索引 — 先缓存后数据库 */
    public Chapter getChapterByIndex(Long ebookId, int index) {
        String key = "ch_" + ebookId + "_" + index;
        ChapterCacheDto dto = cache.getOrLoad(CACHE_EBOOK, key, () ->
                CacheDtoMapper.toChapterDto(loadChapterContent(ebookId, index)));
        return CacheDtoMapper.toChapter(dto);
    }

    /** 查：按章节 ID — 先缓存后数据库 */
    public Chapter getChapterById(Long chapterId) {
        String key = "ch_id_" + chapterId;
        ChapterCacheDto dto = cache.getOrLoad(CACHE_EBOOK, key, () -> {
            long ebookId = chapterId / 1000000L;
            int chapterIndex = (int) (chapterId % 1000000L);
            return CacheDtoMapper.toChapterDto(loadChapterContent(ebookId, chapterIndex));
        });
        return CacheDtoMapper.toChapter(dto);
    }

    /** 改：更新电子书分类后同步缓存 */
    public EBook updateEBookCategories(Long ebookId, List<Long> categoryIds) {
        EBook ebook = ebookRepository.findById(ebookId).orElse(null);
        if (ebook == null) {
            throw new RuntimeException("电子书不存在");
        }
        Set<Long> oldCategoryIds = ebook.getCategories().stream()
                .map(Category::getId).collect(Collectors.toSet());
        Set<Category> categories = new HashSet<>();
        if (categoryIds != null) {
            for (Long catId : categoryIds) {
                categoryRepository.findById(catId).ifPresent(categories::add);
            }
        }
        ebook.setCategories(categories);
        EBook saved = ebookRepository.save(ebook);
        saved.getCategories().size();
        cacheAfterUpdateEBook(saved, oldCategoryIds);
        return saved;
    }

    /** 改：更新电子书信息后同步缓存 */
    public EBook updateEBook(Long id, String title, String author, List<Long> categoryIds, String description) {
        EBook ebook = ebookRepository.findById(id).orElse(null);
        if (ebook == null) {
            throw new RuntimeException("电子书不存在");
        }
        Set<Long> oldCategoryIds = ebook.getCategories().stream()
                .map(Category::getId).collect(Collectors.toSet());
        if (title != null && !title.isBlank()) {
            ebook.setTitle(title.trim());
        }
        if (author != null) {
            ebook.setAuthor(author.trim());
        }
        if (description != null) {
            ebook.setDescription(description.trim());
        }
        if (categoryIds != null) {
            Set<Category> categories = new HashSet<>();
            for (Long catId : categoryIds) {
                categoryRepository.findById(catId).ifPresent(categories::add);
            }
            ebook.setCategories(categories);
        }
        EBook saved = ebookRepository.save(ebook);
        saved.getCategories().size();
        cacheAfterUpdateEBook(saved, oldCategoryIds);
        return saved;
    }

    /** 改：更新封面后同步缓存 */
    public String updateEBookCover(Long ebookId, MultipartFile file) throws Exception {
        EBook ebook = ebookRepository.findById(ebookId).orElse(null);
        if (ebook == null) throw new RuntimeException("电子书不存在");

        com.library.util.ImageFileValidator.validate(file);

        String fn = file.getOriginalFilename();
        String ext = (fn != null && fn.contains("."))
                ? fn.substring(fn.lastIndexOf('.')).toLowerCase() : ".jpg";
        String name = "ebook_" + ebookId + "_" + System.currentTimeMillis() + ext;

        Path dir = Paths.get(System.getProperty("user.dir"),
                "src", "main", "resources", "static", "covers");
        if (!Files.exists(dir)) Files.createDirectories(dir);

        String oldPath = ebook.getCoverPath();
        if (oldPath != null && oldPath.startsWith("/covers/")) {
            try { Files.deleteIfExists(dir.resolve(oldPath.substring(8))); } catch (Exception ignored) {}
        }

        Files.copy(file.getInputStream(), dir.resolve(name), StandardCopyOption.REPLACE_EXISTING);
        String path = "/covers/" + name;
        ebook.setCoverPath(path);
        EBook saved = ebookRepository.save(ebook);
        saved.getCategories().size();
        cacheAfterUpdateEBook(saved, saved.getCategories().stream()
                .map(Category::getId).collect(Collectors.toSet()));
        return path;
    }

    /** 查：按分类 — 先缓存后数据库 */
    public List<EBook> getEBooksByCategoryId(Long categoryId) {
        String key = "cat_v2_" + categoryId;
        List<EBookCacheDto> dtos = cache.getOrLoad(CACHE_EBOOKS, key, () -> {
            List<EBook> ebooks;
            if (categoryId == null) {
                ebooks = ebookRepository.findAll();
            } else {
                ebooks = ebookRepository.findByCategoryId(categoryId);
            }
            ebooks.forEach(e -> e.getCategories().size());
            return CacheDtoMapper.toEBookDtoList(ebooks);
        });
        return CacheDtoMapper.toEBookList(dtos);
    }

    @Transactional
    public String cacheAndExportEBook(Long ebookId) throws IOException {
        EBook ebook = ebookRepository.findById(ebookId).orElse(null);
        if (ebook == null) throw new RuntimeException("电子书不存在");

        cacheProgressMap.put(ebookId, 0);
        Path sourcePath = getResourceBookPath().resolve(ebook.getResourceName());
        List<ParsedChapter> parsed = parseFile(sourcePath.toFile(), ebook.getFileType().toLowerCase());
        String table = chapterTable(ebookId);
        cacheProgressMap.put(ebookId, 10);

        // 确保表存在
        try {
            jdbc.execute("CREATE TABLE IF NOT EXISTS " + table + " (" +
                    "id BIGINT PRIMARY KEY, chapter_index INT NOT NULL, title VARCHAR(500), " +
                    "content LONGTEXT, content_cached BOOLEAN DEFAULT TRUE, word_count INT DEFAULT 0)");
        } catch (Exception ignored) {}

        StringBuilder fullText = new StringBuilder();
        fullText.append(ebook.getTitle()).append("\n");
        fullText.append("作者：").append(ebook.getAuthor() != null ? ebook.getAuthor() : "佚名").append("\n\n");

        int total = parsed.size();
        for (int i = 0; i < total; i++) {
            ParsedChapter pc = parsed.get(i);
            long chapterId = ebookId * 1000000L + i;
            jdbc.update("REPLACE INTO " + table + " (id, chapter_index, title, content, content_cached, word_count) VALUES (?,?,?,?,?,?)",
                    chapterId, i, pc.title, pc.content, true, pc.content.length());

            fullText.append(pc.title).append("\n\n");
            fullText.append(pc.content).append("\n\n");
            cacheProgressMap.put(ebookId, 10 + (int) (80.0 * (i + 1) / total));
        }

        cacheProgressMap.put(ebookId, 90);
        String txtName = sanitizeFilename(ebook.getTitle()) + ".txt";
        Path txtPath = getResourceBookPath().resolve(txtName);
        // 写 UTF-8 BOM + 内容，确保 Windows 记事本正确识别中文
        try (OutputStream os = Files.newOutputStream(txtPath)) {
            os.write(0xEF);
            os.write(0xBB);
            os.write(0xBF);
            os.write(fullText.toString().getBytes(StandardCharsets.UTF_8));
        }

        cache.evict(CACHE_EBOOK, "chapters_" + ebookId);
        cache.evictByKeyPrefix(CACHE_EBOOK, "ch_" + ebookId + "_");

        ebook.setChapterCount(total);
        EBook saved = ebookRepository.save(ebook);
        saved.getCategories().size();
        cache.put(CACHE_EBOOK, ebookId, CacheDtoMapper.toEBookDto(saved));
        cacheProgressMap.put(ebookId, 100);
        log.info("电子书已缓存并导出: ebookId={}, chapters={}, path={}", ebookId, total, txtPath);
        return txtName;
    }

    /** 删：软删除 visible=0，并删除对应缓存 */
    @Transactional
    public void deleteEBook(Long id) {
        EBook ebook = ebookRepository.findById(id).orElse(null);
        if (ebook != null) {
            Set<Long> categoryIds = ebook.getCategories().stream()
                    .map(Category::getId).collect(Collectors.toSet());
            ebook.setVisible(0);
            ebookRepository.save(ebook);
            jdbc.execute("DROP TABLE IF EXISTS " + chapterTable(id));
            cacheAfterDeleteEBook(id, categoryIds);
        }
    }

    // ── 电子书缓存：增 ──
    private void cacheAfterAddEBook(EBook saved) {
        cache.put(CACHE_EBOOK, saved.getId(), CacheDtoMapper.toEBookDto(saved));
        cache.evict(CACHE_EBOOKS, KEY_ALL);
        saved.getCategories().forEach(cat -> cache.evict(CACHE_EBOOKS, "cat_v2_" + cat.getId()));
    }

    // ── 电子书缓存：改 ──
    private void cacheAfterUpdateEBook(EBook saved, Set<Long> oldCategoryIds) {
        cache.put(CACHE_EBOOK, saved.getId(), CacheDtoMapper.toEBookDto(saved));
        cache.evict(CACHE_EBOOKS, KEY_ALL);
        Set<Long> allCategoryIds = new HashSet<>(oldCategoryIds);
        saved.getCategories().forEach(cat -> allCategoryIds.add(cat.getId()));
        allCategoryIds.forEach(catId -> cache.evict(CACHE_EBOOKS, "cat_v2_" + catId));
    }

    // ── 电子书缓存：删 ──
    private void cacheAfterDeleteEBook(Long id, Set<Long> categoryIds) {
        cache.evict(CACHE_EBOOK, id);
        cache.evict(CACHE_EBOOK, "chapters_" + id);
        cache.evictByKeyPrefix(CACHE_EBOOK, "ch_" + id + "_");
        cache.evictByKeyPrefix(CACHE_EBOOK, "ch_id_");
        cache.evict(CACHE_EBOOKS, KEY_ALL);
        categoryIds.forEach(catId -> cache.evict(CACHE_EBOOKS, "cat_v2_" + catId));
    }

    private String getFileExtension(String filename) {
        if (filename == null || filename.isBlank()) return "";
        int lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(lastDot + 1) : "";
    }

    private String resolveTitle(String inputTitle, String detectedTitle, String originalFilename) {
        if (inputTitle != null && !inputTitle.isBlank()) return inputTitle.trim();
        if (detectedTitle != null && !detectedTitle.isBlank()) return detectedTitle.trim();
        return stripExtension(originalFilename);
    }

    private String stripExtension(String filename) {
        if (filename == null || filename.isBlank()) return "未命名图书";
        int lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(0, lastDot).trim() : filename.trim();
    }

    private String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) return "未命名图书";
        return filename.replaceAll("[\\\\/:*?\"<>|]", "_")
                      .replaceAll("\\s+", "_")
                      .trim();
    }

    private boolean looksLikeBookTitle(String text) {
        String normalized = text.trim();
        if (normalized.isEmpty() || normalized.length() > 40) return false;
        if (normalized.startsWith("第") && normalized.matches("^第\\s*[0-9零一二三四五六七八九十百千两]+.*")) return false;
        return !normalized.contains("。") && !normalized.contains("，");
    }

    private boolean isDocxHeading(XWPFParagraph paragraph, String text) {
        String style = paragraph.getStyle();
        if (style != null && style.toLowerCase().contains("heading")) return true;
        return CHAPTER_PATTERN.matcher(text).matches();
    }

    private static class ParsedChapter {
        private final String title;
        private final String content;

        private ParsedChapter(String title, String content) {
            this.title = title;
            this.content = content;
        }
    }
}
