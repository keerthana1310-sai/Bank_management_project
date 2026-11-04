package com.bank.service.impl;

import com.bank.dao.AccountDAO;
import com.bank.dao.CustomerDAO;
import com.bank.model.Account;
import com.bank.model.Customer;
import com.bank.service.AccountService;
import com.bank.service.BankingException;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class JdbcAccountService implements AccountService {

    private final AccountDAO accountDAO;
    private final CustomerDAO customerDAO;

        public JdbcAccountService() {
        this.accountDAO = new AccountDAO();
        this.customerDAO = new CustomerDAO();
    }

    @Override
    public Optional<Account> findAccount(String accountNumber)
            throws BankingException {
        try {
            return accountDAO.findByAccountNumber(accountNumber);
        } catch (SQLException e) {
            throw new BankingException("Failed to find account", e);
        }
    }

    @Override
    public List<Account> getAllAccounts() throws BankingException {
        try {
            return accountDAO.findAll();
        } catch (SQLException e) {
            throw new BankingException("Failed to retrieve accounts", e);
        }
    }

    @Override
    public Account updateAccount(Account account) throws BankingException {
        if (account == null || account.getAccountNumber() == null) {
            throw new BankingException("Account details are required");
        }

        try {
            Optional<Account> existing =
                    accountDAO.findByAccountNumber(account.getAccountNumber());

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
                    && !"CURRENT".equalsIgnoreCase(account.getAccountType())) {
                throw new BankingException("Invalid account type");
            }

            if (account.getBalance() == null
                    || account.getBalance().compareTo(BigDecimal.ZERO) < 0) {
                throw new BankingException("Balance cannot be negative");
            }

            boolean updated = accountDAO.update(account);

            if (!updated) {
                throw new BankingException("Account update failed");
            }

            return account;

        } catch (SQLException e) {
            throw new BankingException("Database error while updating account", e);
        }
    }
}
