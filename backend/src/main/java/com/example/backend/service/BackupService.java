package com.example.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class BackupService {

    private static final Logger logger = LoggerFactory.getLogger(BackupService.class);

    private static final String BACKUPS_DIR = "backups";
    private static final String UPLOADS_DIR = "uploads";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd_HHmmss");

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    @Value("${spring.datasource.username}")
    private String dbUsername;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    private Path getBackupsPath() {
        Path path = Paths.get(System.getProperty("user.dir"), BACKUPS_DIR);
        try {
            Files.createDirectories(path);
        } catch (IOException ignored) {
        }
        return path;
    }

    private Path getUploadsPath() {
        return Paths.get(System.getProperty("user.dir"), UPLOADS_DIR);
    }

    private String extractDatabaseName() {
        String url = datasourceUrl;
        if (url.contains("?")) {
            url = url.substring(0, url.indexOf("?"));
        }
        if (url.contains("/")) {
            return url.substring(url.lastIndexOf("/") + 1);
        }
        return "backend";
    }

    @Async
    public void createBackupAsync() {
        createBackup();
    }

    public Map<String, String> createBackup() {
        Map<String, String> result = new HashMap<>();
        try {
            String timestamp = DATE_FORMAT.format(new Date());
            String backupName = "backup_" + timestamp + ".zip";
            Path tempDir = Files.createTempDirectory("backup_");
            Path sqlFile = tempDir.resolve("database_" + timestamp + ".sql");
            Path uploadsZip = tempDir.resolve("uploads_" + timestamp + ".zip");
            Path finalZip = getBackupsPath().resolve(backupName);

            String dbName = extractDatabaseName();

            ProcessBuilder pb = new ProcessBuilder(
                    "mysqldump",
                    "-u" + dbUsername,
                    "-p" + dbPassword,
                    "--databases", dbName,
                    "--add-drop-database",
                    "--default-character-set=utf8mb4",
                    "--result-file=" + sqlFile.toString()
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                cleanTempDir(tempDir);
                result.put("status", "error");
                result.put("message", "数据库导出失败，mysqldump 退出码: " + exitCode);
                return result;
            }

            Path uploadsPath = getUploadsPath();
            if (Files.exists(uploadsPath)) {
                zipDirectory(uploadsPath, uploadsZip);
            }

            try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(finalZip))) {
                addFileToZip(zos, sqlFile, "database_" + timestamp + ".sql");
                if (Files.exists(uploadsZip)) {
                    addFileToZip(zos, uploadsZip, "uploads_" + timestamp + ".zip");
                }
            }

            cleanTempDir(tempDir);

            long size = Files.size(finalZip);
            result.put("status", "success");
            result.put("name", backupName);
            result.put("size", formatFileSize(size));
            return result;
        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", e.getMessage());
            return result;
        }
    }

    public Map<String, Object> restoreBackup(String filename) {
        Map<String, Object> result = new HashMap<>();
        try {
            Path backupFile = getBackupsPath().resolve(filename);
            if (!Files.exists(backupFile)) {
                result.put("status", "error");
                result.put("message", "备份文件不存在");
                return result;
            }

            Path tempDir = Files.createTempDirectory("restore_");

            try (java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(Files.newInputStream(backupFile))) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    Path extractedPath = tempDir.resolve(entry.getName());
                    if (entry.getName().endsWith(".sql")) {
                        Files.copy(zis, extractedPath, StandardCopyOption.REPLACE_EXISTING);
                    } else if (entry.getName().endsWith(".zip")) {
                        Files.copy(zis, extractedPath, StandardCopyOption.REPLACE_EXISTING);
                    }
                    zis.closeEntry();
                }
            }

            Optional<Path> sqlFile = Files.list(tempDir)
                    .filter(p -> p.toString().endsWith(".sql"))
                    .findFirst();

            if (sqlFile.isPresent()) {
                String dbName = extractDatabaseName();
                ProcessBuilder pb = new ProcessBuilder(
                        "mysql",
                        "-u" + dbUsername,
                        "-p" + dbPassword,
                        dbName
                );
                pb.redirectInput(sqlFile.get().toFile());
                pb.redirectErrorStream(true);
                Process process = pb.start();
                int exitCode = process.waitFor();

                if (exitCode != 0) {
                    cleanTempDir(tempDir);
                    result.put("status", "error");
                    result.put("message", "数据库恢复失败，mysql 退出码: " + exitCode);
                    return result;
                }
            }

            Optional<Path> uploadsZip = Files.list(tempDir)
                    .filter(p -> p.toString().endsWith(".zip"))
                    .findFirst();

            if (uploadsZip.isPresent()) {
                Path uploadsPath = getUploadsPath();
                if (Files.exists(uploadsPath)) {
                    deleteDirectoryRecursively(uploadsPath);
                }
                Files.createDirectories(uploadsPath);
                unzip(uploadsZip.get(), uploadsPath);
            }

            cleanTempDir(tempDir);
            result.put("status", "success");
            result.put("message", "备份恢复成功");
            return result;
        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", e.getMessage());
            return result;
        }
    }

    public Map<String, Object> restoreFromUpload(Path uploadedFile) {
        try {
            String filename = uploadedFile.getFileName().toString();
            Path targetPath = getBackupsPath().resolve(filename);
            Files.move(uploadedFile, targetPath, StandardCopyOption.REPLACE_EXISTING);
            return restoreBackup(filename);
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("status", "error");
            result.put("message", e.getMessage());
            return result;
        }
    }

    public List<Map<String, Object>> listBackups() {
        List<Map<String, Object>> backups = new ArrayList<>();
        Path backupsPath = getBackupsPath();
        logger.info("备份文件夹路径: " + backupsPath.toAbsolutePath());
        
        if (!Files.exists(backupsPath)) {
            logger.warn("备份文件夹不存在");
            return backups;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(backupsPath, "*.zip")) {
            for (Path path : stream) {
                logger.info("找到备份文件: " + path.getFileName());
                Map<String, Object> info = new HashMap<>();
                info.put("name", path.getFileName().toString());
                info.put("size", formatFileSize(Files.size(path)));
                info.put("sizeBytes", Files.size(path));

                BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
                info.put("createdAt", DATE_FORMAT.format(new Date(attrs.creationTime().toMillis())));
                info.put("lastModified", attrs.lastModifiedTime().toMillis());
                backups.add(info);
                logger.info("添加备份信息: " + info);
            }
        } catch (IOException e) {
            logger.info("读取备份文件列表出错: " + e.getMessage());
            logger.error("操作失败: " + e.getMessage(), e);
        }

        logger.info("总共找到 " + backups.size() + " 个备份文件");
        backups.sort((a, b) -> Long.compare(
                (Long) b.get("lastModified"),
                (Long) a.get("lastModified")));
        return backups;
    }

    public boolean deleteBackup(String filename) {
        try {
            Path file = getBackupsPath().resolve(filename);
            return Files.deleteIfExists(file);
        } catch (IOException e) {
            return false;
        }
    }

    private void zipDirectory(Path sourceDir, Path zipFile) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipFile))) {
            Files.walkFileTree(sourceDir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Path relativePath = sourceDir.relativize(file);
                    ZipEntry entry = new ZipEntry(relativePath.toString().replace("\\", "/"));
                    zos.putNextEntry(entry);
                    Files.copy(file, zos);
                    zos.closeEntry();
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    if (!dir.equals(sourceDir)) {
                        Path relativePath = sourceDir.relativize(dir);
                        ZipEntry entry = new ZipEntry(relativePath.toString().replace("\\", "/") + "/");
                        zos.putNextEntry(entry);
                        zos.closeEntry();
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }

    private void addFileToZip(ZipOutputStream zos, Path file, String entryName) throws IOException {
        ZipEntry entry = new ZipEntry(entryName);
        zos.putNextEntry(entry);
        Files.copy(file, zos);
        zos.closeEntry();
    }

    private void unzip(Path zipFile, Path targetDir) throws IOException {
        try (java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(Files.newInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path entryPath = targetDir.resolve(entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(entryPath);
                } else {
                    Files.createDirectories(entryPath.getParent());
                    Files.copy(zis, entryPath, StandardCopyOption.REPLACE_EXISTING);
                }
                zis.closeEntry();
            }
        }
    }

    private void deleteDirectoryRecursively(Path dir) throws IOException {
        if (Files.exists(dir)) {
            Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }

    private void cleanTempDir(Path tempDir) {
        try {
            deleteDirectoryRecursively(tempDir);
        } catch (IOException ignored) {
        }
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }

    public void cleanOldBackups(int keepCount) {
        try {
            Path backupsPath = getBackupsPath();
            if (!Files.exists(backupsPath)) {
                return;
            }

            List<Path> backupFiles = Files.list(backupsPath)
                    .filter(p -> p.toString().endsWith(".zip"))
                    .sorted((a, b) -> {
                        try {
                            return Files.getLastModifiedTime(b).compareTo(Files.getLastModifiedTime(a));
                        } catch (IOException e) {
                            return 0;
                        }
                    })
                    .toList();

            if (backupFiles.size() > keepCount) {
                for (int i = keepCount; i < backupFiles.size(); i++) {
                    Files.deleteIfExists(backupFiles.get(i));
                    logger.info("删除旧备份: " + backupFiles.get(i).getFileName());
                }
            }
        } catch (Exception e) {
            logger.error("清理旧备份失败: " + e.getMessage());
        }
    }
}
