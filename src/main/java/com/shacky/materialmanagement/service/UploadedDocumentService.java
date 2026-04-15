package com.shacky.materialmanagement.service;

import com.shacky.materialmanagement.entity.Customer;
import com.shacky.materialmanagement.entity.ServiceOrder;
import com.shacky.materialmanagement.entity.UploadedDocument;
import com.shacky.materialmanagement.repository.UploadedDocumentRepository;
import com.shacky.materialmanagement.util.FileStorageUtil;
import com.shacky.materialmanagement.web.request.UploadDocumentRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class UploadedDocumentService {

    @Autowired
    private UploadedDocumentRepository uploadedDocumentRepository;

    public UploadedDocument saveDocument(UploadedDocument document) {
        document.setUploadDate(LocalDateTime.now());
        return uploadedDocumentRepository.save(document);
    }

    public List<UploadedDocument> getDocumentsByServiceOrder(Long serviceOrderId) {
        return uploadedDocumentRepository.findByServiceOrderId(serviceOrderId);
    }

    public List<UploadedDocument> getDocumentsByCustomer(Long customerId) {
        return uploadedDocumentRepository.findByCustomerId(customerId);
    }

    /**
     * Stores the uploaded file, constructs the UploadedDocument entity, and persists it.
     */
    public UploadedDocument uploadDocument(UploadDocumentRequest request, ServiceOrder order, Customer customer) throws IOException {
        MultipartFile file = request.getDocument();
        String storedFileName = FileStorageUtil.store(file);
        String documentPath = "/uploads/" + storedFileName;

        UploadedDocument uploadedDocument = new UploadedDocument();
        uploadedDocument.setServiceOrder(order);
        uploadedDocument.setCustomer(customer);
        uploadedDocument.setDocumentPath(documentPath);
        uploadedDocument.setDescription(request.getDescription());
        uploadedDocument.setUploadDate(LocalDateTime.now());

        return uploadedDocumentRepository.save(uploadedDocument);
    }
}
