package com.shacky.materialmanagement.service;

import com.shacky.materialmanagement.entity.Material;
import com.shacky.materialmanagement.repository.MaterialRepository;
import com.shacky.materialmanagement.util.FileStorageUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
