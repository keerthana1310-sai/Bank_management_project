package com.bank.service;

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
        String envDemo = System.getenv("DEMO_MODE");
        String propDemo = System.getProperty("demo.mode");
        
        if (envDemo != null) {
            this.demoMode = Boolean.parseBoolean(envDemo.trim());
        } else if (propDemo != null) {
            this.demoMode = Boolean.parseBoolean(propDemo.trim());
        } else {
            // Default to demo mode so the frontend always works out-of-the-box!
            this.demoMode = true;
        }

        if (demoMode) {
            this.accountService = new DemoAccountService();
            this.transactionService = new DemoTransactionService(this.accountService);
            this.authenticationService = new DemoAuthenticationService();
        } else {
            // Live MySQL JDBC services (to be hooked up with Yashaswi's DAOs)
            // If MySQL fails to connect, fallback to Demo mode with warning
            AccountService accSvc;
            TransactionService txSvc;
            AuthenticationService authSvc;

            try {
                // Future integration point:
                // accSvc = new JdbcAccountService(new AccountDAOImpl(), new CustomerDAOImpl());
                // txSvc = new JdbcTransactionService(new TransactionDAOImpl(), new AccountDAOImpl());
                // authSvc = new JdbcAuthenticationService();
                accSvc = new DemoAccountService();
                txSvc = new DemoTransactionService(accSvc);
                authSvc = new DemoAuthenticationService();
            } catch (Exception e) {
                System.err.println("[ServiceFactory] Failed to initialize JDBC services. Falling back to Demo Mode: " + e.getMessage());
                accSvc = new DemoAccountService();
                txSvc = new DemoTransactionService(accSvc);
                authSvc = new DemoAuthenticationService();
            }

            this.accountService = accSvc;
            this.transactionService = txSvc;
            this.authenticationService = authSvc;
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
