package com.bank;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ViewLoadingTest {

    @BeforeAll
    public static void initJFX() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        try {
            Platform.startup(latch::countDown);
        } catch (IllegalStateException alreadyStarted) {
            latch.countDown();
        }
        assertTrue(latch.await(5, TimeUnit.SECONDS), "JavaFX failed to start");
    }

    private void testFxmlLoad(String fxmlName) throws Exception {
        URL resource = getClass().getResource("/com/bank/" + fxmlName);
        assertNotNull(resource, "Cannot locate /com/bank/" + fxmlName);

        CountDownLatch latch = new CountDownLatch(1);
        final Throwable[] error = new Throwable[1];

        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(resource);
                Parent root = loader.load();
                assertNotNull(root, "Root element was null for " + fxmlName);
            } catch (Throwable t) {
                error[0] = t;
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS), "Loading timed out for " + fxmlName);
        if (error[0] != null) {
            throw new AssertionError("Failed to load " + fxmlName, error[0]);
        }
    }

    @Test
    public void testDashboardLoads() throws Exception {
        testFxmlLoad("dashboard.fxml");
    }

    @Test
    public void testAccountLoads() throws Exception {
        testFxmlLoad("account.fxml");
    }

    @Test
    public void testTransactionLoads() throws Exception {
        testFxmlLoad("transaction.fxml");
    }

    @Test
    public void testHistoryLoads() throws Exception {
        testFxmlLoad("history.fxml");
    }

    @Test
    public void testLoginLoads() throws Exception {
        testFxmlLoad("login.fxml");
    }
}
