
package com.bank;

import com.bank.dao.AccountDAO;
import com.bank.dao.CustomerDAO;
import com.bank.dao.DatabaseConnection;
import com.bank.dao.TransactionDAO;
import com.bank.model.Account;
import com.bank.model.Customer;
import com.bank.model.Transaction;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JdbcAccountRollbackTest {

    @Test
    @DisplayName("Verify customer and account are rolled back when transaction insertion fails")
    void testAccountCreationRollback() throws Exception {

        CustomerDAO customerDAO = new CustomerDAO();
        AccountDAO accountDAO = new AccountDAO();
        TransactionDAO transactionDAO = new TransactionDAO();

        String uniqueId = UUID.randomUUID().toString().replace("-", "");

        String email = "rollback." + uniqueId + "@testbank.in";
        String phone = "9" + uniqueId.substring(0, 9);
        String accountNumber = "ACC-" + uniqueId.substring(0, 12).toUpperCase();

        BigDecimal openingBalance = new BigDecimal("100.00");
        LocalDateTime createdAt = LocalDateTime.now();

        boolean rollbackOccurred = false;

        try (Connection connection = DatabaseConnection.getConnection()) {

            connection.setAutoCommit(false);

            try {
                // Step 1: Insert temporary customer
                Customer customer = new Customer(
                        "Rollback Test Customer",
                        email,
                        phone
                );

                Long customerId = customerDAO.save(customer, connection);

                assertNotNull(customerId,
                        "Customer should be inserted before rollback");

                // Step 2: Insert temporary account
                Account account = new Account(
                        accountNumber,
                        customerId,
                        "Rollback Test Customer",
                        email,
                        phone,
                        "SAVINGS",
                        openingBalance,
                        "ACTIVE",
                        createdAt
                );

                assertTrue(accountDAO.save(account, connection),
                        "Account should be inserted before rollback");

                // Step 3: Deliberately create an invalid transaction.
                // The source account does not exist, so the foreign key
                // constraint should reject this transaction.
                Transaction invalidTransaction = new Transaction();

                invalidTransaction.setTransactionType("DEPOSIT");
                invalidTransaction.setSourceAccount(
                        "ACC-DOES-NOT-EXIST-" + uniqueId);
                invalidTransaction.setDestinationAccount(null);
                invalidTransaction.setAmount(openingBalance);
                invalidTransaction.setBalanceAfter(openingBalance);
                invalidTransaction.setDescription(
                        "Intentional rollback test");
                invalidTransaction.setTimestamp(LocalDateTime.now());

                assertThrows(
                        SQLException.class,
                        () -> transactionDAO.save(
                                invalidTransaction,
                                connection
                        ),
                        "Invalid source account should cause a foreign-key error"
                );

            } finally {
                // Roll back all uncommitted changes
                connection.rollback();
                rollbackOccurred = true;
            }
        }

        assertTrue(rollbackOccurred,
                "Rollback should have been executed");

        // Step 4: Verify customer was removed by rollback
        String customerQuery =
                "SELECT COUNT(*) FROM customers WHERE email = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(customerQuery)) {

            statement.setString(1, email);

            try (ResultSet resultSet = statement.executeQuery()) {
                assertTrue(resultSet.next());
                assertEquals(0, resultSet.getInt(1),
                        "Customer must not remain after rollback");
            }
        }

        // Step 5: Verify account was removed by rollback
        assertTrue(
                accountDAO.findByAccountNumber(accountNumber).isEmpty(),
                "Account must not remain after rollback"
        );
    }
}
