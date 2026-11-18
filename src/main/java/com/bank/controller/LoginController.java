package com.bank.controller;

import java.net.URL;
import java.util.ResourceBundle;

import com.bank.Main;
import com.bank.service.AuthenticationService;
import com.bank.service.ServiceFactory;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

/**
 * Controller for the Login screen.
 * Redesigned for Apex Trust Bank Enterprise Architecture.
 */
public class LoginController implements Initializable {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Button loginButton;

    @FXML
    private Label messageLabel;

    @FXML
    private Label modeBadgeLabel;

    private AuthenticationService authService;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.authService = ServiceFactory.getInstance().getAuthenticationService();

        boolean isDemo = ServiceFactory.getInstance().isDemoMode();
        if (isDemo) {
            modeBadgeLabel.setText("⚡ DEMO MODE (In-Memory Mock Active)");
        } else {
            modeBadgeLabel.setText("🔒 PRODUCTION MODE (MySQL Connection)");
        }

        clearMessage();
    }

    @FXML
public void handleLogin(ActionEvent event) {
    clearMessage();

    String username = usernameField.getText();
    String password = passwordField.getText();

    if (username == null || username.trim().isEmpty()) {
        showErrorMessage("Please enter your username.");
        return;
    }

    if (password == null || password.isEmpty()) {
        showErrorMessage("Please enter your password.");
        return;
    }

    boolean authenticated = authService.authenticate(
            username.trim(),
            password
    );

    if (authenticated) {
        Main.navigateTo(
                "dashboard.fxml",
                "Apex Trust Bank - Operational Dashboard"
        );
    } else {
        showErrorMessage("Invalid username or password.");
    }
}
   @FXML
public void handleBypass(ActionEvent event) {
    clearMessage();

    if (!authService.isDemoMode()) {
        showErrorMessage("Demo bypass is disabled in production mode.");
        return;
    }

    boolean authenticated = authService.authenticate(
            "Priya Nair (Branch Officer)",
            "bypass"
    );

    if (authenticated) {
        Main.navigateTo(
                "dashboard.fxml",
                "Apex Trust Bank - Operational Dashboard"
        );
    } else {
        showErrorMessage("Demo login failed.");
    }
}

    @FXML
    public void fillAdminCredentials(ActionEvent event) {
        usernameField.setText("admin");
        passwordField.setText("admin123");
        clearMessage();
    }

    @FXML
    public void fillKeerthanaCredentials(ActionEvent event) {
        usernameField.setText("priya");
        passwordField.setText("bank2026");
        clearMessage();
    }

    private void showErrorMessage(String msg) {
        messageLabel.setText(msg);
        messageLabel.getStyleClass().setAll("alert-box", "alert-danger");
        messageLabel.setVisible(true);
        messageLabel.setManaged(true);
    }

    private void clearMessage() {
        messageLabel.setText("");
        messageLabel.setVisible(false);
        messageLabel.setManaged(false);
    }
}
