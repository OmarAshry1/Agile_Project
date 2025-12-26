package edu.facilities.ui;

import edu.facilities.model.Course;
import edu.facilities.model.User;
import edu.facilities.service.AuthService;
import edu.facilities.service.StaffCourseAssignmentService;
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
 * Controller for Viewing Assigned Courses (Staff)
 * US 3.3.2 - View Assigned Courses
 */
public class ViewAssignedCoursesController {

    @FXML private TableView<StaffCourseAssignmentService.CourseAssignment> assignmentsTable;
    @FXML private TableColumn<StaffCourseAssignmentService.CourseAssignment, String> codeColumn;
    @FXML private TableColumn<StaffCourseAssignmentService.CourseAssignment, String> nameColumn;
    @FXML private TableColumn<StaffCourseAssignmentService.CourseAssignment, String> departmentColumn;
    @FXML private TableColumn<StaffCourseAssignmentService.CourseAssignment, String> semesterColumn;
    @FXML private TableColumn<StaffCourseAssignmentService.CourseAssignment, String> creditsColumn;
    @FXML private TableColumn<StaffCourseAssignmentService.CourseAssignment, String> roleColumn;
    @FXML private TableColumn<StaffCourseAssignmentService.CourseAssignment, String> assignedDateColumn;
    
    @FXML private Label totalCoursesLabel;
    @FXML private Label statusLabel;
    @FXML private Button refreshButton;
    @FXML private Button backButton;
    
    @FXML private TextArea courseDetailsArea;
    
    private ObservableList<StaffCourseAssignmentService.CourseAssignment> assignmentsList = FXCollections.observableArrayList();
    private StaffCourseAssignmentService assignmentService = new StaffCourseAssignmentService();
    private AuthService authService = AuthService.getInstance();
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    @FXML
    public void initialize() {
        if (!checkAccess()) {
            disableAllControls();
            return;
        }
        
        setupTableColumns();
        loadAssignments();
    }
    
    private boolean checkAccess() {
        if (!authService.isLoggedIn()) {
            showError("Access Denied", "Please login to view your assigned courses.");
            return false;
        }
        
        String userType = authService.getCurrentUserType();
        if (!"STAFF".equals(userType)) {
            showError("Access Denied", "Only staff members can view assigned courses.");
            return false;
        }
        return true;
    }
    
    private void setupTableColumns() {
        codeColumn.setCellValueFactory(cellData -> {
            Course course = cellData.getValue().getCourse();
            return new javafx.beans.property.SimpleStringProperty(course != null ? course.getCode() : "");
        });
        
        nameColumn.setCellValueFactory(cellData -> {
            Course course = cellData.getValue().getCourse();
            return new javafx.beans.property.SimpleStringProperty(course != null ? course.getName() : "");
        });
        
        departmentColumn.setCellValueFactory(cellData -> {
            Course course = cellData.getValue().getCourse();
            return new javafx.beans.property.SimpleStringProperty(course != null ? course.getDepartment() : "");
        });
        
        semesterColumn.setCellValueFactory(cellData -> {
            Course course = cellData.getValue().getCourse();
            return new javafx.beans.property.SimpleStringProperty(course != null ? course.getSemester() : "");
        });
        
        creditsColumn.setCellValueFactory(cellData -> {
            Course course = cellData.getValue().getCourse();
            return new javafx.beans.property.SimpleStringProperty(
                course != null ? String.valueOf(course.getCredits()) : "");
        });
        
        roleColumn.setCellValueFactory(cellData -> {
            String role = cellData.getValue().getRole();
            return new javafx.beans.property.SimpleStringProperty(role != null ? role : "N/A");
        });
        
        assignedDateColumn.setCellValueFactory(cellData -> {
            if (cellData.getValue().getAssignedDate() != null) {
                return new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().getAssignedDate().format(DATE_FORMATTER));
            }
            return new javafx.beans.property.SimpleStringProperty("");
        });
        
        // Add row selection listener to show details
        assignmentsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                showCourseDetails(newSelection);
            }
        });
    }
    
    private void loadAssignments() {
        try {
            User currentUser = authService.getCurrentUser();
            if (currentUser == null) {
                showError("Authentication Error", "User session expired. Please login again.");
                return;
            }
            
            List<StaffCourseAssignmentService.CourseAssignment> assignments = 
                assignmentService.getStaffAssignedCourses(currentUser.getId());
            
            assignmentsList.clear();
            assignmentsList.addAll(assignments);
            
            totalCoursesLabel.setText("Total Assigned Courses: " + assignments.size());
            statusLabel.setText("Loaded " + assignments.size() + " assignment(s)");
        } catch (SQLException e) {
            showError("Database Error", "Failed to load assignments: " + e.getMessage());
        }
    }
    
    private void showCourseDetails(StaffCourseAssignmentService.CourseAssignment assignment) {
        if (courseDetailsArea != null) {
            Course course = assignment.getCourse();
            if (course == null) {
                courseDetailsArea.setText("Course details not available.");
                return;
            }
            
            StringBuilder details = new StringBuilder();
            details.append("Course Code: ").append(course.getCode()).append("\n");
            details.append("Course Name: ").append(course.getName()).append("\n");
            details.append("Description: ").append(course.getDescription() != null ? course.getDescription() : "N/A").append("\n");
            details.append("Credits: ").append(course.getCredits()).append("\n");
            details.append("Department: ").append(course.getDepartment()).append("\n");
            details.append("Semester: ").append(course.getSemester()).append("\n");
            details.append("Type: ").append(course.getType()).append("\n");
            details.append("Max Seats: ").append(course.getMaxSeats()).append("\n");
            details.append("Current Seats: ").append(course.getCurrentSeats()).append("\n");
            details.append("\n");
            details.append("Your Role: ").append(assignment.getRole() != null ? assignment.getRole() : "N/A").append("\n");
            if (assignment.getAssignedDate() != null) {
                details.append("Assigned Date: ").append(assignment.getAssignedDate().format(DATE_FORMATTER)).append("\n");
            }
            
            courseDetailsArea.setText(details.toString());
        }
    }
    
    @FXML
    private void handleRefresh() {
        loadAssignments();
        statusLabel.setText("Refreshed");
    }
    
    @FXML
    private void handleBack(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/dashboard.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Dashboard");
            stage.setMaximized(true);
            stage.show();
        } catch (IOException e) {
            showError("Navigation Error", "Could not return to dashboard: " + e.getMessage());
        }
    }
    
    private void disableAllControls() {
        if (assignmentsTable != null) assignmentsTable.setDisable(true);
        if (refreshButton != null) refreshButton.setDisable(true);
    }
    
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}



