
package com.bank.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;


import com.bank.model.Transaction;

/**
 * Data Access Object for Transactions table.
 * Handles transaction database operations.
 */
public class TransactionDAO {

    /**
     * Inserts a transaction using an existing database connection.
     * Used for atomic deposit, withdrawal and transfer operations.
     */
    public boolean save(Transaction tx, Connection conn) throws SQLException {

        String sql = "INSERT INTO transactions " +
                "(transaction_type, source_account, destination_account, " +
                "amount, balance_after, description, timestamp) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, tx.getTransactionType());
            stmt.setString(2, tx.getSourceAccount());

            if (tx.getDestinationAccount() != null &&
                    !tx.getDestinationAccount().isBlank()) {
                stmt.setString(3, tx.getDestinationAccount());
            } else {
                stmt.setNull(3, Types.VARCHAR);
            }

            stmt.setBigDecimal(4, tx.getAmount());
            stmt.setBigDecimal(5, tx.getBalanceAfter());
            stmt.setString(6, tx.getDescription());
            stmt.setTimestamp(7, Timestamp.valueOf(tx.getTimestamp()));

            int rows = stmt.executeUpdate();

            if (rows > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        tx.setTransactionId(generatedKeys.getLong(1));
                    }
                }
                return true;
            }

            return false;
        }
    }

    /**
     * Inserts a standalone transaction record.
     */
    public boolean save(Transaction tx) throws SQLException {

        try (Connection conn = DatabaseConnection.getConnection()) {
            return save(tx, conn);
        }
    }

    /**
     * Finds transactions related to a specific account.
     */
    public List<Transaction> findByAccountNumber(
            String accountNumber) throws SQLException {

        List<Transaction> transactions = new ArrayList<>();

        String sql = "SELECT transaction_id, transaction_type, source_account, " +
                "destination_account, amount, balance_after, description, timestamp " +
                "FROM transactions " +
                "WHERE source_account = ? OR destination_account = ? " +
                "ORDER BY timestamp DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, accountNumber);
            stmt.setString(2, accountNumber);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    transactions.add(mapResultSetToTransaction(rs));
                }
            }
        }

        return transactions;
    }

    /**
     * Retrieves all transactions in descending order.
     */
    public List<Transaction> findAll() throws SQLException {

        List<Transaction> transactions = new ArrayList<>();

        String sql = "SELECT transaction_id, transaction_type, source_account, " +
                "destination_account, amount, balance_after, description, timestamp " +
                "FROM transactions ORDER BY timestamp DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                transactions.add(mapResultSetToTransaction(rs));
            }
        }

        return transactions;
    }

    /**
     * Returns the total number of transactions.
     */
    public int count() throws SQLException {

        String sql = "SELECT COUNT(*) FROM transactions";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                return rs.getInt(1);
            }
        }

        return 0;
    }

    /**
     * Maps database records to Transaction objects.
     */
    private Transaction mapResultSetToTransaction(
            ResultSet rs) throws SQLException {

        Transaction tx = new Transaction();

        tx.setTransactionId(rs.getLong("transaction_id"));
        tx.setTransactionType(rs.getString("transaction_type"));
        tx.setSourceAccount(rs.getString("source_account"));
        tx.setDestinationAccount(rs.getString("destination_account"));
        tx.setAmount(rs.getBigDecimal("amount"));
        tx.setBalanceAfter(rs.getBigDecimal("balance_after"));
        tx.setDescription(rs.getString("description"));

        Timestamp ts = rs.getTimestamp("timestamp");

        if (ts != null) {
            tx.setTimestamp(ts.toLocalDateTime());
        }

        return tx;
    }
}