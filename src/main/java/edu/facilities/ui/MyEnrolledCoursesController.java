package edu.facilities.ui;

import edu.facilities.model.Enrollment;
import edu.facilities.model.User;
import edu.facilities.service.AuthService;
import edu.facilities.service.EnrollmentService;
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
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Controller for View My Enrolled Courses
 * US 2.4 - View My Enrolled Courses
 */
public class MyEnrolledCoursesController {

    @FXML private TableView<Enrollment> enrollmentsTable;
    @FXML private TableColumn<Enrollment, String> codeColumn;
    @FXML private TableColumn<Enrollment, String> nameColumn;
    @FXML private TableColumn<Enrollment, String> creditsColumn;
    @FXML private TableColumn<Enrollment, String> professorColumn;
    @FXML private TableColumn<Enrollment, String> enrollmentDateColumn;
    @FXML private TableColumn<Enrollment, String> statusColumn;

    @FXML private Label totalCreditsLabel;
    @FXML private Label statusLabel;

    @FXML private Button viewAssignmentsButton;
    @FXML private Button dropCourseButton;
    @FXML private Button backButton;

    private ObservableList<Enrollment> enrollmentsList = FXCollections.observableArrayList();
    private EnrollmentService enrollmentService = new EnrollmentService();
    private AuthService authService = AuthService.getInstance();

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @FXML
    public void initialize() {
        if (!checkStudentAccess()) {
            disableAllControls();
            return;
        }

        setupTableColumns();
        loadEnrollments();
    }

    private boolean checkStudentAccess() {
        if (!authService.isLoggedIn()) {
            showError("Access Denied", "Please login to view enrolled courses.");
            return false;
        }

        String userType = authService.getCurrentUserType();
        if (!"STUDENT".equals(userType)) {
            showError("Access Denied", "Only students can view enrolled courses.");
            return false;
        }
        return true;
    }

    private void setupTableColumns() {
        codeColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().getCourse().getCode())
        );

        nameColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().getCourse().getName())
        );

        creditsColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        String.valueOf(cellData.getValue().getCourse().getCredits()))
        );

        professorColumn.setCellValueFactory(cellData -> {
            List<edu.facilities.model.User> professors = cellData.getValue().getCourse().getProfessors();
            if (professors != null && !professors.isEmpty()) {
                StringBuilder profNames = new StringBuilder();
                for (edu.facilities.model.User prof : professors) {
                    if (profNames.length() > 0) profNames.append(", ");
                    profNames.append(prof.getUsername());
                }
                return new javafx.beans.property.SimpleStringProperty(profNames.toString());
            }
            return new javafx.beans.property.SimpleStringProperty("TBA");
        });

        enrollmentDateColumn.setCellValueFactory(cellData -> {
            if (cellData.getValue().getEnrollmentDate() != null) {
                return new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().getEnrollmentDate().format(DATE_FORMATTER));
            }
            return new javafx.beans.property.SimpleStringProperty("N/A");
        });

        statusColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().getStatus().toString())
        );
    }

    private void loadEnrollments() {
        try {
            User student = authService.getCurrentUser();
            if (student == null) {
                showError("Authentication Error", "User session expired. Please login again.");
                return;
            }

            // US 2.4 - Get enrolled courses only
            List<Enrollment> enrollments = enrollmentService.getStudentEnrollments(student, true);
            enrollmentsList.clear();
            enrollmentsList.addAll(enrollments);
            enrollmentsTable.setItems(enrollmentsList);

            // US 2.4 - Show total enrolled credits
            int totalCredits = enrollmentService.getTotalEnrolledCredits(student);
            totalCreditsLabel.setText("Total Enrolled Credits: " + totalCredits + " / 18");

            statusLabel.setText("Loaded " + enrollments.size() + " enrolled course(s)");
        } catch (SQLException e) {
            showError("Database Error", "Failed to load enrolled courses: " + e.getMessage());
        }
    }

    @FXML
    private void handleViewAssignments() {
        Enrollment selectedEnrollment = enrollmentsTable.getSelectionModel().getSelectedItem();
        if (selectedEnrollment == null) {
            showWarning("No Selection", "Please select a course to view assignments.");
            return;
        }

        // US 2.4 - Link to assignments
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/student_assignments.fxml"));
            Parent root = loader.load();
            
            // Pass course information if needed
            Stage stage = (Stage) viewAssignmentsButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("My Assignments - " + selectedEnrollment.getCourse().getCode());
            stage.show();
        } catch (IOException e) {
            showError("Navigation Error", "Could not open assignments page: " + e.getMessage());
        }
    }

    @FXML
    private void handleDropCourse() {
        Enrollment selectedEnrollment = enrollmentsTable.getSelectionModel().getSelectedItem();
        if (selectedEnrollment == null) {
            showWarning("No Selection", "Please select a course to drop.");
            return;
        }

        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Drop Course");
        confirmDialog.setHeaderText("Are you sure you want to drop this course?");
        confirmDialog.setContentText("Course: " + selectedEnrollment.getCourse().getCode() + 
                                    " - " + selectedEnrollment.getCourse().getName());

        confirmDialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    boolean dropped = enrollmentService.dropEnrollment(selectedEnrollment.getId());
                    if (dropped) {
                        showInfo("Success", "Course dropped successfully!");
                        loadEnrollments();
                    } else {
                        showError("Error", "Failed to drop course.");
                    }
                } catch (SQLException e) {
                    showError("Database Error", "Failed to drop course: " + e.getMessage());
                }
            }
        });
    }

    @FXML
    private void handleBack(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/dashboard.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Dashboard");
            stage.show();
        } catch (IOException e) {
            showError("Navigation Error", "Could not return to dashboard: " + e.getMessage());
        }
    }

    private void disableAllControls() {
        if (enrollmentsTable != null) enrollmentsTable.setDisable(true);
        if (viewAssignmentsButton != null) viewAssignmentsButton.setDisable(true);
        if (dropCourseButton != null) dropCourseButton.setDisable(true);
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

