package com.shacky.materialmanagement.repository;

import com.shacky.materialmanagement.entity.Material;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface MaterialRepository extends JpaRepository<Material,Long> {
    List<Material> findByValidUntilBefore(LocalDateTime dateTime);
    List<Material> findByValidUntilAfter(LocalDateTime dateTime); // For showing only valid materials
}
