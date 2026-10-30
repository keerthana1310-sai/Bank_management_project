package com.bank.model;

import java.time.LocalDateTime;

/**
 * Represents a bank customer.
 * Used for customer profile information associated with bank accounts.
 * 
 * Contributed/Maintained for: Pranita (Account Management) & Yashaswi (CustomerDAO)
 */
public class Customer {
    private Long customerId;
    private String name;
    private String email;
    private String phone;
    private String address;
    private LocalDateTime createdAt;

    public Customer() {
        this.createdAt = LocalDateTime.now();
    }

    public Customer(String name, String email, String phone) {
        this(null, name, email, phone, "", LocalDateTime.now());
    }

    public Customer(String name, String email, String phone, String address) {
        this(null, name, email, phone, address, LocalDateTime.now());
    }

    public Customer(Long customerId, String name, String email, String phone, String address, LocalDateTime createdAt) {
        this.customerId = customerId;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.address = address;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Customer{" +
                "customerId=" + customerId +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", phone='" + phone + '\'' +
                '}';
    }
}
