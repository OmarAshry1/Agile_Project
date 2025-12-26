package edu.community.ui;

import edu.community.service.ParentTeacherMessageService;
import edu.facilities.service.AuthService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

/**
 * Controller for parents to link their child (student) to their account.
 * Allows parents to self-register their relationship with their child.
 */
public class LinkChildController implements Initializable {

    @FXML private TextField studentUsernameField;
    @FXML private Label errorLabel;
    @FXML private Button linkButton;
    @FXML private Button cancelButton;

    private ParentTeacherMessageService messageService;
    private AuthService authService;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        messageService = new ParentTeacherMessageService();
        authService = AuthService.getInstance();

        // Access control - only parents
        if (!authService.isLoggedIn()) {
            showAlert(Alert.AlertType.ERROR, "Access Denied", "Please login to link your child.");
            return;
        }

        String userType = authService.getCurrentUserType();
        if (!"PARENT".equals(userType)) {
            showAlert(Alert.AlertType.ERROR, "Access Denied", 
                "Only parents can link students. Your user type: " + 
                (userType != null ? userType : "Unknown"));
            return;
        }

        // Clear error label initially
        if (errorLabel != null) {
            errorLabel.setVisible(false);
        }
    }

    @FXML
    private void handleLink(ActionEvent event) {
        if (errorLabel != null) {
            errorLabel.setVisible(false);
            errorLabel.setText("");
        }

        String studentUsernameOrID = studentUsernameField.getText();
        if (studentUsernameOrID == null || studentUsernameOrID.trim().isEmpty()) {
            showError("Please enter your child's username or student ID.");
            return;
        }

        try {
            int parentUserID = Integer.parseInt(authService.getCurrentUser().getId());
            boolean success = messageService.linkStudentToParent(parentUserID, studentUsernameOrID.trim());

            if (success) {
                showAlert(Alert.AlertType.INFORMATION, "Success", 
                    "Successfully linked your child to your account! " +
                    "You can now send messages to their teachers.");
                
                // Navigate back to dashboard after a short delay
                navigateToDashboard(event);
            } else {
                showError("Failed to link student. Please verify:\n" +
                         "• The username or student ID is correct\n" +
                         "• The student exists in the system\n" +
                         "• The student is not already linked to your account");
            }
        } catch (SQLException e) {
            showError("Database error: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            showError("An error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleCancel(ActionEvent event) {
        navigateToDashboard(event);
    }

    private void showError(String message) {
        if (errorLabel != null) {
            errorLabel.setText(message);
            errorLabel.setVisible(true);
        } else {
            showAlert(Alert.AlertType.ERROR, "Error", message);
        }
    }

    private void navigateToDashboard(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/dashboard.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("");
            stage.setMaximized(true);
            stage.show();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Navigation Error", "Could not load dashboard: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

