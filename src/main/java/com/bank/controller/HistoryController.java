package com.bank.controller;

import com.bank.Main;
import com.bank.model.Transaction;
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

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controller for Transaction History Ledger.
 * Provides searching, filtering, and detailed auditing of all financial transactions.
 * 
 * Author: Keerthana (Frontend Lead)
 */
public class HistoryController implements Initializable {

    // Sidebar & Header
    @FXML private Label operatorNameLabel;
    @FXML private Label demoBadgeLabel;

    // Filters
    @FXML private TextField accountFilterField;
    @FXML private ComboBox<String> typeFilterCombo;
    @FXML private Label summaryCountLabel;

    // TableView
    @FXML private TableView<Transaction> historyTable;
    @FXML private TableColumn<Transaction, String> colTxId;
    @FXML private TableColumn<Transaction, String> colTimestamp;
    @FXML private TableColumn<Transaction, String> colType;
    @FXML private TableColumn<Transaction, String> colSource;
    @FXML private TableColumn<Transaction, String> colDest;
    @FXML private TableColumn<Transaction, String> colAmount;
    @FXML private TableColumn<Transaction, String> colBalanceAfter;
    @FXML private TableColumn<Transaction, String> colDescription;

    private TransactionService transactionService;
    private AuthenticationService authService;
    private final ObservableList<Transaction> tableData = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.transactionService = ServiceFactory.getInstance().getTransactionService();
        this.authService = ServiceFactory.getInstance().getAuthenticationService();

        // Header info
        operatorNameLabel.setText(authService.getCurrentUser());
        demoBadgeLabel.setText(ServiceFactory.getInstance().isDemoMode() ? "⚡ DEMO AUDIT ACTIVE" : "🔒 LIVE MYSQL");

        // Filter ComboBox
        typeFilterCombo.setItems(FXCollections.observableArrayList("ALL", "DEPOSIT", "WITHDRAWAL", "TRANSFER"));
        typeFilterCombo.setValue("ALL");

        // Table setup
        setupTableColumns();
        historyTable.setItems(tableData);

        // Load all transactions initially
        loadData();
    }

    private void setupTableColumns() {
        colTxId.setCellValueFactory(cellData -> new SimpleStringProperty(String.valueOf(cellData.getValue().getTransactionId())));
        colTimestamp.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getFormattedTimestamp()));
        colType.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getTransactionType()));
        colSource.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getSourceAccount()));
        colDest.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDisplayDestination()));
        colAmount.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getFormattedAmount()));
        colBalanceAfter.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getFormattedBalanceAfter()));
        colDescription.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDescription()));
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
        String accountFilter = accountFilterField.getText();
        String typeFilter = typeFilterCombo.getValue();

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
                if (matchesType) {
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
        accountFilterField.clear();
        typeFilterCombo.setValue("ALL");
        loadData();
    }

    @FXML
    public void handleRefresh(ActionEvent event) {
        handleApplyFilter(event);
    }

    // Navigation handlers
    @FXML public void navToDashboard(ActionEvent e) { Main.navigateTo("dashboard.fxml", "Apex Trust Bank - Operational Dashboard"); }
    @FXML public void navToAccounts(ActionEvent e) { Main.navigateTo("account.fxml", "Apex Trust Bank - Account Management"); }
    @FXML public void navToTransactions(ActionEvent e) { Main.navigateTo("transaction.fxml", "Apex Trust Bank - Process Transactions"); }
    @FXML public void navToHistory(ActionEvent e) { loadData(); }
    @FXML public void handleLogout(ActionEvent e) { authService.logout(); Main.navigateTo("login.fxml", "Apex Trust Bank - Secure Staff Login"); }
}
