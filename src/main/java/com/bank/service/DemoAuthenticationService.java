package com.bank.service;

/**
 * Temporary In-Memory Demo Authentication Service.
 * 
 * NOTICE: This is an in-memory mock intended ONLY for college demonstrations
 * and testing Keerthana's JavaFX frontend before Yashaswi connects MySQL.
 * Do NOT use in production.
 */
public class DemoAuthenticationService implements AuthenticationService {

    private String currentUser = "Priya Nair (Branch Officer)";

    @Override
    public boolean authenticate(String username, String password) {
        // Bypass mode: accept any credentials or default to Priya Nair
        if (username != null && !username.trim().isEmpty()) {
            this.currentUser = username.trim();
        } else {
            this.currentUser = "Priya Nair (Branch Officer)";
        }
        return true;
    }

    @Override
    public String getCurrentUser() {
        return (currentUser != null && !currentUser.isBlank()) ? currentUser : "Priya Nair (Branch Officer)";
    }

    @Override
    public void logout() {
        this.currentUser = "Priya Nair (Branch Officer)";
    }

    @Override
    public boolean isDemoMode() {
        return true;
    }
}
