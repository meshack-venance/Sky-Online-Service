package com.shacky.materialmanagement.controller;

import com.shacky.materialmanagement.auth.CurrentCustomerService;
import com.shacky.materialmanagement.auth.CustomerAuthenticationService;
import com.shacky.materialmanagement.web.request.CustomerLoginRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/orders")
public class CustomerAuthController {

    private final CurrentCustomerService currentCustomerService;
    private final CustomerAuthenticationService customerAuthenticationService;

    public CustomerAuthController(
            CurrentCustomerService currentCustomerService,
            CustomerAuthenticationService customerAuthenticationService
    ) {
        this.currentCustomerService = currentCustomerService;
        this.customerAuthenticationService = customerAuthenticationService;
    }

    @PostMapping("/login")
    public String loginToViewOrders(@Valid @ModelAttribute CustomerLoginRequest request,
                                    HttpServletResponse response,
                                    RedirectAttributes redirectAttributes) {
        if (customerAuthenticationService.authenticate(request.getIdentifier(), request.getPassword(), response).isPresent()) {
            return "redirect:/orders/my-orders";
        }
        redirectAttributes.addFlashAttribute("error", "Invalid credentials. Please try again.");
        return "redirect:/services";
    }

    @GetMapping("/logout")
    public String logout(HttpServletResponse response) {
        currentCustomerService.getCurrentCustomer()
                .ifPresentOrElse(
                        customer -> customerAuthenticationService.logout(customer, response),
                        () -> customerAuthenticationService.clearTokens(response)
                );
        return "redirect:/services";
    }
}
