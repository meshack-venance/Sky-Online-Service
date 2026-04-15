package com.shacky.materialmanagement.controller;

import com.shacky.materialmanagement.entity.OnlineService;
import com.shacky.materialmanagement.entity.ServiceOrder;
import com.shacky.materialmanagement.service.OnlineServiceService;
import com.shacky.materialmanagement.service.ServiceOrderService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/orders")
public class AdminOrderController {

    private final ServiceOrderService serviceOrderService;
    private final OnlineServiceService onlineServiceService;

    public AdminOrderController(ServiceOrderService serviceOrderService, OnlineServiceService onlineServiceService) {
        this.serviceOrderService = serviceOrderService;
        this.onlineServiceService = onlineServiceService;
    }

    @PostMapping("/update-status/{orderId}")
    public String updateOrderStatus(@PathVariable Long orderId,
                                    @RequestParam String status,
                                    RedirectAttributes redirectAttributes) {
        boolean success = serviceOrderService.updateOrderStatus(orderId, status);
        if (success) {
            redirectAttributes.addFlashAttribute("statusMessage", "Order status updated successfully.");
        } else {
            redirectAttributes.addFlashAttribute("statusError", "Order not found.");
        }
        return "redirect:/admin/orders";
    }

    @GetMapping
    public String viewFilteredOrders(@RequestParam(required = false) String status,
                                     @RequestParam(required = false) Long serviceId,
                                     @RequestParam(required = false, defaultValue = "datePlaced") String sort,
                                     @RequestParam(required = false, defaultValue = "desc") String direction,
                                     Model model) {
        List<ServiceOrder> filteredOrders = serviceOrderService.getFilteredAndSortedOrders(status, serviceId, sort, direction);
        List<OnlineService> allServices = onlineServiceService.getAllServices();

        model.addAttribute("orders", filteredOrders);
        model.addAttribute("services", allServices);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedServiceId", serviceId);
        model.addAttribute("sortedBy", sort);
        model.addAttribute("sortDirection", direction);
        return "admin-orders";
    }
}
