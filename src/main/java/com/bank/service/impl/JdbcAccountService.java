
package com.bank.service.impl;

import com.bank.dao.AccountDAO;
import com.bank.dao.CustomerDAO;
import com.bank.dao.DatabaseConnection;
import com.bank.dao.TransactionDAO;
import com.bank.model.Account;
import com.bank.model.Customer;
import com.bank.model.Transaction;
import com.bank.service.AccountService;
import com.bank.service.BankingException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class JdbcAccountService implements AccountService {

    private final AccountDAO accountDAO;
    private final CustomerDAO customerDAO;
    private final TransactionDAO transactionDAO;

    public JdbcAccountService() {
        this.accountDAO = new AccountDAO();
        this.customerDAO = new CustomerDAO();
        this.transactionDAO = new TransactionDAO();
    }

    // 1. Create a new customer and bank account
    @Override
    public Account createAccount(
            String customerName,
            String email,
            String phone,
            String accountType,
            BigDecimal initialDeposit
    ) throws BankingException {

        if (customerName == null || customerName.trim().isEmpty()) {
            throw new BankingException("Customer name is required");
        }

        if (email == null || !email.trim().contains("@")) {
            throw new BankingException("Enter a valid email");
        }

        if (phone == null || phone.trim().isEmpty()) {
            throw new BankingException("Phone number is required");
        }

        if (accountType == null ||
                (!"SAVINGS".equalsIgnoreCase(accountType.trim()) &&
                 !"CURRENT".equalsIgnoreCase(accountType.trim()))) {
            throw new BankingException(
                    "Account type must be SAVINGS or CURRENT");
        }

        if (initialDeposit == null ||
                initialDeposit.compareTo(BigDecimal.ZERO) < 0) {
            throw new BankingException(
                    "Initial deposit cannot be negative");
        }

        String cleanName = customerName.trim();
        String cleanEmail = email.trim();
        String cleanPhone = phone.trim();
        String cleanAccountType = accountType.trim().toUpperCase();

        BigDecimal openingBalance =
                initialDeposit.setScale(2, RoundingMode.HALF_UP);

        try (Connection connection = DatabaseConnection.getConnection()) {

            connection.setAutoCommit(false);

            try {
                // Step 1: Create customer
                Customer customer = new Customer(
                        cleanName,
                        cleanEmail,
                        cleanPhone
                );

                Long customerId = customerDAO.save(customer, connection);

                if (customerId == null) {
                    throw new BankingException(
                            "Failed to create customer");
                }

                // Step 2: Generate account number
                String accountNumber = "ACC-" +
                        UUID.randomUUID()
                                .toString()
                                .replace("-", "")
                                .substring(0, 12)
                                .toUpperCase();

                // Step 3: Create account
                LocalDateTime createdAt = LocalDateTime.now();

                Account account = new Account(
                        accountNumber,
                        customerId,
                        cleanName,
                        cleanEmail,
                        cleanPhone,
                        cleanAccountType,
                        openingBalance,
                        "ACTIVE",
                        createdAt
                );

                if (!accountDAO.save(account, connection)) {
                    throw new BankingException(
                            "Failed to create account");
                }

                // Step 4: Record the opening deposit
                // The transactions table requires a positive amount.
                // Therefore, skip this step when opening balance is zero.
                if (openingBalance.compareTo(BigDecimal.ZERO) > 0) {

                    Transaction initialTransaction = new Transaction();

                    initialTransaction.setTransactionType("DEPOSIT");
                    initialTransaction.setSourceAccount(accountNumber);
                    initialTransaction.setDestinationAccount(null);
                    initialTransaction.setAmount(openingBalance);
                    initialTransaction.setBalanceAfter(openingBalance);
                    initialTransaction.setDescription(
                            "Initial account deposit");
                    initialTransaction.setTimestamp(createdAt);

                    boolean transactionSaved =
                            transactionDAO.save(
                                    initialTransaction,
                                    connection
                            );

                    if (!transactionSaved) {
                        throw new BankingException(
                                "Failed to record initial deposit");
                    }
                }

                // Step 5: Commit all database changes together
                connection.commit();

                return account;

            } catch (Exception e) {

                // Roll back customer, account and transaction together
                try {
                    connection.rollback();
                } catch (SQLException rollbackError) {
                    e.addSuppressed(rollbackError);
                }

                if (e instanceof BankingException bankingException) {
                    throw bankingException;
                }

                if (e instanceof SQLException sqlException) {
                    throw sqlException;
                }

                throw new SQLException(
                        "Unexpected error while creating account", e);
            }

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

        if (account == null ||
                account.getAccountNumber() == null ||
                account.getAccountNumber().trim().isEmpty()) {
            throw new BankingException("Account details are required");
        }

        try {
            Optional<Account> existing =
                    accountDAO.findByAccountNumber(
                            account.getAccountNumber().trim());

            if (existing.isEmpty()) {
                throw new BankingException("Account not found");
            }

            if (account.getCustomerName() == null ||
                    account.getCustomerName().trim().isEmpty()) {
                throw new BankingException("Customer name is required");
            }

            if (account.getEmail() == null ||
                    !account.getEmail().trim().contains("@")) {
                throw new BankingException("Enter a valid email");
            }

            if (account.getPhone() == null ||
                    account.getPhone().trim().isEmpty()) {
                throw new BankingException("Phone number is required");
            }

            if (account.getAccountType() == null ||
                    (!"SAVINGS".equalsIgnoreCase(
                            account.getAccountType().trim()) &&
                     !"CURRENT".equalsIgnoreCase(
                            account.getAccountType().trim()))) {
                throw new BankingException("Invalid account type");
            }

            if (account.getBalance() == null ||
                    account.getBalance().compareTo(
                            BigDecimal.ZERO) < 0) {
                throw new BankingException(
                        "Balance cannot be negative");
            }

            // Preserve existing balance, status and customer details.
            // Balance changes must go through transaction operations.
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
