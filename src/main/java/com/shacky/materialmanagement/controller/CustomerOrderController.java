package com.shacky.materialmanagement.controller;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Image;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.BarcodeQRCode;
import com.itextpdf.text.pdf.ColumnText;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfPageEventHelper;
import com.itextpdf.text.pdf.PdfWriter;
import com.shacky.materialmanagement.auth.CurrentCustomerService;
import com.shacky.materialmanagement.entity.Customer;
import com.shacky.materialmanagement.entity.OnlineService;
import com.shacky.materialmanagement.entity.ServiceOrder;
import com.shacky.materialmanagement.entity.UploadedDocument;
import com.shacky.materialmanagement.service.CustomerService;
import com.shacky.materialmanagement.service.OnlineServiceService;
import com.shacky.materialmanagement.service.ServiceOrderService;
import com.shacky.materialmanagement.service.UploadedDocumentService;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
@RequestMapping("/orders")
public class CustomerOrderController {

    private final CustomerService customerService;
    private final OnlineServiceService onlineServiceService;
    private final ServiceOrderService serviceOrderService;
    private final UploadedDocumentService uploadedDocumentService;
    private final CurrentCustomerService currentCustomerService;

    public CustomerOrderController(
            CustomerService customerService,
            OnlineServiceService onlineServiceService,
            ServiceOrderService serviceOrderService,
            UploadedDocumentService uploadedDocumentService,
            CurrentCustomerService currentCustomerService
    ) {
        this.customerService = customerService;
        this.onlineServiceService = onlineServiceService;
        this.serviceOrderService = serviceOrderService;
        this.uploadedDocumentService = uploadedDocumentService;
        this.currentCustomerService = currentCustomerService;
    }

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
            customer.setPassword(password);
            customer = customerService.saveCustomer(customer);
        }

        OnlineService service = onlineServiceService.getServiceById(serviceId).orElse(null);
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

        return "redirect:/orders/details/" + serviceId;
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

            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "attachment; filename=order-receipt-" + orderId + ".pdf");

            Document document = new Document();
            PdfWriter writer = PdfWriter.getInstance(document, response.getOutputStream());
            writer.setPageEvent(new PdfPageEventHelper() {
                private final Font watermarkFont = new Font(Font.FontFamily.HELVETICA, 52, Font.BOLD, new BaseColor(200, 200, 200));

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

            InputStream logoStream = getClass().getResourceAsStream("/static/images/kyline-logo.jpeg");
            Image logo = Image.getInstance(IOUtils.toByteArray(logoStream));
            logo.scaleAbsolute(100, 100);
            logo.setAlignment(Image.ALIGN_CENTER);
            document.add(logo);

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Paragraph title = new Paragraph("Service Order Receipt", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

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
            content.append("Date Placed: ").append(order.getDatePlaced().format(formatter)).append("\n");

            cell.setPhrase(new Phrase(content.toString(), FontFactory.getFont(FontFactory.HELVETICA, 12)));
            table.addCell(cell);
            document.add(table);

            BarcodeQRCode qrCode = new BarcodeQRCode("Order ID: " + order.getId() +
                    "\nCustomer: " + order.getCustomer().getFirstName() + " " + order.getCustomer().getLastName() +
                    "\nService: " + order.getService().getTitle(), 150, 150, null);
            Image qrImage = qrCode.getImage();
            qrImage.scaleAbsolute(100, 100);
            qrImage.setAlignment(Image.ALIGN_CENTER);
            document.add(qrImage);

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

        serviceOrderService.deleteOrder(order);
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
}
