package com.bank.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Manages JDBC Database Connections for the Bank Management System.
 * Assigned to: Yashaswi
 * 
 * SECURITY NOTICE:
 * Database credentials must NOT be hardcoded in production.
 * This class reads credentials from environment variables (or .env configuration).
 */
public class DatabaseConnection {

    // Default configuration (can be overridden via environment variables)
    private static final String DEFAULT_HOST = "localhost";
    private static final String DEFAULT_PORT = "3306";
    private static final String DEFAULT_DB   = "bank_db";
    private static final String DEFAULT_USER = "root";
    private static final String DEFAULT_PASS = "";

    private static String getEnvOrDefault(String key, String defaultValue) {
        String val = System.getenv(key);
        return (val != null && !val.isBlank()) ? val.trim() : defaultValue;
    }

    public static String getJdbcUrl() {
        String host = getEnvOrDefault("DB_HOST", DEFAULT_HOST);
        String port = getEnvOrDefault("DB_PORT", DEFAULT_PORT);
        String db   = getEnvOrDefault("DB_NAME", DEFAULT_DB);
        // Uses standard MySQL 8.x connection parameters with SSL disabled for local dev
        return String.format("jdbc:mysql://%s:%s/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
                host, port, db);
    }

    public static String getDbUser() {
        return getEnvOrDefault("DB_USER", DEFAULT_USER);
    }

    public static String getDbPassword() {
        return getEnvOrDefault("DB_PASSWORD", DEFAULT_PASS);
    }

    /**
     * Establishes and returns a new JDBC Connection.
     * Yashaswi: Remember to close connections or use try-with-resources in DAOs!
     *
     * @return Active Connection object
     * @throws SQLException if connection fails
     */
    public static Connection getConnection() throws SQLException {
        try {
            // Load MySQL Connector/J driver
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC Driver not found. Ensure mysql-connector-j is in pom.xml.", e);
        }

        return DriverManager.getConnection(getJdbcUrl(), getDbUser(), getDbPassword());
    }

    /**
     * Utility method to quickly test connectivity to the MySQL database.
     *
     * @return true if connection succeeds, false otherwise
     */
    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            System.err.println("[DatabaseConnection] Connection test failed: " + e.getMessage());
            return false;
        }
    }
}
