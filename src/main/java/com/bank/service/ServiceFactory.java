package com.bank.service;

import com.bank.service.impl.JdbcAccountService;
import com.bank.service.impl.JdbcTransactionService;
import com.bank.service.impl.JdbcAuthenticationService;
import com.bank.dao.DatabaseConnection;

/**
 * ServiceFactory manages instantiation and dependency injection of services.
 * 
 * Supports seamless switching between:
 *  1) Demo Mode (In-Memory services for quick college demo / testing frontend without MySQL)
 *  2) Production Mode (JDBC-backed services connecting to MySQL database)
 * 
 * Configured via environment variable DEMO_MODE or system property demo.mode.
 */
public class ServiceFactory {

    private static ServiceFactory instance;

    private final boolean demoMode;
    private final AccountService accountService;
    private final TransactionService transactionService;
    private final AuthenticationService authenticationService;

    private ServiceFactory() {
        // Read configuration (defaults to true for standalone demonstration)
        String configuredMode = System.getProperty("demo.mode");
        if (configuredMode == null || configuredMode.isBlank()) {
            configuredMode = DatabaseConnection.getSetting("DEMO_MODE", "true");
        }
        this.demoMode = Boolean.parseBoolean(configuredMode.trim());

        if (demoMode) {
            this.accountService = new DemoAccountService();
            this.transactionService = new DemoTransactionService(this.accountService);
            this.authenticationService = new DemoAuthenticationService();
        } else {
            // Live mode must use JDBC. If the database is unavailable, fail visibly instead of
            // silently presenting in-memory data as if it were persisted.
            this.accountService = new JdbcAccountService();
            this.transactionService = new JdbcTransactionService();
            this.authenticationService = new JdbcAuthenticationService();
        }
    }

    public static synchronized ServiceFactory getInstance() {
        if (instance == null) {
            instance = new ServiceFactory();
        }
        return instance;
    }

    public AccountService getAccountService() {
        return accountService;
    }

    public TransactionService getTransactionService() {
        return transactionService;
    }

    public AuthenticationService getAuthenticationService() {
        return authenticationService;
    }

    public boolean isDemoMode() {
        return demoMode;
    }
}
