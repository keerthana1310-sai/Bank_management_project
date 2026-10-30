package com.bank.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Represents a banking transaction (Deposit, Withdrawal, or Transfer).
 * Contributed/Maintained for: Jayasree (Transactions) & Yashaswi (TransactionDAO)
 */
public class Transaction {
    private Long transactionId;
    private String transactionType; // "DEPOSIT", "WITHDRAWAL", "TRANSFER"
    private String sourceAccount;
    private String destinationAccount; // Optional for non-transfers
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private String description;
    private LocalDateTime timestamp;

    public Transaction() {
        this.timestamp = LocalDateTime.now();
    }

    public Transaction(Long transactionId, String transactionType, String sourceAccount, 
                       String destinationAccount, BigDecimal amount, BigDecimal balanceAfter, 
                       String description, LocalDateTime timestamp) {
        this.transactionId = transactionId;
        this.transactionType = transactionType != null ? transactionType.toUpperCase() : "DEPOSIT";
        this.sourceAccount = sourceAccount;
        this.destinationAccount = destinationAccount;
        this.amount = amount != null ? amount.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.balanceAfter = balanceAfter != null ? balanceAfter.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.description = description != null ? description : "";
        this.timestamp = timestamp != null ? timestamp : LocalDateTime.now();
    }

    public Long getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Long transactionId) {
        this.transactionId = transactionId;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType != null ? transactionType.toUpperCase() : "DEPOSIT";
    }

    public String getSourceAccount() {
        return sourceAccount;
    }

    public void setSourceAccount(String sourceAccount) {
        this.sourceAccount = sourceAccount;
    }

    public String getDestinationAccount() {
        return destinationAccount;
    }

    public void setDestinationAccount(String destinationAccount) {
        this.destinationAccount = destinationAccount;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount != null ? amount.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public void setBalanceAfter(BigDecimal balanceAfter) {
        this.balanceAfter = balanceAfter != null ? balanceAfter.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    // Presentation helpers for JavaFX TableView
    public String getFormattedAmount() {
        if (amount == null) return "$0.00";
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);
        return currencyFormat.format(amount);
    }

    public String getFormattedBalanceAfter() {
        if (balanceAfter == null) return "$0.00";
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);
        return currencyFormat.format(balanceAfter);
    }

    public String getFormattedTimestamp() {
        if (timestamp == null) return "";
        return timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    public String getDisplayDestination() {
        return (destinationAccount == null || destinationAccount.isBlank()) ? "—" : destinationAccount;
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "id=" + transactionId +
                ", type='" + transactionType + '\'' +
                ", source='" + sourceAccount + '\'' +
                ", destination='" + destinationAccount + '\'' +
                ", amount=" + amount +
                ", timestamp=" + timestamp +
                '}';
    }
}
