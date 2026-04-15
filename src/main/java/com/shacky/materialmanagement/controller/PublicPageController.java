package com.shacky.materialmanagement.controller;

import com.shacky.materialmanagement.entity.Material;
import com.shacky.materialmanagement.entity.OnlineService;
import com.shacky.materialmanagement.service.CommentService;
import com.shacky.materialmanagement.service.MaterialService;
import com.shacky.materialmanagement.service.OnlineServiceService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;

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
        model.addAttribute("materials", materialService.getAllMaterials());
        model.addAttribute("services", onlineServiceService.getAllServices());
        return "home";
    }

    @GetMapping("/services")
    public String viewServices(Model model) {
        model.addAttribute("services", onlineServiceService.getAllServices());
        model.addAttribute("comments", commentService.getAllComments());
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
        try {
            materialService.streamMaterial(material, response);
        } catch (IllegalArgumentException e) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, e.getMessage());
        }
    }
}
