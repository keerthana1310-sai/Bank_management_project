package com.bank.service;

import com.bank.model.Transaction;
import java.math.BigDecimal;
import java.util.List;

/**
 * Service contract for Deposit, Withdrawal, Transfer, and History operations.
 * Assigned to: Jayasree
 * 
 * Jayasree will implement the production version (JdbcTransactionService)
 * using Yashaswi's TransactionDAO and AccountDAO.
 */
public interface TransactionService {

    /**
     * Deposits money into a designated account.
     * Must validate that amount is strictly positive (> 0).
     *
     * @param accountNumber Target account number
     * @param amount        Amount to deposit (must be > 0)
     * @param description   Optional remark or description
     * @return Completed Transaction record
     * @throws BankingException if account not found, amount <= 0, or database error occurs
     */
    Transaction deposit(String accountNumber, BigDecimal amount, String description) throws BankingException;

    /**
     * Withdraws money from a designated account.
     * Must validate that amount is strictly positive (> 0)
     * and that the account has sufficient available balance.
     *
     * @param accountNumber Source account number
     * @param amount        Amount to withdraw (must be > 0 and <= balance)
     * @param description   Optional remark or description
     * @return Completed Transaction record
     * @throws BankingException if insufficient funds, account not found, or database error occurs
     */
    Transaction withdraw(String accountNumber, BigDecimal amount, String description) throws BankingException;

    /**
     * Atomically transfers money from a source account to a destination account.
     * CRITICAL: Both account balances and transaction records must be executed 
     * inside a single database transaction (conn.setAutoCommit(false)).
     * If either update fails, rollback the entire transaction!
     *
     * @param sourceAccount      Account debited
     * @param destinationAccount Account credited
     * @param amount             Amount to transfer (must be > 0 and <= source balance)
     * @param description        Optional remark or transfer note
     * @return Completed Transaction record
     * @throws BankingException if insufficient balance, identical accounts, or failure occurs
     */
    Transaction transfer(String sourceAccount, String destinationAccount, 
                         BigDecimal amount, String description) throws BankingException;

    /**
     * Retrieves the transaction history for a specific account.
     *
     * @param accountNumber Target account number
     * @return List of transactions ordered chronologically (newest first)
     * @throws BankingException on query error
     */
    List<Transaction> getTransactionHistory(String accountNumber) throws BankingException;

    /**
     * Retrieves all transactions across the entire system.
     * Used for the global Transaction History view and reporting.
     *
     * @return List of all transactions ordered chronologically (newest first)
     * @throws BankingException on query error
     */
    List<Transaction> getAllTransactions() throws BankingException;

    /**
     * Calculates the total count of transactions executed.
     * Used for Dashboard KPI metric cards.
     */
    int getTotalTransactionsCount() throws BankingException;
}
