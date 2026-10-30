# 📋 Bank Management System — Team Task Assignments

This document outlines the division of responsibilities, completed milestones, and pending tasks for our college project.

---

## 👩‍💻 1. Keerthana (Frontend Lead & UI/UX Designer)
**Module:** JavaFX Application, Views, Styling, and User Workflows  
**Status:** ✅ **COMPLETED (Phase 1 Frontend & Service Integration Contracts)**

### Completed Deliverables:
- [x] **Project Foundation:** Initialized Maven project (`pom.xml`) with Java 21, JavaFX 21, and MySQL Connector/J.
- [x] **Global Design System:** Created `styles.css` utilizing a cohesive professional navy blue (`#0f2942`), slate, white, and subtle grey color palette with rounded cards, custom buttons, badges, and responsive tables.
- [x] **Screen 1 — Login Screen (`login.fxml` & `LoginController.java`):**
  - Apex Trust Bank branding and logo.
  - Staff credentials input (username & password) with field validation.
  - Development Demo mode badge and 1-click test credentials chip (`admin` / `keerthana`).
  - Seamless navigation to Dashboard upon successful authentication.
- [x] **Screen 2 — Operations Dashboard (`dashboard.fxml` & `DashboardController.java`):**
  - Personalized operator greeting and current date display.
  - Live KPI summary cards (Total Accounts, Total Deposits, Total Transactions, Storage Mode).
  - Data loaded dynamically via service interfaces (no hardcoded fake values).
  - Quick action workflow buttons.
  - Recent transactions ledger preview table.
- [x] **Screen 3 — Account Management (`account.fxml` & `AccountController.java`):**
  - Customer onboarding form (Name, Email, Phone, Type, Initial Deposit).
  - Input validation (valid email format, phone digits, non-negative amounts).
  - Immediate visual feedback banner with generated account number and balance.
  - Real-time Account Inspector / Lookup card.
  - Full accounts directory `TableView` with dynamic text filtering and refresh capability.
- [x] **Screen 4 — Transaction Center (`transaction.fxml` & `TransactionController.java`):**
  - Processing for Deposits, Withdrawals, and Wire Transfers.
  - Dynamic UI adjustments (e.g., Destination Account field displays only for Transfers).
  - Live source account balance telemetry lookup.
  - Positive monetary value validation.
  - Digital Transaction Receipt card on completion.
  - Zero balance calculations in the controller — all delegated to backend services.
- [x] **Screen 5 — Audit History (`history.fxml` & `HistoryController.java`):**
  - Comprehensive ledger `TableView` with IDs, timestamps, types, accounts, amounts, balances, and memos.
  - Multi-parameter filtering by account number and transaction type.
  - Clean empty-state messaging when no records match.
- [x] **Demo In-Memory Mode:** Created `DemoAccountService`, `DemoTransactionService`, `DemoAuthenticationService`, and `ServiceFactory` so the application can be demonstrated immediately before MySQL is configured.

---

## 👩‍💻 2. Pranita (Backend — Account Management)
**Module:** Customer & Account Business Logic  
**Contract Interface:** [`AccountService.java`](../src/main/java/com/bank/service/AccountService.java)  
**Database Access:** [`AccountDAO.java`](../src/main/java/com/bank/dao/AccountDAO.java)  
**Status:** ⏳ **Ready for Implementation**

### Tasks & Responsibilities:
1. **Implement `JdbcAccountService`:**
   - Create class `com.bank.service.impl.JdbcAccountService` implementing `AccountService`.
   - Inject `AccountDAO` and `CustomerDAO`.
2. **Account Number Generation:**
   - Implement unique account number generation (e.g. `ACC-` prefix + sequence or timestamp hash).
   - Ensure numbers do not collide in the database.
3. **Validation & Business Rules:**
   - Enforce mandatory customer name, valid email regex, and minimum initial deposit rules (e.g. $500 minimum for CURRENT, $100 for SAVINGS).
4. **Account Search & Listing:**
   - Wire `findAccount(accountNumber)` to `accountDAO.findByAccountNumber(accountNumber)`.
   - Wire `getAllAccounts()` to `accountDAO.findAll()`.
5. **Account Persistence:**
   - Save customer profile first in `customers` table to get `customerId`, then insert into `accounts` table.

---

## 👩‍💻 3. Jayasree (Backend — Transactions)
**Module:** Deposits, Withdrawals, Transfers & Ledger  
**Contract Interface:** [`TransactionService.java`](../src/main/java/com/bank/service/TransactionService.java)  
**Database Access:** [`TransactionDAO.java`](../src/main/java/com/bank/dao/TransactionDAO.java)  
**Status:** ⏳ **Ready for Implementation**

### Tasks & Responsibilities:
1. **Implement `JdbcTransactionService`:**
   - Create class `com.bank.service.impl.JdbcTransactionService` implementing `TransactionService`.
   - Inject `TransactionDAO` and `AccountDAO`.
2. **Deposits (`deposit`):**
   - Validate `amount > 0`.
   - Verify target account exists and is `ACTIVE`.
   - Compute `newBalance = currentBalance + amount`.
   - Update account balance via `accountDAO.updateBalance(...)`.
   - Record transaction entry via `transactionDAO.save(...)`.
3. **Withdrawals (`withdraw`):**
   - Validate `amount > 0`.
   - Verify account is `ACTIVE`.
   - Check `currentBalance >= amount`. If not, throw `BankingException("Insufficient funds")`.
   - Compute `newBalance = currentBalance - amount`.
   - Update account balance and record transaction.
4. **Atomic Wire Transfers (`transfer`):**
   - Validate `source != destination`, `amount > 0`, and source has sufficient balance.
   - **CRITICAL REQUIREMENT:** Execute inside a single SQL transaction:
     ```java
     Connection conn = DatabaseConnection.getConnection();
     conn.setAutoCommit(false);
     try {
         accountDAO.updateBalance(sourceAccount, newSourceBalance, conn);
         accountDAO.updateBalance(destinationAccount, newDestBalance, conn);
         transactionDAO.save(transferTx, conn);
         conn.commit();
     } catch (Exception e) {
         conn.rollback();
         throw new BankingException("Transfer failed and was rolled back: " + e.getMessage(), e);
     } finally {
         conn.setAutoCommit(true);
         conn.close();
     }
     ```
5. **Ledger Querying:**
   - Implement `getTransactionHistory(accountNumber)` and `getAllTransactions()`.

---

## 👩‍💻 4. Yashaswi (Database & JDBC)
**Module:** MySQL Schema, Connection Management, & DAOs  
**Database Script:** [`database/schema.sql`](../database/schema.sql)  
**DAO Package:** `com.bank.dao`  
**Status:** ⏳ **Ready for Implementation & Testing**

### Tasks & Responsibilities:
1. **Schema Execution & Verification:**
   - Run `database/schema.sql` on local MySQL Server 8.0+.
   - Verify `customers`, `accounts`, `transactions`, and `users` tables are created with proper primary keys, foreign keys (`ON DELETE RESTRICT`), and constraints (`DECIMAL(15,2)`, `CHECK(balance >= 0)`).
2. **Database Connection Configuration:**
   - Review [`DatabaseConnection.java`](../src/main/java/com/bank/dao/DatabaseConnection.java).
   - Configure local MySQL credentials via `.env` file or environment variables (`DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`).
   - Run `DatabaseConnection.testConnection()` to verify connectivity.
3. **DAO Implementation:**
   - Complete any additional SQL helper queries in `AccountDAO.java`, `TransactionDAO.java`, and `CustomerDAO.java`.
   - Ensure all queries strictly use `PreparedStatement` to avoid SQL injection attacks.
4. **Staff Authentication Query:**
   - Add authentication DAO query `findUserByUsername(String username)` to validate teller logins against the `users` table.
