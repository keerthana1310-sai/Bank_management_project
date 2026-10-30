package com.bank.controller;

import com.bank.Main;
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
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.math.BigDecimal;
import java.net.URL;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Controller for the Main Dashboard view.
 * Displays KPI summary cards and recent transaction ledger via service interfaces.
 * 
 * Author: Keerthana (Frontend Lead)
 */
public class DashboardController implements Initializable {

    @FXML
    private Label welcomeLabel;

    @FXML
    private Label currentDateLabel;

    @FXML
    private Label operatorNameLabel;

    @FXML
    private Label demoBadgeLabel;

    @FXML
    private Label kpiAccountsLabel;

    @FXML
    private Label kpiDepositsLabel;

    @FXML
    private Label kpiTransactionsLabel;

    @FXML
    private Label kpiRepoModeLabel;

    @FXML
    private TableView<Transaction> recentTransactionsTable;

    @FXML
    private TableColumn<Transaction, String> colId;

    @FXML
    private TableColumn<Transaction, String> colType;

    @FXML
    private TableColumn<Transaction, String> colSource;

    @FXML
    private TableColumn<Transaction, String> colDest;

    @FXML
    private TableColumn<Transaction, String> colAmount;

    @FXML
    private TableColumn<Transaction, String> colDescription;

    @FXML
    private TableColumn<Transaction, String> colDate;

    private AccountService accountService;
    private TransactionService transactionService;
    private AuthenticationService authService;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            this.accountService = ServiceFactory.getInstance().getAccountService();
            this.transactionService = ServiceFactory.getInstance().getTransactionService();
            this.authService = ServiceFactory.getInstance().getAuthenticationService();

            // Populate User Info
            String currentUser = authService != null ? authService.getCurrentUser() : "Keerthana Rao (Lead Officer)";
            if (currentUser == null || currentUser.isBlank()) {
                currentUser = "Keerthana Rao (Lead Officer)";
            }
            if (welcomeLabel != null) {
                welcomeLabel.setText("Welcome back, " + currentUser + " • Apex Trust Banking Network");
            }
            if (operatorNameLabel != null) {
                operatorNameLabel.setText(currentUser);
            }

            // Date Display
            if (currentDateLabel != null) {
                currentDateLabel.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy")));
            }

            // Demo Badge
            boolean isDemo = ServiceFactory.getInstance().isDemoMode();
            if (demoBadgeLabel != null) {
                demoBadgeLabel.setText(isDemo ? "⚡ DEMO MODE (In-Memory)" : "🔒 LIVE MYSQL");
            }
            if (kpiRepoModeLabel != null) {
                kpiRepoModeLabel.setText(isDemo ? "In-Memory" : "MySQL JDBC");
            }

            // Configure Recent Transactions Table
            setupRecentTable();

            // Load Live Metrics via Service Interfaces
            refreshDashboardData();
        } catch (Exception e) {
            System.err.println("[DashboardController] Error initializing dashboard: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void setupRecentTable() {
        colId.setCellValueFactory(cellData -> new SimpleStringProperty(String.valueOf(cellData.getValue().getTransactionId())));
        colType.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getTransactionType()));
        colSource.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getSourceAccount()));
        colDest.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDisplayDestination()));
        colAmount.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getFormattedAmount()));
        colDescription.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDescription()));
        colDate.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getFormattedTimestamp()));
    }

    private void refreshDashboardData() {
        try {
            // 1. Total Accounts KPI
            int totalAccounts = accountService.getTotalAccountsCount();
            kpiAccountsLabel.setText(String.valueOf(totalAccounts));

            // 2. Total Balance / Deposits KPI
            BigDecimal totalBalance = accountService.getTotalBalanceSum();
            NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);
            kpiDepositsLabel.setText(currencyFormat.format(totalBalance));

            // 3. Total Transactions KPI
            int totalTx = transactionService.getTotalTransactionsCount();
            kpiTransactionsLabel.setText(String.valueOf(totalTx));

            // 4. Load recent transactions (first 5)
            List<Transaction> allTx = transactionService.getAllTransactions();
            List<Transaction> recent = allTx.subList(0, Math.min(5, allTx.size()));
            ObservableList<Transaction> observableList = FXCollections.observableArrayList(recent);
            recentTransactionsTable.setItems(observableList);

        } catch (BankingException e) {
            System.err.println("[DashboardController] Failed to load statistics: " + e.getMessage());
        }
    }

    // Navigation Handlers
    @FXML
    public void navToDashboard(ActionEvent event) {
        refreshDashboardData();
    }

    @FXML
    public void navToAccounts(ActionEvent event) {
        Main.navigateTo("account.fxml", "Apex Trust Bank - Account Management");
    }

    @FXML
    public void navToTransactions(ActionEvent event) {
        Main.navigateTo("transaction.fxml", "Apex Trust Bank - Process Transactions");
    }

    @FXML
    public void navToHistory(ActionEvent event) {
        Main.navigateTo("history.fxml", "Apex Trust Bank - Transaction Audit History");
    }

    @FXML
    public void handleLogout(ActionEvent event) {
        authService.logout();
        Main.navigateTo("login.fxml", "Apex Trust Bank - Secure Staff Login");
    }
}
