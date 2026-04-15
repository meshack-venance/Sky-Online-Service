package com.shacky.materialmanagement.util;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

public final class FileStorageUtil {
    private static final Path UPLOAD_DIR = Paths.get("uploads").toAbsolutePath().normalize();

    private FileStorageUtil() {
    }

    public static Path getUploadDir() {
        ensureUploadDir();
        return UPLOAD_DIR;
    }

    public static String store(MultipartFile file) throws IOException {
        ensureUploadDir();

        String originalName = file.getOriginalFilename();
        String safeName = originalName == null ? "file" : Paths.get(originalName).getFileName().toString();
        String storedName = UUID.randomUUID() + "-" + safeName;

        Files.copy(file.getInputStream(), UPLOAD_DIR.resolve(storedName), StandardCopyOption.REPLACE_EXISTING);
        return storedName;
    }

    public static Path resolveFromUrl(String fileUrl) {
        String fileName = fileUrl.substring(fileUrl.lastIndexOf('/') + 1);
        return getUploadDir().resolve(fileName).normalize();
    }

    public static void deleteIfExists(String fileUrl) {
        try {
            Files.deleteIfExists(resolveFromUrl(fileUrl));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void ensureUploadDir() {
        try {
            Files.createDirectories(UPLOAD_DIR);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
