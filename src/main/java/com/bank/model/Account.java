package com.bank.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Represents a bank account.
 * Follows standard banking practices with BigDecimal for monetary precision.
 * 
 * Contributed/Maintained for: Pranita (Account Management) & Yashaswi (AccountDAO)
 */
public class Account {
    private String accountNumber;
    private Long customerId;
    private String customerName;
    private String email;
    private String phone;
    private String accountType; // "SAVINGS" or "CURRENT"
    private BigDecimal balance;
    private String status;      // "ACTIVE", "INACTIVE", "CLOSED"
    private LocalDateTime createdAt;

    public Account() {
        this.balance = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.status = "ACTIVE";
        this.createdAt = LocalDateTime.now();
    }

    public Account(String accountNumber, String customerName, String email, String phone, 
                   String accountType, BigDecimal initialDeposit) {
        this.accountNumber = accountNumber;
        this.customerName = customerName;
        this.email = email;
        this.phone = phone;
        this.accountType = accountType != null ? accountType.toUpperCase() : "SAVINGS";
        this.balance = initialDeposit != null ? initialDeposit.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.status = "ACTIVE";
        this.createdAt = LocalDateTime.now();
    }

    public Account(String accountNumber, Long customerId, String customerName, String email, String phone,
                   String accountType, BigDecimal balance, String status, LocalDateTime createdAt) {
        this.accountNumber = accountNumber;
        this.customerId = customerId;
        this.customerName = customerName;
        this.email = email;
        this.phone = phone;
        this.accountType = accountType != null ? accountType.toUpperCase() : "SAVINGS";
        this.balance = balance != null ? balance.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.status = status != null ? status.toUpperCase() : "ACTIVE";
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }

    // Getters and Setters
    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
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

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType != null ? accountType.toUpperCase() : "SAVINGS";
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance != null ? balance.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status != null ? status.toUpperCase() : "ACTIVE";
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    // Presentation helpers for JavaFX TableView bindings
    public String getFormattedBalance() {
        if (balance == null) return "$0.00";
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);
        return currencyFormat.format(balance);
    }

    public String getFormattedDate() {
        if (createdAt == null) return "";
        return createdAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

    @Override
    public String toString() {
        return "Account{" +
                "accountNumber='" + accountNumber + '\'' +
                ", customerName='" + customerName + '\'' +
                ", accountType='" + accountType + '\'' +
                ", balance=" + balance +
                ", status='" + status + '\'' +
                '}';
    }
}
