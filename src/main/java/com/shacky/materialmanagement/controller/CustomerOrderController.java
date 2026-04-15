package com.shacky.materialmanagement.controller;

import com.shacky.materialmanagement.auth.CurrentCustomerService;
import com.shacky.materialmanagement.entity.Customer;
import com.shacky.materialmanagement.entity.OnlineService;
import com.shacky.materialmanagement.entity.ServiceOrder;
import com.shacky.materialmanagement.service.CustomerService;
import com.shacky.materialmanagement.service.OnlineServiceService;
import com.shacky.materialmanagement.service.ReceiptService;
import com.shacky.materialmanagement.service.ServiceOrderService;
import com.shacky.materialmanagement.service.UploadedDocumentService;
import com.shacky.materialmanagement.web.request.PlaceOrderRequest;
import com.shacky.materialmanagement.web.request.UploadDocumentRequest;
import jakarta.servlet.http.HttpServletResponse;
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
@RequestMapping("/orders")
public class CustomerOrderController {

    private final CustomerService customerService;
    private final OnlineServiceService onlineServiceService;
    private final ServiceOrderService serviceOrderService;
    private final UploadedDocumentService uploadedDocumentService;
    private final CurrentCustomerService currentCustomerService;
    private final ReceiptService receiptService;

    public CustomerOrderController(
            CustomerService customerService,
            OnlineServiceService onlineServiceService,
            ServiceOrderService serviceOrderService,
            UploadedDocumentService uploadedDocumentService,
            CurrentCustomerService currentCustomerService,
            ReceiptService receiptService
    ) {
        this.customerService = customerService;
        this.onlineServiceService = onlineServiceService;
        this.serviceOrderService = serviceOrderService;
        this.uploadedDocumentService = uploadedDocumentService;
        this.currentCustomerService = currentCustomerService;
        this.receiptService = receiptService;
    }

    @PostMapping("/place")
    public String placeOrder(@Valid @ModelAttribute PlaceOrderRequest request,
                             RedirectAttributes redirectAttributes) {
        OnlineService service = onlineServiceService.getServiceById(request.getServiceId()).orElse(null);
        if (service == null) {
            redirectAttributes.addFlashAttribute("error", "Selected service not found.");
            return "redirect:/services";
        }

        Customer customer = customerService.findOrCreate(request);
        double totalCost = serviceOrderService.calculateTotalCostIncluding(customer, service);
        serviceOrderService.createOrder(customer, service);

        redirectAttributes.addFlashAttribute("success", "Order placed successfully.");
        redirectAttributes.addFlashAttribute("totalCost", totalCost);
        return "redirect:/orders/details/" + request.getServiceId();
    }

    @GetMapping("/details/{id}")
    public String viewServiceDetails(@PathVariable Long id, Model model) {
        if (currentCustomerService.getCurrentCustomer().isPresent()) {
            return "redirect:/orders/my-orders";
        }

        OnlineService service = onlineServiceService.getServiceById(id).orElse(null);
        if (service == null) {
            model.addAttribute("error", "Service not found.");
            return "redirect:/services";
        }

        model.addAttribute("service", service);
        return "service-details";
    }

    @GetMapping("/my-orders")
    public String viewMyOrders(Model model, RedirectAttributes redirectAttributes) {
        Customer customer = currentCustomerService.getCurrentCustomer().orElse(null);

        if (customer == null) {
            redirectAttributes.addFlashAttribute("error", "Please log in to view your orders.");
            return "redirect:/services";
        }

        model.addAttribute("orders", serviceOrderService.getOrdersByCustomer(customer));
        model.addAttribute("customer", customer);
        model.addAttribute("totalCost", serviceOrderService.calculateTotalCostForCustomer(customer));
        return "my-orders";
    }

    @PostMapping("/add-service/{serviceId}")
    public String quickAddServiceToOrders(@PathVariable Long serviceId, RedirectAttributes redirectAttributes) {
        Customer customer = currentCustomerService.getCurrentCustomer().orElse(null);

        if (customer == null) {
            redirectAttributes.addFlashAttribute("error", "Please log in to add a service.");
            return "redirect:/services";
        }

        OnlineService service = onlineServiceService.getServiceById(serviceId).orElse(null);
        if (service == null) {
            redirectAttributes.addFlashAttribute("error", "Service not found");
            return "redirect:/services";
        }

        serviceOrderService.createOrder(customer, service);
        return "redirect:/orders/my-orders";
    }

    @GetMapping("/download/{orderId}")
    public void downloadReceipt(@PathVariable Long orderId, HttpServletResponse response) {
        try {
            Customer customer = currentCustomerService.getCurrentCustomer().orElse(null);
            if (customer == null) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Please log in to download your receipt.");
                return;
            }

            ServiceOrder order = serviceOrderService.getOrderById(orderId).orElse(null);
            if (order == null || !order.getCustomer().getId().equals(customer.getId())) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Order not found.");
                return;
            }

            receiptService.writeReceipt(order, response);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @PostMapping("/cancel/{orderId}")
    public String cancelOrder(@PathVariable Long orderId, RedirectAttributes redirectAttributes) {
        Customer customer = currentCustomerService.getCurrentCustomer().orElse(null);

        if (customer == null) {
            redirectAttributes.addFlashAttribute("error", "Please log in to cancel orders.");
            return "redirect:/services";
        }

        ServiceOrder order = serviceOrderService.getOrderById(orderId).orElse(null);
        if (order == null || !order.getCustomer().getId().equals(customer.getId())) {
            redirectAttributes.addFlashAttribute("error", "Order not found or you do not have permission to cancel it.");
            return "redirect:/orders/my-orders";
        }

        try {
            serviceOrderService.cancelOrder(order);
            redirectAttributes.addFlashAttribute("success", "Order cancelled and deleted successfully.");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/orders/my-orders";
    }

    @PostMapping("/upload-document/{orderId}")
    public String uploadDocument(@PathVariable Long orderId,
                                 @Valid @ModelAttribute UploadDocumentRequest request,
                                 RedirectAttributes redirectAttributes) {
        Customer customer = currentCustomerService.getCurrentCustomer().orElse(null);

        if (customer == null) {
            redirectAttributes.addFlashAttribute("error", "Please log in to upload documents.");
            return "redirect:/services";
        }

        ServiceOrder order = serviceOrderService.getOrderById(orderId).orElse(null);
        if (order == null || !order.getCustomer().getId().equals(customer.getId())) {
            redirectAttributes.addFlashAttribute("error", "Order not found or you do not have permission to upload documents for this order.");
            return "redirect:/orders/my-orders";
        }

        try {
            uploadedDocumentService.uploadDocument(request, order, customer);
            redirectAttributes.addFlashAttribute("success", "Document uploaded successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to upload document.");
        }
        return "redirect:/orders/my-orders";
    }
}
