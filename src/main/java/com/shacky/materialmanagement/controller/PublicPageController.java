package com.shacky.materialmanagement.controller;

import com.shacky.materialmanagement.entity.Comment;
import com.shacky.materialmanagement.entity.Material;
import com.shacky.materialmanagement.entity.OnlineService;
import com.shacky.materialmanagement.service.CommentService;
import com.shacky.materialmanagement.service.MaterialService;
import com.shacky.materialmanagement.service.OnlineServiceService;
import com.shacky.materialmanagement.util.FileStorageUtil;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Controller
@RequestMapping
public class PublicPageController {

    private final MaterialService materialService;
    private final OnlineServiceService onlineServiceService;
    private final CommentService commentService;

    public PublicPageController(
            MaterialService materialService,
            OnlineServiceService onlineServiceService,
            CommentService commentService
    ) {
        this.materialService = materialService;
        this.onlineServiceService = onlineServiceService;
        this.commentService = commentService;
    }

    @GetMapping("/")
    public String homePage(Model model) {
        List<Material> materials = materialService.getAllMaterials();
        model.addAttribute("materials", materials);
        model.addAttribute("services", onlineServiceService.getAllServices());
        return "home";
    }

    @GetMapping("/services")
    public String viewServices(Model model) {
        List<OnlineService> services = onlineServiceService.getAllServices();
        List<Comment> comments = commentService.getAllComments();
        model.addAttribute("services", services);
        model.addAttribute("comments", comments);
        return "services";
    }

    @GetMapping("/service/{id}")
    public String viewService(@PathVariable Long id, Model model) {
        OnlineService onlineService = onlineServiceService.getService(id);
        if (onlineService == null) {
            model.addAttribute("errorMessage", "Service not found.");
            return "error";
        }

        model.addAttribute("service", onlineService);
        return "service-details";
    }

    @GetMapping("/download/{id}")
    public void downloadMaterial(@PathVariable Long id, HttpServletResponse response) throws IOException {
        Material material = materialService.getMaterial(id);
        if (material == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Material not found in database");
            return;
        }

        Path filePath = FileStorageUtil.resolveFromUrl(material.getFileUrl());
        if (!Files.exists(filePath)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Material file not found");
            return;
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
}
