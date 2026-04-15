package com.shacky.materialmanagement.controller;

import com.shacky.materialmanagement.entity.OnlineService;
import com.shacky.materialmanagement.service.MaterialService;
import com.shacky.materialmanagement.service.OnlineServiceService;
import com.shacky.materialmanagement.web.request.AdminServiceRequest;
import com.shacky.materialmanagement.web.request.CreateServiceRequest;
import com.shacky.materialmanagement.web.request.UploadAnnouncementRequest;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
        model.addAttribute("materials", materialService.getAllMaterials());
        model.addAttribute("services", onlineServiceService.getAllServices());
        return "admin";
    }

    @PostMapping("/upload")
    public String uploadMaterial(@Valid @ModelAttribute UploadAnnouncementRequest request,
                                 RedirectAttributes redirectAttributes) {
        try {
            materialService.uploadMaterial(request);
            redirectAttributes.addFlashAttribute("successMessage", "Material uploaded successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to upload material.");
        }
        return "redirect:/admin";
    }

    @PostMapping("/services/add")
    public String addOnlineService(@Valid @ModelAttribute CreateServiceRequest request,
                                   RedirectAttributes redirectAttributes) {
        try {
            onlineServiceService.createService(request);
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
            onlineServiceService.updateService(request);
            redirectAttributes.addFlashAttribute("serviceSuccess", "Service updated successfully.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("serviceError", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("serviceError", "Error updating service.");
        }
        return "redirect:/admin";
    }
}
