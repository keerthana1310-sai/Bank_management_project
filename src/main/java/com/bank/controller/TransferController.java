package com.bank.controller;

import com.bank.Main;
import com.bank.model.Account;
import com.bank.model.Transaction;
import com.bank.service.AccountService;
import com.bank.service.AuthenticationService;
import com.bank.service.BankingException;
import com.bank.service.ServiceFactory;
import com.bank.service.TransactionService;
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
 * Controller for Dedicated Fund Transfer & Beneficiary Management.
 * Manages peer-to-peer and inter-account wire transfers via TransactionService.
 */
public class TransferController implements Initializable {

    // Sidebar & Header
    @FXML private Label operatorNameLabel;
    @FXML private Label demoBadgeLabel;
    @FXML private Label currentDateLabel;
    @FXML private Label statusAlertLabel;

    // Transfer Form
    @FXML private TextField senderAccountField;
    @FXML private Label senderHolderLabel;
    @FXML private Label senderBalanceLabel;
    @FXML private TextField beneficiaryNameField;
    @FXML private TextField beneficiaryAccountField;
    @FXML private TextField transferAmountField;
    @FXML private TextField transferRemarksField;

    // Confirmation Summary
    @FXML private VBox confirmationCard;
    @FXML private Label confirmSenderLabel;
    @FXML private Label confirmBeneficiaryLabel;
    @FXML private Label confirmAmountLabel;
    @FXML private Label confirmRemarksLabel;

    // Receipt Card
    @FXML private VBox receiptBox;
    @FXML private Label receiptTxIdLabel;
    @FXML private Label receiptSenderLabel;
    @FXML private Label receiptBeneficiaryLabel;
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

        String currentUser = authService != null ? authService.getCurrentUser() : "Priya Nair (Branch Officer)";
        if (operatorNameLabel != null) operatorNameLabel.setText(currentUser);
        if (demoBadgeLabel != null) demoBadgeLabel.setText(ServiceFactory.getInstance().isDemoMode() ? "⚡ In-Memory Demo" : "🔒 Live MySQL");
        if (currentDateLabel != null) currentDateLabel.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy")));

        // Preload default sender account
        senderAccountField.setText("ACC-1001");
        handleCheckSender(null);
    }

    @FXML
    public void handleCheckSender(ActionEvent event) {
        String srcAcc = senderAccountField.getText();
        if (srcAcc == null || srcAcc.trim().isEmpty()) {
            senderHolderLabel.setText("No account entered");
            senderBalanceLabel.setText("₹0.00");
            updateConfirmation();
            return;
        }

        try {
            Optional<Account> accOpt = accountService.findAccount(srcAcc.trim());
            if (accOpt.isPresent()) {
                Account acc = accOpt.get();
                senderHolderLabel.setText(acc.getCustomerName() + " (" + acc.getStatus() + ")");
                senderBalanceLabel.setText(acc.getFormattedBalance());
            } else {
                senderHolderLabel.setText("Account Not Found");
                senderBalanceLabel.setText("₹0.00");
            }
        } catch (BankingException e) {
            senderHolderLabel.setText("Lookup Error");
        }
        updateConfirmation();
    }

    @FXML
    public void selectBeneficiary(String name, String accNum) {
        beneficiaryNameField.setText(name);
        beneficiaryAccountField.setText(accNum);
        updateConfirmation();
    }

    @FXML
    public void selectBenAnanya(ActionEvent e) { selectBeneficiary("Ananya Reddy", "ACC-1002"); }

    @FXML
    public void selectBenPriya(ActionEvent e) { selectBeneficiary("Priya Nair", "ACC-1004"); }

    @FXML
    public void selectBenArjun(ActionEvent e) { selectBeneficiary("Arjun Mehta", "ACC-1005"); }

    @FXML
    public void selectBenKarthik(ActionEvent e) { selectBeneficiary("Karthik Iyer", "ACC-1007"); }

    private void updateConfirmation() {
        if (confirmationCard == null) return;
        confirmSenderLabel.setText(senderAccountField.getText().isBlank() ? "—" : senderAccountField.getText().trim().toUpperCase());
        confirmBeneficiaryLabel.setText((beneficiaryNameField.getText().isBlank() ? "—" : beneficiaryNameField.getText().trim())
                + " (" + (beneficiaryAccountField.getText().isBlank() ? "—" : beneficiaryAccountField.getText().trim().toUpperCase()) + ")");
        confirmAmountLabel.setText("₹" + (transferAmountField.getText().isBlank() ? "0.00" : transferAmountField.getText().trim()));
        confirmRemarksLabel.setText(transferRemarksField.getText().isBlank() ? "Direct Wire Transfer" : transferRemarksField.getText().trim());
    }

    @FXML
    public void handleExecuteTransfer(ActionEvent event) {
        clearAlert();
        receiptBox.setVisible(false);
        receiptBox.setManaged(false);

        String sender = senderAccountField.getText();
        String dest = beneficiaryAccountField.getText();
        String amountStr = transferAmountField.getText();
        String memo = transferRemarksField.getText();

        if (sender == null || sender.isBlank()) {
            showError("Please enter sender account number.");
            senderAccountField.requestFocus();
            return;
        }

        if (dest == null || dest.isBlank()) {
            showError("Please enter or select a beneficiary account number.");
            beneficiaryAccountField.requestFocus();
            return;
        }

        if (sender.trim().equalsIgnoreCase(dest.trim())) {
            showError("Sender and beneficiary accounts cannot be identical.");
            beneficiaryAccountField.requestFocus();
            return;
        }

        if (amountStr == null || amountStr.isBlank()) {
            showError("Please enter transfer amount.");
            transferAmountField.requestFocus();
            return;
        }

        BigDecimal amount;
        try {
            amount = new BigDecimal(amountStr.trim());
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                showError("Transfer amount must be strictly greater than ₹0.00.");
                transferAmountField.requestFocus();
                return;
            }
        } catch (NumberFormatException e) {
            showError("Invalid monetary amount. Please enter a valid decimal number (e.g. 5000.00).");
            transferAmountField.requestFocus();
            return;
        }

        // Execute real atomic transfer via TransactionService
        try {
            Transaction tx = transactionService.transfer(sender.trim(), dest.trim(), amount, 
                    memo.isBlank() ? "Wire Transfer to " + beneficiaryNameField.getText() : memo.trim());

            displayReceipt(tx);
            showSuccess(String.format("Transfer of %s from %s to %s successfully settled!",
                    tx.getFormattedAmount(), tx.getSourceAccount(), tx.getDisplayDestination()));

            handleCheckSender(null);
            transferAmountField.clear();
            transferRemarksField.clear();
        } catch (BankingException e) {
            showError("Transfer declined: " + e.getMessage());
        }
    }

    private void displayReceipt(Transaction tx) {
        receiptTxIdLabel.setText("#" + tx.getTransactionId());
        receiptSenderLabel.setText(tx.getSourceAccount());
        receiptBeneficiaryLabel.setText(tx.getDisplayDestination() + " (" + beneficiaryNameField.getText() + ")");
        receiptAmountLabel.setText(tx.getFormattedAmount());
        receiptNewBalanceLabel.setText(tx.getFormattedBalanceAfter());
        receiptTimestampLabel.setText(tx.getFormattedDate());

        receiptBox.setVisible(true);
        receiptBox.setManaged(true);
    }

    @FXML
    public void handleReset(ActionEvent event) {
        transferAmountField.clear();
        transferRemarksField.clear();
        beneficiaryNameField.clear();
        beneficiaryAccountField.clear();
        receiptBox.setVisible(false);
        receiptBox.setManaged(false);
        clearAlert();
        updateConfirmation();
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
    @FXML public void navToTransactions(ActionEvent e) { Main.navigateTo("transaction.fxml", "Apex Trust Bank - Process Transactions"); }
    @FXML public void navToTransfers(ActionEvent e) { /* already on page */ }
    @FXML public void navToHistory(ActionEvent e) { Main.navigateTo("history.fxml", "Apex Trust Bank - Transaction Audit History"); }
    @FXML public void navToReports(ActionEvent e) { Main.navigateTo("reports.fxml", "Apex Trust Bank - Reports & Statements"); }
    @FXML public void navToNotifications(ActionEvent e) { Main.navigateTo("notifications.fxml", "Apex Trust Bank - Notification Centre"); }
    @FXML public void navToSecurity(ActionEvent e) { Main.navigateTo("security.fxml", "Apex Trust Bank - Security Overview"); }
    @FXML public void handleLogout(ActionEvent e) { authService.logout(); Main.navigateTo("login.fxml", "Apex Trust Bank - Secure Staff Login"); }
}

