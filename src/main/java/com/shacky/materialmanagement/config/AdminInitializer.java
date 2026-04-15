package com.shacky.materialmanagement.config;

import com.shacky.materialmanagement.entity.Admin;
import com.shacky.materialmanagement.repository.AdminRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Configuration
public class AdminInitializer {

    @Bean
    CommandLineRunner initAdmin(
            AdminRepository adminRepo,
            BCryptPasswordEncoder encoder,
            @Value("${app.admin.bootstrap.username:}") String bootstrapUsername,
            @Value("${app.admin.bootstrap.password:}") String bootstrapPassword
    ) {
        return args -> {
            if (!adminRepo.findAll().isEmpty()) {
                return;
            }

            if (bootstrapUsername == null || bootstrapUsername.isBlank()
                    || bootstrapPassword == null || bootstrapPassword.isBlank()) {
                System.out.println("No bootstrap admin configured. Skipping admin initialization.");
                return;
            }

            if (adminRepo.findByUsername(bootstrapUsername).isEmpty()) {
                Admin admin = new Admin();
                admin.setUsername(bootstrapUsername);
                admin.setPassword(encoder.encode(bootstrapPassword));
                adminRepo.save(admin);
                System.out.println("Bootstrap admin created");
            }
        };
    }
}
