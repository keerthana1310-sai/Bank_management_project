package com.bank;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

/**
 * Main Application Entry Point for Bank Management System.
 * Author: Keerthana (Frontend Lead)
 */
public class Main extends Application {

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        primaryStage.setMinWidth(1024);
        primaryStage.setMinHeight(720);
        
        // Direct bypass to Dashboard
        navigateTo("dashboard.fxml", "Apex Trust Bank - Operational Dashboard");
    }

    /**
     * Centralized navigation method between views.
     * 
     * @param fxmlName Filename such as "dashboard.fxml", "account.fxml", etc.
     * @param title    Window title bar text
     */
    public static void navigateTo(String fxmlName, String title) {
        try {
            String path = fxmlName.startsWith("/") ? fxmlName : "/com/bank/" + fxmlName;
            URL resource = Main.class.getResource(path);
            if (resource == null) {
                System.err.println("[Main] Cannot find FXML resource at: " + path);
                return;
            }

            FXMLLoader loader = new FXMLLoader(resource);
            Parent root = loader.load();

            Scene scene = primaryStage.getScene();
            if (scene == null) {
                scene = new Scene(root, 1150, 760);
                primaryStage.setScene(scene);
            } else {
                scene.setRoot(root);
            }

            primaryStage.setTitle(title);
            primaryStage.show();
        } catch (IOException e) {
            System.err.println("[Main] Error navigating to " + fxmlName + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
