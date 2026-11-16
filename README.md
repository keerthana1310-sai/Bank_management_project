# 🏦 Bank Management System

A complete, modern, and modular Enterprise Banking Management System desktop application built with **Java 21**, **JavaFX**, **Maven**, **JDBC**, and **MySQL**.

Developed as a college collaborative project with clean architectural tiering and service contracts.

---

## 👥 Project Team & Responsibilities

| Team Member | Role | Assigned Modules |
| :--- | :--- | :--- |
| **Keerthana** | **Frontend Lead & UI/UX** | JavaFX FXML screens, CSS design system, Controllers, Navigation, Form validation, and Client-side architecture |
| **Pranita** | **Backend Developer** | Account management business logic, account creation, unique account numbering, and `AccountService` |
| **Jayasree** | **Backend Developer** | Deposit, withdrawal, atomic funds transfer, transaction ledger, and `TransactionService` |
| **Yashaswi** | **Database & JDBC Lead** | MySQL schema, JDBC connection pooling, DAO implementations, and atomic transaction handling |

---

## 🛠️ Technology Stack

- **Language:** Java 21 LTS
- **UI Framework:** JavaFX 21 (FXML + Custom CSS)
- **Build & Dependency Tool:** Apache Maven
- **Database Engine:** MySQL Server 8.0+
- **Persistence:** JDBC (Java Database Connectivity) with `PreparedStatement`
- **Architecture:** Model-View-Controller (MVC) + Service Layer + DAO Pattern

---

## 🎨 UI & Design Features

- **Apex Trust Bank Enterprise Identity:** Styled with custom CSS tokens (`styles.css`), featuring Deep Midnight Navy (`#10243A`), Royal Blue (`#2563EB`), Emerald Green (`#16A34A`), Crisp White Cards (`#FFFFFF`), Indian Rupee (`₹`) currency formatting, and standard `DD MMM YYYY` dates.
- **Responsive Layout:** Adaptive desktop layouts utilizing JavaFX `BorderPane`, `GridPane`, and `ScrollPane`.
- **Branded Authentication Portal:** Login screen with staff authentication, demo credential shortcuts, and input validation.
- **Executive Operational Dashboard:** 4 KPI cards (Total Balance, Accounts, Transactions, Active Accounts) with comparative trend tags, 4 Native JavaFX Charts (Monthly Deposit vs Withdrawal, Transaction Volume Trend, Account Type Distribution, Account Status Distribution), and recent activity table.
- **Customer 360 Account Center:** Customer onboarding with email/phone regex checking, balance validation, real-time account telemetry inspector, customer directory, and dynamic **Recent Transactions** table per selected account.
- **Teller Operations Terminal:** Dynamic processing for Deposits, Withdrawals, and Wire Transfers with pre-submission confirmation cards, live balance previews, and generated digital receipts.
- **Dedicated Fund Transfer & Remittance Center:** Saved beneficiary directory chips (Ananya Reddy, Priya Nair, Arjun Mehta, Karthik Iyer), instant pre-submission confirmation, and digital remittance receipts.
- **Immutable Transaction Ledger & Audit Trail:** Comprehensive audit history with multi-criteria filtering by account, customer name, transaction type, and status, with one-click **CSV Ledger Export**.
- **Reports & Financial Statements:** Statement generation for customer accounts, monthly cash flow summaries, deposit/withdrawal summaries, and one-click **CSV Report Export**.
- **Notification & Operations Clearance Center:** Real-time stream of transaction clearances, account dormancy warnings, and compliance alerts with category filtering.
- **Security Overview & Audit Telemetry:** Operator identity verification, cryptographic session monitoring, real-time balance integrity audit verification, and immutable security audit log export.

---

## ⚠️ Current Status: Demo Mode vs Production Mode

> [!IMPORTANT]
> **DEMO MODE IS CURRENTLY ACTIVE BY DEFAULT:**
> To enable Keerthana to demonstrate the complete, functional JavaFX frontend before MySQL is configured locally by Yashaswi, the system includes thread-safe in-memory mock services (`DemoAccountService`, `DemoTransactionService`, `DemoAuthenticationService`).
> 
> * **In Demo Mode:** All account creation, deposits, withdrawals, and transfers work dynamically in memory. **These records are NOT persisted to MySQL.**
> * **In Live Production Mode:** Set `DEMO_MODE=false` in `.env` once MySQL is installed and running. See [docs/INTEGRATION_GUIDE.md](docs/INTEGRATION_GUIDE.md) for step-by-step instructions.

---

## 🚀 Getting Started & Run Instructions

### 1. Prerequisites
- **Java 21 JDK** installed and added to `PATH` (Verify via `java -version`).
- **Apache Maven 3.8+** installed (Verify via `mvn -v`).

### 2. Quick Run (One-Click / Command Line)

#### Option A: Using Maven Directly
```bash
# Clone or navigate to the project directory
cd java_project

# Compile source files
mvn clean compile

# Launch the JavaFX application
mvn javafx:run
```

#### Option B: Using the Windows Batch Launcher
Double-click `run.bat` or run in terminal:
```cmd
.\run.bat
```

### 3. Demo Credentials (For College Project Demonstration)
| Username | Password | Role | Description |
| :--- | :--- | :--- | :--- |
| `admin` | `admin123` | Administrator | System Administrator access |
| `priya` / `keerthana` | `bank2026` | Branch Officer | Lead Branch Officer session |
| `teller` | `teller123` | Teller | Front Desk Teller |

*(You can also simply click the "Quick Demo Credentials" chips on the Login screen!)*

---

## 📁 Repository Structure

```
java_project/
├── pom.xml                               # Maven project configuration
├── run.bat                               # Windows launcher script
├── .gitignore                            # Standard Java/Maven gitignore
├── .env.example                          # Environment variable template
├── README.md                             # Project overview & documentation
│
├── database/
│   └── schema.sql                        # MySQL schema & seed data
│
├── docs/
│   ├── TEAM_TASKS.md                     # Detailed team milestone breakdown
│   └── INTEGRATION_GUIDE.md              # Backend & DB integration manual
│
└── src/
    └── main/
        ├── java/com/bank/
        │   ├── Main.java                 # JavaFX main application launcher
        │   │
        │   ├── controller/               # UI Controllers (Keerthana)
        │   │   ├── LoginController.java
        │   │   ├── DashboardController.java
        │   │   ├── AccountController.java
        │   │   ├── TransactionController.java
        │   │   ├── TransferController.java
        │   │   ├── HistoryController.java
        │   │   ├── ReportsController.java
        │   │   ├── NotificationController.java
        │   │   └── SecurityController.java
        │   │
        │   ├── model/                    # Data transfer & domain models
        │   │   ├── Account.java
        │   │   ├── Customer.java
        │   │   └── Transaction.java
        │   │
        │   ├── service/                  # Business logic & Service contracts
        │   │   ├── AccountService.java           # Contract for Pranita
        │   │   ├── TransactionService.java       # Contract for Jayasree
        │   │   ├── AuthenticationService.java
        │   │   ├── BankingException.java
        │   │   ├── ServiceFactory.java           # Dependency Injection locator
        │   │   ├── DemoAccountService.java       # In-memory mock
        │   │   ├── DemoTransactionService.java   # In-memory mock
        │   │   └── DemoAuthenticationService.java# In-memory mock
        │   │
        │   └── dao/                      # Data Access Objects (Yashaswi)
        │       ├── DatabaseConnection.java
        │       ├── AccountDAO.java
        │       ├── TransactionDAO.java
        │       └── CustomerDAO.java
        │
        └── resources/com/bank/           # FXML & Stylesheets (Keerthana)
            ├── login.fxml
            ├── dashboard.fxml
            ├── account.fxml
            ├── transaction.fxml
            ├── transfer.fxml
            ├── history.fxml
            ├── reports.fxml
            ├── notifications.fxml
            ├── security.fxml
            └── styles.css
```

---

## 📜 College Project Evaluation Notes

1. **Clean Code & Separation of Concerns:** UI controllers perform strictly presentation and validation logic. All state mutations and balance calculations are delegated to service interfaces.
2. **ACID Transaction Ready:** The `TransactionDAO` and `AccountDAO` support passing an external `Connection` parameter so that account transfers execute atomically within a single `conn.setAutoCommit(false)` transaction block.
3. **High Security Standards:** Database passwords are read via environment variables (`.env`) rather than being hardcoded into source control. SQL queries utilize parameterized `PreparedStatement`s to prevent SQL injection.
