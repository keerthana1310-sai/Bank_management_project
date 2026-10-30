package com.bank.controller;

import com.bank.Main;
import com.bank.model.Account;
import com.bank.service.AccountService;
import com.bank.service.AuthenticationService;
import com.bank.service.BankingException;
import com.bank.service.ServiceFactory;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Controller for Account Management.
 * Manages customer onboarding, account querying, and directory listing.
 * Prepared for Pranita's AccountService implementation.
 * 
 * Author: Keerthana (Frontend Lead)
 */
public class AccountController implements Initializable {

    // Sidebar & Header
    @FXML private Label operatorNameLabel;
    @FXML private Label demoBadgeLabel;
    @FXML private Label statusMessageLabel;

    // Create Account Form
    @FXML private TextField customerNameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private ComboBox<String> accountTypeCombo;
    @FXML private TextField initialDepositField;
    @FXML private Button createAccountBtn;
    @FXML private VBox newAccountSuccessBox;
    @FXML private Label createdAccNumLabel;
    @FXML private Label createdHolderLabel;
    @FXML private Label createdBalLabel;

    // Search & Details View
    @FXML private TextField searchAccountField;
    @FXML private VBox accountDetailsCard;
    @FXML private Label detailAccNumberLabel;
    @FXML private Label detailCustomerNameLabel;
    @FXML private Label detailStatusBadge;
    @FXML private Label detailBalanceLabel;
    @FXML private Label detailTypeLabel;
    @FXML private Label detailEmailLabel;
    @FXML private Label detailPhoneLabel;
    @FXML private Label detailCreatedLabel;

    // Directory TableView
    @FXML private TextField tableFilterField;
    @FXML private TableView<Account> accountsTable;
    @FXML private TableColumn<Account, String> colAccNumber;
    @FXML private TableColumn<Account, String> colHolderName;
    @FXML private TableColumn<Account, String> colAccType;
    @FXML private TableColumn<Account, String> colBalance;
    @FXML private TableColumn<Account, String> colStatus;
    @FXML private TableColumn<Account, String> colEmail;
    @FXML private TableColumn<Account, String> colPhone;
    @FXML private TableColumn<Account, String> colCreated;

    private AccountService accountService;
    private AuthenticationService authService;
    private ObservableList<Account> accountsMasterList = FXCollections.observableArrayList();
    private FilteredList<Account> filteredAccounts;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.accountService = ServiceFactory.getInstance().getAccountService();
        this.authService = ServiceFactory.getInstance().getAuthenticationService();

        // Header & User
        operatorNameLabel.setText(authService.getCurrentUser());
        demoBadgeLabel.setText(ServiceFactory.getInstance().isDemoMode() ? "⚡ DEMO / PRANITA READY" : "🔒 LIVE MYSQL");

        // Init ComboBox
        accountTypeCombo.setItems(FXCollections.observableArrayList("SAVINGS", "CURRENT"));
        accountTypeCombo.setValue("SAVINGS");

        // Configure Table Columns
        setupTableColumns();

        // Load Accounts
        loadAccountsData();

        // Table Selection Listener -> updates Inspector Card
        accountsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                displayAccountDetails(newVal);
            }
        });

        // Setup Search/Filter for TableView
        filteredAccounts = new FilteredList<>(accountsMasterList, p -> true);
        tableFilterField.textProperty().addListener((obs, oldValue, newValue) -> {
            filteredAccounts.setPredicate(account -> {
                if (newValue == null || newValue.isBlank()) return true;
                String lower = newValue.toLowerCase().trim();
                return account.getAccountNumber().toLowerCase().contains(lower)
                        || account.getCustomerName().toLowerCase().contains(lower)
                        || account.getEmail().toLowerCase().contains(lower);
            });
        });
        accountsTable.setItems(filteredAccounts);
    }

    private void setupTableColumns() {
        colAccNumber.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getAccountNumber()));
        colHolderName.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getCustomerName()));
        colAccType.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getAccountType()));
        colBalance.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getFormattedBalance()));
        colStatus.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getStatus()));
        colEmail.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getEmail()));
        colPhone.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getPhone()));
        colCreated.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getFormattedDate()));
    }

    private void loadAccountsData() {
        try {
            List<Account> all = accountService.getAllAccounts();
            accountsMasterList.setAll(all);

            // Display first account details as default if available
            if (!all.isEmpty() && detailAccNumberLabel.getText().equals("ACC-1001")) {
                displayAccountDetails(all.get(0));
            }
        } catch (BankingException e) {
            showError("Could not retrieve accounts: " + e.getMessage());
        }
    }

    @FXML
    public void handleCreateAccount(ActionEvent event) {
        clearAlert();
        newAccountSuccessBox.setVisible(false);
        newAccountSuccessBox.setManaged(false);

        String name = customerNameField.getText();
        String email = emailField.getText();
        String phone = phoneField.getText();
        String type = accountTypeCombo.getValue();
        String depositStr = initialDepositField.getText();

        // 1. Frontend validation: required fields
        if (name == null || name.trim().isEmpty()) {
            showError("Please enter the customer's legal full name.");
            customerNameField.requestFocus();
            return;
        }

        if (email == null || !email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            showError("Please provide a valid email address (e.g., name@domain.com).");
            emailField.requestFocus();
            return;
        }

        if (phone == null || phone.trim().length() < 7) {
            showError("Please enter a valid phone number (at least 7 digits).");
            phoneField.requestFocus();
            return;
        }

        if (type == null) {
            showError("Please select an account type (SAVINGS or CURRENT).");
            return;
        }

        // 2. Validate monetary amount
        BigDecimal initialDeposit;
        try {
            if (depositStr == null || depositStr.trim().isEmpty()) {
                initialDeposit = BigDecimal.ZERO;
            } else {
                initialDeposit = new BigDecimal(depositStr.trim());
                if (initialDeposit.compareTo(BigDecimal.ZERO) < 0) {
                    showError("Initial deposit cannot be negative.");
                    initialDepositField.requestFocus();
                    return;
                }
            }
        } catch (NumberFormatException e) {
            showError("Invalid monetary amount. Please enter a valid decimal number (e.g., 500.00).");
            initialDepositField.requestFocus();
            return;
        }

        // 3. Delegate to Pranita's AccountService
        try {
            Account created = accountService.createAccount(name, email, phone, type, initialDeposit);
            
            // Show success details banner
            createdAccNumLabel.setText("Account Number: " + created.getAccountNumber());
            createdHolderLabel.setText("Holder: " + created.getCustomerName());
            createdBalLabel.setText("Balance: " + created.getFormattedBalance());
            newAccountSuccessBox.setVisible(true);
            newAccountSuccessBox.setManaged(true);

            showSuccess("Account " + created.getAccountNumber() + " successfully created for " + created.getCustomerName() + "!");

            // Refresh table and inspector
            loadAccountsData();
            displayAccountDetails(created);

            // Clear inputs
            customerNameField.clear();
            emailField.clear();
            phoneField.clear();
            initialDepositField.clear();

        } catch (BankingException e) {
            showError("Failed to create account: " + e.getMessage());
        }
    }

    @FXML
    public void handleSearchAccount(ActionEvent event) {
        clearAlert();
        String query = searchAccountField.getText();
        if (query == null || query.trim().isEmpty()) {
            showError("Please enter an account number to search.");
            searchAccountField.requestFocus();
            return;
        }

        try {
            Optional<Account> result = accountService.findAccount(query.trim());
            if (result.isPresent()) {
                Account account = result.get();
                displayAccountDetails(account);
                showSuccess("Account found: " + account.getAccountNumber() + " (" + account.getCustomerName() + ")");
                // Select in table if present
                accountsTable.getSelectionModel().select(account);
            } else {
                showError("No account found matching '" + query.trim() + "'.");
            }
        } catch (BankingException e) {
            showError("Error searching account: " + e.getMessage());
        }
    }

    private void displayAccountDetails(Account account) {
        detailAccNumberLabel.setText(account.getAccountNumber());
        detailCustomerNameLabel.setText(account.getCustomerName());
        detailStatusBadge.setText(account.getStatus());
        detailBalanceLabel.setText(account.getFormattedBalance());
        detailTypeLabel.setText(account.getAccountType());
        detailEmailLabel.setText(account.getEmail());
        detailPhoneLabel.setText(account.getPhone());
        detailCreatedLabel.setText(account.getFormattedDate());

        if ("ACTIVE".equalsIgnoreCase(account.getStatus())) {
            detailStatusBadge.setStyle("-fx-background-color: #dcfce7; -fx-text-fill: #15803d; -fx-font-weight: bold; -fx-padding: 4px 10px; -fx-background-radius: 20px;");
        } else {
            detailStatusBadge.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #b91c1c; -fx-font-weight: bold; -fx-padding: 4px 10px; -fx-background-radius: 20px;");
        }
    }

    @FXML
    public void handleResetForm(ActionEvent event) {
        customerNameField.clear();
        emailField.clear();
        phoneField.clear();
        initialDepositField.clear();
        accountTypeCombo.setValue("SAVINGS");
        newAccountSuccessBox.setVisible(false);
        newAccountSuccessBox.setManaged(false);
        clearAlert();
    }

    @FXML
    public void handleRefreshTable(ActionEvent event) {
        loadAccountsData();
        showSuccess("Account directory refreshed.");
    }

    private void showSuccess(String msg) {
        statusMessageLabel.setText("✓ " + msg);
        statusMessageLabel.getStyleClass().setAll("alert-box", "alert-success");
        statusMessageLabel.setVisible(true);
        statusMessageLabel.setManaged(true);
    }

    private void showError(String msg) {
        statusMessageLabel.setText("⚠️ " + msg);
        statusMessageLabel.getStyleClass().setAll("alert-box", "alert-danger");
        statusMessageLabel.setVisible(true);
        statusMessageLabel.setManaged(true);
    }

    private void clearAlert() {
        statusMessageLabel.setText("");
        statusMessageLabel.setVisible(false);
        statusMessageLabel.setManaged(false);
    }

    // Navigation handlers
    @FXML public void navToDashboard(ActionEvent e) { Main.navigateTo("dashboard.fxml", "Apex Trust Bank - Operational Dashboard"); }
    @FXML public void navToAccounts(ActionEvent e) { loadAccountsData(); }
    @FXML public void navToTransactions(ActionEvent e) { Main.navigateTo("transaction.fxml", "Apex Trust Bank - Process Transactions"); }
    @FXML public void navToHistory(ActionEvent e) { Main.navigateTo("history.fxml", "Apex Trust Bank - Transaction Audit History"); }
    @FXML public void handleLogout(ActionEvent e) { authService.logout(); Main.navigateTo("login.fxml", "Apex Trust Bank - Secure Staff Login"); }
}
