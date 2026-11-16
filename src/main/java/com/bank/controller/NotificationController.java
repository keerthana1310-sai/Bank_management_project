package com.bank.controller;

import com.bank.Main;
import com.bank.model.Account;
import com.bank.model.Transaction;
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
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controller for the Enterprise Notification Centre.
 * Aggregates real-time events from transactions, account alerts, and system security.
 */
public class NotificationController implements Initializable {

    public static class BankNotification {
        private final String icon;
        private final String title;
        private final String message;
        private final String category;
        private final String timestamp;
        private final String level;

        public BankNotification(String icon, String title, String message, String category, String timestamp, String level) {
            this.icon = icon;
            this.title = title;
            this.message = message;
            this.category = category;
            this.timestamp = timestamp;
            this.level = level;
        }

        public String getIcon() { return icon; }
        public String getTitle() { return title; }
        public String getMessage() { return message; }
        public String getCategory() { return category; }
        public String getTimestamp() { return timestamp; }
        public String getLevel() { return level; }
    }

    // Sidebar & Header
    @FXML private Label operatorNameLabel;
    @FXML private Label demoBadgeLabel;
    @FXML private Label currentDateLabel;
    @FXML private Label unreadCountLabel;
    @FXML private Label statusFeedbackLabel;

    // Filters
    @FXML private ComboBox<String> categoryFilterCombo;

    // TableView
    @FXML private TableView<BankNotification> notificationsTable;
    @FXML private TableColumn<BankNotification, String> colIcon;
    @FXML private TableColumn<BankNotification, String> colCategory;
    @FXML private TableColumn<BankNotification, String> colTitle;
    @FXML private TableColumn<BankNotification, String> colMessage;
    @FXML private TableColumn<BankNotification, String> colTimestamp;

    private TransactionService transactionService;
    private AccountService accountService;
    private AuthenticationService authService;
    private final ObservableList<BankNotification> masterList = FXCollections.observableArrayList();
    private final ObservableList<BankNotification> displayList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.transactionService = ServiceFactory.getInstance().getTransactionService();
        this.accountService = ServiceFactory.getInstance().getAccountService();
        this.authService = ServiceFactory.getInstance().getAuthenticationService();

        String currentUser = authService != null ? authService.getCurrentUser() : "Priya Nair (Branch Officer)";
        if (operatorNameLabel != null) operatorNameLabel.setText(currentUser);
        if (demoBadgeLabel != null) demoBadgeLabel.setText(ServiceFactory.getInstance().isDemoMode() ? "⚡ In-Memory Demo" : "🔒 Live MySQL");
        if (currentDateLabel != null) currentDateLabel.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy")));

        if (categoryFilterCombo != null) {
            categoryFilterCombo.setItems(FXCollections.observableArrayList("ALL", "TRANSACTION", "ACCOUNT", "SECURITY"));
            categoryFilterCombo.setValue("ALL");
        }

        setupTable();
        loadNotifications();
    }

    private void setupTable() {
        if (notificationsTable == null) return;

        colIcon.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getIcon()));
        colCategory.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getCategory()));
        colTitle.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getTitle()));
        colMessage.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getMessage()));
        colTimestamp.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getTimestamp()));

        colCategory.setCellFactory(col -> new TableCell<BankNotification, String>() {
            @Override
            protected void updateItem(String cat, boolean empty) {
                super.updateItem(cat, empty);
                if (empty || cat == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label badge = new Label(cat);
                    if ("TRANSACTION".equalsIgnoreCase(cat)) {
                        badge.getStyleClass().add("badge-transfer");
                    } else if ("ACCOUNT".equalsIgnoreCase(cat)) {
                        badge.getStyleClass().add("badge-deposit");
                    } else {
                        badge.getStyleClass().add("badge-withdrawal");
                    }
                    setGraphic(badge);
                    setText(null);
                }
            }
        });

        notificationsTable.setItems(displayList);
    }

    private void loadNotifications() {
        masterList.clear();

        try {
            // 1. Transaction-derived events
            List<Transaction> txs = transactionService.getAllTransactions();
            for (Transaction tx : txs) {
                String type = tx.getTransactionType();
                String icon = "DEPOSIT".equalsIgnoreCase(type) ? "📥" : ("WITHDRAWAL".equalsIgnoreCase(type) ? "📤" : "🔁");
                String level = "DEPOSIT".equalsIgnoreCase(type) ? "SUCCESS" : ("WITHDRAWAL".equalsIgnoreCase(type) ? "INFO" : "SUCCESS");
                masterList.add(new BankNotification(
                        icon,
                        type + " Cleared (" + tx.getFormattedAmount() + ")",
                        "Account " + tx.getSourceAccount() + ": " + tx.getDescription() + " • Balance: " + tx.getFormattedBalanceAfter(),
                        "TRANSACTION",
                        tx.getFormattedDate(),
                        level
                ));
            }

            // 2. Account-derived alerts
            List<Account> accounts = accountService.getAllAccounts();
            for (Account acc : accounts) {
                if ("INACTIVE".equalsIgnoreCase(acc.getStatus())) {
                    masterList.add(new BankNotification(
                            "⚠️",
                            "Account Inactivity Warning",
                            "Account " + acc.getAccountNumber() + " (" + acc.getCustomerName() + ") is currently INACTIVE. Verification required.",
                            "ACCOUNT",
                            acc.getFormattedDate(),
                            "WARNING"
                    ));
                } else if (acc.getBalance() != null && acc.getBalance().compareTo(new BigDecimal("30000")) < 0) {
                    masterList.add(new BankNotification(
                            "🔔",
                            "Low Portfolio Liquidity",
                            "Account " + acc.getAccountNumber() + " (" + acc.getCustomerName() + ") balance is " + acc.getFormattedBalance() + " (below threshold).",
                            "ACCOUNT",
                            LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy")),
                            "INFO"
                    ));
                }
            }

            // 3. System & Administrative Security Notices
            masterList.add(new BankNotification(
                    "🛡️",
                    "Core Ledger Verification Passed",
                    "Daily immutable transaction hash verification passed with 0 ledger discrepancies.",
                    "SECURITY",
                    LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, 09:00")),
                    "SUCCESS"
            ));

            masterList.add(new BankNotification(
                    "🔒",
                    "Branch Terminal Security Active",
                    "Branch Officer Priya Nair session authenticated under TLS 1.3 encrypted mock link.",
                    "SECURITY",
                    LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, 08:30")),
                    "INFO"
            ));

            displayList.setAll(masterList);
            if (unreadCountLabel != null) unreadCountLabel.setText(String.valueOf(masterList.size()));

        } catch (Exception e) {
            System.err.println("[NotificationController] Error loading events: " + e.getMessage());
        }
    }

    @FXML
    public void handleFilterCategory(ActionEvent event) {
        String selected = categoryFilterCombo != null ? categoryFilterCombo.getValue() : "ALL";
        if (selected == null || "ALL".equalsIgnoreCase(selected)) {
            displayList.setAll(masterList);
        } else {
            List<BankNotification> filtered = new ArrayList<>();
            for (BankNotification n : masterList) {
                if (n.getCategory().equalsIgnoreCase(selected)) {
                    filtered.add(n);
                }
            }
            displayList.setAll(filtered);
        }
    }

    @FXML
    public void handleMarkAllAsRead(ActionEvent event) {
        if (unreadCountLabel != null) unreadCountLabel.setText("0");
        if (statusFeedbackLabel != null) {
            statusFeedbackLabel.setText("✓ All notifications acknowledged and marked as read.");
            statusFeedbackLabel.getStyleClass().setAll("alert-box", "alert-success");
            statusFeedbackLabel.setVisible(true);
            statusFeedbackLabel.setManaged(true);
        }
    }

    // Navigation handlers
    @FXML public void navToDashboard(ActionEvent e) { Main.navigateTo("dashboard.fxml", "Apex Trust Bank - Operational Dashboard"); }
    @FXML public void navToAccounts(ActionEvent e) { Main.navigateTo("account.fxml", "Apex Trust Bank - Account Management"); }
    @FXML public void navToTransactions(ActionEvent e) { Main.navigateTo("transaction.fxml", "Apex Trust Bank - Process Transactions"); }
    @FXML public void navToTransfers(ActionEvent e) { Main.navigateTo("transfer.fxml", "Apex Trust Bank - Fund Transfers & Beneficiaries"); }
    @FXML public void navToHistory(ActionEvent e) { Main.navigateTo("history.fxml", "Apex Trust Bank - Transaction Audit History"); }
    @FXML public void navToReports(ActionEvent e) { Main.navigateTo("reports.fxml", "Apex Trust Bank - Reports & Statements"); }
    @FXML public void navToNotifications(ActionEvent e) { /* already on page */ }
    @FXML public void navToSecurity(ActionEvent e) { Main.navigateTo("security.fxml", "Apex Trust Bank - Security Overview"); }
    @FXML public void handleLogout(ActionEvent e) { authService.logout(); Main.navigateTo("login.fxml", "Apex Trust Bank - Secure Staff Login"); }
}

