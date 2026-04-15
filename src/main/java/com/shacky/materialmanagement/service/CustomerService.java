package com.shacky.materialmanagement.service;

import com.shacky.materialmanagement.entity.Customer;
import com.shacky.materialmanagement.repository.CustomerRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CustomerService {
    private final CustomerRepository customerRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public CustomerService(CustomerRepository customerRepository, BCryptPasswordEncoder passwordEncoder) {
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Customer saveCustomer(Customer customer) {
        if (customer.getPassword() != null && !isEncoded(customer.getPassword())) {
            customer.setPassword(passwordEncoder.encode(customer.getPassword()));
        }
        return customerRepository.save(customer);
    }
    public Optional<Customer> findByLastNameAndPhoneNumber(String lastName, Integer phoneNumber) {
        return customerRepository.findByLastNameAndPhoneNumber(lastName, phoneNumber);
    }
    public Optional<Customer> getCustomerById(Long id) {
        return customerRepository.findById(id);
    }
    public Optional<Customer> findByPhoneNumber(Integer phoneNumber) {
        return customerRepository.findByPhoneNumber(phoneNumber);
    }

    public Optional<Customer> findByEmail(String email) {
        return customerRepository.findByEmailIgnoreCase(email);
    }

    public boolean passwordMatches(Customer customer, String rawPassword) {
        String storedPassword = customer.getPassword();
        if (storedPassword == null || rawPassword == null) {
            return false;
        }

        if (isEncoded(storedPassword)) {
            return passwordEncoder.matches(rawPassword, storedPassword);
        }

        boolean matches = storedPassword.equals(rawPassword);
        if (matches) {
            customer.setPassword(passwordEncoder.encode(rawPassword));
            customerRepository.save(customer);
        }
        return matches;
    }

    private boolean isEncoded(String password) {
        return password.startsWith("$2a$")
                || password.startsWith("$2b$")
                || password.startsWith("$2y$");
    }
}
