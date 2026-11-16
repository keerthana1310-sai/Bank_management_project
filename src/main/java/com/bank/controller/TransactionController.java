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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Controller for Bank Teller Transaction Processing Operations.
 * Manages deposits, withdrawals, and wire transfers with live balance and confirmation preview.
 * 
 * Redesigned for Apex Trust Bank Enterprise Architecture.
 */
public class TransactionController implements Initializable {

    // Sidebar & Header
    @FXML private Label operatorNameLabel;
    @FXML private Label demoBadgeLabel;
    @FXML private Label currentDateLabel;
    @FXML private Label statusAlertLabel;

    // Transaction Form
    @FXML private ComboBox<String> transactionTypeCombo;
    @FXML private TextField sourceAccountField;
    @FXML private Label customerNameDisplayLabel;
    @FXML private VBox destAccountBox;
    @FXML private TextField destAccountField;
    @FXML private TextField amountField;
    @FXML private TextField descriptionField;
    @FXML private TextField referenceField;
    @FXML private TextField txDateField;
    @FXML private Button submitButton;

    // Source Preview
    @FXML private Label previewStatusLabel;
    @FXML private Label previewHolderLabel;
    @FXML private Label previewBalanceLabel;

    // Confirmation Summary
    @FXML private VBox confirmationCard;
    @FXML private Label confirmTypeLabel;
    @FXML private Label confirmSourceLabel;
    @FXML private Label confirmHolderLabel;
    @FXML private Label confirmDestLabel;
    @FXML private Label confirmAmountLabel;
    @FXML private Label confirmEstBalanceLabel;

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

        // Init Transaction Date & Reference
        if (txDateField != null) {
            txDateField.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy")));
        }
        if (referenceField != null) {
            referenceField.setText("ATB-TX-" + System.currentTimeMillis() % 1000000);
        }

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
            submitButton.setText("⚡ Execute Fund Transfer");
        } else if ("WITHDRAWAL".equalsIgnoreCase(selected)) {
            destAccountBox.setVisible(false);
            destAccountBox.setManaged(false);
            submitButton.setText("📤 Process Cash Withdrawal");
        } else {
            // DEPOSIT
            destAccountBox.setVisible(false);
            destAccountBox.setManaged(false);
            submitButton.setText("📥 Process Cash Deposit");
        }
        updateConfirmationPreview();
    }

    @FXML
    public void handleCheckSourceAccount(ActionEvent event) {
        String srcAcc = sourceAccountField.getText();
        if (srcAcc == null || srcAcc.trim().isEmpty()) {
            previewStatusLabel.setText("No account entered");
            previewStatusLabel.getStyleClass().clear();
            previewHolderLabel.setText("--");
            previewBalanceLabel.setText("₹0.00");
            if (customerNameDisplayLabel != null) customerNameDisplayLabel.setText("—");
            updateConfirmationPreview();
            return;
        }

        try {
            Optional<Account> accOpt = accountService.findAccount(srcAcc.trim());
            if (accOpt.isPresent()) {
                Account acc = accOpt.get();
                previewStatusLabel.setText(acc.getStatus());
                previewHolderLabel.setText(acc.getCustomerName());
                previewBalanceLabel.setText(acc.getFormattedBalance());
                if (customerNameDisplayLabel != null) customerNameDisplayLabel.setText(acc.getCustomerName());

                previewStatusLabel.getStyleClass().clear();
                if ("ACTIVE".equalsIgnoreCase(acc.getStatus())) {
                    previewStatusLabel.getStyleClass().add("badge-active");
                } else {
                    previewStatusLabel.getStyleClass().add("badge-inactive");
                }
            } else {
                previewStatusLabel.setText("NOT FOUND");
                previewStatusLabel.getStyleClass().clear();
                previewStatusLabel.getStyleClass().add("badge-inactive");
                previewHolderLabel.setText("Unregistered Account");
                previewBalanceLabel.setText("₹0.00");
                if (customerNameDisplayLabel != null) customerNameDisplayLabel.setText("Unregistered");
            }
        } catch (BankingException e) {
            previewStatusLabel.setText("Lookup Error");
        }
        updateConfirmationPreview();
    }

    private void updateConfirmationPreview() {
        if (confirmationCard == null) return;

        String type = transactionTypeCombo != null ? transactionTypeCombo.getValue() : "DEPOSIT";
        String src = sourceAccountField != null ? sourceAccountField.getText() : "";
        String dest = destAccountField != null ? destAccountField.getText() : "—";
        String amt = amountField != null ? amountField.getText() : "0.00";

        if (confirmTypeLabel != null) confirmTypeLabel.setText(type);
        if (confirmSourceLabel != null) confirmSourceLabel.setText(src.isBlank() ? "—" : src.toUpperCase());
        if (confirmHolderLabel != null) confirmHolderLabel.setText(previewHolderLabel != null ? previewHolderLabel.getText() : "—");
        if (confirmDestLabel != null) confirmDestLabel.setText("TRANSFER".equalsIgnoreCase(type) ? (dest.isBlank() ? "—" : dest.toUpperCase()) : "N/A (Cash Operation)");
        if (confirmAmountLabel != null) confirmAmountLabel.setText("₹" + amt);

        confirmationCard.setVisible(true);
        confirmationCard.setManaged(true);
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
                showError("Please enter the destination account number for wire transfer.");
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
                showError("Transaction amount must be strictly greater than ₹0.00.");
                amountField.requestFocus();
                return;
            }
        } catch (NumberFormatException e) {
            showError("Invalid monetary amount. Please enter a valid number (e.g. 5000.00).");
            amountField.requestFocus();
            return;
        }

        // 4. Delegate to TransactionService
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
            if (referenceField != null) {
                referenceField.setText("ATB-TX-" + System.currentTimeMillis() % 1000000);
            }

        } catch (BankingException e) {
            showError("Transaction declined: " + e.getMessage());
        }
    }

    private void displayReceipt(Transaction tx) {
        receiptTypeBadge.setText(tx.getTransactionType());
        receiptTypeBadge.getStyleClass().clear();
        String upper = tx.getTransactionType().toUpperCase();
        if ("DEPOSIT".equals(upper)) {
            receiptTypeBadge.getStyleClass().add("badge-deposit");
        } else if ("WITHDRAWAL".equals(upper)) {
            receiptTypeBadge.getStyleClass().add("badge-withdrawal");
        } else {
            receiptTypeBadge.getStyleClass().add("badge-transfer");
        }

        receiptTxIdLabel.setText("#" + tx.getTransactionId());
        receiptSourceLabel.setText(tx.getSourceAccount());
        receiptDestLabel.setText(tx.getDisplayDestination());
        receiptAmountLabel.setText(tx.getFormattedAmount());
        receiptNewBalanceLabel.setText(tx.getFormattedBalanceAfter());
        receiptTimestampLabel.setText(tx.getFormattedDate());

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
        updateConfirmationPreview();
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
    @FXML public void navToTransfers(ActionEvent e) { Main.navigateTo("transfer.fxml", "Apex Trust Bank - Fund Transfers & Beneficiaries"); }
    @FXML public void navToHistory(ActionEvent e) { Main.navigateTo("history.fxml", "Apex Trust Bank - Transaction Audit History"); }
    @FXML public void navToReports(ActionEvent e) { Main.navigateTo("reports.fxml", "Apex Trust Bank - Reports & Statements"); }
    @FXML public void navToNotifications(ActionEvent e) { Main.navigateTo("notifications.fxml", "Apex Trust Bank - Notification Centre"); }
    @FXML public void navToSecurity(ActionEvent e) { Main.navigateTo("security.fxml", "Apex Trust Bank - Security Overview"); }
    @FXML public void handleLogout(ActionEvent e) { authService.logout(); Main.navigateTo("login.fxml", "Apex Trust Bank - Secure Staff Login"); }
}
