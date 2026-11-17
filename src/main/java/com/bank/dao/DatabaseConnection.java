package com.bank.dao;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/** Central JDBC connection configuration. Environment variables and JVM properties
 * override values in an optional project-root .env file. */
public final class DatabaseConnection {
    private static final Map<String, String> DOTENV = loadDotEnv();
    private DatabaseConnection() { }

    private static Map<String, String> loadDotEnv() {
        Map<String, String> values = new HashMap<>();
        Path file = Path.of(".env");
        if (!Files.isRegularFile(file)) return values;
        try {
            for (String line : Files.readAllLines(file)) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#") || !trimmed.contains("=")) continue;
                int split = trimmed.indexOf('=');
                String key = trimmed.substring(0, split).trim();
                String value = trimmed.substring(split + 1).trim();
                if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'")))
                    value = value.substring(1, value.length() - 1);
                if (!key.isEmpty()) values.put(key, value);
            }
        } catch (IOException e) {
            System.err.println("[DatabaseConnection] Could not read .env: " + e.getMessage());
        }
        return values;
    }

    public static String getSetting(String key, String fallback) {
        String value = System.getProperty(key);
        if (value == null || value.isBlank()) value = System.getenv(key);
        if (value == null || value.isBlank()) value = DOTENV.get(key);
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    public static String getJdbcUrl() {
        return String.format("jdbc:mysql://%s:%s/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
                getSetting("DB_HOST", "localhost"), getSetting("DB_PORT", "3306"), getSetting("DB_NAME", "bank_db"));
    }
    public static String getDbUser() { return getSetting("DB_USER", "root"); }
    public static String getDbPassword() { return getSetting("DB_PASSWORD", ""); }

    public static Connection getConnection() throws SQLException {
        try { Class.forName("com.mysql.cj.jdbc.Driver"); }
        catch (ClassNotFoundException e) { throw new SQLException("MySQL JDBC Driver not found. Check pom.xml dependencies.", e); }
        return DriverManager.getConnection(getJdbcUrl(), getDbUser(), getDbPassword());
    }

    public static boolean testConnection() {
        try (Connection connection = getConnection()) { return connection != null && !connection.isClosed(); }
        catch (SQLException e) { System.err.println("[DatabaseConnection] Connection test failed: " + e.getMessage()); return false; }
    }
}
