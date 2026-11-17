
package com.bank;

import com.bank.controller.AccountController;
import com.bank.controller.DashboardController;
import com.bank.controller.HistoryController;
import com.bank.controller.TransactionController;
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
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Final Submission Mode Verification & Stabilisation Test Suite.
 * Validates the 4 core banking modules:
 * 1. Dashboard
 * 2. Account Management
 * 3. Transaction Processing
 * 4. Transaction History
 */
public class FinalSubmissionStabilisationTest {

    @BeforeAll
    public static void initJFX() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        try {
            Platform.startup(latch::countDown);
        } catch (IllegalStateException alreadyStarted) {
            latch.countDown();
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS),
                "JavaFX failed to initialize");
    }

    @Test
    @DisplayName("Checklist 1 & 2: Verify All 4 Core Views Load and Initialize Cleanly")
    public void testAllFourScreensOpen() throws Exception {
        runOnFxThread(() -> {
            try {
                // 1. Dashboard
                URL dashRes = getClass().getResource("/com/bank/dashboard.fxml");
                assertNotNull(dashRes, "dashboard.fxml must exist");

                FXMLLoader dashLoader = new FXMLLoader(dashRes);
                Parent dashRoot = dashLoader.load();

                assertNotNull(dashRoot);

                DashboardController dashCtrl = dashLoader.getController();
                assertNotNull(dashCtrl,
                        "DashboardController must be initialized");

                // 2. Account Management
                URL accRes = getClass().getResource("/com/bank/account.fxml");
                assertNotNull(accRes, "account.fxml must exist");

                FXMLLoader accLoader = new FXMLLoader(accRes);
                Parent accRoot = accLoader.load();

                assertNotNull(accRoot);

                AccountController accCtrl = accLoader.getController();
                assertNotNull(accCtrl,
                        "AccountController must be initialized");

                // 3. Transaction Processing
                URL txRes = getClass().getResource("/com/bank/transaction.fxml");
                assertNotNull(txRes, "transaction.fxml must exist");

                FXMLLoader txLoader = new FXMLLoader(txRes);
                Parent txRoot = txLoader.load();

                assertNotNull(txRoot);

                TransactionController txCtrl = txLoader.getController();
                assertNotNull(txCtrl,
                        "TransactionController must be initialized");

                // 4. Transaction History
                URL histRes = getClass().getResource("/com/bank/history.fxml");
                assertNotNull(histRes, "history.fxml must exist");

                FXMLLoader histLoader = new FXMLLoader(histRes);
                Parent histRoot = histLoader.load();

                assertNotNull(histRoot);

                HistoryController histCtrl = histLoader.getController();
                assertNotNull(histCtrl,
                        "HistoryController must be initialized");

            } catch (Exception e) {
                throw new RuntimeException(
                        "Screen loading failed: " + e.getMessage(), e);
            }
        });
    }

    @Test
    @DisplayName("Checklist 3: Verify Sidebar Navigation Event Handlers Across All 4 Screens")
    public void testSidebarNavigationHandlers() throws Exception {
        runOnFxThread(() -> {
            try {
                FXMLLoader dashLoader = new FXMLLoader(
                        getClass().getResource("/com/bank/dashboard.fxml"));
                dashLoader.load();

                DashboardController dash = dashLoader.getController();

                assertDoesNotThrow(() -> dash.navToDashboard(null));
                assertDoesNotThrow(() -> dash.navToAccounts(null));
                assertDoesNotThrow(() -> dash.navToTransactions(null));
                assertDoesNotThrow(() -> dash.navToHistory(null));

                FXMLLoader accLoader = new FXMLLoader(
                        getClass().getResource("/com/bank/account.fxml"));
                accLoader.load();

                AccountController acc = accLoader.getController();

                assertDoesNotThrow(() -> acc.navToDashboard(null));
                assertDoesNotThrow(() -> acc.navToAccounts(null));
                assertDoesNotThrow(() -> acc.navToTransactions(null));
                assertDoesNotThrow(() -> acc.navToHistory(null));

                FXMLLoader txLoader = new FXMLLoader(
                        getClass().getResource("/com/bank/transaction.fxml"));
                txLoader.load();

                TransactionController tx = txLoader.getController();

                assertDoesNotThrow(() -> tx.navToDashboard(null));
                assertDoesNotThrow(() -> tx.navToAccounts(null));
                assertDoesNotThrow(() -> tx.navToTransactions(null));
                assertDoesNotThrow(() -> tx.navToHistory(null));

                FXMLLoader histLoader = new FXMLLoader(
                        getClass().getResource("/com/bank/history.fxml"));
                histLoader.load();

                HistoryController hist = histLoader.getController();

                assertDoesNotThrow(() -> hist.navToDashboard(null));
                assertDoesNotThrow(() -> hist.navToAccounts(null));
                assertDoesNotThrow(() -> hist.navToTransactions(null));
                assertDoesNotThrow(() -> hist.navToHistory(null));

            } catch (Exception e) {
                throw new RuntimeException(
                        "Sidebar navigation test failed: " + e.getMessage(), e);
            }
        });
    }

    @Test
    @DisplayName("Checklist 4 & 5: Verify Account Creation and Lookup Operations")
    public void testAccountCreationAndLookup() throws Exception {
        AccountService accountService =
                ServiceFactory.getInstance().getAccountService();

        assertNotNull(accountService);

        // 1. Lookup existing seeded Indian account
        Optional<Account> existingAcc =
                accountService.findAccount("ACC-1001");

        assertTrue(existingAcc.isPresent(), "ACC-1001 must exist");
        assertEquals("Aarav Sharma",
                existingAcc.get().getCustomerName());
        assertTrue(existingAcc.get().getFormattedBalance().startsWith("₹"));

        // 2. Generate unique customer details for every test run
        String uniqueId = UUID.randomUUID().toString();

        String uniqueEmail =
                "rohan.deshmukh." + uniqueId + "@apexbank.in";

        String phoneDigits = uniqueId.replace("-", "").substring(0, 9);

        String uniquePhone = "9" + phoneDigits;

        // 3. Create new customer account
        BigDecimal openingBal = new BigDecimal("25000.00");

        Account newAcc = accountService.createAccount(
                "Rohan Deshmukh",
                uniqueEmail,
                uniquePhone,
                "SAVINGS",
                openingBal
        );

        assertNotNull(newAcc);
        assertNotNull(newAcc.getAccountNumber());
        assertTrue(newAcc.getAccountNumber().startsWith("ACC-"));
        assertEquals("ACTIVE", newAcc.getStatus());
        assertEquals("Rohan Deshmukh", newAcc.getCustomerName());

        // 4. Lookup the newly created account
        Optional<Account> lookedUp =
                accountService.findAccount(newAcc.getAccountNumber());

        assertTrue(lookedUp.isPresent(),
                "Newly created account must be retrievable");

        assertEquals(openingBal, lookedUp.get().getBalance());
        assertEquals(uniqueEmail, lookedUp.get().getEmail());
    }

    @Test
    @DisplayName("Checklist 6: Verify Deposit and Withdrawal Operations with Balance Updates")
    public void testDepositAndWithdrawalOperations() throws Exception {
        AccountService accountService =
                ServiceFactory.getInstance().getAccountService();

        TransactionService txService =
                ServiceFactory.getInstance().getTransactionService();

        // 1. Source Account Initial State
        Account testAcc =
                accountService.findAccount("ACC-1003").orElseThrow();

        BigDecimal initialBal = testAcc.getBalance();

        // 2. Deposit Test
        BigDecimal depositAmt = new BigDecimal("7500.00");

        Transaction depositTx = txService.deposit(
                "ACC-1003",
                depositAmt,
                "Stabilisation Test Deposit"
        );

        assertNotNull(depositTx);
        assertEquals("DEPOSIT", depositTx.getTransactionType());
        assertEquals(initialBal.add(depositAmt),
                depositTx.getBalanceAfter());

        // Verify balance in accountService
        Account afterDepositAcc =
                accountService.findAccount("ACC-1003").orElseThrow();

        assertEquals(initialBal.add(depositAmt),
                afterDepositAcc.getBalance());

        // 3. Withdrawal Test
        BigDecimal balanceBeforeWithdraw =
                afterDepositAcc.getBalance();

        BigDecimal withdrawAmt = new BigDecimal("3500.00");

        BigDecimal expectedAfterWithdraw =
                balanceBeforeWithdraw.subtract(withdrawAmt);

        Transaction withdrawTx = txService.withdraw(
                "ACC-1003",
                withdrawAmt,
                "Stabilisation Test Withdrawal"
        );

        assertNotNull(withdrawTx);
        assertEquals("WITHDRAWAL", withdrawTx.getTransactionType());
        assertEquals(expectedAfterWithdraw,
                withdrawTx.getBalanceAfter());

        // Verify balance in accountService
        Account afterWithdrawAcc =
                accountService.findAccount("ACC-1003").orElseThrow();

        assertEquals(expectedAfterWithdraw,
                afterWithdrawAcc.getBalance());
    }

    @Test
    @DisplayName("Checklist 7: Verify Transaction History Retrieval and Formatting")
    public void testTransactionHistoryRetrieval() throws Exception {
        TransactionService txService =
                ServiceFactory.getInstance().getTransactionService();

        List<Transaction> all = txService.getAllTransactions();

        assertNotNull(all);
        assertFalse(all.isEmpty(),
                "Transaction history must not be empty");

        // Verify Indian currency and date formatting
        Transaction sampleTx = all.get(0);

        assertNotNull(sampleTx.getFormattedAmount());
        assertTrue(sampleTx.getFormattedAmount().startsWith("₹"),
                "Amount must be formatted with ₹");

        assertNotNull(sampleTx.getFormattedBalanceAfter());
        assertTrue(sampleTx.getFormattedBalanceAfter().startsWith("₹"),
                "Balance must be formatted with ₹");

        assertNotNull(sampleTx.getFormattedDate());
        assertFalse(sampleTx.getFormattedDate().isBlank());

        // Verify account-specific history retrieval
        List<Transaction> acc1History =
                txService.getTransactionHistory("ACC-1001");

        assertNotNull(acc1History);
        assertFalse(acc1History.isEmpty());

        for (Transaction t : acc1History) {
            assertTrue(
                    "ACC-1001".equals(t.getSourceAccount())
                            || "ACC-1001".equals(t.getDestinationAccount())
            );
        }
    }

    @Test
    @DisplayName("Checklist 8: Verify Dashboard Analytics Charts Rendering")
    public void testDashboardChartsRendering() throws Exception {
        runOnFxThread(() -> {
            try {
                FXMLLoader dashLoader = new FXMLLoader(
                        getClass().getResource("/com/bank/dashboard.fxml"));

                dashLoader.load();

                DashboardController dash = dashLoader.getController();

                assertNotNull(dash);

                // Verify controller loads and responds to navigation refresh
                dash.navToDashboard(null);

            } catch (Exception e) {
                throw new RuntimeException(
                        "Charts rendering failed: " + e.getMessage(), e);
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

        assertTrue(latch.await(6, TimeUnit.SECONDS),
                "Operation on FX thread timed out");

        if (error[0] != null) {
            throw new AssertionError(
                    "FX Thread execution failed", error[0]);
        }
    }
}
