package edu.curriculum.ui;

import edu.curriculum.model.Assignment;
import edu.curriculum.model.AssignmentSubmission;
import edu.curriculum.service.SubmissionService;
import edu.facilities.model.Student;
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
 * Controller for Grade Submissions Screen (grade_submissions.fxml)
 */
public class GradeSubmissionsController {

    @FXML private Label titleLabel;
    @FXML private Label assignmentInfoLabel;
    @FXML private TableView<AssignmentSubmission> submissionsTable;
    @FXML private TableColumn<AssignmentSubmission, String> studentNameColumn;
    @FXML private TableColumn<AssignmentSubmission, String> studentNumberColumn;
    @FXML private TableColumn<AssignmentSubmission, String> submissionDateColumn;
    @FXML private TableColumn<AssignmentSubmission, String> currentScoreColumn;
    @FXML private TableColumn<AssignmentSubmission, String> statusColumn;
    @FXML private Spinner<Integer> scoreSpinner;
    @FXML private TextArea feedbackArea;
    @FXML private Button saveGradeButton;
    @FXML private Button refreshButton;
    @FXML private Button backButton;

    private Assignment assignment;
    private ObservableList<AssignmentSubmission> submissionsList = FXCollections.observableArrayList();
    private SubmissionService submissionService = new SubmissionService();
    private AuthService authService = AuthService.getInstance();

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML
    public void initialize() {
        // Check if user is logged in
        if (!authService.isLoggedIn()) {
            showError("Access Denied", "Please login to grade submissions.");
            disableAllControls();
            return;
        }

        // Check user type - only PROFESSOR can grade submissions
        String userType = authService.getCurrentUserType();
        if (!"PROFESSOR".equals(userType)) {
            showError("Access Denied", "Only professors can grade submissions.");
            disableAllControls();
            return;
        }

        // Setup table columns
        setupTableColumns();

        // Setup score spinner
        SpinnerValueFactory<Integer> scoreFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 1000, 0);
        scoreSpinner.setValueFactory(scoreFactory);

        // Add selection listener to update spinner max value
        submissionsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null && assignment != null) {
                SpinnerValueFactory<Integer> factory = new SpinnerValueFactory.IntegerSpinnerValueFactory(
                        0, assignment.getTotalPoints(), newSelection.getScore() != null ? newSelection.getScore() : 0);
                scoreSpinner.setValueFactory(factory);
                feedbackArea.setText(newSelection.getFeedback() != null ? newSelection.getFeedback() : "");
            }
        });

        System.out.println("GradeSubmissionsController initialized successfully!");
    }

    /**
     * Set assignment from ProfessorAssignmentsController
     */
    public void setAssignment(Assignment assignment) {
        this.assignment = assignment;

        if (assignment != null) {
            assignmentInfoLabel.setText("Assignment: " + assignment.getTitle() + " (" + assignment.getCourse().getName() + ")");
            
            // Update score spinner max value
            SpinnerValueFactory<Integer> factory = new SpinnerValueFactory.IntegerSpinnerValueFactory(
                    0, assignment.getTotalPoints(), 0);
            scoreSpinner.setValueFactory(factory);

            // Load submissions
            loadSubmissions();
        }
    }

    /**
     * Setup table columns to display submission data
     */
    private void setupTableColumns() {
        studentNameColumn.setCellValueFactory(cellData -> {
            Student student = cellData.getValue().getStudent();
            return new javafx.beans.property.SimpleStringProperty(
                    student != null ? student.getUsername() : "");
        });

        studentNumberColumn.setCellValueFactory(cellData -> {
            Student student = cellData.getValue().getStudent();
            // Note: Student model doesn't have student number, using ID as fallback
            return new javafx.beans.property.SimpleStringProperty(
                    student != null ? student.getId() : "");
        });

        submissionDateColumn.setCellValueFactory(cellData -> {
            LocalDateTime submittedDate = cellData.getValue().getSubmittedDate();
            String dateStr = submittedDate != null ? submittedDate.format(DATE_TIME_FORMATTER) : "";
            return new javafx.beans.property.SimpleStringProperty(dateStr);
        });

        currentScoreColumn.setCellValueFactory(cellData -> {
            Integer score = cellData.getValue().getScore();
            if (score != null) {
                int maxPoints = cellData.getValue().getAssignment().getTotalPoints();
                return new javafx.beans.property.SimpleStringProperty(score + "/" + maxPoints);
            } else {
                return new javafx.beans.property.SimpleStringProperty("Not Graded");
            }
        });

        statusColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().getStatus().toString())
        );
    }

    /**
     * Load submissions for the assignment
     */
    private void loadSubmissions() {
        if (assignment == null) {
            return;
        }

        try {
            List<AssignmentSubmission> submissions = submissionService.getSubmissionsByAssignment(assignment.getId());
            submissionsList.setAll(submissions);
            submissionsTable.setItems(submissionsList);
            submissionsTable.refresh();

            System.out.println("Loaded " + submissions.size() + " submissions for assignment " + assignment.getId());
        } catch (SQLException e) {
            showError("Database Error", "Failed to load submissions: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleSaveGrade() {
        AssignmentSubmission selectedSubmission = submissionsTable.getSelectionModel().getSelectedItem();

        if (selectedSubmission == null) {
            showWarning("No Selection", "Please select a submission to grade");
            return;
        }

        if (assignment == null) {
            showError("Error", "No assignment selected.");
            return;
        }

        // Validate score
        int score = scoreSpinner.getValue();
        if (score < 0 || score > assignment.getTotalPoints()) {
            showError("Invalid Score", "Score must be between 0 and " + assignment.getTotalPoints());
            return;
        }

        String feedback = feedbackArea.getText().trim();

        try {
            boolean success = submissionService.gradeSubmission(
                    selectedSubmission.getId(), score, feedback);

            if (success) {
                showSuccess("Submission graded successfully!");
                loadSubmissions();
            } else {
                showError("Error", "Failed to grade submission.");
            }
        } catch (SQLException e) {
            showError("Database Error", "Failed to grade submission: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleRefresh() {
        loadSubmissions();
    }

    @FXML
    private void handleBack(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/professor_assignments.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));        stage.setMaximized(true);stage.show();
        } catch (IOException e) {
            showError("Navigation Error", "Unable to return to assignments page: " + e.getMessage());
        }
    }

    private void disableAllControls() {
        if (submissionsTable != null) submissionsTable.setDisable(true);
        if (scoreSpinner != null) scoreSpinner.setDisable(true);
        if (feedbackArea != null) feedbackArea.setDisable(true);
        if (saveGradeButton != null) saveGradeButton.setDisable(true);
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

    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

