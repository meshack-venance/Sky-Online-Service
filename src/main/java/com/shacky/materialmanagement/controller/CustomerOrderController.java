package com.shacky.materialmanagement.controller;

import com.shacky.materialmanagement.auth.CurrentCustomerService;
import com.shacky.materialmanagement.entity.Customer;
import com.shacky.materialmanagement.entity.OnlineService;
import com.shacky.materialmanagement.entity.ServiceOrder;
import com.shacky.materialmanagement.entity.UploadedDocument;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;

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

        Customer customer = customerService.findByLastNameAndPhoneNumber(request.getLastName(), request.getPhoneNumber()).orElse(null);

        if (customer == null) {
            customer = new Customer();
            customer.setFirstName(request.getFirstName());
            customer.setLastName(request.getLastName());
            customer.setPhoneNumber(request.getPhoneNumber());
            customer.setEmail(request.getEmail());
            customer.setRegion(request.getRegion());
            customer.setDistrict(request.getDistrict());
            customer.setPassword(request.getPassword());
            customer = customerService.saveCustomer(customer);
        }

        OnlineService service = onlineServiceService.getServiceById(request.getServiceId()).orElse(null);
        if (service == null) {
            redirectAttributes.addFlashAttribute("error", "Selected service not found.");
            return "redirect:/services";
        }

        double totalCost = service.getCost() != null ? service.getCost() : 0.0;
        List<ServiceOrder> existingOrders = serviceOrderService.getOrdersByCustomer(customer);
        for (ServiceOrder existingOrder : existingOrders) {
            if (existingOrder.getService().getCost() != null) {
                totalCost += existingOrder.getService().getCost();
            }
        }

        ServiceOrder order = new ServiceOrder();
        order.setCustomer(customer);
        order.setService(service);
        order.setStatus("Pending");
        order.setDatePlaced(LocalDateTime.now());
        serviceOrderService.saveOrder(order);

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

        List<ServiceOrder> orders = serviceOrderService.getOrdersByCustomer(customer);
        model.addAttribute("orders", orders);
        model.addAttribute("customer", customer);

        double totalCost = orders.stream()
                .mapToDouble(order -> order.getService().getCost() != null ? order.getService().getCost() : 0.0)
                .sum();
        model.addAttribute("totalCost", totalCost);

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

        ServiceOrder order = new ServiceOrder();
        order.setCustomer(customer);
        order.setService(service);
        order.setDatePlaced(LocalDateTime.now());
        order.setStatus("Pending");

        serviceOrderService.saveOrder(order);
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

        if (!"Pending".equals(order.getStatus())) {
            redirectAttributes.addFlashAttribute("error", "Only pending orders can be cancelled.");
            return "redirect:/orders/my-orders";
        }

        serviceOrderService.deleteOrder(order);
        redirectAttributes.addFlashAttribute("success", "Order cancelled and deleted successfully.");
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

        MultipartFile file = request.getDocument();
        String documentPath = "/path/to/uploaded/document/" + file.getOriginalFilename();

        UploadedDocument uploadedDocument = new UploadedDocument();
        uploadedDocument.setServiceOrder(order);
        uploadedDocument.setCustomer(customer);
        uploadedDocument.setDocumentPath(documentPath);
        uploadedDocument.setDescription(request.getDescription());

        uploadedDocumentService.saveDocument(uploadedDocument);
        redirectAttributes.addFlashAttribute("success", "Document uploaded successfully.");
        return "redirect:/orders/my-orders";
    }
}
