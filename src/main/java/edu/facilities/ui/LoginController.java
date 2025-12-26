package edu.facilities.ui;

import edu.facilities.model.User;
import edu.facilities.service.AuthService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;

public class LoginController {

    @FXML
    private Button loginbutton;

    @FXML
    private Label passwordError;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Hyperlink registerLink;

    @FXML
    private Label usernameError;

    @FXML
    private TextField usernameField;

    @FXML
    void initialize() {
        // Show demo credentials if demo mode is enabled
        // Check if demo mode is enabled by trying to access the constant via reflection
        try {
            java.lang.reflect.Field demoModeField = edu.facilities.service.AuthService.class.getDeclaredField("DEMO_MODE");
            demoModeField.setAccessible(true);
            boolean demoMode = demoModeField.getBoolean(null);
            
            if (demoMode) {
                // Print demo credentials to console
                System.out.println("========================================");
                System.out.println("DEMO MODE ENABLED");
                System.out.println("Demo Credentials:");
                System.out.println("  Student:    student / student123");
                System.out.println("  Professor:  professor / professor123");
                System.out.println("  Admin:      admin / admin");
                System.out.println("  Staff:      staff / staff123");
                System.out.println("========================================");
            }
        } catch (Exception e) {
            // If reflection fails, just continue normally
            System.out.println("Could not check demo mode status");
        }
    }

    @FXML
    void handleLogin(ActionEvent event) {
        clearErrors();

        boolean hasErrors = false;

        if (usernameField == null || usernameField.getText().isBlank()) {
            usernameField.setText("ID is required");
            usernameField.setVisible(true);
            hasErrors = true;
        }

        if (passwordField == null || passwordField.getText().isBlank()) {
            passwordError.setText("Password is required");
            passwordError.setVisible(true);
            hasErrors = true;
        }

        if (hasErrors) {
            return;
        }


        AuthService authService = AuthService.getInstance();
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        try {
            User authenticatedUser = authService.login(username, password);
            
            if (authenticatedUser != null) {

                try {
                    // Use Main.class to ensure consistent resource loading from classpath root
                    FXMLLoader loader = new FXMLLoader(edu.facilities.Main.class.getResource("/fxml/dashboard.fxml"));
                    Parent root = loader.load();
                    Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                    stage.setScene(new Scene(root));
                    stage.setTitle("University Management System - Dashboard");
                    stage.setMaximized(true);
                    stage.show();
                } catch (IOException e) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Navigation Error");
                    alert.setHeaderText("Unable to load dashboard");
                    alert.setContentText("Error loading dashboard.fxml: " + e.getMessage() + 
                                        "\n\nPlease ensure the file exists at: src/main/resources/fxml/dashboard.fxml");
                    alert.showAndWait();
                    e.printStackTrace();
                } catch (NullPointerException e) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Navigation Error");
                    alert.setHeaderText("Resource not found");
                    alert.setContentText("dashboard.fxml not found in classpath.\n" +
                                        "Expected location: src/main/resources/fxml/dashboard.fxml");
                    alert.showAndWait();
                    e.printStackTrace();
                }
            } else {
                // Login failed
                passwordError.setText("Invalid username or password");
                passwordError.setVisible(true);
            }
        } catch (SQLException e) {
            // Only show database error if not in demo mode
            // In demo mode, SQLException shouldn't occur, but handle it gracefully
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Database Error");
            alert.setHeaderText("Failed to connect to database");
            alert.setContentText("Please check your database connection: " + e.getMessage() + 
                                "\n\nNote: If you're using demo mode, this error shouldn't occur.");
            alert.showAndWait();
            e.printStackTrace();
        }
    }

    @FXML
    void register(ActionEvent event) throws IOException {
        try {
            FXMLLoader loader = new FXMLLoader(edu.facilities.Main.class.getResource("/fxml/register.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            // Preserve maximized state
            boolean wasMaximized = stage.isMaximized();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("University Management System - Register");
            // Always keep window maximized
            stage.setMaximized(true);
            stage.show();
            // Double-check maximized state after showing to prevent resize
            javafx.application.Platform.runLater(() -> {
                if (wasMaximized) {
                    stage.setMaximized(true);
                }
            });
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Navigation Error");
            alert.setHeaderText("Unable to load registration page");
            alert.setContentText("Error loading register.fxml: " + e.getMessage());
            alert.showAndWait();
            e.printStackTrace();
        } catch (NullPointerException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Navigation Error");
            alert.setHeaderText("Resource not found");
            alert.setContentText("register.fxml not found in classpath.\n" +
                                "Expected location: src/main/resources/fxml/register.fxml");
            alert.showAndWait();
            e.printStackTrace();
        }
    }

    private void clearErrors() {
        if (usernameError != null) {
            usernameError.setText("");
            usernameError.setVisible(false);
        }
        if (passwordError != null) {
            passwordError.setText("");
            passwordError.setVisible(false);
        }
    }

}
