package com.shacky.materialmanagement.controller;

import com.shacky.materialmanagement.service.AdminService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
public class AdminAccountController {

    private final AdminService adminService;

    public AdminAccountController(AdminService adminService) {
        this.adminService = adminService;
    }

    @PostMapping("/change-password")
    public String changePassword(HttpServletRequest request,
                                 @RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 RedirectAttributes redirectAttributes) {
        String username = request.getUserPrincipal().getName();
        try {
            adminService.changePassword(username, currentPassword, newPassword, confirmPassword);
            redirectAttributes.addFlashAttribute("passwordSuccess", "Password changed successfully.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("passwordError", e.getMessage());
        }
        return "redirect:/admin";
    }
}
