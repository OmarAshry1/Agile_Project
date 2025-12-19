package edu.curriculum.ui;

import edu.curriculum.model.Assignment;
import edu.curriculum.model.AssignmentSubmission;
import edu.curriculum.service.SubmissionService;
import edu.facilities.model.User;
import edu.facilities.service.AuthService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;

/**
 * Controller for Submit Assignment Screen (submit_assignment.fxml)
 */
public class SubmitAssignmentController {

    @FXML private Label titleLabel;
    @FXML private Label courseLabel;
    @FXML private Label dueDateLabel;
    @FXML private Label totalPointsLabel;
    @FXML private Label instructionsLabel;
    @FXML private TextArea submissionTextArea;
    @FXML private TextField fileNameField;
    @FXML private Button submitButton;
    @FXML private Button cancelButton;
    @FXML private Label statusLabel;
    @FXML private Label submissionTextError;

    private Assignment assignment;
    private SubmissionService submissionService = new SubmissionService();
    private AuthService authService = AuthService.getInstance();

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML
    public void initialize() {
        // Check if user is logged in
        if (!authService.isLoggedIn()) {
            showError("Access Denied", "Please login to submit assignments.");
            disableAllControls();
            return;
        }

        // Check user type - only STUDENT can submit assignments
        String userType = authService.getCurrentUserType();
        if (!"STUDENT".equals(userType)) {
            showError("Access Denied", "Only students can submit assignments.");
            disableAllControls();
            return;
        }

        System.out.println("SubmitAssignmentController initialized");
    }

    /**
     * Set assignment data from StudentAssignmentsController
     */
    public void setAssignmentData(Assignment assignment) {
        this.assignment = assignment;

        if (assignment != null) {
            titleLabel.setText(assignment.getTitle());
            courseLabel.setText(assignment.getCourse() != null ? assignment.getCourse().getName() : "");
            dueDateLabel.setText(assignment.getDueDate() != null ?
                    assignment.getDueDate().format(DATE_TIME_FORMATTER) : "");
            totalPointsLabel.setText(String.valueOf(assignment.getTotalPoints()));
            instructionsLabel.setText(assignment.getInstructions() != null ? assignment.getInstructions() : "");

            // Check if already submitted
            try {
                User currentUser = authService.getCurrentUser();
                if (currentUser != null) {
                    AssignmentSubmission existingSubmission = submissionService.getSubmission(
                            assignment.getId(), currentUser.getId());
                    if (existingSubmission != null) {
                        submissionTextArea.setText(existingSubmission.getSubmissionText());
                        fileNameField.setText(existingSubmission.getFileName());
                        submitButton.setText("Update Submission");
                    }
                }
            } catch (SQLException e) {
                System.err.println("Error checking existing submission: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleSubmit() {
        if (assignment == null) {
            showError("Error", "No assignment selected.");
            return;
        }

        // Clear previous errors
        submissionTextError.setVisible(false);

        // Validate inputs
        if (!validateInputs()) {
            return;
        }

        try {
            User currentUser = authService.getCurrentUser();
            if (currentUser == null) {
                showError("Error", "User not logged in.");
                return;
            }

            String submissionText = submissionTextArea.getText().trim();
            String fileName = fileNameField.getText().trim();

            // Submit assignment
            AssignmentSubmission submission = submissionService.submitAssignment(
                    assignment.getId(), currentUser.getId(), submissionText, fileName);

            if (submission != null) {
                showSuccess("Assignment submitted successfully!");
                closeWindow();
            } else {
                showError("Error", "Failed to submit assignment.");
            }
        } catch (IllegalArgumentException e) {
            // US 2.8 - Handle deadline validation errors
            showError("Submission Error", e.getMessage());
        } catch (SQLException e) {
            showError("Database Error", "Failed to submit assignment: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleCancel() {
        closeWindow();
    }

    /**
     * Validate form inputs
     */
    private boolean validateInputs() {
        boolean isValid = true;

        // Validate submission text if TEXT or BOTH type
        if (assignment.getSubmissionType() == edu.curriculum.model.SubmissionType.TEXT ||
            assignment.getSubmissionType() == edu.curriculum.model.SubmissionType.BOTH) {
            if (submissionTextArea.getText().trim().isEmpty()) {
                submissionTextError.setText("Submission text is required");
                submissionTextError.setVisible(true);
                isValid = false;
            }
        }

        return isValid;
    }

    /**
     * Close the current window
     */
    private void closeWindow() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }

    private void disableAllControls() {
        if (submissionTextArea != null) submissionTextArea.setDisable(true);
        if (fileNameField != null) fileNameField.setDisable(true);
        if (submitButton != null) submitButton.setDisable(true);
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

