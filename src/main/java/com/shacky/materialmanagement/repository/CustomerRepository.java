package com.shacky.materialmanagement.repository;

import com.shacky.materialmanagement.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByLastNameAndPhoneNumber(String lastName, Integer phoneNumber);
    Optional<Customer> findByPhoneNumber(Integer phoneNumber);
    Optional<Customer> findByEmailIgnoreCase(String email);
}
