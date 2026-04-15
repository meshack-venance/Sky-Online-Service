package com.shacky.materialmanagement.auth;

import com.shacky.materialmanagement.entity.Customer;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class CurrentCustomerModelAdvice {

    private final CurrentCustomerService currentCustomerService;

    public CurrentCustomerModelAdvice(CurrentCustomerService currentCustomerService) {
        this.currentCustomerService = currentCustomerService;
    }

    @ModelAttribute("currentCustomer")
    public Customer currentCustomer() {
        return currentCustomerService.getCurrentCustomer().orElse(null);
    }
}
