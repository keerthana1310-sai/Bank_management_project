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

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.net.URL;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Controller for Banking Reports & Financial Statements.
 * Generates customer account statements, monthly summaries, and CSV report exports.
 */
public class ReportsController implements Initializable {

    public static class ReportRow {
        private final String col1;
        private final String col2;
        private final String col3;
        private final String col4;
        private final String col5;
        private final String col6;

        public ReportRow(String c1, String c2, String c3, String c4, String c5, String c6) {
            this.col1 = c1; this.col2 = c2; this.col3 = c3;
            this.col4 = c4; this.col5 = c5; this.col6 = c6;
        }

        public String getCol1() { return col1; }
        public String getCol2() { return col2; }
        public String getCol3() { return col3; }
        public String getCol4() { return col4; }
        public String getCol5() { return col5; }
        public String getCol6() { return col6; }
    }

    // Sidebar & Header
    @FXML private Label operatorNameLabel;
    @FXML private Label demoBadgeLabel;
    @FXML private Label currentDateLabel;
    @FXML private Label statusFeedbackLabel;

    // Report Controls
    @FXML private ComboBox<String> reportTypeCombo;
    @FXML private ComboBox<String> accountSelectCombo;
    @FXML private ComboBox<String> periodSelectCombo;

    // Report Metrics
    @FXML private Label metricReportTitle;
    @FXML private Label metricRecordCount;
    @FXML private Label metricTotalVolume;
    @FXML private Label metricNetSummary;

    // Table
    @FXML private TableView<ReportRow> reportTable;
    @FXML private TableColumn<ReportRow, String> tCol1;
    @FXML private TableColumn<ReportRow, String> tCol2;
    @FXML private TableColumn<ReportRow, String> tCol3;
    @FXML private TableColumn<ReportRow, String> tCol4;
    @FXML private TableColumn<ReportRow, String> tCol5;
    @FXML private TableColumn<ReportRow, String> tCol6;

    private TransactionService transactionService;
    private AccountService accountService;
    private AuthenticationService authService;
    private final ObservableList<ReportRow> reportRows = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.transactionService = ServiceFactory.getInstance().getTransactionService();
        this.accountService = ServiceFactory.getInstance().getAccountService();
        this.authService = ServiceFactory.getInstance().getAuthenticationService();

        String currentUser = authService != null ? authService.getCurrentUser() : "Priya Nair (Branch Officer)";
        if (operatorNameLabel != null) operatorNameLabel.setText(currentUser);
        if (demoBadgeLabel != null) demoBadgeLabel.setText(ServiceFactory.getInstance().isDemoMode() ? "⚡ In-Memory Demo" : "🔒 Live MySQL");
        if (currentDateLabel != null) currentDateLabel.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy")));

        reportTypeCombo.setItems(FXCollections.observableArrayList(
                "Customer Account Statement",
                "Monthly Transaction Summary",
                "Deposit Operations Report",
                "Withdrawal Operations Report",
                "Account Registration Directory Report"
        ));
        reportTypeCombo.setValue("Customer Account Statement");

        periodSelectCombo.setItems(FXCollections.observableArrayList("All Historical Records", "Current Month (Nov 2026)", "Last 30 Days"));
        periodSelectCombo.setValue("All Historical Records");

        // Load account numbers into accountSelectCombo
        try {
            List<Account> accs = accountService.getAllAccounts();
            ObservableList<String> accNums = FXCollections.observableArrayList();
            for (Account a : accs) {
                accNums.add(a.getAccountNumber() + " - " + a.getCustomerName());
            }
            accountSelectCombo.setItems(accNums);
            if (!accNums.isEmpty()) accountSelectCombo.setValue(accNums.get(0));
        } catch (Exception ignored) {}

        setupTable();
        handleGenerateReport(null);
    }

    private void setupTable() {
        if (reportTable == null) return;
        tCol1.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getCol1()));
        tCol2.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getCol2()));
        tCol3.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getCol3()));
        tCol4.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getCol4()));
        tCol5.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getCol5()));
        tCol6.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getCol6()));
        reportTable.setItems(reportRows);
    }

    @FXML
    public void handleGenerateReport(ActionEvent event) {
        reportRows.clear();
        String selectedType = reportTypeCombo.getValue();
        if (selectedType == null) selectedType = "Customer Account Statement";

        try {
            if ("Customer Account Statement".equalsIgnoreCase(selectedType)) {
                tCol1.setText("Tx ID");
                tCol2.setText("Date & Time");
                tCol3.setText("Type");
                tCol4.setText("Amount");
                tCol5.setText("Balance After");
                tCol6.setText("Description");

                String selectedAccFull = accountSelectCombo.getValue();
                String accNum = (selectedAccFull != null && selectedAccFull.contains(" - ")) 
                        ? selectedAccFull.split(" - ")[0].trim() : "ACC-1001";

                List<Transaction> txs = transactionService.getTransactionHistory(accNum);
                BigDecimal totalVol = BigDecimal.ZERO;

                for (Transaction tx : txs) {
                    reportRows.add(new ReportRow(
                            "#" + tx.getTransactionId(),
                            tx.getFormattedDate(),
                            tx.getTransactionType(),
                            tx.getFormattedAmount(),
                            tx.getFormattedBalanceAfter(),
                            tx.getDescription()
                    ));
                    if (tx.getAmount() != null) totalVol = totalVol.add(tx.getAmount());
                }

                metricReportTitle.setText("Account Statement: " + accNum);
                metricRecordCount.setText(String.valueOf(txs.size()) + " Entries");
                metricTotalVolume.setText(formatIndianCurrency(totalVol));
                metricNetSummary.setText("Statement Verified");

            } else if ("Deposit Operations Report".equalsIgnoreCase(selectedType)) {
                tCol1.setText("Tx ID");
                tCol2.setText("Date & Time");
                tCol3.setText("Source Account");
                tCol4.setText("Amount");
                tCol5.setText("Balance After");
                tCol6.setText("Memo");

                List<Transaction> txs = transactionService.getAllTransactions();
                BigDecimal totalVol = BigDecimal.ZERO;
                int count = 0;

                for (Transaction tx : txs) {
                    if ("DEPOSIT".equalsIgnoreCase(tx.getTransactionType())) {
                        reportRows.add(new ReportRow(
                                "#" + tx.getTransactionId(),
                                tx.getFormattedDate(),
                                tx.getSourceAccount(),
                                tx.getFormattedAmount(),
                                tx.getFormattedBalanceAfter(),
                                tx.getDescription()
                        ));
                        if (tx.getAmount() != null) totalVol = totalVol.add(tx.getAmount());
                        count++;
                    }
                }

                metricReportTitle.setText("Deposit Operations Ledger");
                metricRecordCount.setText(count + " Deposits");
                metricTotalVolume.setText(formatIndianCurrency(totalVol));
                metricNetSummary.setText("Total Credits Inflow");

            } else if ("Withdrawal Operations Report".equalsIgnoreCase(selectedType)) {
                tCol1.setText("Tx ID");
                tCol2.setText("Date & Time");
                tCol3.setText("Source Account");
                tCol4.setText("Amount");
                tCol5.setText("Balance After");
                tCol6.setText("Memo");

                List<Transaction> txs = transactionService.getAllTransactions();
                BigDecimal totalVol = BigDecimal.ZERO;
                int count = 0;

                for (Transaction tx : txs) {
                    if ("WITHDRAWAL".equalsIgnoreCase(tx.getTransactionType())) {
                        reportRows.add(new ReportRow(
                                "#" + tx.getTransactionId(),
                                tx.getFormattedDate(),
                                tx.getSourceAccount(),
                                tx.getFormattedAmount(),
                                tx.getFormattedBalanceAfter(),
                                tx.getDescription()
                        ));
                        if (tx.getAmount() != null) totalVol = totalVol.add(tx.getAmount());
                        count++;
                    }
                }

                metricReportTitle.setText("Withdrawal Operations Ledger");
                metricRecordCount.setText(count + " Withdrawals");
                metricTotalVolume.setText(formatIndianCurrency(totalVol));
                metricNetSummary.setText("Total Debits Outflow");

            } else {
                // Account Registration Directory Report
                tCol1.setText("Account #");
                tCol2.setText("Customer Name");
                tCol3.setText("Account Type");
                tCol4.setText("Balance");
                tCol5.setText("Status");
                tCol6.setText("Opening Date");

                List<Account> accounts = accountService.getAllAccounts();
                BigDecimal totalBal = BigDecimal.ZERO;

                for (Account a : accounts) {
                    reportRows.add(new ReportRow(
                            a.getAccountNumber(),
                            a.getCustomerName(),
                            a.getAccountType(),
                            a.getFormattedBalance(),
                            a.getStatus(),
                            a.getFormattedDate()
                    ));
                    if (a.getBalance() != null) totalBal = totalBal.add(a.getBalance());
                }

                metricReportTitle.setText("Customer Account Directory Report");
                metricRecordCount.setText(accounts.size() + " Accounts");
                metricTotalVolume.setText(formatIndianCurrency(totalBal));
                metricNetSummary.setText("Total Vault Liquidity");
            }
        } catch (Exception e) {
            System.err.println("[ReportsController] Error generating report: " + e.getMessage());
        }
    }

    @FXML
    public void handleExportCSV(ActionEvent event) {
        try {
            String filename = "apex_bank_report_" + System.currentTimeMillis() % 100000 + ".csv";
            File file = new File(filename);
            try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
                writer.printf("%s,%s,%s,%s,%s,%s%n",
                        tCol1.getText(), tCol2.getText(), tCol3.getText(),
                        tCol4.getText(), tCol5.getText(), tCol6.getText());

                for (ReportRow row : reportRows) {
                    writer.printf("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"%n",
                            row.getCol1(), row.getCol2(), row.getCol3(),
                            row.getCol4(), row.getCol5(), row.getCol6());
                }
            }

            if (statusFeedbackLabel != null) {
                statusFeedbackLabel.setText("✓ Report successfully exported to " + file.getName() + " (" + reportRows.size() + " rows)");
                statusFeedbackLabel.getStyleClass().setAll("alert-box", "alert-success");
                statusFeedbackLabel.setVisible(true);
                statusFeedbackLabel.setManaged(true);
            }
        } catch (Exception e) {
            if (statusFeedbackLabel != null) {
                statusFeedbackLabel.setText("⚠️ Failed to export report: " + e.getMessage());
                statusFeedbackLabel.getStyleClass().setAll("alert-box", "alert-danger");
                statusFeedbackLabel.setVisible(true);
                statusFeedbackLabel.setManaged(true);
            }
        }
    }

    private String formatIndianCurrency(BigDecimal amount) {
        if (amount == null) return "₹0.00";
        NumberFormat nf = NumberFormat.getNumberInstance(new Locale("en", "IN"));
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        return "₹" + nf.format(amount);
    }

    // Navigation handlers
    @FXML public void navToDashboard(ActionEvent e) { Main.navigateTo("dashboard.fxml", "Apex Trust Bank - Operational Dashboard"); }
    @FXML public void navToAccounts(ActionEvent e) { Main.navigateTo("account.fxml", "Apex Trust Bank - Account Management"); }
    @FXML public void navToTransactions(ActionEvent e) { Main.navigateTo("transaction.fxml", "Apex Trust Bank - Process Transactions"); }
    @FXML public void navToTransfers(ActionEvent e) { Main.navigateTo("transfer.fxml", "Apex Trust Bank - Fund Transfers & Beneficiaries"); }
    @FXML public void navToHistory(ActionEvent e) { Main.navigateTo("history.fxml", "Apex Trust Bank - Transaction Audit History"); }
    @FXML public void navToReports(ActionEvent e) { /* already on page */ }
    @FXML public void navToNotifications(ActionEvent e) { Main.navigateTo("notifications.fxml", "Apex Trust Bank - Notification Centre"); }
    @FXML public void navToSecurity(ActionEvent e) { Main.navigateTo("security.fxml", "Apex Trust Bank - Security Overview"); }
    @FXML public void handleLogout(ActionEvent e) { authService.logout(); Main.navigateTo("login.fxml", "Apex Trust Bank - Secure Staff Login"); }
}

