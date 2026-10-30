package com.bank.service;

import com.bank.model.Account;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Service contract for Account Management operations.
 * Assigned to: Pranita
 * 
 * Pranita will implement the production version (JdbcAccountService)
 * using Yashaswi's AccountDAO.
 */
public interface AccountService {

    /**
     * Creates a new bank account with the given customer details and initial balance.
     * Must generate a unique account number (e.g., ACC-1001 or timestamp/sequence based).
     *
     * @param customerName   Full legal name of the customer
     * @param email          Customer contact email address
     * @param phone          Customer contact phone number
     * @param accountType    "SAVINGS" or "CURRENT"
     * @param initialDeposit Initial amount to deposit (must be >= 0, or meet minimum balance)
     * @return The newly created and persisted Account object
     * @throws BankingException if validation fails or persistence error occurs
     */
    Account createAccount(String customerName, String email, String phone, 
                          String accountType, BigDecimal initialDeposit) throws BankingException;

    /**
     * Finds an account by its unique account number.
     *
     * @param accountNumber The account number to look up
     * @return Optional containing the Account if found, or empty Optional
     * @throws BankingException on database error
     */
    Optional<Account> findAccount(String accountNumber) throws BankingException;

    /**
     * Retrieves all accounts in the system.
     * Used for the Account Management TableView and administrative overview.
     *
     * @return List of all accounts
     * @throws BankingException on database error
     */
    List<Account> getAllAccounts() throws BankingException;

    /**
     * Updates an existing account's details (e.g., status, email, phone).
     *
     * @param account The account with updated fields
     * @return The updated Account
     * @throws BankingException if account doesn't exist or update fails
     */
    Account updateAccount(Account account) throws BankingException;

    /**
     * Calculates the total number of accounts in the system.
     * Used for Dashboard KPI metric cards.
     */
    int getTotalAccountsCount() throws BankingException;

    /**
     * Calculates the total aggregate balance across all accounts.
     * Used for Dashboard KPI metric cards.
     */
    BigDecimal getTotalBalanceSum() throws BankingException;
}
