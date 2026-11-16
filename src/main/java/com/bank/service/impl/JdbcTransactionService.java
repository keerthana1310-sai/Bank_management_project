package com.bank.service.impl;

import com.bank.dao.AccountDAO;
import com.bank.dao.DatabaseConnection;
import com.bank.dao.TransactionDAO;
import com.bank.model.Account;
import com.bank.model.Transaction;
import com.bank.service.BankingException;
import com.bank.service.TransactionService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Production JDBC implementation of the TransactionService interface.
 * Assigned to: Jayasree (Transaction Backend)
 * 
 * Provides robust, ACID-compliant banking transaction operations:
 * - Validated Deposits
 * - Validated Withdrawals with non-negative balance enforcement
 * - Atomic Funds Transfers with row-level locking (SELECT ... FOR UPDATE) and full rollback
 * - Chronological Transaction Ledger History & Aggregate Metrics
 * 
 * Integrates with:
 * - Keerthana's JavaFX controllers (TransactionController, TransferController, HistoryController, DashboardController)
 * - Pranita's Account records via AccountDAO
 * - Yashaswi's DatabaseConnection and TransactionDAO / AccountDAO persistence layer
 */
public class JdbcTransactionService implements TransactionService {

    private final TransactionDAO transactionDAO;
    private final AccountDAO accountDAO;

    /**
     * Dependency injection constructor.
     *
     * @param transactionDAO Data access object for transactions
     * @param accountDAO     Data access object for accounts
     */
    public JdbcTransactionService(TransactionDAO transactionDAO, AccountDAO accountDAO) {
        if (transactionDAO == null) {
            throw new IllegalArgumentException("TransactionDAO must not be null.");
        }
        if (accountDAO == null) {
            throw new IllegalArgumentException("AccountDAO must not be null.");
        }
        this.transactionDAO = transactionDAO;
        this.accountDAO = accountDAO;
    }

    /**
     * Default constructor for ServiceFactory instantiation.
     */
    public JdbcTransactionService() {
        this(new TransactionDAO(), new AccountDAO());
    }

    /**
     * Obtains a JDBC Connection. Protected to allow controlled mock connection injection in tests.
     */
    protected Connection getConnection() throws SQLException {
        return DatabaseConnection.getConnection();
    }

    @Override
    public Transaction deposit(String accountNumber, BigDecimal amount, String description) throws BankingException {
        // 1. Validation
        if (amount == null) {
            throw new BankingException("Deposit amount cannot be null.");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BankingException("Deposit amount must be strictly greater than zero.");
        }
        if (accountNumber == null || accountNumber.isBlank()) {
            throw new BankingException("Account number is required for deposit.");
        }

        String targetAcc = accountNumber.trim();
        Connection conn = null;

        try {
            conn = getConnection();
            conn.setAutoCommit(false);

            // 2. Retrieve account with row lock to prevent race conditions
            Optional<Account> accountOpt = accountDAO.findByAccountNumberForUpdate(targetAcc, conn);
            if (accountOpt.isEmpty()) {
                throw new BankingException("Account " + targetAcc + " does not exist.");
            }

            Account account = accountOpt.get();
            if (!"ACTIVE".equalsIgnoreCase(account.getStatus())) {
                throw new BankingException("Cannot deposit into an inactive or closed account (" + account.getStatus() + ").");
            }

            // 3. Balance calculation using BigDecimal
            BigDecimal currentBalance = account.getBalance();
            BigDecimal newBalance = currentBalance.add(amount).setScale(2, RoundingMode.HALF_UP);

            // 4. Update account balance
            boolean updated = accountDAO.updateBalance(account.getAccountNumber(), newBalance, conn);
            if (!updated) {
                throw new BankingException("Failed to update account balance for " + account.getAccountNumber());
            }

            // 5. Build and save transaction record
            Transaction tx = new Transaction();
            tx.setTransactionType("DEPOSIT");
            tx.setSourceAccount(account.getAccountNumber());
            tx.setDestinationAccount(null);
            tx.setAmount(amount.setScale(2, RoundingMode.HALF_UP));
            tx.setBalanceAfter(newBalance);
            tx.setDescription((description == null || description.isBlank()) ? "Cash Deposit" : description.trim());
            tx.setTimestamp(LocalDateTime.now());

            boolean saved = transactionDAO.save(tx, conn);
            if (!saved) {
                throw new BankingException("Failed to save transaction record for deposit.");
            }

            conn.commit();
            return tx;

        } catch (SQLException e) {
            rollbackQuietly(conn);
            throw new BankingException("Deposit failed due to database error: " + e.getMessage(), e);
        } catch (BankingException e) {
            rollbackQuietly(conn);
            throw e;
        } finally {
            closeQuietly(conn);
        }
    }

    @Override
    public Transaction withdraw(String accountNumber, BigDecimal amount, String description) throws BankingException {
        // 1. Validation
        if (amount == null) {
            throw new BankingException("Withdrawal amount cannot be null.");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BankingException("Withdrawal amount must be strictly greater than zero.");
        }
        if (accountNumber == null || accountNumber.isBlank()) {
            throw new BankingException("Account number is required for withdrawal.");
        }

        String targetAcc = accountNumber.trim();
        Connection conn = null;

        try {
            conn = getConnection();
            conn.setAutoCommit(false);

            // 2. Retrieve account with row lock
            Optional<Account> accountOpt = accountDAO.findByAccountNumberForUpdate(targetAcc, conn);
            if (accountOpt.isEmpty()) {
                throw new BankingException("Account " + targetAcc + " does not exist.");
            }

            Account account = accountOpt.get();
            if (!"ACTIVE".equalsIgnoreCase(account.getStatus())) {
                throw new BankingException("Cannot withdraw from an inactive or closed account (" + account.getStatus() + ").");
            }

            // 3. Sufficient funds check
            BigDecimal currentBalance = account.getBalance();
            if (currentBalance.compareTo(amount) < 0) {
                throw new BankingException(String.format("Insufficient funds! Available balance: %s, Requested withdrawal: %s",
                        account.getFormattedBalance(), amount.setScale(2, RoundingMode.HALF_UP)));
            }

            // 4. Calculate new balance
            BigDecimal newBalance = currentBalance.subtract(amount).setScale(2, RoundingMode.HALF_UP);

            // 5. Update account balance
            boolean updated = accountDAO.updateBalance(account.getAccountNumber(), newBalance, conn);
            if (!updated) {
                throw new BankingException("Failed to update account balance for " + account.getAccountNumber());
            }

            // 6. Build and save transaction record
            Transaction tx = new Transaction();
            tx.setTransactionType("WITHDRAWAL");
            tx.setSourceAccount(account.getAccountNumber());
            tx.setDestinationAccount(null);
            tx.setAmount(amount.setScale(2, RoundingMode.HALF_UP));
            tx.setBalanceAfter(newBalance);
            tx.setDescription((description == null || description.isBlank()) ? "Cash Withdrawal" : description.trim());
            tx.setTimestamp(LocalDateTime.now());

            boolean saved = transactionDAO.save(tx, conn);
            if (!saved) {
                throw new BankingException("Failed to save transaction record for withdrawal.");
            }

            conn.commit();
            return tx;

        } catch (SQLException e) {
            rollbackQuietly(conn);
            throw new BankingException("Withdrawal failed due to database error: " + e.getMessage(), e);
        } catch (BankingException e) {
            rollbackQuietly(conn);
            throw e;
        } finally {
            closeQuietly(conn);
        }
    }

    @Override
    public Transaction transfer(String sourceAccount, String destinationAccount, 
                                BigDecimal amount, String description) throws BankingException {
        // 1. Validation
        if (sourceAccount == null || sourceAccount.isBlank()) {
            throw new BankingException("Source account number is required.");
        }
        if (destinationAccount == null || destinationAccount.isBlank()) {
            throw new BankingException("Destination account number is required.");
        }

        String srcNum = sourceAccount.trim();
        String destNum = destinationAccount.trim();

        if (srcNum.equalsIgnoreCase(destNum)) {
            throw new BankingException("Source and destination accounts cannot be identical.");
        }
        if (amount == null) {
            throw new BankingException("Transfer amount cannot be null.");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BankingException("Transfer amount must be strictly greater than zero.");
        }

        Connection conn = null;

        try {
            conn = getConnection();
            conn.setAutoCommit(false);

            // 2. Prevent deadlock by locking accounts in consistent alphabetical order
            String firstLock = srcNum.compareToIgnoreCase(destNum) < 0 ? srcNum : destNum;
            String secondLock = srcNum.compareToIgnoreCase(destNum) < 0 ? destNum : srcNum;

            Optional<Account> firstOpt = accountDAO.findByAccountNumberForUpdate(firstLock, conn);
            Optional<Account> secondOpt = accountDAO.findByAccountNumberForUpdate(secondLock, conn);

            Optional<Account> srcOpt = srcNum.equalsIgnoreCase(firstLock) ? firstOpt : secondOpt;
            Optional<Account> destOpt = destNum.equalsIgnoreCase(firstLock) ? firstOpt : secondOpt;

            if (srcOpt.isEmpty()) {
                throw new BankingException("Source account " + srcNum + " not found.");
            }
            if (destOpt.isEmpty()) {
                throw new BankingException("Destination account " + destNum + " not found.");
            }

            Account src = srcOpt.get();
            Account dest = destOpt.get();

            if (!"ACTIVE".equalsIgnoreCase(src.getStatus())) {
                throw new BankingException("Source account is not active (" + src.getStatus() + ").");
            }
            if (!"ACTIVE".equalsIgnoreCase(dest.getStatus())) {
                throw new BankingException("Destination account is not active (" + dest.getStatus() + ").");
            }

            // 3. Verify sufficient funds in source account
            BigDecimal srcBalance = src.getBalance();
            if (srcBalance.compareTo(amount) < 0) {
                throw new BankingException(String.format("Transfer failed: Insufficient balance in source account %s. Current balance: %s",
                        src.getAccountNumber(), src.getFormattedBalance()));
            }

            // 4. Calculate new balances using BigDecimal
            BigDecimal newSourceBalance = srcBalance.subtract(amount).setScale(2, RoundingMode.HALF_UP);
            BigDecimal newDestBalance = dest.getBalance().add(amount).setScale(2, RoundingMode.HALF_UP);

            // 5. Debit source account
            boolean srcUpdated = accountDAO.updateBalance(src.getAccountNumber(), newSourceBalance, conn);
            if (!srcUpdated) {
                throw new BankingException("Failed to debit source account " + src.getAccountNumber());
            }

            // 6. Credit destination account
            boolean destUpdated = accountDAO.updateBalance(dest.getAccountNumber(), newDestBalance, conn);
            if (!destUpdated) {
                throw new BankingException("Failed to credit destination account " + dest.getAccountNumber());
            }

            // 7. Save atomic transfer transaction record
            Transaction tx = new Transaction();
            tx.setTransactionType("TRANSFER");
            tx.setSourceAccount(src.getAccountNumber());
            tx.setDestinationAccount(dest.getAccountNumber());
            tx.setAmount(amount.setScale(2, RoundingMode.HALF_UP));
            tx.setBalanceAfter(newSourceBalance);
            tx.setDescription((description == null || description.isBlank()) ? "Account-to-Account Transfer" : description.trim());
            tx.setTimestamp(LocalDateTime.now());

            boolean saved = transactionDAO.save(tx, conn);
            if (!saved) {
                throw new BankingException("Failed to save transaction record for transfer.");
            }

            // 8. Commit atomic transaction
            conn.commit();
            return tx;

        } catch (SQLException e) {
            rollbackQuietly(conn);
            throw new BankingException("Transfer failed and was rolled back: " + e.getMessage(), e);
        } catch (BankingException e) {
            rollbackQuietly(conn);
            throw e;
        } finally {
            closeQuietly(conn);
        }
    }

    @Override
    public List<Transaction> getTransactionHistory(String accountNumber) throws BankingException {
        if (accountNumber == null || accountNumber.isBlank()) {
            return getAllTransactions();
        }
        try {
            return transactionDAO.findByAccountNumber(accountNumber.trim());
        } catch (SQLException e) {
            throw new BankingException("Failed to retrieve transaction history for account " + accountNumber + ": " + e.getMessage(), e);
        }
    }

    @Override
    public List<Transaction> getAllTransactions() throws BankingException {
        try {
            return transactionDAO.findAll();
        } catch (SQLException e) {
            throw new BankingException("Failed to retrieve all transactions: " + e.getMessage(), e);
        }
    }

    @Override
    public int getTotalTransactionsCount() throws BankingException {
        try {
            return transactionDAO.count();
        } catch (SQLException e) {
            throw new BankingException("Failed to retrieve total transaction count: " + e.getMessage(), e);
        }
    }

    // Helper methods for safe transaction lifecycle management
    private void rollbackQuietly(Connection conn) {
        if (conn != null) {
            try {
                conn.rollback();
            } catch (SQLException ex) {
                System.err.println("[JdbcTransactionService] Rollback failed: " + ex.getMessage());
            }
        }
    }

    private void closeQuietly(Connection conn) {
        if (conn != null) {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException ignored) {
            }
            try {
                conn.close();
            } catch (SQLException ex) {
                System.err.println("[JdbcTransactionService] Connection close failed: " + ex.getMessage());
            }
        }
    }
}
