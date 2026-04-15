package com.shacky.materialmanagement.controller;

import com.shacky.materialmanagement.entity.Material;
import com.shacky.materialmanagement.entity.OnlineService;
import com.shacky.materialmanagement.service.MaterialService;
import com.shacky.materialmanagement.service.OnlineServiceService;
import com.shacky.materialmanagement.util.FileStorageUtil;
import com.shacky.materialmanagement.web.request.AdminServiceRequest;
import com.shacky.materialmanagement.web.request.CreateServiceRequest;
import com.shacky.materialmanagement.web.request.UploadAnnouncementRequest;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminPageController {

    private final MaterialService materialService;
    private final OnlineServiceService onlineServiceService;

    public AdminPageController(MaterialService materialService, OnlineServiceService onlineServiceService) {
        this.materialService = materialService;
        this.onlineServiceService = onlineServiceService;
    }

    @GetMapping("/login")
    public String showLoginPage() {
        return "login";
    }

    @GetMapping
    public String showAdminPage(Model model) {
        List<Material> materials = materialService.getAllMaterials();
        model.addAttribute("materials", materials);
        model.addAttribute("services", onlineServiceService.getAllServices());
        return "admin";
    }

    @PostMapping("/upload")
    public String uploadMaterial(@Valid @ModelAttribute UploadAnnouncementRequest request,
                                 RedirectAttributes redirectAttributes) {
        try {
            MultipartFile file = request.getFile();
            String storedFileName = FileStorageUtil.store(file);
            Material material = new Material();
            material.setName(request.getName());
            material.setFileName(file.getOriginalFilename());
            material.setContentType(file.getContentType());
            material.setFileUrl("/uploads/" + storedFileName);
            material.setUploadTime(LocalDateTime.now());
            material.setValidUntil(LocalDateTime.now().plusDays(request.getValidDays()));
            materialService.saveMaterial(material);
            redirectAttributes.addFlashAttribute("successMessage", "Material uploaded successfully!");
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to upload material.");
        }

        return "redirect:/admin";
    }

    @PostMapping("/services/add")
    public String addOnlineService(@Valid @ModelAttribute CreateServiceRequest request,
                                   RedirectAttributes redirectAttributes) {
        try {
            OnlineService onlineService = new OnlineService();
            onlineService.setTitle(request.getTitle());
            onlineService.setRequirements(request.getRequirements());
            onlineService.setCost(request.getCost());
            onlineServiceService.saveService(onlineService);
            redirectAttributes.addFlashAttribute("serviceSuccess", "Service added successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("serviceError", "Error adding service.");
        }
        return "redirect:/admin";
    }

    @GetMapping("/services/delete/{id}")
    public String deleteOnlineService(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            onlineServiceService.deleteService(id);
            redirectAttributes.addFlashAttribute("serviceSuccess", "Service deleted successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("serviceError", "Failed to delete service.");
        }
        return "redirect:/admin";
    }

    @GetMapping("/services/edit/{id}")
    public String editOnlineService(@PathVariable Long id, Model model) {
        OnlineService service = onlineServiceService.getService(id);
        if (service == null) {
            model.addAttribute("serviceError", "Service not found.");
            return "redirect:/admin";
        }

        model.addAttribute("editService", service);
        model.addAttribute("services", onlineServiceService.getAllServices());
        model.addAttribute("materials", materialService.getAllMaterials());
        return "admin";
    }

    @PostMapping("/services/update")
    public String updateOnlineService(@Valid @ModelAttribute AdminServiceRequest request,
                                      RedirectAttributes redirectAttributes) {
        try {
            OnlineService existing = onlineServiceService.getService(request.getId());
            if (existing != null) {
                existing.setTitle(request.getTitle());
                existing.setRequirements(request.getRequirements());
                existing.setCost(request.getCost());
                onlineServiceService.saveService(existing);
                redirectAttributes.addFlashAttribute("serviceSuccess", "Service updated successfully.");
            } else {
                redirectAttributes.addFlashAttribute("serviceError", "Service not found.");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("serviceError", "Error updating service.");
        }
        return "redirect:/admin";
    }
}
