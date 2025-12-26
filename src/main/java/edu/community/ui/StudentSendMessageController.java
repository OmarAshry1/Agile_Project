package edu.community.ui;

import edu.community.service.StudentStaffMessageService;
import edu.facilities.model.User;
import edu.facilities.service.AuthService;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controller for students to send messages to staff.
 * US 4.4 - Send Message to Staff
 */
public class StudentSendMessageController implements Initializable {

    @FXML private ComboBox<User> staffComboBox;
    @FXML private TextField subjectField;
    @FXML private TextArea messageBodyArea;
    @FXML private Button sendButton;
    @FXML private Button cancelButton;

    private StudentStaffMessageService messageService;
    private AuthService authService;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        messageService = new StudentStaffMessageService();
        authService = AuthService.getInstance();

        // Access control - only students
        if (!authService.isLoggedIn()) {
            showAlert(Alert.AlertType.ERROR, "Access Denied", "Please login to send messages.");
            return;
        }

        String userType = authService.getCurrentUserType();
        if (!"STUDENT".equals(userType)) {
            showAlert(Alert.AlertType.ERROR, "Access Denied", "Only students can use this feature.");
            return;
        }

        loadStaffMembers();
    }

    private void loadStaffMembers() {
        try {
            List<User> staff = messageService.getAllStaff();
            staffComboBox.setItems(FXCollections.observableArrayList(staff));

            staffComboBox.setCellFactory(param -> new ListCell<User>() {
                @Override
                protected void updateItem(User item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(item.getUsername() + " (" + item.getUserType() + ")");
                    }
                }
            });
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to load staff members: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleSend(ActionEvent event) {
        if (staffComboBox.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Please select a staff member.");
            return;
        }

        if (subjectField.getText() == null || subjectField.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Subject is required.");
            return;
        }

        if (messageBodyArea.getText() == null || messageBodyArea.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Message content is required.");
            return;
        }

        try {
            int studentUserID = Integer.parseInt(authService.getCurrentUser().getId());
            int staffUserID = Integer.parseInt(staffComboBox.getValue().getId());
            String subject = subjectField.getText().trim();
            String messageBody = messageBodyArea.getText().trim();

            boolean success = messageService.sendStudentMessage(
                studentUserID, staffUserID, subject, messageBody
            );

            if (success) {
                showAlert(Alert.AlertType.INFORMATION, "Success", "Message sent successfully!");
                navigateToDashboard(event);
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to send message.");
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to send message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleCancel(ActionEvent event) {
        navigateToDashboard(event);
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

