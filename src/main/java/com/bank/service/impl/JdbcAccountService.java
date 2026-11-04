
package com.bank.service.impl;

import com.bank.dao.AccountDAO;
import com.bank.dao.CustomerDAO;
import com.bank.model.Account;
import com.bank.model.Customer;
import com.bank.service.AccountService;
import com.bank.service.BankingException;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class JdbcAccountService implements AccountService {

    private final AccountDAO accountDAO;
    private final CustomerDAO customerDAO;

    public JdbcAccountService() {
        this.accountDAO = new AccountDAO();
        this.customerDAO = new CustomerDAO();
    }

    // 1. Create a new customer and bank account
    @Override
    public Account createAccount(String customerName, String email,
                                 String phone, String accountType,
                                 BigDecimal initialDeposit)
            throws BankingException {

        if (customerName == null || customerName.trim().isEmpty()) {
            throw new BankingException("Customer name is required");
        }

        if (email == null || !email.trim().contains("@")) {
            throw new BankingException("Enter a valid email");
        }

        if (phone == null || phone.trim().isEmpty()) {
            throw new BankingException("Phone number is required");
        }

        if (accountType == null
                || (!"SAVINGS".equalsIgnoreCase(accountType)
                && !"CURRENT".equalsIgnoreCase(accountType))) {
            throw new BankingException(
                    "Account type must be SAVINGS or CURRENT");
        }

        if (initialDeposit == null
                || initialDeposit.compareTo(BigDecimal.ZERO) < 0) {
            throw new BankingException(
                    "Initial deposit cannot be negative");
        }

        try {
            Customer customer = new Customer(
                    customerName.trim(),
                    email.trim(),
                    phone.trim()
            );

            Long customerId = customerDAO.save(customer);

            if (customerId == null) {
                throw new BankingException("Failed to create customer");
            }

            String accountNumber = "ACC-" +
                    UUID.randomUUID().toString()
                            .replace("-", "")
                            .substring(0, 12)
                            .toUpperCase();

            Account account = new Account(
                    accountNumber,
                    customerId,
                    customerName.trim(),
                    email.trim(),
                    phone.trim(),
                    accountType.toUpperCase(),
                    initialDeposit,
                    "ACTIVE",
                    LocalDateTime.now()
            );

            boolean saved = accountDAO.save(account);

            if (!saved) {
                throw new BankingException("Failed to create account");
            }

            return account;

        } catch (SQLException e) {
            throw new BankingException(
                    "Database error while creating account", e);
        }
    }

    // 2. Find an account using its account number
    @Override
    public Optional<Account> findAccount(String accountNumber)
            throws BankingException {

        if (accountNumber == null || accountNumber.trim().isEmpty()) {
            throw new BankingException("Account number is required");
        }

        try {
            return accountDAO.findByAccountNumber(accountNumber.trim());
        } catch (SQLException e) {
            throw new BankingException("Failed to find account", e);
        }
    }

    // 3. Retrieve all accounts
    @Override
    public List<Account> getAllAccounts() throws BankingException {
        try {
            return accountDAO.findAll();
        } catch (SQLException e) {
            throw new BankingException(
                    "Failed to retrieve accounts", e);
        }
    }

    // 4. Update account details
    @Override
    public Account updateAccount(Account account)
            throws BankingException {

        if (account == null
                || account.getAccountNumber() == null
                || account.getAccountNumber().trim().isEmpty()) {
            throw new BankingException("Account details are required");
        }

        try {
            Optional<Account> existing =
                    accountDAO.findByAccountNumber(
                            account.getAccountNumber());

            if (existing.isEmpty()) {
                throw new BankingException("Account not found");
            }

            if (account.getCustomerName() == null
                    || account.getCustomerName().trim().isEmpty()) {
                throw new BankingException("Customer name is required");
            }

            if (account.getEmail() == null
                    || !account.getEmail().contains("@")) {
                throw new BankingException("Enter a valid email");
            }

            if (account.getPhone() == null
                    || account.getPhone().trim().isEmpty()) {
                throw new BankingException("Phone number is required");
            }

            if (!"SAVINGS".equalsIgnoreCase(account.getAccountType())
                    && !"CURRENT".equalsIgnoreCase(
                            account.getAccountType())) {
                throw new BankingException("Invalid account type");
            }

            if (account.getBalance() == null
                    || account.getBalance().compareTo(
                            BigDecimal.ZERO) < 0) {
                throw new BankingException(
                        "Balance cannot be negative");
            }

            // Preserve the existing balance and status.
            // Balance changes should go through transaction logic.
            Account existingAccount = existing.get();
            account.setBalance(existingAccount.getBalance());
            account.setStatus(existingAccount.getStatus());
            account.setCustomerId(existingAccount.getCustomerId());
            account.setCreatedAt(existingAccount.getCreatedAt());

            boolean updated = accountDAO.update(account);

            if (!updated) {
                throw new BankingException("Account update failed");
            }

            return account;

        } catch (SQLException e) {
            throw new BankingException(
                    "Database error while updating account", e);
        }
    }

    // 5. Get the total number of accounts
    @Override
    public int getTotalAccountsCount() throws BankingException {
        try {
            return accountDAO.countAll();
        } catch (SQLException e) {
            throw new BankingException(
                    "Failed to retrieve total account count", e);
        }
    }

    // 6. Calculate the total balance of all accounts
    @Override
    public BigDecimal getTotalBalanceSum() throws BankingException {
        try {
            return accountDAO.sumAllBalances();
        } catch (SQLException e) {
            throw new BankingException(
                    "Failed to calculate total account balance", e);
        }
    }
}
