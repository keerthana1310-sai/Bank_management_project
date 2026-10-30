# 🛠️ Bank Management System — Integration Guide

This guide explains how backend and database teammates (**Pranita**, **Jayasree**, and **Yashaswi**) integrate their code with the JavaFX frontend developed by **Keerthana**.

---

## 🏗️ 1. Architecture & Service Layer Contracts

The application is structured into distinct, decoupled tiers:

```
┌────────────────────────────────────────────────────────┐
│             JavaFX Frontend (Keerthana)               │
│   Controllers: Login, Dashboard, Account, Tx, History  │
└───────────────────────────┬────────────────────────────┘
                            │
                            ▼
┌────────────────────────────────────────────────────────┐
│             Service Contracts (Interfaces)             │
│   • AccountService (Pranita)                           │
│   • TransactionService (Jayasree)                      │
│   • AuthenticationService                              │
└─────────────┬────────────────────────────┬─────────────┘
              │                            │
   (Demo Mode Active)             (Live Production Mode)
              ▼                            ▼
┌───────────────────────────┐ ┌───────────────────────────┐
│     In-Memory Demo        │ │  JDBC Service Impl        │
│ • DemoAccountService      │ │ • JdbcAccountService      │
│ • DemoTransactionService  │ │ • JdbcTransactionService  │
└───────────────────────────┘ └─────────────┬─────────────┘
                                            │
                                            ▼
                              ┌───────────────────────────┐
                              │  DAO & MySQL (Yashaswi)   │
                              │ • AccountDAO              │
                              │ • TransactionDAO          │
                              │ • CustomerDAO             │
                              │ • DatabaseConnection      │
                              └───────────────────────────┘
```

---

## 🔒 2. Service Contracts That MUST Remain Consistent

The JavaFX controllers call the backend through standard interfaces. **Do not change the method names, parameter types, or return types** of these methods, as the UI depends directly on them:

### A. `AccountService` (Pranita)
```java
Account createAccount(String customerName, String email, String phone, 
                      String accountType, BigDecimal initialDeposit) throws BankingException;

Optional<Account> findAccount(String accountNumber) throws BankingException;

List<Account> getAllAccounts() throws BankingException;

Account updateAccount(Account account) throws BankingException;

int getTotalAccountsCount() throws BankingException;

BigDecimal getTotalBalanceSum() throws BankingException;
```

### B. `TransactionService` (Jayasree)
```java
Transaction deposit(String accountNumber, BigDecimal amount, String description) throws BankingException;

Transaction withdraw(String accountNumber, BigDecimal amount, String description) throws BankingException;

Transaction transfer(String sourceAccount, String destinationAccount, 
                     BigDecimal amount, String description) throws BankingException;

List<Transaction> getTransactionHistory(String accountNumber) throws BankingException;

List<Transaction> getAllTransactions() throws BankingException;

int getTotalTransactionsCount() throws BankingException;
```

---

## 🔄 3. How to Replace Demo Services with Real JDBC Implementations

In [`src/main/java/com/bank/service/ServiceFactory.java`](../src/main/java/com/bank/service/ServiceFactory.java):

1. Set `DEMO_MODE=false` in your `.env` file (or set JVM flag `-Ddemo.mode=false`).
2. When `DEMO_MODE=false`, `ServiceFactory` instantiates your JDBC implementations:
   ```java
   AccountDAO accountDAO = new AccountDAO();
   CustomerDAO customerDAO = new CustomerDAO();
   TransactionDAO transactionDAO = new TransactionDAO();

   AccountService accSvc = new JdbcAccountService(accountDAO, customerDAO);
   TransactionService txSvc = new JdbcTransactionService(transactionDAO, accountDAO);
   ```

---

## 🗄️ 4. How to Configure MySQL Locally (Yashaswi)

### Step 1: Install & Start MySQL Server
Ensure MySQL Server 8.0+ is installed and running on port `3306`.

### Step 2: Initialize Database and Schema
Open MySQL Workbench, MySQL Shell, or command line:
```bash
mysql -u root -p < database/schema.sql
```
This automatically creates:
- The `bank_db` schema.
- The `customers`, `accounts`, `transactions`, and `users` tables.
- Foreign keys and `DECIMAL(15,2)` constraints.
- Seed data for accounts (`ACC-1001` through `ACC-1004`).

### Step 3: Configure Environment Credentials
Copy `.env.example` to `.env`:
```properties
DB_HOST=localhost
DB_PORT=3306
DB_NAME=bank_db
DB_USER=root
DB_PASSWORD=your_actual_mysql_password
DEMO_MODE=false
```

---

## ⚡ 5. Implementing Atomic Transfers (Jayasree's Priority)

Transfers between two bank accounts must be **atomic** (ACID compliant). If updating the destination account or writing the transaction log fails, the debit from the source account must be rolled back immediately.

```java
public Transaction transfer(String sourceAcc, String destAcc, BigDecimal amount, String memo) throws BankingException {
    Connection conn = null;
    try {
        conn = DatabaseConnection.getConnection();
        conn.setAutoCommit(false); // Begin SQL Transaction

        // 1. Fetch and validate source
        // 2. Fetch and validate destination
        // 3. Compute balances
        accountDAO.updateBalance(sourceAcc, newSourceBal, conn);
        accountDAO.updateBalance(destAcc, newDestBal, conn);

        Transaction tx = new Transaction(...);
        transactionDAO.save(tx, conn);

        conn.commit(); // Commit transaction atomically
        return tx;
    } catch (Exception e) {
        if (conn != null) {
            try { conn.rollback(); } catch (SQLException ex) { /* log */ }
        }
        throw new BankingException("Transfer failed: " + e.getMessage(), e);
    } finally {
        if (conn != null) {
            try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ex) { /* log */ }
        }
    }
}
```

---

## 🚀 6. How to Build & Run the Application

### Prerequisites:
- Java JDK 21+ (`java -version`)
- Apache Maven 3.8+ (`mvn -v`)

### Compile the project:
```bash
mvn clean compile
```

### Run the JavaFX Application:
```bash
mvn javafx:run
```
*(Or run `run.bat` on Windows)*

---

## 🧪 7. Complete System Verification Checklist

Before presenting the college project:

- [ ] **Login Screen:** Login with `admin` / `admin123` or `keerthana` / `bank2026`.
- [ ] **Dashboard:** Verify Total Accounts displays `4` initially and Total Deposits reflects correct sums.
- [ ] **Account Onboarding:** Create account `ACC-1005` for a new customer. Verify table displays it immediately.
- [ ] **Deposit:** Deposit `$500.00` into `ACC-1001`. Verify updated balance is `$13,000.00` and receipt appears.
- [ ] **Withdrawal:** Attempt to withdraw `$999,999.00` from `ACC-1001` to test the "Insufficient funds" error message.
- [ ] **Transfer:** Transfer `$1,000.00` from `ACC-1001` to `ACC-1002`. Verify source is debited and destination is credited.
- [ ] **Audit History:** Navigate to Transaction History. Filter by "TRANSFER". Verify all transfer records are displayed.
- [ ] **Logout:** Verify logout cleanly brings the user back to the branded login screen.
