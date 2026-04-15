package com.shacky.materialmanagement.controller;

import com.shacky.materialmanagement.auth.CurrentCustomerService;
import com.shacky.materialmanagement.auth.CustomerAuthCookieService;
import com.shacky.materialmanagement.auth.JwtTokenService;
import com.shacky.materialmanagement.auth.RefreshTokenService;
import com.shacky.materialmanagement.entity.*;
import com.shacky.materialmanagement.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;


import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
@RequestMapping("/orders")
public class OrderController {

    @Autowired
    private CustomerService customerService;

    @Autowired
    private OnlineServiceService onlineServiceService;

    @Autowired
    private ServiceOrderService serviceOrderService;
    @Autowired
    private UploadedDocumentService uploadedDocumentService;
    @Autowired
    private CurrentCustomerService currentCustomerService;
    @Autowired
    private JwtTokenService jwtTokenService;
    @Autowired
    private RefreshTokenService refreshTokenService;
    @Autowired
    private CustomerAuthCookieService customerAuthCookieService;

    @PostMapping("/place")
    public String placeOrder(@RequestParam String firstName,
                             @RequestParam String lastName,
                             @RequestParam Integer phoneNumber,
                             @RequestParam String email,
                             @RequestParam String region,
                             @RequestParam String district,
                             @RequestParam String password,
                             @RequestParam Long serviceId,
                             RedirectAttributes redirectAttributes) {

        Customer customer = customerService.findByLastNameAndPhoneNumber(lastName, phoneNumber).orElse(null);

        if (customer == null) {
            customer = new Customer();
            customer.setFirstName(firstName);
            customer.setLastName(lastName);
            customer.setPhoneNumber(phoneNumber);
            customer.setEmail(email);
            customer.setRegion(region);
            customer.setDistrict(district);
            customer.setPassword(password); // You can later hash this
            customer = customerService.saveCustomer(customer);
        }

        OnlineService service = onlineServiceService.getServiceById(serviceId).orElse(null);
        if (service == null) {
            redirectAttributes.addFlashAttribute("error", "Selected service not found.");
            return "redirect:/services";
        }

        double totalCost = service.getCost() != null ? service.getCost() : 0.0;
        List<ServiceOrder> existingOrders = serviceOrderService.getOrdersByCustomer(customer);
        for (ServiceOrder o : existingOrders) {
            if (o.getService().getCost() != null) {
                totalCost += o.getService().getCost();
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

        return "redirect:/orders/details/" + serviceId;

    }

    @GetMapping("/details/{id}")
    public String viewServiceDetails(@PathVariable Long id, Model model) {
        Customer loggedInCustomer = currentCustomerService.getCurrentCustomer().orElse(null);
        if (loggedInCustomer != null) {
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


    @PostMapping("/login")
    public String loginToViewOrders(@RequestParam String identifier,
                                    @RequestParam String password,
                                    HttpServletResponse response,
                                    RedirectAttributes redirectAttributes) {

        String normalizedIdentifier = identifier == null ? "" : identifier.trim();
        Customer customer;
        try {
            Integer phoneNumber = Integer.parseInt(normalizedIdentifier);
            customer = customerService.findByPhoneNumber(phoneNumber).orElse(null);
        } catch (NumberFormatException e) {
            customer = customerService.findByEmail(normalizedIdentifier).orElse(null);
        }

        if (customer != null && customer.getPassword().equals(password)) {
            String accessToken = jwtTokenService.generateAccessToken(customer);
            JwtTokenService.RefreshTokenPayload refreshToken = jwtTokenService.generateRefreshToken(customer);
            refreshTokenService.create(customer, refreshToken);
            customerAuthCookieService.writeAccessToken(response, accessToken);
            customerAuthCookieService.writeRefreshToken(response, refreshToken.token());
            return "redirect:/orders/my-orders";
        }
        customerAuthCookieService.clearTokens(response);
        redirectAttributes.addFlashAttribute("error", "Invalid credentials. Please try again.");
        return "redirect:/services";
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

        // Calculate total cost
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
        order.setService(service); // Make sure to use setService (not setOnlineService)
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

            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "attachment; filename=order-receipt-" + orderId + ".pdf");

            Document document = new Document();
            PdfWriter writer = PdfWriter.getInstance(document, response.getOutputStream());

            // Add watermark
            writer.setPageEvent(new PdfPageEventHelper() {
                Font watermarkFont = new Font(Font.FontFamily.HELVETICA, 52, Font.BOLD, new BaseColor(200, 200, 200));

                @Override
                public void onEndPage(PdfWriter writer, Document document) {
                    PdfContentByte canvas = writer.getDirectContentUnder();
                    Phrase watermark = new Phrase("OFFICIAL RECEIPT", watermarkFont);
                    ColumnText.showTextAligned(canvas, Element.ALIGN_CENTER,
                            watermark,
                            (document.right() + document.left()) / 2,
                            (document.top() + document.bottom()) / 2,
                            45);
                }
            });

            document.open();

            // Logo
//            String logoPath = "src/main/resources/static/images/kyline-logo.jpeg"; // adjust path
//            Image logo = Image.getInstance(logoPath);
            InputStream logoStream = getClass().getResourceAsStream("/static/images/kyline-logo.jpeg");
            Image logo = Image.getInstance(IOUtils.toByteArray(logoStream));

            logo.scaleAbsolute(100, 100);
            logo.setAlignment(Image.ALIGN_CENTER);
            document.add(logo);

            // Title
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Paragraph title = new Paragraph("Service Order Receipt", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            // Bordered Table
            PdfPTable table = new PdfPTable(1);
            table.setWidthPercentage(90);
            table.setSpacingBefore(10f);
            table.setSpacingAfter(10f);

            PdfPCell cell = new PdfPCell();
            cell.setBorderWidth(2f);
            cell.setPadding(10f);

            StringBuilder content = new StringBuilder();
            content.append("Order ID: ").append(order.getId()).append("\n");
            content.append("Customer: ").append(order.getCustomer().getFirstName()).append(" ").append(order.getCustomer().getLastName()).append("\n");
            content.append("Phone: ").append(order.getCustomer().getPhoneNumber()).append("\n");
            content.append("Email: ").append(order.getCustomer().getEmail()).append("\n");
            content.append("Region/District: ").append(order.getCustomer().getRegion()).append(" / ").append(order.getCustomer().getDistrict()).append("\n");
            content.append("Service: ").append(order.getService().getTitle()).append("\n");
            content.append("Cost: TSH ").append(order.getService().getCost()).append("\n");
            content.append("Status: ").append(order.getStatus()).append("\n");
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy 'at' hh:mm a");
            String formattedDateTime = order.getDatePlaced().format(formatter);
            content.append("Date Placed: ").append(formattedDateTime).append("\n");

            cell.setPhrase(new Phrase(content.toString(), FontFactory.getFont(FontFactory.HELVETICA, 12)));
            table.addCell(cell);
            document.add(table);

            // QR Code
            BarcodeQRCode qrCode = new BarcodeQRCode("Order ID: " + order.getId() +
                    "\nCustomer: " + order.getCustomer().getFirstName() + " " + order.getCustomer().getLastName() +
                    "\nService: " + order.getService().getTitle(), 150, 150, null);
            Image qrImage = qrCode.getImage();
            qrImage.scaleAbsolute(100, 100);
            qrImage.setAlignment(Image.ALIGN_CENTER);
            document.add(qrImage);

            // Admin Signature Line
//            Paragraph signature = new Paragraph("\n\nAuthorized By (Admin): ___________________________", FontFactory.getFont(FontFactory.HELVETICA, 12));
//            signature.setSpacingBefore(30);
//            document.add(signature);

            // Footer
            Font footerFont = FontFactory.getFont(FontFactory.HELVETICA, 10, BaseColor.GRAY);
            Phrase footer = new Phrase("For assistance, contact  (+255) 750 613 191 ", footerFont);
            ColumnText.showTextAligned(writer.getDirectContent(),
                    Element.ALIGN_CENTER,
                    footer,
                    (document.right() + document.left()) / 2,
                    document.bottom() - 10, 0);

            document.close();
            writer.close();

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

        serviceOrderService.deleteOrder(order); // Delete from database

        redirectAttributes.addFlashAttribute("success", "Order cancelled and deleted successfully.");
        return "redirect:/orders/my-orders";
    }
    @PostMapping("/upload-document/{orderId}")
    public String uploadDocument(@PathVariable Long orderId,
                                 @RequestParam("document") MultipartFile file,
                                 @RequestParam String description,
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

        // Save the document file (e.g., in a local directory or cloud storage)
        // Assuming the file is saved successfully, you get its path
        String documentPath = "/path/to/uploaded/document/" + file.getOriginalFilename();

        UploadedDocument uploadedDocument = new UploadedDocument();
        uploadedDocument.setServiceOrder(order);
        uploadedDocument.setCustomer(customer);
        uploadedDocument.setDocumentPath(documentPath);
        uploadedDocument.setDescription(description);

        uploadedDocumentService.saveDocument(uploadedDocument);

        redirectAttributes.addFlashAttribute("success", "Document uploaded successfully.");
        return "redirect:/orders/my-orders";
    }


    @GetMapping("/logout")
    public String logout(HttpServletResponse response) {
        currentCustomerService.getCurrentCustomer().ifPresent(refreshTokenService::revokeAllForCustomer);
        customerAuthCookieService.clearTokens(response);
        return "redirect:/services";
    }


}
