package edu.curriculum.ui;

import edu.curriculum.model.Assignment;
import edu.curriculum.model.Course;
import edu.curriculum.service.AssignmentService;
import edu.curriculum.service.CourseService;
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
 * Controller for Professor Assignment Management Screen (professor_assignments.fxml)
 */
public class ProfessorAssignmentsController {

    @FXML private Button backButton;
    @FXML private Button createButton;
    @FXML private Button deleteButton;
    @FXML private Button viewSubmissionsButton;

    @FXML private TableView<Assignment> assignmentsTable;
    @FXML private TableColumn<Assignment, String> assignmentIdColumn;
    @FXML private TableColumn<Assignment, String> courseNameColumn;
    @FXML private TableColumn<Assignment, String> titleColumn;
    @FXML private TableColumn<Assignment, String> dueDateColumn;
    @FXML private TableColumn<Assignment, String> totalPointsColumn;
    @FXML private TableColumn<Assignment, String> submissionsCountColumn;

    private ObservableList<Assignment> assignmentsList = FXCollections.observableArrayList();
    private AssignmentService assignmentService = new AssignmentService();
    private CourseService courseService = new CourseService();
    private SubmissionService submissionService = new SubmissionService();
    private AuthService authService = AuthService.getInstance();

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML
    public void initialize() throws SQLException {
        // Check if user is logged in
        if (!authService.isLoggedIn()) {
            showError("Access Denied", "Please login to view assignments.");
            disableAllControls();
            return;
        }

        // Check user type - only PROFESSOR can manage assignments
        String userType = authService.getCurrentUserType();
        if (!"PROFESSOR".equals(userType)) {
            showError("Access Denied", "Only professors can manage assignments.");
            disableAllControls();
            return;
        }

        // Setup table columns
        setupTableColumns();

        // Load assignments
        loadAssignments();

        System.out.println("ProfessorAssignmentsController initialized successfully!");
    }

    /**
     * Setup table columns to display assignment data
     */
    private void setupTableColumns() {
        assignmentIdColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getId())
        );

        courseNameColumn.setCellValueFactory(cellData -> {
            Course course = cellData.getValue().getCourse();
            return new javafx.beans.property.SimpleStringProperty(course != null ? course.getName() : "");
        });

        titleColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getTitle())
        );

        dueDateColumn.setCellValueFactory(cellData -> {
            LocalDateTime dueDate = cellData.getValue().getDueDate();
            String dateStr = dueDate != null ? dueDate.format(DATE_TIME_FORMATTER) : "";
            return new javafx.beans.property.SimpleStringProperty(dateStr);
        });

        totalPointsColumn.setCellValueFactory(cellData -> {
            int points = cellData.getValue().getTotalPoints();
            return new javafx.beans.property.SimpleStringProperty(String.valueOf(points));
        });

        submissionsCountColumn.setCellValueFactory(cellData -> {
            try {
                String assignmentId = cellData.getValue().getId();
                List<edu.curriculum.model.AssignmentSubmission> submissions = submissionService.getSubmissionsByAssignment(assignmentId);
                return new javafx.beans.property.SimpleStringProperty(String.valueOf(submissions.size()));
            } catch (SQLException e) {
                return new javafx.beans.property.SimpleStringProperty("0");
            }
        });
    }

    /**
     * Load assignments for the professor's courses
     */
    private void loadAssignments() throws SQLException {
        try {
            User currentUser = authService.getCurrentUser();
            if (currentUser == null) {
                return;
            }

            List<Course> courses = courseService.getCoursesByProfessor(currentUser.getId());
            assignmentsList.clear();

            for (Course course : courses) {
                List<Assignment> courseAssignments = assignmentService.getAssignmentsByCourse(course.getId());
                assignmentsList.addAll(courseAssignments);
            }

            assignmentsTable.setItems(assignmentsList);
            assignmentsTable.refresh();

            System.out.println("Loaded " + assignmentsList.size() + " assignments");
        } catch (SQLException e) {
            showError("Database Error", "Failed to load assignments: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleCreateAssignment() {
        try {
            System.out.println("Opening Create Assignment window...");

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/create_assignment.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Create New Assignment");
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));

            stage.setOnHidden(event -> {
                try {
                    loadAssignments();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            });

            stage.showAndWait();

        } catch (IOException e) {
            showError("Error", "Could not open Create Assignment window: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleDeleteAssignment() {
        System.out.println("handleDeleteAssignment() called");
        
        Assignment selectedAssignment = assignmentsTable.getSelectionModel().getSelectedItem();

        if (selectedAssignment == null) {
            System.out.println("No assignment selected for deletion");
            showWarning("No Selection", "Please select an assignment to delete");
            return;
        }

        System.out.println("Deleting assignment: " + selectedAssignment.getId() + " - " + selectedAssignment.getTitle());

        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Delete Assignment");
        confirmDialog.setHeaderText("Are you sure you want to delete this assignment?");
        confirmDialog.setContentText("Assignment: " + selectedAssignment.getTitle() + "\n\nThis action cannot be undone.");

        confirmDialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    System.out.println("User confirmed deletion. Deleting assignment ID: " + selectedAssignment.getId());
                    boolean deleted = assignmentService.deleteAssignment(selectedAssignment.getId());

                    if (deleted) {
                        System.out.println("Assignment deleted successfully. Reloading assignments list.");
                        loadAssignments();
                        showInfo("Success", "Assignment deleted successfully!");
                    } else {
                        System.err.println("ERROR: Assignment deletion returned false");
                        showError("Error", "Failed to delete assignment. Please try again.");
                    }
                } catch (SQLException e) {
                    System.err.println("SQLException while deleting assignment: " + e.getMessage());
                    e.printStackTrace();
                    showError("Database Error", "Failed to delete assignment: " + e.getMessage());
                } catch (Exception e) {
                    System.err.println("Unexpected error while deleting assignment: " + e.getMessage());
                    e.printStackTrace();
                    showError("Error", "An unexpected error occurred: " + e.getMessage());
                }
            } else {
                System.out.println("User cancelled deletion");
            }
        });
    }

    @FXML
    private void handleViewSubmissions() {
        System.out.println("handleViewSubmissions() called");
        
        Assignment selectedAssignment = assignmentsTable.getSelectionModel().getSelectedItem();

        if (selectedAssignment == null) {
            System.out.println("No assignment selected for viewing submissions");
            showWarning("No Selection", "Please select an assignment to view submissions");
            return;
        }

        System.out.println("Opening Grade Submissions window for assignment: " + selectedAssignment.getId() + " - " + selectedAssignment.getTitle());

        try {
            // Load grade_submissions.fxml (NOT student_assignments.fxml)
            String fxmlPath = "/fxml/grade_submissions.fxml";
            System.out.println("Loading FXML from: " + fxmlPath);
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            
            if (loader.getLocation() == null) {
                System.err.println("ERROR: Could not find FXML file at: " + fxmlPath);
                showError("Error", "Could not find Grade Submissions page. File not found: " + fxmlPath);
                return;
            }
            
            Parent root = loader.load();
            System.out.println("FXML loaded successfully");

            GradeSubmissionsController controller = loader.getController();
            if (controller == null) {
                System.err.println("ERROR: Controller is null after loading FXML");
                showError("Error", "Failed to initialize Grade Submissions controller.");
                return;
            }
            
            controller.setAssignment(selectedAssignment);
            System.out.println("Assignment set in controller");

            Stage stage = new Stage();
            stage.setTitle("Grade Submissions - " + selectedAssignment.getTitle());
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            System.out.println("Showing Grade Submissions window");
            stage.showAndWait();
            System.out.println("Grade Submissions window closed");

        } catch (IOException e) {
            System.err.println("IOException while opening Grade Submissions window: " + e.getMessage());
            e.printStackTrace();
            showError("Error", "Could not open Grade Submissions window: " + e.getMessage() + 
                    "\n\nExpected file: /fxml/grade_submissions.fxml");
        } catch (Exception e) {
            System.err.println("Unexpected error while opening Grade Submissions window: " + e.getMessage());
            e.printStackTrace();
            showError("Error", "An unexpected error occurred: " + e.getMessage());
        }
    }

    @FXML
    private void handleBack(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/dashboard.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));        stage.setMaximized(true);stage.show();
        } catch (IOException e) {
            showError("Navigation Error", "Unable to return to dashboard: " + e.getMessage());
        }
    }

    private void disableAllControls() {
        if (createButton != null) createButton.setDisable(true);
        if (deleteButton != null) deleteButton.setDisable(true);
        if (viewSubmissionsButton != null) viewSubmissionsButton.setDisable(true);
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

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

