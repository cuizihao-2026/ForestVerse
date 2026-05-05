package com.example.backend.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/file")
public class FileController {

    private static final Logger logger = LoggerFactory.getLogger(FileController.class);

    private static final String AVATAR_UPLOAD_DIR = "uploads/avatars";
    private static final String UPLOADS_DIR = "uploads";

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of(
            ".jpg", ".jpeg", ".png", ".gif", ".webp", ".ico"
    );

    private static final Set<String> ALLOWED_IMAGE_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp",
            "image/x-icon",
            "image/vnd.microsoft.icon"
    );
    
    // 通用文件允许的扩展名
    private static final Set<String> ALLOWED_GENERAL_EXTENSIONS = Set.of(
            ".jpg", ".jpeg", ".png", ".gif", ".webp", ".ico",
            ".pdf", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx",
            ".txt", ".zip", ".rar", ".mp4", ".mp3", ".webm"
    );

    public FileController() {
        // 创建上传目录
        String projectRoot = System.getProperty("user.dir");
        Path uploadsPath = Paths.get(projectRoot, UPLOADS_DIR);
        Path avatarPath = Paths.get(projectRoot, AVATAR_UPLOAD_DIR);
        
        if (!Files.exists(uploadsPath)) {
            try {
                Files.createDirectories(uploadsPath);
                logger.info("上传目录创建成功: " + uploadsPath.toString());
            } catch (IOException e) {
                logger.error("上传目录创建失败: " + e.getMessage(), e);
            }
        }
        
        if (!Files.exists(avatarPath)) {
            try {
                Files.createDirectories(avatarPath);
                logger.info("头像上传目录创建成功: " + avatarPath.toString());
            } catch (IOException e) {
                logger.error("头像上传目录创建失败: " + e.getMessage(), e);
            }
        } else {
            logger.info("头像上传目录已存在: " + avatarPath.toString());
        }
    }

    @PostMapping("/upload/avatar")
    public ResponseEntity<?> uploadAvatar(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("文件不能为空");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            return ResponseEntity.badRequest().body("文件大小不能超过5MB");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            return ResponseEntity.badRequest().body("文件名无效");
        }

        String extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        if (!ALLOWED_IMAGE_EXTENSIONS.contains(extension)) {
            return ResponseEntity.badRequest().body("不支持的图片格式，仅支持：jpg、jpeg、png、gif、webp、ico");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_CONTENT_TYPES.contains(contentType)) {
            return ResponseEntity.badRequest().body("文件类型不合法");
        }

        try {
            String projectRoot = System.getProperty("user.dir");
            String fileName = generateFileName(extension);
            Path filePath = Paths.get(projectRoot, AVATAR_UPLOAD_DIR, fileName);

            Files.copy(file.getInputStream(), filePath);

            String fileUrl = "/" + AVATAR_UPLOAD_DIR + "/" + fileName;
            return ResponseEntity.ok(fileUrl);
        } catch (IOException e) {
            logger.error("文件上传失败: " + e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("文件上传失败");
        }
    }
    
    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "directory", required = false, defaultValue = "") String directory) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("文件不能为空");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            return ResponseEntity.badRequest().body("文件大小不能超过10MB");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            return ResponseEntity.badRequest().body("文件名无效");
        }

        String extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        if (!ALLOWED_GENERAL_EXTENSIONS.contains(extension)) {
            return ResponseEntity.badRequest().body("不支持的文件格式");
        }

        try {
            String projectRoot = System.getProperty("user.dir");
            Path baseUploadPath = Paths.get(projectRoot, UPLOADS_DIR);
            
            // 验证并规范化目录路径，防止目录遍历
            Path targetPath = baseUploadPath;
            if (!directory.isEmpty()) {
                // 规范化路径并检查是否在上传目录内
                targetPath = baseUploadPath.resolve(directory).normalize();
                if (!targetPath.startsWith(baseUploadPath)) {
                    return ResponseEntity.badRequest().body("无效的目录路径");
                }
            }
            
            // 创建目标目录（如果不存在）
            if (!Files.exists(targetPath)) {
                Files.createDirectories(targetPath);
            }
            
            String fileName = generateFileName(extension);
            Path filePath = targetPath.resolve(fileName);

            Files.copy(file.getInputStream(), filePath);

            // 构建返回的URL
            Path relativePath = baseUploadPath.relativize(filePath);
            String fileUrl = "/" + UPLOADS_DIR + "/" + relativePath.toString().replace("\\", "/");
            return ResponseEntity.ok(fileUrl);
        } catch (IOException e) {
            logger.error("文件上传失败: " + e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("文件上传失败");
        }
    }
    
    /**
     * 生成带时间前缀的文件名
     * 格式：yyyyMMddHHmmss_{uuid}.extension
     */
    private String generateFileName(String extension) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
                .withZone(ZoneId.systemDefault());
        String timestamp = formatter.format(Instant.now());
        String shortUuid = UUID.randomUUID().toString().replace("-", "");
        return timestamp + "_" + shortUuid + extension;
    }

    @GetMapping("/list")
    public ResponseEntity<?> listFiles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String directory) {
        try {
            String projectRoot = System.getProperty("user.dir");
            Path uploadsPath = Paths.get(projectRoot, UPLOADS_DIR);
            
            if (!Files.exists(uploadsPath)) {
                Map<String, Object> emptyResult = new HashMap<>();
                emptyResult.put("content", Collections.emptyList());
                emptyResult.put("totalElements", 0);
                emptyResult.put("totalPages", 0);
                emptyResult.put("page", page);
                emptyResult.put("size", size);
                return ResponseEntity.ok(emptyResult);
            }

            List<Map<String, Object>> fileList = new ArrayList<>();
            
            try (Stream<Path> paths = Files.walk(uploadsPath)) {
                paths.filter(Files::isRegularFile)
                     .forEach(path -> {
                         try {
                             BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
                             Path relativePath = uploadsPath.relativize(path);
                             String fileUrl = "/" + UPLOADS_DIR + "/" + relativePath.toString().replace("\\", "/");
                             String fileDir = relativePath.getNameCount() > 1 
                                 ? relativePath.subpath(0, relativePath.getNameCount() - 1).toString() 
                                 : "";
                             
                             // 如果指定了目录，只返回该目录下的文件
                             if (directory != null && !directory.isEmpty()) {
                                 if (!directory.equals(fileDir)) {
                                     return;
                                 }
                             }
                             
                             Map<String, Object> fileInfo = new HashMap<>();
                             fileInfo.put("name", path.getFileName().toString());
                             fileInfo.put("path", relativePath.toString());
                             fileInfo.put("url", fileUrl);
                             fileInfo.put("size", attrs.size());
                             fileInfo.put("sizeFormatted", formatSize(attrs.size()));
                             fileInfo.put("createdTime", formatTime(attrs.creationTime().toInstant()));
                             fileInfo.put("modifiedTime", formatTime(attrs.lastModifiedTime().toInstant()));
                             fileInfo.put("directory", fileDir);
                             
                             fileList.add(fileInfo);
                        } catch (IOException e) {
                            logger.error("读取文件属性失败: " + e.getMessage(), e);
                        }
                    });
            }

            // 按修改时间倒序排列
            fileList.sort((a, b) -> {
                return ((String) b.get("modifiedTime")).compareTo((String) a.get("modifiedTime"));
            });

            // 分页处理
            int totalElements = fileList.size();
            int totalPages = (int) Math.ceil((double) totalElements / size);
            int fromIndex = page * size;
            int toIndex = Math.min(fromIndex + size, totalElements);
            
            List<Map<String, Object>> paginatedContent = fromIndex < totalElements 
                ? fileList.subList(fromIndex, toIndex) 
                : Collections.emptyList();

            Map<String, Object> result = new HashMap<>();
            result.put("content", paginatedContent);
            result.put("totalElements", totalElements);
            result.put("totalPages", totalPages);
            result.put("page", page);
            result.put("size", size);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("获取文件列表失败: " + e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("获取文件列表失败");
        }
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteFile(@RequestParam("path") String filePath) {
        try {
            String projectRoot = System.getProperty("user.dir");
            Path baseUploadPath = Paths.get(projectRoot, UPLOADS_DIR);
            
            // 规范化路径并检查是否在上传目录内，防止目录遍历
            Path fullPath = baseUploadPath.resolve(filePath).normalize();
            if (!fullPath.startsWith(baseUploadPath)) {
                return ResponseEntity.badRequest().body("无效的文件路径");
            }
            
            if (!Files.exists(fullPath)) {
                return ResponseEntity.badRequest().body("文件不存在");
            }

            Files.delete(fullPath);
            return ResponseEntity.ok("文件删除成功");
        } catch (Exception e) {
            logger.error("文件删除失败: " + e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("文件删除失败");
        }
    }

    private String formatSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.1f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", size / (1024.0 * 1024.0));
        } else {
            return String.format("%.1f GB", size / (1024.0 * 1024.0 * 1024.0));
        }
    }

    private String formatTime(Instant instant) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(ZoneId.systemDefault());
        return formatter.format(instant);
    }
}
