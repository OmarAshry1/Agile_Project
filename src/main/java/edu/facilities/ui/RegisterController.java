package edu.facilities.ui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;

import edu.facilities.service.AuthService;


public class RegisterController {

    @FXML private TextField emailField;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private ComboBox<String> roleComboBox;
    @FXML private Hyperlink loginLink;

    @FXML private Label emailError;
    @FXML private Label usernameError;
    @FXML private Label passwordError;
    @FXML private Label confirmPasswordError;
    @FXML private Label roleError;

    @FXML
    private void initialize() {
        if (roleComboBox != null && roleComboBox.getItems().isEmpty()) {
            roleComboBox.getItems().addAll("Student", "Professor", "Staff", "Admin");
        }
    }

    @FXML
    private void handleRegister(ActionEvent event) {
        clearErrors();

        boolean hasErrors = false;

        if (isBlank(emailField)) {
            showFieldError(emailError, "Email is required");
            hasErrors = true;
        }
        if (isBlank(usernameField)) {
            showFieldError(usernameError, "Username is required");
            hasErrors = true;
        }
        if (isBlank(passwordField)) {
            showFieldError(passwordError, "Password is required");
            hasErrors = true;
        }
        if (isBlank(confirmPasswordField)) {
            showFieldError(confirmPasswordError, "Please confirm the password");
            hasErrors = true;
        } else if (!passwordField.getText().equals(confirmPasswordField.getText())) {
            showFieldError(confirmPasswordError, "Passwords do not match");
            hasErrors = true;
        }
        if (roleComboBox == null || roleComboBox.getValue() == null) {
            showFieldError(roleError, "Select a role");
            hasErrors = true;
        }

        if (hasErrors) {
            return;
        }


        AuthService authService = AuthService.getInstance();
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        String email = emailField.getText().trim();
        String role = roleComboBox.getValue();
        

        String userType = mapRoleToUserType(role);

        try {
            boolean success = authService.register(username, password, email, userType);
            
            if (success) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Registration Successful");
                alert.setHeaderText("Account Created");
                alert.setContentText("Your account has been created successfully. Please login.");
                alert.showAndWait();
                

                try {
                    login(null);
                } catch (IOException ioException) {
                    // Should not happen, but handle it
                    ioException.printStackTrace();
                }
            } else {
                showFieldError(usernameError, "Username already exists");
            }
        } catch (SQLException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Database Error");
            alert.setHeaderText("Failed to register user");
            alert.setContentText("Please check your database connection: " + e.getMessage());
            alert.showAndWait();
            e.printStackTrace();
        }
    }

    @FXML
    private void selectrole(ActionEvent event) {
        if (roleComboBox != null) {
            System.out.println("Role selected: " + roleComboBox.getValue());
        }
    }

    @FXML
    private void login(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/login.fxml"));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    private boolean isBlank(TextField field) {
        return field == null || field.getText() == null || field.getText().isBlank();
    }

    private void showFieldError(Label label, String message) {
        if (label != null) {
            label.setText(message);
            label.setVisible(true);
        }
    }

    private void clearErrors() {
        Arrays.asList(emailError, usernameError, passwordError, confirmPasswordError, roleError)
                .forEach(label -> {
                    if (label != null) {
                        label.setText("");
                        label.setVisible(false);
                    }
                });
    }


    private String mapRoleToUserType(String role) {
        if (role == null) return "STUDENT";
        switch (role.toLowerCase()) {
            case "student": return "STUDENT";
            case "professor": return "PROFESSOR";
            case "staff": return "STAFF";
            case "admin": return "ADMIN";
            default: return "STUDENT";
        }
    }
}
