package com.bank.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/** JDBC access to staff login records. */
public class UserDAO {
    public record UserRecord(String username, String passwordHash, String fullName, String role, boolean active) { }

    public Optional<UserRecord> findByUsername(String username) throws SQLException {
        String sql = "SELECT username, password_hash, full_name, role, is_active FROM users WHERE username = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) return Optional.of(new UserRecord(rs.getString("username"), rs.getString("password_hash"),
                        rs.getString("full_name"), rs.getString("role"), rs.getBoolean("is_active")));
            }
        }
        return Optional.empty();
    }
}
