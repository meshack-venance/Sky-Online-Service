package com.shacky.materialmanagement.service;

import com.shacky.materialmanagement.entity.Material;
import com.shacky.materialmanagement.repository.MaterialRepository;
import com.shacky.materialmanagement.util.FileStorageUtil;
import com.shacky.materialmanagement.web.request.UploadAnnouncementRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class MaterialService {

    @Autowired
    private MaterialRepository materialRepository;

    public List<Material> getAllMaterials() {
        return materialRepository.findByValidUntilAfter(LocalDateTime.now());
    }

    public Material getMaterial(Long id) {
        return materialRepository.findById(id).orElse(null);
    }

    public Material saveMaterial(Material material) {
        return materialRepository.save(material);
    }

    /**
     * Stores the uploaded file, constructs the Material entity, and persists it.
     */
    public Material uploadMaterial(UploadAnnouncementRequest request) throws IOException {
        MultipartFile file = request.getFile();
        String storedFileName = FileStorageUtil.store(file);

        Material material = new Material();
        material.setName(request.getName());
        material.setFileName(file.getOriginalFilename());
        material.setContentType(file.getContentType());
        material.setFileUrl("/uploads/" + storedFileName);
        material.setUploadTime(LocalDateTime.now());
        material.setValidUntil(LocalDateTime.now().plusDays(request.getValidDays()));

        return materialRepository.save(material);
    }

    /**
     * Writes the material file contents to the HTTP response.
     *
     * @throws IllegalArgumentException if the material file does not exist on disk
     */
    public void streamMaterial(Material material, HttpServletResponse response) throws IOException {
        Path filePath = FileStorageUtil.resolveFromUrl(material.getFileUrl());
        if (!Files.exists(filePath)) {
            throw new IllegalArgumentException("Material file not found");
        }

        if (material.getContentType() != null && !material.getContentType().isBlank()) {
            response.setContentType(material.getContentType());
        }
        response.setHeader("Content-Disposition", "attachment; filename=\"" + material.getFileName() + "\"");
        response.setContentLengthLong(Files.size(filePath));

        try (OutputStream outputStream = response.getOutputStream()) {
            Files.copy(filePath, outputStream);
            outputStream.flush();
        }
    }

    public void removeOutdatedMaterials() {
        LocalDateTime now = LocalDateTime.now();
        List<Material> outdatedMaterials = materialRepository.findByValidUntilBefore(now);
        outdatedMaterials.stream()
                .map(Material::getFileUrl)
                .filter(fileUrl -> fileUrl != null && !fileUrl.isBlank())
                .forEach(FileStorageUtil::deleteIfExists);
        materialRepository.deleteAll(outdatedMaterials);
    }
}
