package edu.facilities.ui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Controller for Student Enrollment Page
 * Redirects to Course Catalog for enrollment
 * US 2.3 - Enroll in Courses
 */
public class StudentEnrollmentController {

    @FXML
    private Button backButton;

    @FXML
    public void initialize() {
        // Auto-redirect to course catalog
        navigateToCatalog();
    }

    @FXML
    private void handleGoToCatalog(ActionEvent event) {
        navigateToCatalog();
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

    private void navigateToCatalog() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/student_course_catalog.fxml"));
            Stage stage;
            if (backButton != null && backButton.getScene() != null) {
                stage = (Stage) backButton.getScene().getWindow();
            } else {
                stage = new Stage();
            }
            stage.setScene(new Scene(root));
            stage.setTitle("");
            stage.setMaximized(true);
            stage.show();
        } catch (IOException e) {
            showError("Navigation Error", "Could not open course catalog: " + e.getMessage());
        }
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
