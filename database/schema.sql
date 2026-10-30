-- ===================================================================
-- Bank Management System Database Schema
-- Database: MySQL 8.0+
-- Assigned to: Yashaswi (Database & JDBC)
-- Collaborators: Pranita (Accounts), Jayasree (Transactions), Keerthana (Frontend)
-- ===================================================================

CREATE DATABASE IF NOT EXISTS bank_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE bank_db;

-- -------------------------------------------------------------------
-- 1. Table: customers
-- Stores legal profile information for bank customers
-- -------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS customers (
    customer_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(120) NOT NULL UNIQUE,
    phone VARCHAR(20) NOT NULL,
    address TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- -------------------------------------------------------------------
-- 2. Table: accounts
-- Stores bank account details. Uses DECIMAL(15,2) for exact monetary values.
-- -------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS accounts (
    account_number VARCHAR(30) PRIMARY KEY,
    customer_id BIGINT,
    customer_name VARCHAR(100) NOT NULL,
    email VARCHAR(120) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    account_type ENUM('SAVINGS', 'CURRENT') NOT NULL DEFAULT 'SAVINGS',
    balance DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    status ENUM('ACTIVE', 'INACTIVE', 'CLOSED') NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_accounts_customer FOREIGN KEY (customer_id) 
        REFERENCES customers(customer_id) ON DELETE SET NULL,
    CONSTRAINT chk_positive_balance CHECK (balance >= 0.00)
) ENGINE=InnoDB;

-- -------------------------------------------------------------------
-- 3. Table: transactions
-- Records all deposits, withdrawals, and transfers.
-- Foreign keys link to source and destination accounts.
-- -------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS transactions (
    transaction_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    transaction_type ENUM('DEPOSIT', 'WITHDRAWAL', 'TRANSFER') NOT NULL,
    source_account VARCHAR(30) NOT NULL,
    destination_account VARCHAR(30) NULL,
    amount DECIMAL(15, 2) NOT NULL,
    balance_after DECIMAL(15, 2) NOT NULL,
    description VARCHAR(255) DEFAULT '',
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_tx_source FOREIGN KEY (source_account) 
        REFERENCES accounts(account_number) ON DELETE RESTRICT,
    CONSTRAINT fk_tx_dest FOREIGN KEY (destination_account) 
        REFERENCES accounts(account_number) ON DELETE RESTRICT,
    CONSTRAINT chk_positive_amount CHECK (amount > 0.00)
) ENGINE=InnoDB;

-- -------------------------------------------------------------------
-- 4. Table: users
-- Stores staff/teller authentication credentials.
-- NOTE: In production, password_hash should store BCrypt or Argon2 hashes.
-- -------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    role ENUM('ADMIN', 'TELLER', 'MANAGER') NOT NULL DEFAULT 'TELLER',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- -------------------------------------------------------------------
-- SEED DATA (For local development and testing)
-- -------------------------------------------------------------------
INSERT IGNORE INTO customers (customer_id, name, email, phone, address) VALUES
(1, 'Keerthana Rao', 'keerthana@bank.com', '9876543210', '124 College Boulevard, Suite 4A'),
(2, 'Pranita Sharma', 'pranita@bank.com', '9876543211', '88 University Avenue'),
(3, 'Jayasree Patel', 'jayasree@bank.com', '9876543212', '45 Tech Park Road'),
(4, 'Yashaswi Reddy', 'yashaswi@bank.com', '9876543213', '10 Innovation Drive');

INSERT IGNORE INTO accounts (account_number, customer_id, customer_name, email, phone, account_type, balance, status, created_at) VALUES
('ACC-1001', 1, 'Keerthana Rao', 'keerthana@bank.com', '9876543210', 'SAVINGS', 12500.00, 'ACTIVE', '2026-09-01 10:00:00'),
('ACC-1002', 2, 'Pranita Sharma', 'pranita@bank.com', '9876543211', 'CURRENT', 45000.00, 'ACTIVE', '2026-09-15 11:30:00'),
('ACC-1003', 3, 'Jayasree Patel', 'jayasree@bank.com', '9876543212', 'SAVINGS', 8200.50, 'ACTIVE', '2026-10-01 14:15:00'),
('ACC-1004', 4, 'Yashaswi Reddy', 'yashaswi@bank.com', '9876543213', 'SAVINGS', 19400.75, 'ACTIVE', '2026-10-10 16:45:00');

INSERT IGNORE INTO transactions (transaction_id, transaction_type, source_account, destination_account, amount, balance_after, description, timestamp) VALUES
(5001, 'DEPOSIT', 'ACC-1001', NULL, 5000.00, 12500.00, 'Initial Deposit', '2026-09-01 10:05:00'),
(5002, 'DEPOSIT', 'ACC-1002', NULL, 45000.00, 45000.00, 'Corporate Opening Balance', '2026-09-15 11:35:00'),
(5003, 'TRANSFER', 'ACC-1001', 'ACC-1003', 500.00, 12000.00, 'College Project Team Fund', '2026-10-05 09:30:00'),
(5004, 'WITHDRAWAL', 'ACC-1004', NULL, 600.00, 19400.75, 'ATM Cash Withdrawal', '2026-10-15 17:20:00');

INSERT IGNORE INTO users (username, password_hash, full_name, role) VALUES
('admin', 'admin123', 'System Administrator', 'ADMIN'),
('keerthana', 'bank2026', 'Keerthana Rao (Lead)', 'TELLER'),
('pranita', 'pranita123', 'Pranita Sharma', 'TELLER'),
('jayasree', 'jayasree123', 'Jayasree Patel', 'TELLER'),
('yashaswi', 'yashaswi123', 'Yashaswi Reddy', 'TELLER');
