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
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.math.BigDecimal;
import java.net.URL;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Controller for the Executive Dashboard view.
 * Displays KPI summary cards, financial analytics charts, and recent transaction ledger.
 * 
 * Redesigned for Apex Trust Bank Enterprise Architecture.
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
    private TextField globalSearchField;

    @FXML
    private Label notificationBadgeLabel;

    // KPI Cards
    @FXML
    private Label kpiAccountsLabel;

    @FXML
    private Label kpiDepositsLabel;

    @FXML
    private Label kpiTransactionsLabel;

    @FXML
    private Label kpiActiveAccountsLabel;

    @FXML
    private Label kpiRepoModeLabel;

    // Charts
    @FXML
    private BarChart<String, Number> monthlyTxChart;

    @FXML
    private CategoryAxis monthlyChartXAxis;

    @FXML
    private NumberAxis monthlyChartYAxis;

    @FXML
    private PieChart txTypePieChart;

    @FXML
    private PieChart accountTypePieChart;

    @FXML
    private PieChart accountStatusPieChart;

    // Recent Transactions Table
    @FXML
    private TableView<Transaction> recentTransactionsTable;

    @FXML
    private TableColumn<Transaction, String> colId;

    @FXML
    private TableColumn<Transaction, String> colCustomer;

    @FXML
    private TableColumn<Transaction, String> colType;

    @FXML
    private TableColumn<Transaction, String> colSource;

    @FXML
    private TableColumn<Transaction, String> colDest;

    @FXML
    private TableColumn<Transaction, String> colAmount;

    @FXML
    private TableColumn<Transaction, String> colStatus;

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

            // Populate User Info & Time-aware Greeting
            String currentUser = authService != null ? authService.getCurrentUser() : "Priya Nair (Branch Officer)";
            if (currentUser == null || currentUser.isBlank()) {
                currentUser = "Priya Nair (Branch Officer)";
            }

            String greeting = "Good Morning";
            int hour = LocalTime.now().getHour();
            if (hour >= 12 && hour < 17) {
                greeting = "Good Afternoon";
            } else if (hour >= 17) {
                greeting = "Good Evening";
            }

            if (welcomeLabel != null) {
                welcomeLabel.setText(greeting + ", Administrator • Core Banking Operations");
            }
            if (operatorNameLabel != null) {
                operatorNameLabel.setText(currentUser);
            }

            // Consistent Date Display: DD MMM YYYY
            if (currentDateLabel != null) {
                currentDateLabel.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy")));
            }

            // Demo Badge
            boolean isDemo = ServiceFactory.getInstance().isDemoMode();
            if (demoBadgeLabel != null) {
                demoBadgeLabel.setText(isDemo ? "⚡ In-Memory Demo" : "🔒 Live MySQL");
            }
            if (kpiRepoModeLabel != null) {
                kpiRepoModeLabel.setText(isDemo ? "In-Memory" : "MySQL JDBC");
            }

            // Configure Recent Transactions Table
            setupRecentTable();

            // Load Live Metrics and Charts
            refreshDashboardData();
        } catch (Exception e) {
            System.err.println("[DashboardController] Error initializing dashboard: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void setupRecentTable() {
        if (recentTransactionsTable == null) return;

        colId.setCellValueFactory(cellData -> new SimpleStringProperty("#" + cellData.getValue().getTransactionId()));
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
        colDescription.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDescription()));
        colDate.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getFormattedDate()));

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

        // Status pill badge for Transaction Type
        colType.setCellFactory(col -> new TableCell<Transaction, String>() {
            @Override
            protected void updateItem(String type, boolean empty) {
                super.updateItem(type, empty);
                if (empty || type == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label badge = new Label();
                    badge.setText(type.toUpperCase());
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

    private void refreshDashboardData() {
        try {
            // 1. Total Registered Accounts KPI
            int totalAccounts = accountService.getTotalAccountsCount();
            if (kpiAccountsLabel != null) {
                kpiAccountsLabel.setText(String.valueOf(totalAccounts));
            }

            // 2. Total Bank Balance KPI (in Indian Rupees ₹)
            BigDecimal totalBalance = accountService.getTotalBalanceSum();
            if (kpiDepositsLabel != null) {
                kpiDepositsLabel.setText(formatIndianCurrency(totalBalance));
            }

            // 3. Total Transactions KPI
            int totalTx = transactionService.getTotalTransactionsCount();
            if (kpiTransactionsLabel != null) {
                kpiTransactionsLabel.setText(String.valueOf(totalTx));
            }

            // 4. Active Accounts KPI
            List<Account> allAccounts = accountService.getAllAccounts();
            long activeCount = allAccounts.stream()
                    .filter(a -> "ACTIVE".equalsIgnoreCase(a.getStatus()))
                    .count();
            if (kpiActiveAccountsLabel != null) {
                kpiActiveAccountsLabel.setText(String.valueOf(activeCount));
            }

            if (kpiRepoModeLabel != null) {
                kpiRepoModeLabel.setText(ServiceFactory.getInstance().isDemoMode() ? "In-Memory" : "MySQL JDBC");
            }

            // 5. Load recent transactions (first 5)
            List<Transaction> allTx = transactionService.getAllTransactions();
            List<Transaction> recent = allTx.subList(0, Math.min(5, allTx.size()));
            ObservableList<Transaction> observableList = FXCollections.observableArrayList(recent);
            if (recentTransactionsTable != null) {
                recentTransactionsTable.setItems(observableList);
            }

            // 6. Populate Charts
            populateCharts(allTx, allAccounts);

        } catch (BankingException e) {
            System.err.println("[DashboardController] Failed to load statistics: " + e.getMessage());
        }
    }

    private void populateCharts(List<Transaction> allTx, List<Account> allAccounts) {
        // A. Monthly Deposit and Withdrawal Comparison (BarChart)
        if (monthlyTxChart != null) {
            monthlyTxChart.getData().clear();

            XYChart.Series<String, Number> depositSeries = new XYChart.Series<>();
            depositSeries.setName("Deposits (₹)");

            XYChart.Series<String, Number> withdrawalSeries = new XYChart.Series<>();
            withdrawalSeries.setName("Withdrawals (₹)");

            Map<YearMonth, BigDecimal> monthlyDeposits = new TreeMap<>();
            Map<YearMonth, BigDecimal> monthlyWithdrawals = new TreeMap<>();

            for (Transaction tx : allTx) {
                if (tx.getTimestamp() != null && tx.getAmount() != null) {
                    YearMonth ym = YearMonth.from(tx.getTimestamp());
                    if ("DEPOSIT".equalsIgnoreCase(tx.getTransactionType())) {
                        monthlyDeposits.merge(ym, tx.getAmount(), BigDecimal::add);
                    } else if ("WITHDRAWAL".equalsIgnoreCase(tx.getTransactionType())) {
                        monthlyWithdrawals.merge(ym, tx.getAmount(), BigDecimal::add);
                    }
                }
            }

            Set<YearMonth> allMonths = new TreeSet<>(monthlyDeposits.keySet());
            allMonths.addAll(monthlyWithdrawals.keySet());

            DateTimeFormatter monthFmt = DateTimeFormatter.ofPattern("MMM yyyy");
            // Populate chronological months
            for (YearMonth ym : allMonths) {
                String label = ym.format(monthFmt);
                BigDecimal d = monthlyDeposits.getOrDefault(ym, BigDecimal.ZERO);
                BigDecimal w = monthlyWithdrawals.getOrDefault(ym, BigDecimal.ZERO);
                depositSeries.getData().add(new XYChart.Data<>(label, d.doubleValue()));
                withdrawalSeries.getData().add(new XYChart.Data<>(label, w.doubleValue()));
            }

            monthlyTxChart.getData().addAll(depositSeries, withdrawalSeries);
        }

        // B. Account Type Distribution (PieChart: Savings vs Current)
        if (accountTypePieChart != null) {
            accountTypePieChart.getData().clear();
            long savingsCount = allAccounts.stream().filter(a -> "SAVINGS".equalsIgnoreCase(a.getAccountType())).count();
            long currentCount = allAccounts.stream().filter(a -> "CURRENT".equalsIgnoreCase(a.getAccountType())).count();

            if (savingsCount > 0) {
                accountTypePieChart.getData().add(new PieChart.Data("Savings (" + savingsCount + ")", savingsCount));
            }
            if (currentCount > 0) {
                accountTypePieChart.getData().add(new PieChart.Data("Current (" + currentCount + ")", currentCount));
            }
        }

        // D. Account Status Distribution (PieChart)
        if (accountStatusPieChart != null) {
            accountStatusPieChart.getData().clear();
            long activeAccounts = 0;
            long inactiveAccounts = 0;
            long closedAccounts = 0;

            for (Account acc : allAccounts) {
                String st = acc.getStatus() != null ? acc.getStatus().toUpperCase() : "ACTIVE";
                switch (st) {
                    case "ACTIVE" -> activeAccounts++;
                    case "INACTIVE" -> inactiveAccounts++;
                    case "CLOSED" -> closedAccounts++;
                    default -> activeAccounts++;
                }
            }

            if (activeAccounts > 0) {
                accountStatusPieChart.getData().add(new PieChart.Data("Active (" + activeAccounts + ")", activeAccounts));
            }
            if (inactiveAccounts > 0) {
                accountStatusPieChart.getData().add(new PieChart.Data("Inactive (" + inactiveAccounts + ")", inactiveAccounts));
            }
            if (closedAccounts > 0) {
                accountStatusPieChart.getData().add(new PieChart.Data("Closed (" + closedAccounts + ")", closedAccounts));
            }
        }

        // Transaction Type PieChart (if present)
        if (txTypePieChart != null) {
            txTypePieChart.getData().clear();
            BigDecimal depositSum = BigDecimal.ZERO;
            BigDecimal withdrawalSum = BigDecimal.ZERO;
            BigDecimal transferSum = BigDecimal.ZERO;

            for (Transaction tx : allTx) {
                if (tx.getAmount() == null) continue;
                String type = tx.getTransactionType() != null ? tx.getTransactionType().toUpperCase() : "DEPOSIT";
                switch (type) {
                    case "DEPOSIT" -> depositSum = depositSum.add(tx.getAmount());
                    case "WITHDRAWAL" -> withdrawalSum = withdrawalSum.add(tx.getAmount());
                    case "TRANSFER" -> transferSum = transferSum.add(tx.getAmount());
                }
            }

            if (depositSum.compareTo(BigDecimal.ZERO) > 0) {
                txTypePieChart.getData().add(new PieChart.Data("Deposits", depositSum.doubleValue()));
            }
            if (withdrawalSum.compareTo(BigDecimal.ZERO) > 0) {
                txTypePieChart.getData().add(new PieChart.Data("Withdrawals", withdrawalSum.doubleValue()));
            }
            if (transferSum.compareTo(BigDecimal.ZERO) > 0) {
                txTypePieChart.getData().add(new PieChart.Data("Transfers", transferSum.doubleValue()));
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

    @FXML
    public void handleGlobalSearch(ActionEvent event) {
        String query = globalSearchField != null ? globalSearchField.getText() : null;
        if (query != null && !query.isBlank()) {
            Main.navigateTo("account.fxml", "Apex Trust Bank - Account Management");
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
    public void navToTransfers(ActionEvent event) {
        Main.navigateTo("transfer.fxml", "Apex Trust Bank - Fund Transfers & Beneficiaries");
    }

    @FXML
    public void navToHistory(ActionEvent event) {
        Main.navigateTo("history.fxml", "Apex Trust Bank - Transaction Audit History");
    }

    @FXML
    public void navToReports(ActionEvent event) {
        Main.navigateTo("reports.fxml", "Apex Trust Bank - Reports & Statements");
    }

    @FXML
    public void navToNotifications(ActionEvent event) {
        Main.navigateTo("notifications.fxml", "Apex Trust Bank - Notification Centre");
    }

    @FXML
    public void navToSecurity(ActionEvent event) {
        Main.navigateTo("security.fxml", "Apex Trust Bank - Security Overview");
    }

    @FXML
    public void handleLogout(ActionEvent event) {
        authService.logout();
        Main.navigateTo("login.fxml", "Apex Trust Bank - Secure Staff Login");
    }
}
