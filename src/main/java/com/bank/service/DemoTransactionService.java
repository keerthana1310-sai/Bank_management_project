package com.bank.service;

import com.bank.model.Account;
import com.bank.model.Transaction;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Temporary In-Memory Demo Transaction Service.
 * 
 * NOTICE: This is an in-memory implementation for college project demonstration.
 * Operations update in-memory Account objects and transaction history.
 * NONE of these records are saved to MySQL.
 * 
 * Teammate Jayasree will replace/supplement this with JdbcTransactionService.
 */
public class DemoTransactionService implements TransactionService {

    private final AccountService accountService;
    private final List<Transaction> transactionHistory = new CopyOnWriteArrayList<>();
    private final AtomicLong transactionIdSequence = new AtomicLong(5001);

    public DemoTransactionService(AccountService accountService) {
        this.accountService = accountService;
        seedSampleTransactions();
    }

    private void seedSampleTransactions() {
        transactionHistory.add(new Transaction(5001L, "DEPOSIT", "ACC-1001", null, 
                new BigDecimal("5000.00"), new BigDecimal("12500.00"), "Initial Deposit", LocalDateTime.now().minusDays(25)));
        transactionHistory.add(new Transaction(5002L, "DEPOSIT", "ACC-1002", null, 
                new BigDecimal("45000.00"), new BigDecimal("45000.00"), "Corporate Opening Balance", LocalDateTime.now().minusDays(18)));
        transactionHistory.add(new Transaction(5003L, "TRANSFER", "ACC-1001", "ACC-1003", 
                new BigDecimal("500.00"), new BigDecimal("12000.00"), "College Project Team Fund", LocalDateTime.now().minusDays(10)));
        transactionHistory.add(new Transaction(5004L, "WITHDRAWAL", "ACC-1004", null, 
                new BigDecimal("600.00"), new BigDecimal("19400.75"), "ATM Cash Withdrawal", LocalDateTime.now().minusDays(3)));
    }

    @Override
    public synchronized Transaction deposit(String accountNumber, BigDecimal amount, String description) throws BankingException {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BankingException("Deposit amount must be greater than zero.");
        }

        Optional<Account> accountOpt = accountService.findAccount(accountNumber);
        if (accountOpt.isEmpty()) {
            throw new BankingException("Account " + accountNumber + " does not exist.");
        }

        Account account = accountOpt.get();
        if (!"ACTIVE".equalsIgnoreCase(account.getStatus())) {
            throw new BankingException("Cannot deposit into an inactive or closed account (" + account.getStatus() + ").");
        }

        BigDecimal newBalance = account.getBalance().add(amount).setScale(2, RoundingMode.HALF_UP);
        account.setBalance(newBalance);
        accountService.updateAccount(account);

        Transaction tx = new Transaction(
                transactionIdSequence.getAndIncrement(),
                "DEPOSIT",
                account.getAccountNumber(),
                null,
                amount.setScale(2, RoundingMode.HALF_UP),
                newBalance,
                (description == null || description.isBlank()) ? "Cash/Check Deposit" : description.trim(),
                LocalDateTime.now()
        );

        transactionHistory.add(0, tx); // prepend newest
        return tx;
    }

    @Override
    public synchronized Transaction withdraw(String accountNumber, BigDecimal amount, String description) throws BankingException {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BankingException("Withdrawal amount must be greater than zero.");
        }

        Optional<Account> accountOpt = accountService.findAccount(accountNumber);
        if (accountOpt.isEmpty()) {
            throw new BankingException("Account " + accountNumber + " does not exist.");
        }

        Account account = accountOpt.get();
        if (!"ACTIVE".equalsIgnoreCase(account.getStatus())) {
            throw new BankingException("Cannot withdraw from an inactive or closed account (" + account.getStatus() + ").");
        }

        if (account.getBalance().compareTo(amount) < 0) {
            throw new BankingException(String.format("Insufficient funds! Available balance: %s, Requested withdrawal: %s",
                    account.getFormattedBalance(), amount.setScale(2, RoundingMode.HALF_UP)));
        }

        BigDecimal newBalance = account.getBalance().subtract(amount).setScale(2, RoundingMode.HALF_UP);
        account.setBalance(newBalance);
        accountService.updateAccount(account);

        Transaction tx = new Transaction(
                transactionIdSequence.getAndIncrement(),
                "WITHDRAWAL",
                account.getAccountNumber(),
                null,
                amount.setScale(2, RoundingMode.HALF_UP),
                newBalance,
                (description == null || description.isBlank()) ? "Cash Withdrawal" : description.trim(),
                LocalDateTime.now()
        );

        transactionHistory.add(0, tx);
        return tx;
    }

    @Override
    public synchronized Transaction transfer(String sourceAccount, String destinationAccount, 
                                            BigDecimal amount, String description) throws BankingException {
        if (sourceAccount == null || sourceAccount.isBlank()) {
            throw new BankingException("Source account number is required.");
        }
        if (destinationAccount == null || destinationAccount.isBlank()) {
            throw new BankingException("Destination account number is required.");
        }
        if (sourceAccount.trim().equalsIgnoreCase(destinationAccount.trim())) {
            throw new BankingException("Source and destination accounts cannot be identical.");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BankingException("Transfer amount must be strictly greater than zero.");
        }

        Optional<Account> srcOpt = accountService.findAccount(sourceAccount);
        if (srcOpt.isEmpty()) {
            throw new BankingException("Source account " + sourceAccount + " not found.");
        }

        Optional<Account> destOpt = accountService.findAccount(destinationAccount);
        if (destOpt.isEmpty()) {
            throw new BankingException("Destination account " + destinationAccount + " not found.");
        }

        Account src = srcOpt.get();
        Account dest = destOpt.get();

        if (!"ACTIVE".equalsIgnoreCase(src.getStatus())) {
            throw new BankingException("Source account is not active (" + src.getStatus() + ").");
        }
        if (!"ACTIVE".equalsIgnoreCase(dest.getStatus())) {
            throw new BankingException("Destination account is not active (" + dest.getStatus() + ").");
        }

        if (src.getBalance().compareTo(amount) < 0) {
            throw new BankingException(String.format("Transfer failed: Insufficient balance in source account %s. Current balance: %s",
                    src.getAccountNumber(), src.getFormattedBalance()));
        }

        // Atomic operation in memory:
        BigDecimal srcNewBalance = src.getBalance().subtract(amount).setScale(2, RoundingMode.HALF_UP);
        BigDecimal destNewBalance = dest.getBalance().add(amount).setScale(2, RoundingMode.HALF_UP);

        src.setBalance(srcNewBalance);
        dest.setBalance(destNewBalance);

        accountService.updateAccount(src);
        accountService.updateAccount(dest);

        Transaction tx = new Transaction(
                transactionIdSequence.getAndIncrement(),
                "TRANSFER",
                src.getAccountNumber(),
                dest.getAccountNumber(),
                amount.setScale(2, RoundingMode.HALF_UP),
                srcNewBalance,
                (description == null || description.isBlank()) ? "Account-to-Account Transfer" : description.trim(),
                LocalDateTime.now()
        );

        transactionHistory.add(0, tx);
        return tx;
    }

    @Override
    public List<Transaction> getTransactionHistory(String accountNumber) throws BankingException {
        if (accountNumber == null || accountNumber.isBlank()) {
            return getAllTransactions();
        }

        String target = accountNumber.trim().toUpperCase();
        List<Transaction> filtered = new ArrayList<>();
        for (Transaction tx : transactionHistory) {
            if (target.equalsIgnoreCase(tx.getSourceAccount()) || target.equalsIgnoreCase(tx.getDestinationAccount())) {
                filtered.add(tx);
            }
        }
        return filtered;
    }

    @Override
    public List<Transaction> getAllTransactions() throws BankingException {
        return new ArrayList<>(transactionHistory);
    }

    @Override
    public int getTotalTransactionsCount() {
        return transactionHistory.size();
    }
}
