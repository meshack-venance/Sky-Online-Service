package com.shacky.materialmanagement.auth;

import com.shacky.materialmanagement.entity.Customer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class CustomerPrincipal implements UserDetails {

    private final Long id;
    private final String email;
    private final Integer phoneNumber;
    private final String password;
    private final Collection<? extends GrantedAuthority> authorities;

    public CustomerPrincipal(Customer customer) {
        this.id = customer.getId();
        this.email = customer.getEmail();
        this.phoneNumber = customer.getPhoneNumber();
        this.password = customer.getPassword();
        this.authorities = List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
    }

    public Long getId() {
        return id;
    }

    public Integer getPhoneNumber() {
        return phoneNumber;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email != null && !email.isBlank() ? email : String.valueOf(phoneNumber);
    }
}
