package com.bank.controller;

import com.bank.Main;
import com.bank.model.Account;
import com.bank.model.Transaction;
import com.bank.service.AccountService;
import com.bank.service.AuthenticationService;
import com.bank.service.BankingException;
import com.bank.service.ServiceFactory;
import com.bank.service.TransactionService;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Controller for Processing Financial Transactions (Deposit, Withdrawal, Transfer).
 * All monetary updates are performed strictly inside Jayasree's TransactionService.
 * 
 * Author: Keerthana (Frontend Lead)
 */
public class TransactionController implements Initializable {

    // Sidebar & Header
    @FXML private Label operatorNameLabel;
    @FXML private Label demoBadgeLabel;
    @FXML private Label statusAlertLabel;

    // Transaction Form
    @FXML private ComboBox<String> transactionTypeCombo;
    @FXML private TextField sourceAccountField;
    @FXML private VBox destAccountBox;
    @FXML private TextField destAccountField;
    @FXML private TextField amountField;
    @FXML private TextField descriptionField;
    @FXML private Button submitButton;

    // Source Preview
    @FXML private Label previewStatusLabel;
    @FXML private Label previewHolderLabel;
    @FXML private Label previewBalanceLabel;

    // Receipt Box
    @FXML private VBox receiptBox;
    @FXML private Label receiptTypeBadge;
    @FXML private Label receiptTxIdLabel;
    @FXML private Label receiptSourceLabel;
    @FXML private Label receiptDestLabel;
    @FXML private Label receiptAmountLabel;
    @FXML private Label receiptNewBalanceLabel;
    @FXML private Label receiptTimestampLabel;

    private TransactionService transactionService;
    private AccountService accountService;
    private AuthenticationService authService;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.transactionService = ServiceFactory.getInstance().getTransactionService();
        this.accountService = ServiceFactory.getInstance().getAccountService();
        this.authService = ServiceFactory.getInstance().getAuthenticationService();

        // User & Badge
        operatorNameLabel.setText(authService.getCurrentUser());
        demoBadgeLabel.setText(ServiceFactory.getInstance().isDemoMode() ? "⚡ DEMO / JAYASREE READY" : "🔒 LIVE MYSQL");

        // Init Transaction Types
        transactionTypeCombo.setItems(FXCollections.observableArrayList("DEPOSIT", "WITHDRAWAL", "TRANSFER"));
        transactionTypeCombo.setValue("DEPOSIT");
        handleTypeChanged(null);

        // Preload default account preview
        sourceAccountField.setText("ACC-1001");
        handleCheckSourceAccount(null);
    }

    @FXML
    public void handleTypeChanged(ActionEvent event) {
        String selected = transactionTypeCombo.getValue();
        if ("TRANSFER".equalsIgnoreCase(selected)) {
            destAccountBox.setVisible(true);
            destAccountBox.setManaged(true);
            submitButton.setText("⚡ Execute Wire Transfer");
        } else if ("WITHDRAWAL".equalsIgnoreCase(selected)) {
            destAccountBox.setVisible(false);
            destAccountBox.setManaged(false);
            submitButton.setText("📤 Process Cash Withdrawal");
        } else {
            // DEPOSIT
            destAccountBox.setVisible(false);
            destAccountBox.setManaged(false);
            submitButton.setText("📥 Process Deposit");
        }
    }

    @FXML
    public void handleCheckSourceAccount(ActionEvent event) {
        String srcAcc = sourceAccountField.getText();
        if (srcAcc == null || srcAcc.trim().isEmpty()) {
            previewStatusLabel.setText("No account entered");
            previewHolderLabel.setText("--");
            previewBalanceLabel.setText("$0.00");
            return;
        }

        try {
            Optional<Account> accOpt = accountService.findAccount(srcAcc.trim());
            if (accOpt.isPresent()) {
                Account acc = accOpt.get();
                previewStatusLabel.setText(acc.getStatus());
                previewHolderLabel.setText(acc.getCustomerName());
                previewBalanceLabel.setText(acc.getFormattedBalance());
                if ("ACTIVE".equalsIgnoreCase(acc.getStatus())) {
                    previewStatusLabel.setStyle("-fx-text-fill: #15803d; -fx-font-weight: bold;");
                } else {
                    previewStatusLabel.setStyle("-fx-text-fill: #b91c1c; -fx-font-weight: bold;");
                }
            } else {
                previewStatusLabel.setText("Not Found");
                previewStatusLabel.setStyle("-fx-text-fill: #b91c1c; -fx-font-weight: bold;");
                previewHolderLabel.setText("Unregistered Account");
                previewBalanceLabel.setText("$0.00");
            }
        } catch (BankingException e) {
            previewStatusLabel.setText("Lookup Error");
        }
    }

    @FXML
    public void handleSubmitTransaction(ActionEvent event) {
        clearAlert();
        receiptBox.setVisible(false);
        receiptBox.setManaged(false);

        String type = transactionTypeCombo.getValue();
        String sourceAcc = sourceAccountField.getText();
        String destAcc = destAccountField.getText();
        String amountStr = amountField.getText();
        String memo = descriptionField.getText();

        // 1. Validation: Source account
        if (sourceAcc == null || sourceAcc.trim().isEmpty()) {
            showError("Please enter the source account number.");
            sourceAccountField.requestFocus();
            return;
        }

        // 2. Validation: Transfer destination
        if ("TRANSFER".equalsIgnoreCase(type)) {
            if (destAcc == null || destAcc.trim().isEmpty()) {
                showError("Please enter the destination account number for funds transfer.");
                destAccountField.requestFocus();
                return;
            }
            if (sourceAcc.trim().equalsIgnoreCase(destAcc.trim())) {
                showError("Source and destination accounts cannot be identical.");
                destAccountField.requestFocus();
                return;
            }
        }

        // 3. Validation: Positive monetary amount
        if (amountStr == null || amountStr.trim().isEmpty()) {
            showError("Please enter the transaction amount.");
            amountField.requestFocus();
            return;
        }

        BigDecimal amount;
        try {
            amount = new BigDecimal(amountStr.trim());
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                showError("Transaction amount must be strictly greater than $0.00.");
                amountField.requestFocus();
                return;
            }
        } catch (NumberFormatException e) {
            showError("Invalid monetary amount. Please enter a valid number (e.g. 150.00).");
            amountField.requestFocus();
            return;
        }

        // 4. Delegate to Jayasree's TransactionService
        try {
            Transaction completedTx;
            if ("DEPOSIT".equalsIgnoreCase(type)) {
                completedTx = transactionService.deposit(sourceAcc.trim(), amount, memo);
            } else if ("WITHDRAWAL".equalsIgnoreCase(type)) {
                completedTx = transactionService.withdraw(sourceAcc.trim(), amount, memo);
            } else if ("TRANSFER".equalsIgnoreCase(type)) {
                completedTx = transactionService.transfer(sourceAcc.trim(), destAcc.trim(), amount, memo);
            } else {
                showError("Unknown transaction type selected.");
                return;
            }

            // Show success receipt
            displayReceipt(completedTx);
            showSuccess(String.format("Successfully executed %s of %s for account %s!",
                    completedTx.getTransactionType(), completedTx.getFormattedAmount(), completedTx.getSourceAccount()));

            // Refresh preview
            handleCheckSourceAccount(null);

            // Clear amount and memo
            amountField.clear();
            descriptionField.clear();

        } catch (BankingException e) {
            showError("Transaction declined: " + e.getMessage());
        }
    }

    private void displayReceipt(Transaction tx) {
        receiptTypeBadge.setText(tx.getTransactionType());
        receiptTxIdLabel.setText("#" + tx.getTransactionId());
        receiptSourceLabel.setText(tx.getSourceAccount());
        receiptDestLabel.setText(tx.getDisplayDestination());
        receiptAmountLabel.setText(tx.getFormattedAmount());
        receiptNewBalanceLabel.setText(tx.getFormattedBalanceAfter());
        receiptTimestampLabel.setText(tx.getFormattedTimestamp());

        receiptBox.setVisible(true);
        receiptBox.setManaged(true);
    }

    @FXML
    public void handleClearForm(ActionEvent event) {
        amountField.clear();
        descriptionField.clear();
        destAccountField.clear();
        receiptBox.setVisible(false);
        receiptBox.setManaged(false);
        clearAlert();
    }

    private void showSuccess(String msg) {
        statusAlertLabel.setText("✓ " + msg);
        statusAlertLabel.getStyleClass().setAll("alert-box", "alert-success");
        statusAlertLabel.setVisible(true);
        statusAlertLabel.setManaged(true);
    }

    private void showError(String msg) {
        statusAlertLabel.setText("⚠️ " + msg);
        statusAlertLabel.getStyleClass().setAll("alert-box", "alert-danger");
        statusAlertLabel.setVisible(true);
        statusAlertLabel.setManaged(true);
    }

    private void clearAlert() {
        statusAlertLabel.setText("");
        statusAlertLabel.setVisible(false);
        statusAlertLabel.setManaged(false);
    }

    // Navigation handlers
    @FXML public void navToDashboard(ActionEvent e) { Main.navigateTo("dashboard.fxml", "Apex Trust Bank - Operational Dashboard"); }
    @FXML public void navToAccounts(ActionEvent e) { Main.navigateTo("account.fxml", "Apex Trust Bank - Account Management"); }
    @FXML public void navToTransactions(ActionEvent e) { /* already on page */ }
    @FXML public void navToHistory(ActionEvent e) { Main.navigateTo("history.fxml", "Apex Trust Bank - Transaction Audit History"); }
    @FXML public void handleLogout(ActionEvent e) { authService.logout(); Main.navigateTo("login.fxml", "Apex Trust Bank - Secure Staff Login"); }
}
