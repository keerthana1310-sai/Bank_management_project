package com.bank.service;

/**
 * Service contract for Bank Staff / Teller Authentication.
 * Handles login credentials validation and session state.
 */
public interface AuthenticationService {

    /**
     * Authenticates a bank staff/teller or administrator.
     *
     * @param username Provided username
     * @param password Provided password
     * @return true if credentials match valid active user; false otherwise
     */
    boolean authenticate(String username, String password);

    /**
     * Retrieves the username or full name of the currently logged-in user.
     *
     * @return Logged-in user's display identifier, or null if unauthenticated
     */
    String getCurrentUser();

    /**
     * Clears current authentication session.
     */
    void logout();

    /**
     * Indicates whether the service is running in development/demo mode.
     */
    boolean isDemoMode();
}
