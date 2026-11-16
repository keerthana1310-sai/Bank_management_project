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
(1, 'Aarav Sharma', 'aarav.sharma@apexbank.in', '9876543210', '42 MG Road, Bengaluru, Karnataka'),
(2, 'Ananya Reddy', 'ananya.reddy@apexbank.in', '9876543211', '15 Jubilee Hills, Hyderabad, Telangana'),
(3, 'Rahul Varma', 'rahul.varma@apexbank.in', '9876543212', '88 Bandra West, Mumbai, Maharashtra'),
(4, 'Priya Nair', 'priya.nair@apexbank.in', '9876543213', '24 Marine Drive, Kochi, Kerala'),
(5, 'Arjun Mehta', 'arjun.mehta@apexbank.in', '9876543214', '7 Park Street, Kolkata, West Bengal'),
(6, 'Sneha Rao', 'sneha.rao@apexbank.in', '9876543215', '120 Anna Nagar, Chennai, Tamil Nadu'),
(7, 'Karthik Iyer', 'karthik.iyer@apexbank.in', '9876543216', '33 Koregaon Park, Pune, Maharashtra'),
(8, 'Divya Sharma', 'divya.sharma@apexbank.in', '9876543217', '55 Civil Lines, Jaipur, Rajasthan');

INSERT IGNORE INTO accounts (account_number, customer_id, customer_name, email, phone, account_type, balance, status, created_at) VALUES
('ACC-1001', 1, 'Aarav Sharma', 'aarav.sharma@apexbank.in', '9876543210', 'SAVINGS', 85500.00, 'ACTIVE', '2026-08-15 10:30:00'),
('ACC-1002', 2, 'Ananya Reddy', 'ananya.reddy@apexbank.in', '9876543211', 'CURRENT', 245000.00, 'ACTIVE', '2026-08-28 11:15:00'),
('ACC-1003', 3, 'Rahul Varma', 'rahul.varma@apexbank.in', '9876543212', 'SAVINGS', 62300.50, 'ACTIVE', '2026-09-05 09:45:00'),
('ACC-1004', 4, 'Priya Nair', 'priya.nair@apexbank.in', '9876543213', 'SAVINGS', 115400.75, 'ACTIVE', '2026-09-18 14:20:00'),
('ACC-1005', 5, 'Arjun Mehta', 'arjun.mehta@apexbank.in', '9876543214', 'CURRENT', 310000.00, 'ACTIVE', '2026-10-02 12:00:00'),
('ACC-1006', 6, 'Sneha Rao', 'sneha.rao@apexbank.in', '9876543215', 'SAVINGS', 28500.00, 'INACTIVE', '2026-10-15 16:30:00'),
('ACC-1007', 7, 'Karthik Iyer', 'karthik.iyer@apexbank.in', '9876543216', 'SAVINGS', 74800.00, 'ACTIVE', '2026-10-25 10:00:00'),
('ACC-1008', 8, 'Divya Sharma', 'divya.sharma@apexbank.in', '9876543217', 'CURRENT', 180000.00, 'ACTIVE', '2026-11-02 15:10:00');

INSERT IGNORE INTO transactions (transaction_id, transaction_type, source_account, destination_account, amount, balance_after, description, timestamp) VALUES
(5001, 'DEPOSIT', 'ACC-1001', NULL, 50000.00, 50000.00, 'Initial Account Opening Deposit', '2026-08-15 10:35:00'),
(5002, 'DEPOSIT', 'ACC-1002', NULL, 200000.00, 270000.00, 'Working Capital Deposit', '2026-08-28 11:20:00'),
(5003, 'DEPOSIT', 'ACC-1003', NULL, 40000.00, 72300.50, 'Salary Direct Credit', '2026-09-05 10:00:00'),
(5004, 'TRANSFER', 'ACC-1001', 'ACC-1004', 15000.00, 70500.00, 'Consulting Remittance', '2026-09-20 14:30:00'),
(5005, 'WITHDRAWAL', 'ACC-1002', NULL, 25000.00, 245000.00, 'Branch Cash Withdrawal', '2026-09-29 16:15:00'),
(5006, 'DEPOSIT', 'ACC-1005', NULL, 310000.00, 310000.00, 'Commercial Client Deposit', '2026-10-02 12:15:00'),
(5007, 'WITHDRAWAL', 'ACC-1003', NULL, 10000.00, 62300.50, 'ATM Cash Withdrawal', '2026-10-18 11:45:00'),
(5008, 'TRANSFER', 'ACC-1005', 'ACC-1007', 25000.00, 285000.00, 'Vendor Payment Transfer', '2026-10-28 15:30:00'),
(5009, 'DEPOSIT', 'ACC-1008', NULL, 180000.00, 180000.00, 'Business Proceeds Deposit', '2026-11-02 15:20:00'),
(5010, 'WITHDRAWAL', 'ACC-1004', NULL, 8000.00, 115400.75, 'Utility & Merchant Payment', '2026-11-06 13:10:00'),
(5011, 'DEPOSIT', 'ACC-1001', NULL, 15000.00, 85500.00, 'Cheque Clearance Deposit', '2026-11-09 10:15:00');

INSERT IGNORE INTO users (username, password_hash, full_name, role) VALUES
('admin', 'admin123', 'System Administrator', 'ADMIN'),
('priya', 'bank2026', 'Priya Nair (Branch Officer)', 'TELLER'),
('teller', 'teller123', 'Front Desk Teller', 'TELLER');
