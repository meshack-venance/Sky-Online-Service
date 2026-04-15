package com.shacky.materialmanagement.web.request;

import jakarta.validation.constraints.NotBlank;

public class CustomerLoginRequest {

    @NotBlank
    private String identifier;

    @NotBlank
    private String password;

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
