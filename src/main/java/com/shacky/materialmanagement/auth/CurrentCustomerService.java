package com.shacky.materialmanagement.auth;

import com.shacky.materialmanagement.entity.Customer;
import com.shacky.materialmanagement.service.CustomerService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CurrentCustomerService {

    private final CustomerService customerService;

    public CurrentCustomerService(CustomerService customerService) {
        this.customerService = customerService;
    }

    public Optional<Customer> getCurrentCustomer() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomerPrincipal principal)) {
            return Optional.empty();
        }
        return customerService.getCustomerById(principal.getId());
    }
}
