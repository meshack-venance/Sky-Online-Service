package com.shacky.materialmanagement.controller;

import com.shacky.materialmanagement.auth.CurrentCustomerService;
import com.shacky.materialmanagement.auth.CustomerAuthenticationService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    public String loginToViewOrders(@RequestParam String identifier,
                                    @RequestParam String password,
                                    HttpServletResponse response,
                                    RedirectAttributes redirectAttributes) {
        if (customerAuthenticationService.authenticate(identifier, password, response).isPresent()) {
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
