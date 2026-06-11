package com.library.util;

import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

/**
 * 图片文件上传校验工具
 * 校验内容类型、文件扩展名、魔数、文件大小，防止任意文件上传攻击
 */
public final class ImageFileValidator {

    private static final long MAX_SIZE = 5 * 1024 * 1024; // 5MB

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".jpg", ".jpeg", ".png", ".gif", ".webp"
    );

    private static final byte[][] MAGIC_NUMBERS = {
            // JPEG: FF D8 FF
            new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF},
            // PNG: 89 50 4E 47
            new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47},
            // GIF: 47 49 46 38 (GIF8)
            new byte[]{0x47, 0x49, 0x46, 0x38},
            // WebP: 52 49 46 46 ... 57 45 42 50 (RIFF....WEBP)
    };

    private static final String[] MAGIC_LABELS = {"JPEG", "PNG", "GIF", "WebP"};

    private ImageFileValidator() {}

    public static void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("上传文件为空");
        }

        // 1. 文件大小校验
        if (file.getSize() > MAX_SIZE) {
            throw new RuntimeException("文件大小不能超过 5MB");
        }

        // 2. Content-Type 校验
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new RuntimeException("不支持的文件类型: " + contentType + "，仅允许 JPEG/PNG/GIF/WebP");
        }

        // 3. 扩展名校验
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.contains(".")) {
            throw new RuntimeException("无法识别文件扩展名");
        }
        String ext = filename.substring(filename.lastIndexOf('.')).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new RuntimeException("不支持的文件扩展名: " + ext + "，仅允许 jpg/jpeg/png/gif/webp");
        }

        // 4. 魔数校验（文件头字节验证，防止扩展名伪造）
        boolean magicMatch = false;
        try {
            byte[] header = new byte[12];
            int read = file.getInputStream().read(header);
            if (read >= 4) {
                // JPEG: FF D8 FF
                if (header[0] == (byte) 0xFF && header[1] == (byte) 0xD8 && header[2] == (byte) 0xFF) {
                    magicMatch = true;
                }
                // PNG: 89 50 4E 47
                else if (header[0] == (byte) 0x89 && header[1] == 0x50 && header[2] == 0x4E && header[3] == 0x47) {
                    magicMatch = true;
                }
                // GIF: 47 49 46 38
                else if (header[0] == 0x47 && header[1] == 0x49 && header[2] == 0x46 && header[3] == 0x38) {
                    magicMatch = true;
                }
                // WebP: 52 49 46 46 ... 57 45 42 50
                else if (header[0] == 0x52 && header[1] == 0x49 && header[2] == 0x46 && header[3] == 0x46
                        && read >= 12 && header[8] == 0x57 && header[9] == 0x45
                        && header[10] == 0x42 && header[11] == 0x50) {
                    magicMatch = true;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("无法读取文件内容进行校验");
        }

        if (!magicMatch) {
            throw new RuntimeException("文件内容与扩展名不匹配，可能是伪造的文件类型");
        }
    }
}
