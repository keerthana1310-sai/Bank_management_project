package com.bank.service;

import com.bank.model.Account;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Temporary In-Memory Demo Account Service.
 * 
 * NOTICE: This is an in-memory implementation for college project demonstration
 * and UI testing. All operations mutate an in-memory map and ARE NOT saved to MySQL.
 * 
 * Teammate Pranita will replace/supplement this with JdbcAccountService.
 */
public class DemoAccountService implements AccountService {

    private final Map<String, Account> accounts = new ConcurrentHashMap<>();
    private final AtomicInteger accountSequence = new AtomicInteger(1005);

    public DemoAccountService() {
        seedSampleAccounts();
    }

    private void seedSampleAccounts() {
        // Pre-populate realistic accounts for demonstration
        addAccountInternal(new Account("ACC-1001", 1L, "Keerthana Rao", "keerthana@bank.com", "9876543210", 
                "SAVINGS", new BigDecimal("12500.00"), "ACTIVE", LocalDateTime.now().minusDays(30)));
        addAccountInternal(new Account("ACC-1002", 2L, "Pranita Sharma", "pranita@bank.com", "9876543211", 
                "CURRENT", new BigDecimal("45000.00"), "ACTIVE", LocalDateTime.now().minusDays(20)));
        addAccountInternal(new Account("ACC-1003", 3L, "Jayasree Patel", "jayasree@bank.com", "9876543212", 
                "SAVINGS", new BigDecimal("8200.50"), "ACTIVE", LocalDateTime.now().minusDays(15)));
        addAccountInternal(new Account("ACC-1004", 4L, "Yashaswi Reddy", "yashaswi@bank.com", "9876543213", 
                "SAVINGS", new BigDecimal("19400.75"), "ACTIVE", LocalDateTime.now().minusDays(10)));
    }

    private void addAccountInternal(Account account) {
        accounts.put(account.getAccountNumber(), account);
    }

    @Override
    public Account createAccount(String customerName, String email, String phone, 
                                 String accountType, BigDecimal initialDeposit) throws BankingException {
        // Form & business validations
        if (customerName == null || customerName.trim().isEmpty()) {
            throw new BankingException("Customer name cannot be empty.");
        }
        if (email == null || !email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new BankingException("Please provide a valid email address.");
        }
        if (phone == null || phone.trim().length() < 7) {
            throw new BankingException("Please provide a valid phone number (at least 7 digits).");
        }
        if (accountType == null || (!accountType.equalsIgnoreCase("SAVINGS") && !accountType.equalsIgnoreCase("CURRENT"))) {
            throw new BankingException("Account type must be either SAVINGS or CURRENT.");
        }
        if (initialDeposit == null || initialDeposit.compareTo(BigDecimal.ZERO) < 0) {
            throw new BankingException("Initial deposit must be non-negative.");
        }

        String newAccountNumber = "ACC-" + accountSequence.getAndIncrement();
        Account newAccount = new Account(
                newAccountNumber,
                customerName.trim(),
                email.trim(),
                phone.trim(),
                accountType.toUpperCase(),
                initialDeposit.setScale(2, RoundingMode.HALF_UP)
        );

        accounts.put(newAccountNumber, newAccount);
        return newAccount;
    }

    @Override
    public Optional<Account> findAccount(String accountNumber) throws BankingException {
        if (accountNumber == null || accountNumber.trim().isEmpty()) {
            throw new BankingException("Account number cannot be empty.");
        }
        return Optional.ofNullable(accounts.get(accountNumber.trim().toUpperCase()));
    }

    @Override
    public List<Account> getAllAccounts() throws BankingException {
        List<Account> list = new ArrayList<>(accounts.values());
        list.sort(Comparator.comparing(Account::getAccountNumber));
        return list;
    }

    @Override
    public Account updateAccount(Account account) throws BankingException {
        if (account == null || account.getAccountNumber() == null) {
            throw new BankingException("Invalid account object provided for update.");
        }
        if (!accounts.containsKey(account.getAccountNumber())) {
            throw new BankingException("Account " + account.getAccountNumber() + " not found.");
        }
        accounts.put(account.getAccountNumber(), account);
        return account;
    }

    @Override
    public int getTotalAccountsCount() {
        return accounts.size();
    }

    @Override
    public BigDecimal getTotalBalanceSum() {
        BigDecimal sum = BigDecimal.ZERO;
        for (Account a : accounts.values()) {
            if (a.getBalance() != null) {
                sum = sum.add(a.getBalance());
            }
        }
        return sum.setScale(2, RoundingMode.HALF_UP);
    }
}
