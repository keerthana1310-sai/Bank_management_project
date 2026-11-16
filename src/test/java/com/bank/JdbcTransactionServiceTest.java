package com.bank;

import com.bank.dao.AccountDAO;
import com.bank.dao.TransactionDAO;
import com.bank.model.Account;
import com.bank.model.Transaction;
import com.bank.service.BankingException;
import com.bank.service.impl.JdbcTransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive Unit and Integration Test Suite for Jayasree's Transaction Backend (JdbcTransactionService).
 * Covers all 41 checklist requirements outlined in the team plan and specification:
 * - Deposit Tests (1-9)
 * - Withdrawal Tests (10-18)
 * - Atomic Transfer Tests (19-34)
 * - Transaction Ledger & History Tests (35-41)
 */
public class JdbcTransactionServiceTest {

    private TestAccountDAO testAccountDAO;
    private TestTransactionDAO testTransactionDAO;
    private TestJdbcTransactionService transactionService;
    private MockConnectionTracker mockConnection;

    // Accounts for testing
    private static final String ACC_SOURCE = "ACC-1001";
    private static final String ACC_DEST = "ACC-1002";
    private static final String ACC_INACTIVE = "ACC-1006";
    private static final String ACC_CLOSED = "ACC-1099";

    @BeforeEach
    public void setUp() {
        mockConnection = new MockConnectionTracker();
        testAccountDAO = new TestAccountDAO(mockConnection);
        testTransactionDAO = new TestTransactionDAO(mockConnection);

        // Seed test accounts with verified initial state
        testAccountDAO.seedAccount(new Account(ACC_SOURCE, 1L, "Aarav Sharma", "aarav@bank.in", "9876543210",
                "SAVINGS", new BigDecimal("50000.00"), "ACTIVE", LocalDateTime.now()));

        testAccountDAO.seedAccount(new Account(ACC_DEST, 2L, "Ananya Reddy", "ananya@bank.in", "9876543211",
                "CURRENT", new BigDecimal("25000.00"), "ACTIVE", LocalDateTime.now()));

        testAccountDAO.seedAccount(new Account(ACC_INACTIVE, 6L, "Sneha Rao", "sneha@bank.in", "9876543215",
                "SAVINGS", new BigDecimal("10000.00"), "INACTIVE", LocalDateTime.now()));

        testAccountDAO.seedAccount(new Account(ACC_CLOSED, 9L, "Closed User", "closed@bank.in", "9876543219",
                "SAVINGS", new BigDecimal("0.00"), "CLOSED", LocalDateTime.now()));

        transactionService = new TestJdbcTransactionService(testTransactionDAO, testAccountDAO, mockConnection);
    }

    // =========================================================================
    // PART A: DEPOSIT TESTS (1 - 9)
    // =========================================================================

    @Test
    @DisplayName("1. Successful Deposit updates balance and saves transaction")
    public void test01_SuccessfulDeposit() throws BankingException {
        BigDecimal depositAmount = new BigDecimal("5000.00");
        Transaction tx = transactionService.deposit(ACC_SOURCE, depositAmount, "Salary Deposit");

        assertNotNull(tx, "Returned transaction must not be null");
        assertEquals("DEPOSIT", tx.getTransactionType());
        assertEquals(ACC_SOURCE, tx.getSourceAccount());
        assertNull(tx.getDestinationAccount());
        assertEquals(new BigDecimal("5000.00"), tx.getAmount());
        assertEquals(new BigDecimal("55000.00"), tx.getBalanceAfter());
        assertEquals("Salary Deposit", tx.getDescription());
        assertNotNull(tx.getTimestamp());

        // Verify account balance in persistence layer
        Account updatedAcc = testAccountDAO.findAccountDirect(ACC_SOURCE);
        assertEquals(new BigDecimal("55000.00"), updatedAcc.getBalance());

        // Verify transaction committed in database
        assertTrue(mockConnection.committed, "Transaction must be committed");
        assertFalse(mockConnection.rolledBack, "Transaction must not be rolled back");
    }

    @Test
    @DisplayName("2. Deposit rejects null amount")
    public void test02_DepositNullAmount() {
        BankingException ex = assertThrows(BankingException.class, () ->
                transactionService.deposit(ACC_SOURCE, null, "Null deposit"));
        assertTrue(ex.getMessage().contains("null"), "Error should explain amount cannot be null");
    }

    @Test
    @DisplayName("3. Deposit rejects zero amount")
    public void test03_DepositZeroAmount() {
        BankingException ex = assertThrows(BankingException.class, () ->
                transactionService.deposit(ACC_SOURCE, BigDecimal.ZERO, "Zero deposit"));
        assertTrue(ex.getMessage().contains("greater than zero"));
    }

    @Test
    @DisplayName("4. Deposit rejects negative amount")
    public void test04_DepositNegativeAmount() {
        BankingException ex = assertThrows(BankingException.class, () ->
                transactionService.deposit(ACC_SOURCE, new BigDecimal("-100.00"), "Negative deposit"));
        assertTrue(ex.getMessage().contains("greater than zero"));
    }

    @Test
    @DisplayName("5. Deposit rejects missing or blank account number")
    public void test05_DepositMissingAccount() {
        assertThrows(BankingException.class, () ->
                transactionService.deposit(null, new BigDecimal("100.00"), "Missing account"));
        assertThrows(BankingException.class, () ->
                transactionService.deposit("   ", new BigDecimal("100.00"), "Blank account"));
    }

    @Test
    @DisplayName("6. Deposit rejects nonexistent account")
    public void test06_DepositNonexistentAccount() {
        BankingException ex = assertThrows(BankingException.class, () ->
                transactionService.deposit("ACC-9999", new BigDecimal("500.00"), "Unknown acc"));
        assertTrue(ex.getMessage().contains("does not exist"));
        assertTrue(mockConnection.rolledBack, "Rollback must be triggered on failure");
    }

    @Test
    @DisplayName("7. Deposit rejects inactive or closed accounts")
    public void test07_DepositInactiveAccount() {
        BankingException exInactive = assertThrows(BankingException.class, () ->
                transactionService.deposit(ACC_INACTIVE, new BigDecimal("500.00"), "Inactive"));
        assertTrue(exInactive.getMessage().contains("inactive or closed"));

        BankingException exClosed = assertThrows(BankingException.class, () ->
                transactionService.deposit(ACC_CLOSED, new BigDecimal("500.00"), "Closed"));
        assertTrue(exClosed.getMessage().contains("inactive or closed"));
    }

    @Test
    @DisplayName("8. Deposit handles transaction insertion failure")
    public void test08_DepositTransactionSaveFailure() {
        testTransactionDAO.setReturnFalseOnSave(true);

        BankingException ex = assertThrows(BankingException.class, () ->
                transactionService.deposit(ACC_SOURCE, new BigDecimal("1000.00"), "Failed tx save"));

        assertTrue(ex.getMessage().contains("Failed to save transaction record"));
        assertTrue(mockConnection.rolledBack, "Rollback must be executed on save failure");
    }

    @Test
    @DisplayName("9. Deposit failure leaves balance and ledger consistent")
    public void test09_DepositConsistencyOnFailure() {
        BigDecimal initialBal = testAccountDAO.findAccountDirect(ACC_SOURCE).getBalance();
        int initialTxCount = testTransactionDAO.countDirect();

        testTransactionDAO.setReturnFalseOnSave(true);

        assertThrows(BankingException.class, () ->
                transactionService.deposit(ACC_SOURCE, new BigDecimal("1000.00"), "Fail"));

        // Rollback ensures balance remains strictly equal to initial value
        assertEquals(initialBal, testAccountDAO.findAccountDirect(ACC_SOURCE).getBalance());
        assertEquals(initialTxCount, testTransactionDAO.countDirect());
        assertTrue(mockConnection.rolledBack);
    }

    // =========================================================================
    // PART B: WITHDRAWAL TESTS (10 - 18)
    // =========================================================================

    @Test
    @DisplayName("10. Successful Withdrawal reduces balance and records transaction")
    public void test10_SuccessfulWithdrawal() throws BankingException {
        BigDecimal withdrawAmount = new BigDecimal("10000.00");
        Transaction tx = transactionService.withdraw(ACC_SOURCE, withdrawAmount, "ATM Cash");

        assertNotNull(tx);
        assertEquals("WITHDRAWAL", tx.getTransactionType());
        assertEquals(ACC_SOURCE, tx.getSourceAccount());
        assertNull(tx.getDestinationAccount());
        assertEquals(new BigDecimal("10000.00"), tx.getAmount());
        assertEquals(new BigDecimal("40000.00"), tx.getBalanceAfter());

        Account updated = testAccountDAO.findAccountDirect(ACC_SOURCE);
        assertEquals(new BigDecimal("40000.00"), updated.getBalance());
        assertTrue(mockConnection.committed);
        assertFalse(mockConnection.rolledBack);
    }

    @Test
    @DisplayName("11. Withdrawal rejects null amount")
    public void test11_WithdrawalNullAmount() {
        BankingException ex = assertThrows(BankingException.class, () ->
                transactionService.withdraw(ACC_SOURCE, null, "Null"));
        assertTrue(ex.getMessage().contains("null"));
    }

    @Test
    @DisplayName("12. Withdrawal rejects zero amount")
    public void test12_WithdrawalZeroAmount() {
        BankingException ex = assertThrows(BankingException.class, () ->
                transactionService.withdraw(ACC_SOURCE, BigDecimal.ZERO, "Zero"));
        assertTrue(ex.getMessage().contains("greater than zero"));
    }

    @Test
    @DisplayName("13. Withdrawal rejects negative amount")
    public void test13_WithdrawalNegativeAmount() {
        BankingException ex = assertThrows(BankingException.class, () ->
                transactionService.withdraw(ACC_SOURCE, new BigDecimal("-50.00"), "Neg"));
        assertTrue(ex.getMessage().contains("greater than zero"));
    }

    @Test
    @DisplayName("14. Withdrawal rejects insufficient funds")
    public void test14_WithdrawalInsufficientFunds() {
        BigDecimal excessive = new BigDecimal("1000000.00");
        BankingException ex = assertThrows(BankingException.class, () ->
                transactionService.withdraw(ACC_SOURCE, excessive, "Excessive"));

        assertTrue(ex.getMessage().contains("Insufficient funds"));
        assertTrue(mockConnection.rolledBack);
        assertEquals(new BigDecimal("50000.00"), testAccountDAO.findAccountDirect(ACC_SOURCE).getBalance());
    }

    @Test
    @DisplayName("15. Withdrawal rejects nonexistent account")
    public void test15_WithdrawalNonexistentAccount() {
        BankingException ex = assertThrows(BankingException.class, () ->
                transactionService.withdraw("ACC-9999", new BigDecimal("500.00"), "Ghost"));
        assertTrue(ex.getMessage().contains("does not exist"));
        assertTrue(mockConnection.rolledBack);
    }

    @Test
    @DisplayName("16. Withdrawal rejects inactive or closed accounts")
    public void test16_WithdrawalInactiveAccount() {
        BankingException ex = assertThrows(BankingException.class, () ->
                transactionService.withdraw(ACC_INACTIVE, new BigDecimal("100.00"), "Inactive"));
        assertTrue(ex.getMessage().contains("inactive or closed"));
    }

    @Test
    @DisplayName("17. Withdrawal handles transaction insertion failure")
    public void test17_WithdrawalTransactionSaveFailure() {
        testTransactionDAO.setReturnFalseOnSave(true);
        BankingException ex = assertThrows(BankingException.class, () ->
                transactionService.withdraw(ACC_SOURCE, new BigDecimal("1000.00"), "Save fail"));

        assertTrue(ex.getMessage().contains("Failed to save transaction record"));
        assertTrue(mockConnection.rolledBack);
    }

    @Test
    @DisplayName("18. Withdrawal failure leaves balance and ledger consistent")
    public void test18_WithdrawalConsistencyOnFailure() {
        BigDecimal initialBal = testAccountDAO.findAccountDirect(ACC_SOURCE).getBalance();
        testTransactionDAO.setReturnFalseOnSave(true);

        assertThrows(BankingException.class, () ->
                transactionService.withdraw(ACC_SOURCE, new BigDecimal("2000.00"), "Fail"));

        assertEquals(initialBal, testAccountDAO.findAccountDirect(ACC_SOURCE).getBalance());
        assertTrue(mockConnection.rolledBack);
    }

    // =========================================================================
    // PART C: ATOMIC TRANSFER TESTS (19 - 34)
    // =========================================================================

    @Test
    @DisplayName("19. Successful Transfer executes atomically")
    public void test19_SuccessfulTransfer() throws BankingException {
        BigDecimal transferAmount = new BigDecimal("15000.00");
        Transaction tx = transactionService.transfer(ACC_SOURCE, ACC_DEST, transferAmount, "Rent Payment");

        assertNotNull(tx);
        assertEquals("TRANSFER", tx.getTransactionType());
        assertEquals(ACC_SOURCE, tx.getSourceAccount());
        assertEquals(ACC_DEST, tx.getDestinationAccount());
        assertEquals(new BigDecimal("15000.00"), tx.getAmount());
        assertEquals(new BigDecimal("35000.00"), tx.getBalanceAfter());

        // Verify both balances
        assertEquals(new BigDecimal("35000.00"), testAccountDAO.findAccountDirect(ACC_SOURCE).getBalance());
        assertEquals(new BigDecimal("40000.00"), testAccountDAO.findAccountDirect(ACC_DEST).getBalance());

        assertTrue(mockConnection.committed, "Transfer must be committed");
        assertFalse(mockConnection.rolledBack);
    }

    @Test
    @DisplayName("20. Transfer rejects nonexistent source account")
    public void test20_TransferNonexistentSource() {
        BankingException ex = assertThrows(BankingException.class, () ->
                transactionService.transfer("ACC-GHOST", ACC_DEST, new BigDecimal("100.00"), "Invalid src"));
        assertTrue(ex.getMessage().contains("Source account"));
        assertTrue(mockConnection.rolledBack);
    }

    @Test
    @DisplayName("21. Transfer rejects nonexistent destination account")
    public void test21_TransferNonexistentDestination() {
        BankingException ex = assertThrows(BankingException.class, () ->
                transactionService.transfer(ACC_SOURCE, "ACC-GHOST", new BigDecimal("100.00"), "Invalid dest"));
        assertTrue(ex.getMessage().contains("Destination account"));
        assertTrue(mockConnection.rolledBack);
    }

    @Test
    @DisplayName("22. Transfer rejects identical source and destination")
    public void test22_TransferIdenticalAccounts() {
        BankingException ex = assertThrows(BankingException.class, () ->
                transactionService.transfer(ACC_SOURCE, ACC_SOURCE, new BigDecimal("100.00"), "Self"));
        assertTrue(ex.getMessage().contains("identical"));
    }

    @Test
    @DisplayName("23. Transfer rejects zero amount")
    public void test23_TransferZeroAmount() {
        BankingException ex = assertThrows(BankingException.class, () ->
                transactionService.transfer(ACC_SOURCE, ACC_DEST, BigDecimal.ZERO, "Zero"));
        assertTrue(ex.getMessage().contains("greater than zero"));
    }

    @Test
    @DisplayName("24. Transfer rejects negative amount")
    public void test24_TransferNegativeAmount() {
        BankingException ex = assertThrows(BankingException.class, () ->
                transactionService.transfer(ACC_SOURCE, ACC_DEST, new BigDecimal("-500.00"), "Neg"));
        assertTrue(ex.getMessage().contains("greater than zero"));
    }

    @Test
    @DisplayName("25. Transfer rejects insufficient source balance")
    public void test25_TransferInsufficientBalance() {
        BigDecimal overAmount = new BigDecimal("80000.00"); // Source balance is 50,000
        BankingException ex = assertThrows(BankingException.class, () ->
                transactionService.transfer(ACC_SOURCE, ACC_DEST, overAmount, "Overdraft"));

        assertTrue(ex.getMessage().contains("Insufficient balance"));
        assertTrue(mockConnection.rolledBack);

        // Balances must remain unchanged
        assertEquals(new BigDecimal("50000.00"), testAccountDAO.findAccountDirect(ACC_SOURCE).getBalance());
        assertEquals(new BigDecimal("25000.00"), testAccountDAO.findAccountDirect(ACC_DEST).getBalance());
    }

    @Test
    @DisplayName("26. Transfer rejects inactive source account")
    public void test26_TransferInactiveSource() {
        BankingException ex = assertThrows(BankingException.class, () ->
                transactionService.transfer(ACC_INACTIVE, ACC_DEST, new BigDecimal("100.00"), "From inactive"));
        assertTrue(ex.getMessage().contains("Source account is not active"));
        assertTrue(mockConnection.rolledBack);
    }

    @Test
    @DisplayName("27. Transfer rejects inactive destination account")
    public void test27_TransferInactiveDestination() {
        BankingException ex = assertThrows(BankingException.class, () ->
                transactionService.transfer(ACC_SOURCE, ACC_INACTIVE, new BigDecimal("100.00"), "To inactive"));
        assertTrue(ex.getMessage().contains("Destination account is not active"));
        assertTrue(mockConnection.rolledBack);
    }

    @Test
    @DisplayName("28. Transfer correctly reduces source balance")
    public void test28_TransferSourceReduced() throws BankingException {
        transactionService.transfer(ACC_SOURCE, ACC_DEST, new BigDecimal("12000.00"), "Transfer test");
        assertEquals(new BigDecimal("38000.00"), testAccountDAO.findAccountDirect(ACC_SOURCE).getBalance());
    }

    @Test
    @DisplayName("29. Transfer correctly increases destination balance")
    public void test29_TransferDestinationIncreased() throws BankingException {
        transactionService.transfer(ACC_SOURCE, ACC_DEST, new BigDecimal("12000.00"), "Transfer test");
        assertEquals(new BigDecimal("37000.00"), testAccountDAO.findAccountDirect(ACC_DEST).getBalance());
    }

    @Test
    @DisplayName("30. Transfer transaction record is saved with source and destination")
    public void test30_TransferTransactionSaved() throws BankingException {
        Transaction tx = transactionService.transfer(ACC_SOURCE, ACC_DEST, new BigDecimal("7500.00"), "Fee payment");
        assertNotNull(tx.getTransactionId());
        assertEquals(ACC_SOURCE, tx.getSourceAccount());
        assertEquals(ACC_DEST, tx.getDestinationAccount());

        List<Transaction> history = transactionService.getTransactionHistory(ACC_SOURCE);
        assertFalse(history.isEmpty());
        assertEquals("TRANSFER", history.get(0).getTransactionType());
    }

    @Test
    @DisplayName("31. Transfer failure during source balance update triggers rollback")
    public void test31_TransferSourceUpdateFailure() {
        testAccountDAO.setFailOnSourceUpdate(true);

        assertThrows(BankingException.class, () ->
                transactionService.transfer(ACC_SOURCE, ACC_DEST, new BigDecimal("5000.00"), "Fail src"));

        assertTrue(mockConnection.rolledBack);
        assertEquals(new BigDecimal("50000.00"), testAccountDAO.findAccountDirect(ACC_SOURCE).getBalance());
        assertEquals(new BigDecimal("25000.00"), testAccountDAO.findAccountDirect(ACC_DEST).getBalance());
    }

    @Test
    @DisplayName("32. Transfer failure during destination balance update triggers rollback")
    public void test32_TransferDestinationUpdateFailure() {
        testAccountDAO.setFailOnDestUpdate(true);

        assertThrows(BankingException.class, () ->
                transactionService.transfer(ACC_SOURCE, ACC_DEST, new BigDecimal("5000.00"), "Fail dest"));

        assertTrue(mockConnection.rolledBack);
        assertEquals(new BigDecimal("50000.00"), testAccountDAO.findAccountDirect(ACC_SOURCE).getBalance());
        assertEquals(new BigDecimal("25000.00"), testAccountDAO.findAccountDirect(ACC_DEST).getBalance());
    }

    @Test
    @DisplayName("33. Transfer failure during transaction insertion triggers rollback")
    public void test33_TransferTransactionSaveFailure() {
        testTransactionDAO.setReturnFalseOnSave(true);

        assertThrows(BankingException.class, () ->
                transactionService.transfer(ACC_SOURCE, ACC_DEST, new BigDecimal("5000.00"), "Fail save"));

        assertTrue(mockConnection.rolledBack);
        assertEquals(new BigDecimal("50000.00"), testAccountDAO.findAccountDirect(ACC_SOURCE).getBalance());
        assertEquals(new BigDecimal("25000.00"), testAccountDAO.findAccountDirect(ACC_DEST).getBalance());
    }

    @Test
    @DisplayName("34. Verify atomic rollback leaves both original balances unchanged")
    public void test34_VerifyRollbackPreservesBalances() {
        BigDecimal srcInitial = testAccountDAO.findAccountDirect(ACC_SOURCE).getBalance();
        BigDecimal destInitial = testAccountDAO.findAccountDirect(ACC_DEST).getBalance();

        testTransactionDAO.setReturnFalseOnSave(true);

        assertThrows(BankingException.class, () ->
                transactionService.transfer(ACC_SOURCE, ACC_DEST, new BigDecimal("10000.00"), "Atomic fail"));

        assertEquals(srcInitial, testAccountDAO.findAccountDirect(ACC_SOURCE).getBalance());
        assertEquals(destInitial, testAccountDAO.findAccountDirect(ACC_DEST).getBalance());
        assertTrue(mockConnection.rolledBack);
    }

    // =========================================================================
    // PART D: TRANSACTION HISTORY & REPORTING TESTS (35 - 41)
    // =========================================================================

    @Test
    @DisplayName("35. Account history returns relevant deposits")
    public void test35_HistoryContainsDeposits() throws BankingException {
        transactionService.deposit(ACC_SOURCE, new BigDecimal("1000.00"), "Deposit 1");
        List<Transaction> history = transactionService.getTransactionHistory(ACC_SOURCE);

        boolean found = history.stream().anyMatch(tx -> "DEPOSIT".equals(tx.getTransactionType()) &&
                ACC_SOURCE.equals(tx.getSourceAccount()));
        assertTrue(found, "History must include deposited transactions");
    }

    @Test
    @DisplayName("36. Account history returns relevant withdrawals")
    public void test36_HistoryContainsWithdrawals() throws BankingException {
        transactionService.withdraw(ACC_SOURCE, new BigDecimal("500.00"), "Withdrawal 1");
        List<Transaction> history = transactionService.getTransactionHistory(ACC_SOURCE);

        boolean found = history.stream().anyMatch(tx -> "WITHDRAWAL".equals(tx.getTransactionType()) &&
                ACC_SOURCE.equals(tx.getSourceAccount()));
        assertTrue(found, "History must include withdrawal transactions");
    }

    @Test
    @DisplayName("37. Sent transfers appear in source account history")
    public void test37_SentTransferInSourceHistory() throws BankingException {
        transactionService.transfer(ACC_SOURCE, ACC_DEST, new BigDecimal("3000.00"), "Wire");
        List<Transaction> history = transactionService.getTransactionHistory(ACC_SOURCE);

        boolean found = history.stream().anyMatch(tx -> "TRANSFER".equals(tx.getTransactionType()) &&
                ACC_SOURCE.equals(tx.getSourceAccount()) && ACC_DEST.equals(tx.getDestinationAccount()));
        assertTrue(found, "Source history must show outbound wire transfer");
    }

    @Test
    @DisplayName("38. Received transfers appear in destination account history")
    public void test38_ReceivedTransferInDestinationHistory() throws BankingException {
        transactionService.transfer(ACC_SOURCE, ACC_DEST, new BigDecimal("3000.00"), "Wire");
        List<Transaction> history = transactionService.getTransactionHistory(ACC_DEST);

        boolean found = history.stream().anyMatch(tx -> "TRANSFER".equals(tx.getTransactionType()) &&
                ACC_DEST.equals(tx.getDestinationAccount()));
        assertTrue(found, "Destination history must show inbound wire transfer");
    }

    @Test
    @DisplayName("39. Newest transactions appear first (chronological sort)")
    public void test39_NewestTransactionsFirst() throws BankingException {
        transactionService.deposit(ACC_SOURCE, new BigDecimal("100.00"), "First");
        transactionService.deposit(ACC_SOURCE, new BigDecimal("200.00"), "Second");

        List<Transaction> history = transactionService.getTransactionHistory(ACC_SOURCE);
        assertTrue(history.size() >= 2);
        assertTrue(history.get(0).getTimestamp().isAfter(history.get(1).getTimestamp()) ||
                   history.get(0).getTimestamp().isEqual(history.get(1).getTimestamp()));
    }

    @Test
    @DisplayName("40. All-transactions retrieval returns entire system transactions")
    public void test40_GetAllTransactions() throws BankingException {
        transactionService.deposit(ACC_SOURCE, new BigDecimal("500.00"), "Tx A");
        transactionService.deposit(ACC_DEST, new BigDecimal("600.00"), "Tx B");

        List<Transaction> all = transactionService.getAllTransactions();
        assertTrue(all.size() >= 2);
    }

    @Test
    @DisplayName("41. Total transactions count reflects exact executed records")
    public void test41_TotalTransactionsCount() throws BankingException {
        int countBefore = transactionService.getTotalTransactionsCount();
        transactionService.deposit(ACC_SOURCE, new BigDecimal("100.00"), "A");
        transactionService.withdraw(ACC_SOURCE, new BigDecimal("50.00"), "B");
        transactionService.transfer(ACC_SOURCE, ACC_DEST, new BigDecimal("20.00"), "C");

        assertEquals(countBefore + 3, transactionService.getTotalTransactionsCount());
    }

    // =========================================================================
    // TEST DOUBLES & MOCK INFRASTRUCTURE (ACID & Row-locking simulation)
    // =========================================================================

    private static class TestJdbcTransactionService extends JdbcTransactionService {
        private final MockConnectionTracker tracker;

        public TestJdbcTransactionService(TransactionDAO transactionDAO, AccountDAO accountDAO, MockConnectionTracker tracker) {
            super(transactionDAO, accountDAO);
            this.tracker = tracker;
        }

        @Override
        protected Connection getConnection() throws SQLException {
            tracker.reset();
            return tracker.getConnection();
        }
    }

    private static class MockConnectionTracker {
        boolean committed = false;
        boolean rolledBack = false;
        boolean closed = false;
        boolean autoCommit = true;

        final List<Runnable> commitListeners = new ArrayList<>();
        final List<Runnable> rollbackListeners = new ArrayList<>();

        void reset() {
            committed = false;
            rolledBack = false;
            closed = false;
            autoCommit = true;
        }

        Connection getConnection() {
            return (Connection) Proxy.newProxyInstance(
                    Connection.class.getClassLoader(),
                    new Class[]{Connection.class},
                    (proxy, method, args) -> {
                        switch (method.getName()) {
                            case "setAutoCommit":
                                autoCommit = (boolean) args[0];
                                return null;
                            case "getAutoCommit":
                                return autoCommit;
                            case "commit":
                                committed = true;
                                for (Runnable r : new ArrayList<>(commitListeners)) {
                                    r.run();
                                }
                                return null;
                            case "rollback":
                                rolledBack = true;
                                for (Runnable r : new ArrayList<>(rollbackListeners)) {
                                    r.run();
                                }
                                return null;
                            case "close":
                                closed = true;
                                return null;
                            case "isClosed":
                                return closed;
                            default:
                                return null;
                        }
                    }
            );
        }
    }

    private static class TestAccountDAO extends AccountDAO {
        private final Map<String, Account> committedAccounts = new ConcurrentHashMap<>();
        private final Map<String, BigDecimal> stagedBalances = new ConcurrentHashMap<>();
        private boolean failOnSourceUpdate = false;
        private boolean failOnDestUpdate = false;

        TestAccountDAO(MockConnectionTracker tracker) {
            tracker.commitListeners.add(() -> {
                for (Map.Entry<String, BigDecimal> entry : stagedBalances.entrySet()) {
                    Account acc = committedAccounts.get(entry.getKey());
                    if (acc != null) {
                        acc.setBalance(entry.getValue());
                    }
                }
            });

            tracker.rollbackListeners.add(() -> {
                stagedBalances.clear();
                for (Map.Entry<String, Account> entry : committedAccounts.entrySet()) {
                    stagedBalances.put(entry.getKey(), entry.getValue().getBalance());
                }
            });
        }

        void seedAccount(Account account) {
            Account copy = cloneAccount(account);
            committedAccounts.put(account.getAccountNumber(), copy);
            stagedBalances.put(account.getAccountNumber(), account.getBalance());
        }

        Account findAccountDirect(String accNum) {
            Account acc = committedAccounts.get(accNum);
            return acc != null ? cloneAccount(acc) : null;
        }

        void setFailOnSourceUpdate(boolean fail) {
            this.failOnSourceUpdate = fail;
        }

        void setFailOnDestUpdate(boolean fail) {
            this.failOnDestUpdate = fail;
        }

        @Override
        public Optional<Account> findByAccountNumber(String accountNumber) {
            Account acc = committedAccounts.get(accountNumber);
            if (acc == null) return Optional.empty();
            Account view = cloneAccount(acc);
            BigDecimal staged = stagedBalances.get(accountNumber);
            if (staged != null) {
                view.setBalance(staged);
            }
            return Optional.of(view);
        }

        @Override
        public Optional<Account> findByAccountNumber(String accountNumber, Connection conn) {
            return findByAccountNumber(accountNumber);
        }

        @Override
        public Optional<Account> findByAccountNumberForUpdate(String accountNumber, Connection conn) {
            return findByAccountNumber(accountNumber);
        }

        @Override
        public boolean updateBalance(String accountNumber, BigDecimal newBalance, Connection conn) throws SQLException {
            if (failOnSourceUpdate && accountNumber.contains("1001")) {
                throw new SQLException("Simulated database failure updating source account.");
            }
            if (failOnDestUpdate && accountNumber.contains("1002")) {
                throw new SQLException("Simulated database failure updating destination account.");
            }

            if (committedAccounts.containsKey(accountNumber)) {
                stagedBalances.put(accountNumber, newBalance.setScale(2, RoundingMode.HALF_UP));
                return true;
            }
            return false;
        }

        private Account cloneAccount(Account src) {
            return new Account(src.getAccountNumber(), src.getCustomerId(), src.getCustomerName(),
                    src.getEmail(), src.getPhone(), src.getAccountType(), src.getBalance(),
                    src.getStatus(), src.getCreatedAt());
        }
    }

    private static class TestTransactionDAO extends TransactionDAO {
        private final List<Transaction> committedTransactions = Collections.synchronizedList(new ArrayList<>());
        private final List<Transaction> stagedTransactions = Collections.synchronizedList(new ArrayList<>());
        private final AtomicLong idSeq = new AtomicLong(9001);
        private boolean returnFalseOnSave = false;

        TestTransactionDAO(MockConnectionTracker tracker) {
            tracker.commitListeners.add(() -> {
                committedTransactions.addAll(0, stagedTransactions);
                stagedTransactions.clear();
            });

            tracker.rollbackListeners.add(() -> {
                stagedTransactions.clear();
            });
        }

        void setReturnFalseOnSave(boolean returnFalse) {
            this.returnFalseOnSave = returnFalse;
        }

        int countDirect() {
            return committedTransactions.size();
        }

        @Override
        public boolean save(Transaction tx, Connection conn) throws SQLException {
            if (returnFalseOnSave) {
                return false;
            }
            tx.setTransactionId(idSeq.getAndIncrement());
            stagedTransactions.add(0, tx);
            return true;
        }

        @Override
        public boolean save(Transaction tx) throws SQLException {
            tx.setTransactionId(idSeq.getAndIncrement());
            committedTransactions.add(0, tx);
            return true;
        }

        @Override
        public List<Transaction> findByAccountNumber(String accountNumber) {
            List<Transaction> list = new ArrayList<>();
            List<Transaction> all = new ArrayList<>(committedTransactions);
            for (Transaction tx : all) {
                if (accountNumber.equalsIgnoreCase(tx.getSourceAccount()) ||
                    accountNumber.equalsIgnoreCase(tx.getDestinationAccount())) {
                    list.add(tx);
                }
            }
            return list;
        }

        @Override
        public List<Transaction> findAll() {
            return new ArrayList<>(committedTransactions);
        }

        @Override
        public int count() {
            return committedTransactions.size();
        }
    }
}
