package edu.staff.ui;

import edu.staff.model.PerformanceEvaluation;
import edu.staff.service.PerformanceEvaluationService;
import edu.facilities.service.AuthService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controller for recording performance evaluations (Admin only)
 * US 3.7 - Record Staff Performance
 */
public class RecordPerformanceEvaluationController implements Initializable {

    @FXML
    private ComboBox<Integer> staffUserIDComboBox;
    @FXML
    private TextField evaluationPeriodField;
    @FXML
    private TextField scoreField;
    @FXML
    private DatePicker evaluationDatePicker;
    @FXML
    private TextArea notesArea;
    @FXML
    private Button saveButton;
    @FXML
    private Button backButton;

    private PerformanceEvaluationService evaluationService;
    private AuthService authService;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        evaluationService = new PerformanceEvaluationService();
        authService = AuthService.getInstance();

        // Check admin access
        if (!checkAdminAccess()) {
            return;
        }

        loadStaffUserIDs();
    }

    private boolean checkAdminAccess() {
        if (authService == null || !authService.isLoggedIn()) {
            showError("Access Denied", "You must be logged in to access this page.");
            return false;
        }

        String userType = authService.getCurrentUserType();
        if (!"ADMIN".equals(userType)) {
            showError("Access Denied", "Only administrators can record performance evaluations.");
            return false;
        }
        return true;
    }

    private void loadStaffUserIDs() {
        try {
            List<Integer> staffIDs = evaluationService.getAllStaffUserIDs();
            ObservableList<Integer> staffList = FXCollections.observableArrayList(staffIDs);
            staffUserIDComboBox.setItems(staffList);
        } catch (SQLException e) {
            showError("Error", "Failed to load staff list: " + e.getMessage());
        }
    }

    @FXML
    private void handleSave(ActionEvent event) {
        if (!validateInput()) {
            return;
        }

        try {
            PerformanceEvaluation evaluation = new PerformanceEvaluation();
            evaluation.setStaffUserID(staffUserIDComboBox.getValue());
            evaluation.setEvaluationPeriod(evaluationPeriodField.getText().trim());
            evaluation.setScore(Double.parseDouble(scoreField.getText().trim()));
            
            if (evaluationDatePicker.getValue() != null) {
                evaluation.setEvaluationDate(evaluationDatePicker.getValue().atStartOfDay());
            } else {
                evaluation.setEvaluationDate(LocalDateTime.now());
            }
            
            evaluation.setNotes(notesArea.getText().trim());
            
            // Set the admin who recorded this evaluation
            if (authService.getCurrentUser() != null) {
                evaluation.setEvaluatedByUserID(Integer.parseInt(authService.getCurrentUser().getId()));
            }

            evaluationService.recordEvaluation(evaluation);
            showInfo("Success", "Performance evaluation recorded successfully.");
            clearForm();
        } catch (SQLException e) {
            showError("Error", "Failed to record evaluation: " + e.getMessage());
        } catch (NumberFormatException e) {
            showError("Validation Error", "Score must be a valid number.");
        }
    }

    @FXML
    private void handleBack(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/dashboard.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("");
            stage.setMaximized(true);
            stage.show();
        } catch (IOException e) {
            showError("Navigation Error", "Could not return to dashboard: " + e.getMessage());
        }
    }

    private boolean validateInput() {
        StringBuilder errors = new StringBuilder();

        if (staffUserIDComboBox.getValue() == null) {
            errors.append("Staff member is required.\n");
        }

        if (evaluationPeriodField.getText() == null || evaluationPeriodField.getText().trim().isEmpty()) {
            errors.append("Evaluation period is required.\n");
        }

        if (scoreField.getText() == null || scoreField.getText().trim().isEmpty()) {
            errors.append("Score is required.\n");
        } else {
            try {
                double score = Double.parseDouble(scoreField.getText().trim());
                if (score < 0 || score > 100) {
                    errors.append("Score must be between 0 and 100.\n");
                }
            } catch (NumberFormatException e) {
                errors.append("Score must be a valid number.\n");
            }
        }

        if (errors.length() > 0) {
            showError("Validation Error", "Please correct the following errors:\n" + errors.toString());
            return false;
        }

        return true;
    }

    private void clearForm() {
        staffUserIDComboBox.getSelectionModel().clearSelection();
        evaluationPeriodField.clear();
        scoreField.clear();
        evaluationDatePicker.setValue(null);
        notesArea.clear();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

