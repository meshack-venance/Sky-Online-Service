package com.shacky.materialmanagement.service;

import com.shacky.materialmanagement.entity.OnlineService;
import com.shacky.materialmanagement.repository.OnlineServiceRepository;
import com.shacky.materialmanagement.web.request.AdminServiceRequest;
import com.shacky.materialmanagement.web.request.CreateServiceRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class OnlineServiceService {

    @Autowired
    private OnlineServiceRepository onlineServiceRepository;

    public List<OnlineService> getAllServices() {
        return onlineServiceRepository.findAll();
    }

    public void saveService(OnlineService service) {
        onlineServiceRepository.save(service);
    }

    public void deleteService(Long id) {
        onlineServiceRepository.deleteById(id);
    }

    public OnlineService getService(Long id) {
        return onlineServiceRepository.findById(id).orElse(null);
    }

    public Optional<OnlineService> getServiceById(Long id) {
        return onlineServiceRepository.findById(id);
    }

    /**
     * Creates and persists a new OnlineService from the given request.
     */
    public OnlineService createService(CreateServiceRequest request) {
        OnlineService onlineService = new OnlineService();
        onlineService.setTitle(request.getTitle());
        onlineService.setRequirements(request.getRequirements());
        onlineService.setCost(request.getCost());
        return onlineServiceRepository.save(onlineService);
    }

    /**
     * Updates an existing OnlineService identified by request ID.
     *
     * @throws IllegalArgumentException if no service with the given ID exists
     */
    public OnlineService updateService(AdminServiceRequest request) {
        OnlineService existing = onlineServiceRepository.findById(request.getId())
                .orElseThrow(() -> new IllegalArgumentException("Service not found."));
        existing.setTitle(request.getTitle());
        existing.setRequirements(request.getRequirements());
        existing.setCost(request.getCost());
        return onlineServiceRepository.save(existing);
    }
}
