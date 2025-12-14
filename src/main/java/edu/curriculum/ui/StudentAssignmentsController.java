package edu.curriculum.ui;

import edu.curriculum.model.Assignment;
import edu.curriculum.model.AssignmentSubmission;
import edu.curriculum.service.AssignmentService;
import edu.curriculum.service.SubmissionService;
import edu.facilities.model.User;
import edu.facilities.service.AuthService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Controller for Student Assignments Screen (student_assignments.fxml)
 */
public class StudentAssignmentsController {

    @FXML private TableView<Assignment> assignmentsTable;
    @FXML private TableColumn<Assignment, String> courseNameColumn;
    @FXML private TableColumn<Assignment, String> titleColumn;
    @FXML private TableColumn<Assignment, String> dueDateColumn;
    @FXML private TableColumn<Assignment, String> submissionStatusColumn;
    @FXML private TableColumn<Assignment, String> scoreColumn;
    @FXML private TableColumn<Assignment, String> actionColumn;

    @FXML private Button submitButton;
    @FXML private Button viewDetailsButton;
    @FXML private Button refreshButton;
    @FXML private Button backButton;
    @FXML private Label statusLabel;

    private ObservableList<Assignment> assignmentsList = FXCollections.observableArrayList();
    private AssignmentService assignmentService = new AssignmentService();
    private SubmissionService submissionService = new SubmissionService();
    private AuthService authService = AuthService.getInstance();

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML
    public void initialize() {
        // Check if user is logged in
        if (!authService.isLoggedIn()) {
            showError("Access Denied", "Please login to view assignments.");
            disableAllControls();
            return;
        }

        // Check user type - only STUDENT can view assignments
        String userType = authService.getCurrentUserType();
        if (!"STUDENT".equals(userType)) {
            showError("Access Denied", "Only students can view assignments.");
            disableAllControls();
            return;
        }

        // Setup table columns
        setupTableColumns();

        // Load assignments
        loadAssignments();

        System.out.println("StudentAssignmentsController initialized successfully!");
    }

    /**
     * Setup table columns to display assignment data
     */
    private void setupTableColumns() {
        courseNameColumn.setCellValueFactory(cellData -> {
            String courseName = cellData.getValue().getCourse() != null ?
                    cellData.getValue().getCourse().getName() : "";
            return new javafx.beans.property.SimpleStringProperty(courseName);
        });

        titleColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getTitle())
        );

        dueDateColumn.setCellValueFactory(cellData -> {
            LocalDateTime dueDate = cellData.getValue().getDueDate();
            String dateStr = dueDate != null ? dueDate.format(DATE_TIME_FORMATTER) : "";
            return new javafx.beans.property.SimpleStringProperty(dateStr);
        });

        submissionStatusColumn.setCellValueFactory(cellData -> {
            try {
                User currentUser = authService.getCurrentUser();
                if (currentUser == null) {
                    return new javafx.beans.property.SimpleStringProperty("Unknown");
                }
                AssignmentSubmission submission = submissionService.getSubmission(
                        cellData.getValue().getId(), currentUser.getId());
                if (submission != null) {
                    return new javafx.beans.property.SimpleStringProperty("Submitted");
                } else {
                    return new javafx.beans.property.SimpleStringProperty("Not Submitted");
                }
            } catch (SQLException e) {
                return new javafx.beans.property.SimpleStringProperty("Error");
            }
        });

        scoreColumn.setCellValueFactory(cellData -> {
            try {
                User currentUser = authService.getCurrentUser();
                if (currentUser == null) {
                    return new javafx.beans.property.SimpleStringProperty("N/A");
                }
                AssignmentSubmission submission = submissionService.getSubmission(
                        cellData.getValue().getId(), currentUser.getId());
                if (submission != null && submission.getScore() != null) {
                    return new javafx.beans.property.SimpleStringProperty(
                            submission.getScore() + "/" + cellData.getValue().getTotalPoints());
                } else {
                    return new javafx.beans.property.SimpleStringProperty("N/A");
                }
            } catch (SQLException e) {
                return new javafx.beans.property.SimpleStringProperty("N/A");
            }
        });

        actionColumn.setCellValueFactory(cellData -> {
            try {
                User currentUser = authService.getCurrentUser();
                if (currentUser == null) {
                    return new javafx.beans.property.SimpleStringProperty("");
                }
                AssignmentSubmission submission = submissionService.getSubmission(
                        cellData.getValue().getId(), currentUser.getId());
                if (submission != null) {
                    return new javafx.beans.property.SimpleStringProperty("View Submission");
                } else {
                    return new javafx.beans.property.SimpleStringProperty("Submit");
                }
            } catch (SQLException e) {
                return new javafx.beans.property.SimpleStringProperty("");
            }
        });
    }

    /**
     * Load assignments for the current student
     */
    private void loadAssignments() {
        try {
            User currentUser = authService.getCurrentUser();
            if (currentUser == null) {
                return;
            }

            List<Assignment> assignments = assignmentService.getStudentAssignments(currentUser.getId());
            assignmentsList.setAll(assignments);
            assignmentsTable.setItems(assignmentsList);
            assignmentsTable.refresh();

            System.out.println("Loaded " + assignments.size() + " assignments for student");
        } catch (SQLException e) {
            showError("Database Error", "Failed to load assignments: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleSubmitAssignment() {
        Assignment selectedAssignment = assignmentsTable.getSelectionModel().getSelectedItem();

        if (selectedAssignment == null) {
            showWarning("No Selection", "Please select an assignment to submit");
            return;
        }

        // Check if already submitted
        try {
            User currentUser = authService.getCurrentUser();
            if (currentUser == null) {
                return;
            }

            AssignmentSubmission existingSubmission = submissionService.getSubmission(
                    selectedAssignment.getId(), currentUser.getId());

            if (existingSubmission != null) {
                showWarning("Already Submitted", "You have already submitted this assignment.");
                return;
            }

            // Open submit assignment window
            try {
                System.out.println("Opening Submit Assignment window for: " + selectedAssignment.getTitle());

                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/submit_assignment.fxml"));
                Parent root = loader.load();

                SubmitAssignmentController controller = loader.getController();
                controller.setAssignmentData(selectedAssignment);

                Stage stage = new Stage();
                stage.setTitle("Submit Assignment - " + selectedAssignment.getTitle());
                stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
                stage.setScene(new Scene(root));

                stage.setOnHidden(event -> {
                    loadAssignments();
                });

                stage.showAndWait();

            } catch (IOException e) {
                showError("Error", "Could not open Submit Assignment window: " + e.getMessage());
                e.printStackTrace();
            }

        } catch (SQLException e) {
            showError("Database Error", "Failed to check submission status: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleViewDetails() {
        Assignment selectedAssignment = assignmentsTable.getSelectionModel().getSelectedItem();

        if (selectedAssignment == null) {
            showWarning("No Selection", "Please select an assignment to view details");
            return;
        }

        // Show assignment details in an alert
        StringBuilder details = new StringBuilder();
        details.append("Title: ").append(selectedAssignment.getTitle()).append("\n");
        details.append("Course: ").append(selectedAssignment.getCourse().getName()).append("\n");
        details.append("Due Date: ").append(selectedAssignment.getDueDate().format(DATE_TIME_FORMATTER)).append("\n");
        details.append("Total Points: ").append(selectedAssignment.getTotalPoints()).append("\n");
        details.append("Instructions:\n").append(selectedAssignment.getInstructions());

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Assignment Details");
        alert.setHeaderText(null);
        alert.setContentText(details.toString());
        alert.showAndWait();
    }

    @FXML
    private void handleRefresh() {
        loadAssignments();
        showStatus("Assignments refreshed successfully", true);
    }

    @FXML
    private void handleBack(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/dashboard.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            showError("Navigation Error", "Unable to return to dashboard: " + e.getMessage());
        }
    }

    private void disableAllControls() {
        if (submitButton != null) submitButton.setDisable(true);
        if (viewDetailsButton != null) viewDetailsButton.setDisable(true);
        if (refreshButton != null) refreshButton.setDisable(true);
        if (assignmentsTable != null) assignmentsTable.setDisable(true);
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showStatus(String message, boolean success) {
        if (statusLabel != null) {
            statusLabel.setText(message);
            statusLabel.setVisible(true);
            statusLabel.setStyle(success ? "-fx-text-fill: #4CAF50;" : "-fx-text-fill: #E53935;");
        }
    }
}

