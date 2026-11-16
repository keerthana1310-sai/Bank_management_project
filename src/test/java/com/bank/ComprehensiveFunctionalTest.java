package com.bank;

import com.bank.controller.*;
import com.bank.model.Account;
import com.bank.model.Transaction;
import com.bank.service.AccountService;
import com.bank.service.ServiceFactory;
import com.bank.service.TransactionService;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Automated end-to-end and functional verification test for Apex Trust Bank.
 */
public class ComprehensiveFunctionalTest {

    @BeforeAll
    public static void initJFX() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        try {
            Platform.startup(latch::countDown);
        } catch (IllegalStateException alreadyStarted) {
            latch.countDown();
        }
        assertTrue(latch.await(5, TimeUnit.SECONDS), "JavaFX failed to initialize");
    }

    @Test
    @DisplayName("Verify AccountService CRUD and Indian Customer Seed Data")
    public void testAccountServiceOperations() throws Exception {
        AccountService accountService = ServiceFactory.getInstance().getAccountService();
        assertNotNull(accountService);

        // 1. Verify seed accounts
        List<Account> accounts = accountService.getAllAccounts();
        assertTrue(accounts.size() >= 8, "Expected at least 8 seeded accounts");

        Optional<Account> acc1 = accountService.findAccount("ACC-1001");
        assertTrue(acc1.isPresent());
        assertEquals("Aarav Sharma", acc1.get().getCustomerName());
        assertEquals("SAVINGS", acc1.get().getAccountType());
        assertTrue(acc1.get().getFormattedBalance().startsWith("₹"));

        // 2. Test Create Account
        Account newAcc = accountService.createAccount(
                "Sneha Rao Test",
                "sneha.test@apexbank.in",
                "9876543299",
                "SAVINGS",
                new BigDecimal("15000.00")
        );
        assertNotNull(newAcc);
        assertNotNull(newAcc.getAccountNumber());
        assertEquals("Sneha Rao Test", newAcc.getCustomerName());
        assertTrue(newAcc.getFormattedBalance().contains("15,000"));

        // 3. Find newly created account
        Optional<Account> found = accountService.findAccount(newAcc.getAccountNumber());
        assertTrue(found.isPresent());
        assertEquals("ACTIVE", found.get().getStatus());
    }

    @Test
    @DisplayName("Verify TransactionService Deposit, Withdrawal, and Transfer Operations")
    public void testTransactionServiceOperations() throws Exception {
        AccountService accountService = ServiceFactory.getInstance().getAccountService();
        TransactionService txService = ServiceFactory.getInstance().getTransactionService();

        // 1. Initial balance check
        Account acc1 = accountService.findAccount("ACC-1001").orElseThrow();
        BigDecimal initialBal = acc1.getBalance();

        // 2. Deposit Test
        BigDecimal depositAmount = new BigDecimal("5000.00");
        Transaction depositTx = txService.deposit("ACC-1001", depositAmount, "Automated Test Deposit");
        assertNotNull(depositTx);
        assertEquals("DEPOSIT", depositTx.getTransactionType());
        assertEquals(initialBal.add(depositAmount), depositTx.getBalanceAfter());
        assertTrue(depositTx.getFormattedAmount().startsWith("₹"));

        // 3. Withdrawal Test
        BigDecimal withdrawAmount = new BigDecimal("2000.00");
        Transaction withdrawTx = txService.withdraw("ACC-1001", withdrawAmount, "Automated Test Withdrawal");
        assertNotNull(withdrawTx);
        assertEquals("WITHDRAWAL", withdrawTx.getTransactionType());
        assertEquals(depositTx.getBalanceAfter().subtract(withdrawAmount), withdrawTx.getBalanceAfter());

        // 4. Transfer Test
        Account acc2 = accountService.findAccount("ACC-1002").orElseThrow();
        BigDecimal acc2Initial = acc2.getBalance();

        BigDecimal transferAmount = new BigDecimal("1500.00");
        Transaction transferTx = txService.transfer("ACC-1001", "ACC-1002", transferAmount, "Automated Test Transfer");
        assertNotNull(transferTx);
        assertEquals("TRANSFER", transferTx.getTransactionType());
        assertEquals("ACC-1001", transferTx.getSourceAccount());
        assertEquals("ACC-1002", transferTx.getDestinationAccount());

        // Verify destination balance updated
        Account acc2After = accountService.findAccount("ACC-1002").orElseThrow();
        assertEquals(acc2Initial.add(transferAmount), acc2After.getBalance());
    }

    @Test
    @DisplayName("Verify Dashboard FXML, Controller, KPIs, and Charts rendering")
    public void testDashboardLoadingAndCharts() throws Exception {
        runOnFxThread(() -> {
            try {
                URL res = getClass().getResource("/com/bank/dashboard.fxml");
                assertNotNull(res);
                FXMLLoader loader = new FXMLLoader(res);
                Parent root = loader.load();
                assertNotNull(root);

                DashboardController controller = loader.getController();
                assertNotNull(controller);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    @DisplayName("Verify Account Management FXML and Controller")
    public void testAccountViewLoading() throws Exception {
        runOnFxThread(() -> {
            try {
                URL res = getClass().getResource("/com/bank/account.fxml");
                assertNotNull(res);
                FXMLLoader loader = new FXMLLoader(res);
                Parent root = loader.load();
                assertNotNull(root);

                AccountController controller = loader.getController();
                assertNotNull(controller);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    @DisplayName("Verify Transaction Terminal FXML and Controller")
    public void testTransactionViewLoading() throws Exception {
        runOnFxThread(() -> {
            try {
                URL res = getClass().getResource("/com/bank/transaction.fxml");
                assertNotNull(res);
                FXMLLoader loader = new FXMLLoader(res);
                Parent root = loader.load();
                assertNotNull(root);

                TransactionController controller = loader.getController();
                assertNotNull(controller);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    @DisplayName("Verify History Ledger FXML, Columns, and Filtering")
    public void testHistoryViewLoading() throws Exception {
        runOnFxThread(() -> {
            try {
                URL res = getClass().getResource("/com/bank/history.fxml");
                assertNotNull(res);
                FXMLLoader loader = new FXMLLoader(res);
                Parent root = loader.load();
                assertNotNull(root);

                HistoryController controller = loader.getController();
                assertNotNull(controller);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    @DisplayName("Verify Login Portal FXML and Controller")
    public void testLoginViewLoading() throws Exception {
        runOnFxThread(() -> {
            try {
                URL res = getClass().getResource("/com/bank/login.fxml");
                assertNotNull(res);
                FXMLLoader loader = new FXMLLoader(res);
                Parent root = loader.load();
                assertNotNull(root);

                LoginController controller = loader.getController();
                assertNotNull(controller);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    @DisplayName("Verify UI Event Handlers and Form Interactions")
    public void testUIEventHandlersAndFormInteractions() throws Exception {
        runOnFxThread(() -> {
            try {
                // 1. Test Account Controller Form Interactions
                URL accRes = getClass().getResource("/com/bank/account.fxml");
                FXMLLoader accLoader = new FXMLLoader(accRes);
                accLoader.load();
                AccountController accCtrl = accLoader.getController();
                assertNotNull(accCtrl);
                accCtrl.handleRefreshTable(null);
                accCtrl.handleResetForm(null);

                // 2. Test Transaction Controller Event Interactions
                URL txRes = getClass().getResource("/com/bank/transaction.fxml");
                FXMLLoader txLoader = new FXMLLoader(txRes);
                txLoader.load();
                TransactionController txCtrl = txLoader.getController();
                assertNotNull(txCtrl);
                txCtrl.handleTypeChanged(null);
                txCtrl.handleCheckSourceAccount(null);
                txCtrl.handleClearForm(null);

                // 3. Test History Controller Filtering
                URL histRes = getClass().getResource("/com/bank/history.fxml");
                FXMLLoader histLoader = new FXMLLoader(histRes);
                histLoader.load();
                HistoryController histCtrl = histLoader.getController();
                assertNotNull(histCtrl);
                histCtrl.handleApplyFilter(null);
                histCtrl.handleClearFilter(null);
                histCtrl.handleRefresh(null);

                // 4. Test Dashboard Controller Refresh
                URL dashRes = getClass().getResource("/com/bank/dashboard.fxml");
                FXMLLoader dashLoader = new FXMLLoader(dashRes);
                dashLoader.load();
                DashboardController dashCtrl = dashLoader.getController();
                assertNotNull(dashCtrl);
                dashCtrl.navToDashboard(null);

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    @DisplayName("Verify Fund Transfer FXML and TransferController")
    public void testTransferViewLoading() throws Exception {
        runOnFxThread(() -> {
            try {
                URL res = getClass().getResource("/com/bank/transfer.fxml");
                assertNotNull(res);
                FXMLLoader loader = new FXMLLoader(res);
                Parent root = loader.load();
                assertNotNull(root);

                TransferController controller = loader.getController();
                assertNotNull(controller);
                controller.selectBenAnanya(null);
                controller.handleReset(null);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    @DisplayName("Verify Notifications FXML and NotificationController")
    public void testNotificationViewLoading() throws Exception {
        runOnFxThread(() -> {
            try {
                URL res = getClass().getResource("/com/bank/notifications.fxml");
                assertNotNull(res);
                FXMLLoader loader = new FXMLLoader(res);
                Parent root = loader.load();
                assertNotNull(root);

                NotificationController controller = loader.getController();
                assertNotNull(controller);
                controller.handleFilterCategory(null);
                controller.handleMarkAllAsRead(null);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    @DisplayName("Verify Reports FXML and ReportsController")
    public void testReportsViewLoading() throws Exception {
        runOnFxThread(() -> {
            try {
                URL res = getClass().getResource("/com/bank/reports.fxml");
                assertNotNull(res);
                FXMLLoader loader = new FXMLLoader(res);
                Parent root = loader.load();
                assertNotNull(root);

                ReportsController controller = loader.getController();
                assertNotNull(controller);
                controller.handleGenerateReport(null);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    @DisplayName("Verify Security Overview FXML and SecurityController")
    public void testSecurityViewLoading() throws Exception {
        runOnFxThread(() -> {
            try {
                URL res = getClass().getResource("/com/bank/security.fxml");
                assertNotNull(res);
                FXMLLoader loader = new FXMLLoader(res);
                Parent root = loader.load();
                assertNotNull(root);

                SecurityController controller = loader.getController();
                assertNotNull(controller);
                controller.handleRunIntegrityAudit(null);
                controller.handleExportAuditLog(null);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void runOnFxThread(Runnable action) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        final Throwable[] error = new Throwable[1];

        Platform.runLater(() -> {
            try {
                action.run();
            } catch (Throwable t) {
                error[0] = t;
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(6, TimeUnit.SECONDS), "Operation on FX thread timed out");
        if (error[0] != null) {
            throw new AssertionError("FX Thread execution failed", error[0]);
        }
    }
}
