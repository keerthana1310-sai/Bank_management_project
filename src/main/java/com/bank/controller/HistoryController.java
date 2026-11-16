package com.bank.controller;

import com.bank.Main;
import com.bank.model.Account;
import com.bank.model.Transaction;
import com.bank.service.AccountService;
import com.bank.service.AuthenticationService;
import com.bank.service.BankingException;
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
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Controller for Transaction History & Audit Ledger.
 * Provides searching, multi-criteria filtering, and CSV ledger export.
 * 
 * Redesigned for Apex Trust Bank Enterprise Architecture.
 */
public class HistoryController implements Initializable {

    // Sidebar & Header
    @FXML private Label operatorNameLabel;
    @FXML private Label demoBadgeLabel;
    @FXML private Label currentDateLabel;
    @FXML private Label statusFeedbackLabel;

    // Filters
    @FXML private TextField accountFilterField;
    @FXML private TextField customerFilterField;
    @FXML private ComboBox<String> typeFilterCombo;
    @FXML private ComboBox<String> statusFilterCombo;
    @FXML private Label summaryCountLabel;

    // TableView
    @FXML private TableView<Transaction> historyTable;
    @FXML private TableColumn<Transaction, String> colTxId;
    @FXML private TableColumn<Transaction, String> colTimestamp;
    @FXML private TableColumn<Transaction, String> colType;
    @FXML private TableColumn<Transaction, String> colSource;
    @FXML private TableColumn<Transaction, String> colCustomer;
    @FXML private TableColumn<Transaction, String> colDest;
    @FXML private TableColumn<Transaction, String> colAmount;
    @FXML private TableColumn<Transaction, String> colBalanceAfter;
    @FXML private TableColumn<Transaction, String> colStatus;
    @FXML private TableColumn<Transaction, String> colDescription;

    private TransactionService transactionService;
    private AccountService accountService;
    private AuthenticationService authService;
    private final ObservableList<Transaction> tableData = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.transactionService = ServiceFactory.getInstance().getTransactionService();
        this.accountService = ServiceFactory.getInstance().getAccountService();
        this.authService = ServiceFactory.getInstance().getAuthenticationService();

        // Header info
        String currentUser = authService != null ? authService.getCurrentUser() : "Priya Nair (Branch Officer)";
        if (operatorNameLabel != null) {
            operatorNameLabel.setText(currentUser);
        }
        if (demoBadgeLabel != null) {
            demoBadgeLabel.setText(ServiceFactory.getInstance().isDemoMode() ? "⚡ In-Memory Demo" : "🔒 Live MySQL");
        }
        if (currentDateLabel != null) {
            currentDateLabel.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy")));
        }

        // Filter ComboBoxes
        typeFilterCombo.setItems(FXCollections.observableArrayList("ALL", "DEPOSIT", "WITHDRAWAL", "TRANSFER"));
        typeFilterCombo.setValue("ALL");

        if (statusFilterCombo != null) {
            statusFilterCombo.setItems(FXCollections.observableArrayList("ALL", "COMPLETED"));
            statusFilterCombo.setValue("ALL");
        }

        // Table setup
        setupTableColumns();
        historyTable.setItems(tableData);

        // Load all transactions initially
        loadData();
    }

    private void setupTableColumns() {
        colTxId.setCellValueFactory(cellData -> new SimpleStringProperty("#" + cellData.getValue().getTransactionId()));
        colTimestamp.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getFormattedDate()));
        colType.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getTransactionType()));
        colSource.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getSourceAccount()));

        if (colCustomer != null) {
            colCustomer.setCellValueFactory(cellData -> {
                String accNum = cellData.getValue().getSourceAccount();
                try {
                    Optional<Account> acc = accountService.findAccount(accNum);
                    return new SimpleStringProperty(acc.map(Account::getCustomerName).orElse("—"));
                } catch (Exception e) {
                    return new SimpleStringProperty("—");
                }
            });
        }

        colDest.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDisplayDestination()));
        colAmount.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getFormattedAmount()));
        colBalanceAfter.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getFormattedBalanceAfter()));

        if (colStatus != null) {
            colStatus.setCellValueFactory(cellData -> new SimpleStringProperty("COMPLETED"));
            colStatus.setCellFactory(col -> new TableCell<Transaction, String>() {
                @Override
                protected void updateItem(String status, boolean empty) {
                    super.updateItem(status, empty);
                    if (empty || status == null) {
                        setText(null);
                        setGraphic(null);
                    } else {
                        Label badge = new Label("COMPLETED");
                        badge.getStyleClass().add("badge-completed");
                        setGraphic(badge);
                        setText(null);
                    }
                }
            });
        }

        colDescription.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDescription()));

        // Pill badge for Transaction Type
        colType.setCellFactory(col -> new TableCell<Transaction, String>() {
            @Override
            protected void updateItem(String type, boolean empty) {
                super.updateItem(type, empty);
                if (empty || type == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label badge = new Label();
                    String upper = type.toUpperCase();
                    if ("DEPOSIT".equals(upper)) {
                        badge.getStyleClass().add("badge-deposit");
                        badge.setText("+ DEPOSIT");
                    } else if ("WITHDRAWAL".equals(upper)) {
                        badge.getStyleClass().add("badge-withdrawal");
                        badge.setText("- WITHDRAWAL");
                    } else {
                        badge.getStyleClass().add("badge-transfer");
                        badge.setText("⇄ TRANSFER");
                    }
                    setGraphic(badge);
                    setText(null);
                }
            }
        });

        // Color coding for Amount column
        colAmount.setCellFactory(col -> new TableCell<Transaction, String>() {
            @Override
            protected void updateItem(String amountStr, boolean empty) {
                super.updateItem(amountStr, empty);
                if (empty || amountStr == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(amountStr);
                    Transaction tx = getTableRow() != null ? getTableRow().getItem() : null;
                    if (tx != null) {
                        String type = tx.getTransactionType();
                        if ("DEPOSIT".equalsIgnoreCase(type)) {
                            setStyle("-fx-text-fill: #16A34A; -fx-font-weight: bold;");
                        } else if ("WITHDRAWAL".equalsIgnoreCase(type)) {
                            setStyle("-fx-text-fill: #DC2626; -fx-font-weight: bold;");
                        } else {
                            setStyle("-fx-text-fill: #2563EB; -fx-font-weight: bold;");
                        }
                    } else {
                        setStyle("-fx-font-weight: bold;");
                    }
                }
            }
        });
    }

    private void loadData() {
        try {
            List<Transaction> list = transactionService.getAllTransactions();
            tableData.setAll(list);
            summaryCountLabel.setText("Showing: " + list.size() + " total transactions");
        } catch (BankingException e) {
            System.err.println("[HistoryController] Failed to load transactions: " + e.getMessage());
        }
    }

    @FXML
    public void handleApplyFilter(ActionEvent event) {
        String accountFilter = accountFilterField != null ? accountFilterField.getText() : null;
        String custFilter = customerFilterField != null ? customerFilterField.getText() : null;
        String typeFilter = typeFilterCombo != null ? typeFilterCombo.getValue() : "ALL";

        try {
            List<Transaction> baseList;
            if (accountFilter != null && !accountFilter.isBlank()) {
                baseList = transactionService.getTransactionHistory(accountFilter.trim());
            } else {
                baseList = transactionService.getAllTransactions();
            }

            List<Transaction> filtered = new ArrayList<>();
            for (Transaction tx : baseList) {
                boolean matchesType = (typeFilter == null || "ALL".equalsIgnoreCase(typeFilter) 
                        || tx.getTransactionType().equalsIgnoreCase(typeFilter));

                boolean matchesCustomer = true;
                if (custFilter != null && !custFilter.isBlank()) {
                    try {
                        Optional<Account> acc = accountService.findAccount(tx.getSourceAccount());
                        matchesCustomer = acc.isPresent() && acc.get().getCustomerName().toLowerCase().contains(custFilter.trim().toLowerCase());
                    } catch (Exception e) {
                        matchesCustomer = false;
                    }
                }

                if (matchesType && matchesCustomer) {
                    filtered.add(tx);
                }
            }

            tableData.setAll(filtered);
            summaryCountLabel.setText("Showing: " + filtered.size() + " filtered records");

        } catch (BankingException e) {
            System.err.println("[HistoryController] Error filtering: " + e.getMessage());
        }
    }

    @FXML
    public void handleClearFilter(ActionEvent event) {
        if (accountFilterField != null) accountFilterField.clear();
        if (customerFilterField != null) customerFilterField.clear();
        if (typeFilterCombo != null) typeFilterCombo.setValue("ALL");
        if (statusFilterCombo != null) statusFilterCombo.setValue("ALL");
        if (statusFeedbackLabel != null) {
            statusFeedbackLabel.setVisible(false);
            statusFeedbackLabel.setManaged(false);
        }
        loadData();
    }

    @FXML
    public void handleRefresh(ActionEvent event) {
        handleApplyFilter(event);
    }

    @FXML
    public void handleExportCSV(ActionEvent event) {
        try {
            File exportFile = new File("apex_trust_bank_ledger.csv");
            try (PrintWriter writer = new PrintWriter(new FileWriter(exportFile))) {
                writer.println("Transaction ID,Date & Time,Source Account,Customer Name,Destination Account,Type,Amount,Balance After,Status,Description");
                for (Transaction tx : tableData) {
                    String custName = "—";
                    try {
                        Optional<Account> acc = accountService.findAccount(tx.getSourceAccount());
                        if (acc.isPresent()) custName = acc.get().getCustomerName();
                    } catch (Exception ignored) {}

                    writer.printf("%d,%s,%s,\"%s\",%s,%s,%s,%s,%s,\"%s\"%n",
                            tx.getTransactionId(),
                            tx.getFormattedDate(),
                            tx.getSourceAccount(),
                            custName,
                            tx.getDisplayDestination(),
                            tx.getTransactionType(),
                            tx.getFormattedAmount(),
                            tx.getFormattedBalanceAfter(),
                            "COMPLETED",
                            tx.getDescription()
                    );
                }
            }

            if (statusFeedbackLabel != null) {
                statusFeedbackLabel.setText("✓ Successfully exported " + tableData.size() + " ledger records to " + exportFile.getName() + "!");
                statusFeedbackLabel.getStyleClass().setAll("alert-box", "alert-success");
                statusFeedbackLabel.setVisible(true);
                statusFeedbackLabel.setManaged(true);
            }
        } catch (Exception e) {
            if (statusFeedbackLabel != null) {
                statusFeedbackLabel.setText("⚠️ Failed to export ledger: " + e.getMessage());
                statusFeedbackLabel.getStyleClass().setAll("alert-box", "alert-danger");
                statusFeedbackLabel.setVisible(true);
                statusFeedbackLabel.setManaged(true);
            }
        }
    }

    // Navigation handlers
    @FXML public void navToDashboard(ActionEvent e) { Main.navigateTo("dashboard.fxml", "Apex Trust Bank - Operational Dashboard"); }
    @FXML public void navToAccounts(ActionEvent e) { Main.navigateTo("account.fxml", "Apex Trust Bank - Account Management"); }
    @FXML public void navToTransactions(ActionEvent e) { Main.navigateTo("transaction.fxml", "Apex Trust Bank - Process Transactions"); }
    @FXML public void navToTransfers(ActionEvent e) { Main.navigateTo("transfer.fxml", "Apex Trust Bank - Fund Transfers & Beneficiaries"); }
    @FXML public void navToHistory(ActionEvent e) { loadData(); }
    @FXML public void navToReports(ActionEvent e) { Main.navigateTo("reports.fxml", "Apex Trust Bank - Reports & Statements"); }
    @FXML public void navToNotifications(ActionEvent e) { Main.navigateTo("notifications.fxml", "Apex Trust Bank - Notification Centre"); }
    @FXML public void navToSecurity(ActionEvent e) { Main.navigateTo("security.fxml", "Apex Trust Bank - Security Overview"); }
    @FXML public void handleLogout(ActionEvent e) { authService.logout(); Main.navigateTo("login.fxml", "Apex Trust Bank - Secure Staff Login"); }
}
