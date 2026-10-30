package com.bank.dao;

import com.bank.model.Account;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for Accounts table.
 * Assigned to: Yashaswi (Database/JDBC) & Pranita (Account Management)
 * 
 * TODO [YASHASWI]:
 * 1. Verify table and column names against database/schema.sql.
 * 2. Ensure all SQL queries use PreparedStatement to prevent SQL injection.
 * 3. Note that updateBalance supports an external Connection parameter so that
 *    Jayasree can execute atomic transfers in a single transaction!
 */
public class AccountDAO {

    /**
     * Inserts a new account record into MySQL.
     * 
     * SQL Template:
     * INSERT INTO accounts (account_number, customer_id, customer_name, email, phone, account_type, balance, status, created_at)
     * VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW());
     */
    public boolean save(Account account) throws SQLException {
        String sql = "INSERT INTO accounts (account_number, customer_id, customer_name, email, phone, " +
                     "account_type, balance, status, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, account.getAccountNumber());
            if (account.getCustomerId() != null) {
                stmt.setLong(2, account.getCustomerId());
            } else {
                stmt.setNull(2, Types.BIGINT);
            }
            stmt.setString(3, account.getCustomerName());
            stmt.setString(4, account.getEmail());
            stmt.setString(5, account.getPhone());
            stmt.setString(6, account.getAccountType());
            stmt.setBigDecimal(7, account.getBalance());
            stmt.setString(8, account.getStatus());
            stmt.setTimestamp(9, Timestamp.valueOf(account.getCreatedAt()));

            int rows = stmt.executeUpdate();
            return rows > 0;
        }
    }

    /**
     * Looks up an account by its unique account number.
     */
    public Optional<Account> findByAccountNumber(String accountNumber) throws SQLException {
        String sql = "SELECT account_number, customer_id, customer_name, email, phone, " +
                     "account_type, balance, status, created_at FROM accounts WHERE account_number = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, accountNumber);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Account account = mapResultSetToAccount(rs);
                    return Optional.of(account);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Retrieves all accounts in the database.
     */
    public List<Account> findAll() throws SQLException {
        List<Account> accounts = new ArrayList<>();
        String sql = "SELECT account_number, customer_id, customer_name, email, phone, " +
                     "account_type, balance, status, created_at FROM accounts ORDER BY created_at DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                accounts.add(mapResultSetToAccount(rs));
            }
        }
        return accounts;
    }

    /**
     * Updates an account's profile details.
     */
    public boolean update(Account account) throws SQLException {
        String sql = "UPDATE accounts SET customer_name = ?, email = ?, phone = ?, " +
                     "account_type = ?, balance = ?, status = ? WHERE account_number = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, account.getCustomerName());
            stmt.setString(2, account.getEmail());
            stmt.setString(3, account.getPhone());
            stmt.setString(4, account.getAccountType());
            stmt.setBigDecimal(5, account.getBalance());
            stmt.setString(6, account.getStatus());
            stmt.setString(7, account.getAccountNumber());

            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Updates account balance within an existing transaction context.
     * Yashaswi & Jayasree: This method accepts an existing Connection object so that
     * multiple balance changes (e.g., transfers) can be committed or rolled back together.
     */
    public boolean updateBalance(String accountNumber, BigDecimal newBalance, Connection conn) throws SQLException {
        String sql = "UPDATE accounts SET balance = ? WHERE account_number = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBigDecimal(1, newBalance);
            stmt.setString(2, accountNumber);
            return stmt.executeUpdate() > 0;
        }
    }

    private Account mapResultSetToAccount(ResultSet rs) throws SQLException {
        Account a = new Account();
        a.setAccountNumber(rs.getString("account_number"));
        long custId = rs.getLong("customer_id");
        if (!rs.wasNull()) {
            a.setCustomerId(custId);
        }
        a.setCustomerName(rs.getString("customer_name"));
        a.setEmail(rs.getString("email"));
        a.setPhone(rs.getString("phone"));
        a.setAccountType(rs.getString("account_type"));
        a.setBalance(rs.getBigDecimal("balance"));
        a.setStatus(rs.getString("status"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            a.setCreatedAt(ts.toLocalDateTime());
        }
        return a;
    }
}
