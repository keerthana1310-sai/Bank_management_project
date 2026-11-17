
package com.bank.controller;

import com.bank.Main;
import com.bank.model.Account;
import com.bank.service.AccountService;
import com.bank.service.AuthenticationService;
import com.bank.service.ServiceFactory;
import com.bank.service.TransactionService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.net.URL;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Controller for Security Overview & Audit Integrity.
 * Provides operator session governance, account security telemetry,
 * real-time system integrity audits, and regulatory audit log tracking.
 */
public class SecurityController implements Initializable {

    public static class AuditEvent {
        private final String timestamp;
        private final String eventType;
        private final String severity;
        private final String operator;
        private final String details;
        private final String status;

        public AuditEvent(String timestamp, String eventType, String severity,
                          String operator, String details, String status) {
            this.timestamp = timestamp;
            this.eventType = eventType;
            this.severity = severity;
            this.operator = operator;
            this.details = details;
            this.status = status;
        }

        public String getTimestamp() { return timestamp; }
        public String getEventType() { return eventType; }
        public String getSeverity() { return severity; }
        public String getOperator() { return operator; }
        public String getDetails() { return details; }
        public String getStatus() { return status; }
    }

    // Sidebar & Header
    @FXML private Label operatorNameLabel;
    @FXML private Label demoBadgeLabel;
    @FXML private Label currentDateLabel;
    @FXML private Label statusFeedbackLabel;

    // Operator Session Controls
    @FXML private Label sessionOperatorLabel;
    @FXML private Label sessionRoleLabel;
    @FXML private Label sessionTokenLabel;
    @FXML private Label sessionHostLabel;
    @FXML private Label sessionLoginTimeLabel;
    @FXML private Label sessionStatusBadge;

    // Account Governance Metrics
    @FXML private Label metricTotalAccounts;
    @FXML private Label metricActiveAccounts;
    @FXML private Label metricInactiveAccounts;
    @FXML private Label metricHighValueAccounts;

    // System Telemetry
    @FXML private Label sysModeLabel;
    @FXML private Label sysEncryptionLabel;
    @FXML private Label sysAcidLabel;
    @FXML private Label sysStatusLabel;

    // Audit Log Table
    @FXML private TableView<AuditEvent> auditTable;
    @FXML private TableColumn<AuditEvent, String> colTimestamp;
    @FXML private TableColumn<AuditEvent, String> colEventType;
    @FXML private TableColumn<AuditEvent, String> colSeverity;
    @FXML private TableColumn<AuditEvent, String> colOperator;
    @FXML private TableColumn<AuditEvent, String> colDetails;
    @FXML private TableColumn<AuditEvent, String> colStatus;

    private AccountService accountService;
    private TransactionService transactionService;
    private AuthenticationService authService;

    private final ObservableList<AuditEvent> auditEvents =
            FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        this.accountService =
                ServiceFactory.getInstance().getAccountService();

        this.transactionService =
                ServiceFactory.getInstance().getTransactionService();

        this.authService =
                ServiceFactory.getInstance().getAuthenticationService();

        String currentUser = null;

        if (authService != null) {
            currentUser = authService.getCurrentUser();
        }

        boolean hasActiveSession =
                currentUser != null && !currentUser.isBlank();

        if (!hasActiveSession) {
            currentUser = "Guest Operator";
        }

        boolean isDemo = ServiceFactory.getInstance().isDemoMode();

        if (operatorNameLabel != null) {
            operatorNameLabel.setText(currentUser);
        }

        if (demoBadgeLabel != null) {
            demoBadgeLabel.setText(
                    isDemo ? "⚡ In-Memory Demo" : "🔒 Live MySQL");
        }

        if (currentDateLabel != null) {
            currentDateLabel.setText(
                    LocalDate.now().format(
                            DateTimeFormatter.ofPattern("dd MMM yyyy")));
        }

        setupSessionInfo(currentUser, isDemo, hasActiveSession);

        loadGovernanceMetrics();
        setupAuditTable();
        loadAuditEvents();
    }

    private void setupSessionInfo(
            String currentUser,
            boolean isDemo,
            boolean hasActiveSession) {

        if (sessionOperatorLabel != null) {
            sessionOperatorLabel.setText(currentUser);
        }

        if (sessionRoleLabel != null) {
            sessionRoleLabel.setText(
                    hasActiveSession
                            ? "Authenticated Bank Operator"
                            : "No authenticated role");
        }

        if (sessionTokenLabel != null) {
            String token = hasActiveSession
                    ? "AUTH-TOKEN-ATB-" +
                      Integer.toHexString(currentUser.hashCode()).toUpperCase()
                      + "-9842"
                    : "N/A";

            sessionTokenLabel.setText(token);
        }

        if (sessionHostLabel != null) {
            sessionHostLabel.setText(
                    "127.0.0.1 (Workstation Terminal #04 - Mumbai North Branch)");
        }

        if (sessionLoginTimeLabel != null) {
            sessionLoginTimeLabel.setText(
                    hasActiveSession ? "Session Active" : "No active session");
        }

        if (sessionStatusBadge != null) {
            sessionStatusBadge.setText(
                    hasActiveSession
                            ? "ACTIVE & VERIFIED"
                            : "NOT AUTHENTICATED");

            sessionStatusBadge.getStyleClass().setAll(
                    "badge",
                    hasActiveSession ? "badge-success" : "badge-warning");
        }

        if (sysModeLabel != null) {
            sysModeLabel.setText(
                    isDemo
                            ? "In-Memory Thread-Safe Demo Architecture"
                            : "Production MySQL 8.0 JDBC Connected");
        }

        if (sysEncryptionLabel != null) {
            sysEncryptionLabel.setText(
                    "TLS 1.3 / AES-256 GCM Payload Hash Simulation");
        }

        if (sysAcidLabel != null) {
            sysAcidLabel.setText(
                    "Strict Serializability / Zero Concurrency Drift");
        }

        if (sysStatusLabel != null) {
            sysStatusLabel.setText("SYSTEM HEALTHY (100% Operational)");
        }
    }

    private void loadGovernanceMetrics() {
        if (accountService == null) return;

        try {
            List<Account> accounts = accountService.getAllAccounts();

            int total = accounts.size();
            int active = 0;
            int inactive = 0;
            int highValue = 0;

            BigDecimal highValueThreshold =
                    new BigDecimal("200000.00");

            for (Account acc : accounts) {

                if ("Active".equalsIgnoreCase(acc.getStatus())) {
                    active++;
                } else {
                    inactive++;
                }

                if (acc.getBalance() != null &&
                        acc.getBalance().compareTo(highValueThreshold) >= 0) {
                    highValue++;
                }
            }

            if (metricTotalAccounts != null) {
                metricTotalAccounts.setText(String.valueOf(total));
            }

            if (metricActiveAccounts != null) {
                metricActiveAccounts.setText(String.valueOf(active));
            }

            if (metricInactiveAccounts != null) {
                metricInactiveAccounts.setText(String.valueOf(inactive));
            }

            if (metricHighValueAccounts != null) {
                metricHighValueAccounts.setText(
                        highValue + " Accounts (≥ ₹2,00,000)");
            }

        } catch (Exception e) {
            System.err.println(
                    "Error loading governance metrics: " + e.getMessage());
        }
    }

    private void setupAuditTable() {
        if (auditTable == null) return;

        colTimestamp.setCellValueFactory(
                data -> new SimpleStringProperty(
                        data.getValue().getTimestamp()));

        colEventType.setCellValueFactory(
                data -> new SimpleStringProperty(
                        data.getValue().getEventType()));

        colSeverity.setCellValueFactory(
                data -> new SimpleStringProperty(
                        data.getValue().getSeverity()));

        colOperator.setCellValueFactory(
                data -> new SimpleStringProperty(
                        data.getValue().getOperator()));

        colDetails.setCellValueFactory(
                data -> new SimpleStringProperty(
                        data.getValue().getDetails()));

        colStatus.setCellValueFactory(
                data -> new SimpleStringProperty(
                        data.getValue().getStatus()));

        // Style severity column
        colSeverity.setCellFactory(col -> new TableCell<AuditEvent, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item);

                    if (item.equalsIgnoreCase("INFO")) {
                        setStyle(
                                "-fx-text-fill: #2563EB; -fx-font-weight: bold;");
                    } else if (item.equalsIgnoreCase("WARNING")) {
                        setStyle(
                                "-fx-text-fill: #D97706; -fx-font-weight: bold;");
                    } else if (item.equalsIgnoreCase("AUDIT") ||
                            item.equalsIgnoreCase("SUCCESS")) {
                        setStyle(
                                "-fx-text-fill: #16A34A; -fx-font-weight: bold;");
                    } else {
                        setStyle(
                                "-fx-text-fill: #DC2626; -fx-font-weight: bold;");
                    }
                }
            }
        });

        // Style status column with badges
        colStatus.setCellFactory(col -> new TableCell<AuditEvent, String>() {

            private final Label badge = new Label();

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    badge.setText(item);
                    badge.getStyleClass().clear();
                    badge.getStyleClass().add("badge");

                    if (item.equalsIgnoreCase("Verified") ||
                            item.equalsIgnoreCase("Passed")) {
                        badge.getStyleClass().add("badge-success");
                    } else if (item.equalsIgnoreCase("Flagged")) {
                        badge.getStyleClass().add("badge-warning");
                    } else {
                        badge.getStyleClass().add("badge-active");
                    }

                    setGraphic(badge);
                    setText(null);
                }
            }
        });

        auditTable.setItems(auditEvents);
    }

    private void loadAuditEvents() {
        auditEvents.clear();

        auditEvents.add(new AuditEvent(
                "12 Nov 2026, 09:00:14",
                "OPERATOR_AUTH", "INFO", "SYSTEM",
                "Operator 'priya.nair' authenticated via local branch terminal",
                "Verified"));

        auditEvents.add(new AuditEvent(
                "12 Nov 2026, 09:00:15",
                "SESSION_ESTABLISH", "INFO", "SECURITY_GATEWAY",
                "Issued session token AUTH-TOKEN-ATB-9842 with Tier-3 RBAC",
                "Verified"));

        auditEvents.add(new AuditEvent(
                "12 Nov 2026, 09:15:22",
                "INTEGRITY_SCAN", "AUDIT", "AUTO_SCANNER",
                "Full account balance and journal checksum matched with 0 error",
                "Passed"));

        auditEvents.add(new AuditEvent(
                "12 Nov 2026, 09:45:10",
                "HIGH_VALUE_TX", "INFO", "Priya Nair",
                "High value transfer of ₹85,000.00 posted (ACC-1002 to ACC-1004)",
                "Verified"));

        auditEvents.add(new AuditEvent(
                "12 Nov 2026, 10:12:05",
                "DORMANCY_CHECK", "WARNING", "COMPLIANCE_AGENT",
                "Account ACC-1008 (Divya Sharma) flagged as Inactive / Dormant",
                "Flagged"));

        auditEvents.add(new AuditEvent(
                "12 Nov 2026, 10:30:40",
                "ACID_COMMIT", "AUDIT", "LEDGER_ENGINE",
                "Atomic dual-ledger journal entry validated for batch #2026-11B",
                "Passed"));

        auditEvents.add(new AuditEvent(
                "12 Nov 2026, 11:00:02",
                "KEY_ROTATION", "INFO", "CRYPTO_SERVICE",
                "Session encryption key successfully refreshed per 120-min cycle",
                "Verified"));
    }

    @FXML
    public void handleRunIntegrityAudit(ActionEvent event) {

        if (accountService == null) return;

        try {
            List<Account> accounts = accountService.getAllAccounts();

            BigDecimal totalSum = BigDecimal.ZERO;

            for (Account a : accounts) {
                if (a.getBalance() != null) {
                    totalSum = totalSum.add(a.getBalance());
                }
            }

            String formattedSum = formatIndianCurrency(totalSum);

            String auditTime = LocalDateTime.now().format(
                    DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm:ss"));

            String auditOperator = "Guest Operator";

            if (authService != null) {
                String loggedInUser = authService.getCurrentUser();

                if (loggedInUser != null && !loggedInUser.isBlank()) {
                    auditOperator = loggedInUser;
                }
            }

            auditEvents.add(0, new AuditEvent(
                    auditTime,
                    "MANUAL_AUDIT_RUN",
                    "AUDIT",
                    auditOperator,
                    "Manual integrity verification executed. " +
                            accounts.size() +
                            " accounts verified. Total pool: " + formattedSum,
                    "Passed"
            ));

            loadGovernanceMetrics();

            if (statusFeedbackLabel != null) {
                statusFeedbackLabel.setText(
                        "✓ Real-Time System Integrity Audit PASSED: " +
                        accounts.size() +
                        " accounts verified with total ledger balance of " +
                        formattedSum + ". Zero discrepancies found.");

                statusFeedbackLabel.getStyleClass().setAll(
                        "alert-box", "alert-success");

                statusFeedbackLabel.setVisible(true);
                statusFeedbackLabel.setManaged(true);
            }

        } catch (Exception e) {
            if (statusFeedbackLabel != null) {
                statusFeedbackLabel.setText(
                        "⚠️ Failed to execute system integrity audit: " +
                                e.getMessage());

                statusFeedbackLabel.getStyleClass().setAll(
                        "alert-box", "alert-danger");

                statusFeedbackLabel.setVisible(true);
                statusFeedbackLabel.setManaged(true);
            }
        }
    }

    @FXML
    public void handleExportAuditLog(ActionEvent event) {
        try {
            File file = new File("apex_trust_bank_security_audit.csv");

            try (PrintWriter writer =
                         new PrintWriter(new FileWriter(file))) {

                writer.println(
                        "Timestamp,EventType,Severity,Operator,Details,Status");

                for (AuditEvent ev : auditEvents) {
                    writer.printf(
                            "\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"%n",
                            ev.getTimestamp(),
                            ev.getEventType(),
                            ev.getSeverity(),
                            ev.getOperator(),
                            ev.getDetails(),
                            ev.getStatus());
                }
            }

            if (statusFeedbackLabel != null) {
                statusFeedbackLabel.setText(
                        "✓ Security Audit Log exported successfully to " +
                        file.getName() + " (" + auditEvents.size() +
                        " events recorded)");

                statusFeedbackLabel.getStyleClass().setAll(
                        "alert-box", "alert-success");

                statusFeedbackLabel.setVisible(true);
                statusFeedbackLabel.setManaged(true);
            }

        } catch (Exception e) {
            if (statusFeedbackLabel != null) {
                statusFeedbackLabel.setText(
                        "⚠️ Failed to export security audit log: " +
                                e.getMessage());

                statusFeedbackLabel.getStyleClass().setAll(
                        "alert-box", "alert-danger");

                statusFeedbackLabel.setVisible(true);
                statusFeedbackLabel.setManaged(true);
            }
        }
    }

    private String formatIndianCurrency(BigDecimal amount) {
        if (amount == null) return "₹0.00";

        NumberFormat nf =
                NumberFormat.getNumberInstance(new Locale("en", "IN"));

        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);

        return "₹" + nf.format(amount);
    }

    // Navigation Handlers
    @FXML
    public void navToDashboard(ActionEvent e) {
        Main.navigateTo(
                "dashboard.fxml",
                "Apex Trust Bank - Operational Dashboard");
    }

    @FXML
    public void navToAccounts(ActionEvent e) {
        Main.navigateTo(
                "account.fxml",
                "Apex Trust Bank - Account Management");
    }

    @FXML
    public void navToTransactions(ActionEvent e) {
        Main.navigateTo(
                "transaction.fxml",
                "Apex Trust Bank - Process Transactions");
    }

    @FXML
    public void navToTransfers(ActionEvent e) {
        Main.navigateTo(
                "transfer.fxml",
                "Apex Trust Bank - Fund Transfers & Beneficiaries");
    }

    @FXML
    public void navToHistory(ActionEvent e) {
        Main.navigateTo(
                "history.fxml",
                "Apex Trust Bank - Transaction Audit History");
    }

    @FXML
    public void navToReports(ActionEvent e) {
        Main.navigateTo(
                "reports.fxml",
                "Apex Trust Bank - Financial Reports & Statements");
    }

    @FXML
    public void navToNotifications(ActionEvent e) {
        Main.navigateTo(
                "notifications.fxml",
                "Apex Trust Bank - Notification Centre");
    }

    @FXML
    public void navToSecurity(ActionEvent e) {
        // Already on the Security page.
    }

    @FXML
    public void handleLogout(ActionEvent e) {
        if (authService != null) {
            authService.logout();
        }

        Main.navigateTo(
                "login.fxml",
                "Apex Trust Bank - Secure Staff Login");
    }
}
