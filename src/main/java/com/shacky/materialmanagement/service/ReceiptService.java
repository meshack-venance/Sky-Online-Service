package com.shacky.materialmanagement.service;

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
import com.shacky.materialmanagement.entity.ServiceOrder;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.format.DateTimeFormatter;

@Service
public class ReceiptService {

    public void writeReceipt(ServiceOrder order, HttpServletResponse response) throws Exception {
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=order-receipt-" + order.getId() + ".pdf");

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

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy 'at' hh:mm a");
        StringBuilder content = new StringBuilder();
        content.append("Order ID: ").append(order.getId()).append("\n");
        content.append("Customer: ").append(order.getCustomer().getFirstName()).append(" ").append(order.getCustomer().getLastName()).append("\n");
        content.append("Phone: ").append(order.getCustomer().getPhoneNumber()).append("\n");
        content.append("Email: ").append(order.getCustomer().getEmail()).append("\n");
        content.append("Region/District: ").append(order.getCustomer().getRegion()).append(" / ").append(order.getCustomer().getDistrict()).append("\n");
        content.append("Service: ").append(order.getService().getTitle()).append("\n");
        content.append("Cost: TSH ").append(order.getService().getCost()).append("\n");
        content.append("Status: ").append(order.getStatus()).append("\n");
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
    }
}
