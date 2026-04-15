package com.shacky.materialmanagement.controller;

import com.shacky.materialmanagement.entity.Admin;
import com.shacky.materialmanagement.service.AdminService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
@RequestMapping("/admin")
public class AdminAccountController {

    private final AdminService adminService;
    private final BCryptPasswordEncoder passwordEncoder;

    public AdminAccountController(AdminService adminService, BCryptPasswordEncoder passwordEncoder) {
        this.adminService = adminService;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/change-password")
    public String changePassword(HttpServletRequest request,
                                 @RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 RedirectAttributes redirectAttributes) {
        String username = request.getUserPrincipal().getName();
        Optional<Admin> optionalAdmin = adminService.findByUsername(username);

        if (optionalAdmin.isEmpty()) {
            redirectAttributes.addFlashAttribute("passwordError", "Admin not found.");
            return "redirect:/admin";
        }

        Admin admin = optionalAdmin.get();
        if (!passwordEncoder.matches(currentPassword, admin.getPassword())) {
            redirectAttributes.addFlashAttribute("passwordError", "Current password is incorrect.");
            return "redirect:/admin";
        }

        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("passwordError", "New passwords do not match.");
            return "redirect:/admin";
        }

        admin.setPassword(passwordEncoder.encode(newPassword));
        adminService.save(admin);
        redirectAttributes.addFlashAttribute("passwordSuccess", "Password changed successfully.");
        return "redirect:/admin";
    }
}
