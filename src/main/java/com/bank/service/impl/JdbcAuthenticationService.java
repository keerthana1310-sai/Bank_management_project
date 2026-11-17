package com.bank.service.impl;

import com.bank.dao.UserDAO;
import com.bank.security.PasswordUtil;
import com.bank.service.AuthenticationService;
import java.sql.SQLException;

/** Database-backed staff authentication. */
public class JdbcAuthenticationService implements AuthenticationService {
    private final UserDAO userDAO;
    private String currentUser;
    public JdbcAuthenticationService() { this(new UserDAO()); }
    public JdbcAuthenticationService(UserDAO userDAO) { this.userDAO = userDAO; }

    @Override public boolean authenticate(String username, String password) {
        logout();
        if (username == null || username.isBlank() || password == null || password.isEmpty()) return false;
        try {
            var user = userDAO.findByUsername(username.trim());
            if (user.isEmpty() || !user.get().active() || !PasswordUtil.verify(password, user.get().passwordHash())) return false;
            currentUser = user.get().fullName();
            return true;
        } catch (SQLException e) {
            System.err.println("[Authentication] Database lookup failed: " + e.getMessage());
            return false;
        }
    }
    @Override public String getCurrentUser() { return currentUser; }
    @Override public void logout() { currentUser = null; }
    @Override public boolean isDemoMode() { return false; }
}
