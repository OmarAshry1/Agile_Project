package edu.facilities.ui;

import edu.facilities.model.Course;
import edu.facilities.model.User;
import edu.facilities.service.AuthService;
import edu.facilities.service.CourseService;
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
import java.util.ArrayList;
import java.util.List;

/**
 * Controller for Assigning Staff to Courses
 * US 3.3.1 - Assign Staff to Course
 */
public class AssignStaffToCourseController {

    @FXML private ComboBox<User> staffComboBox;
    @FXML private ComboBox<String> roleComboBox;
    @FXML private TableView<Course> coursesTable;
    @FXML private TableColumn<Course, String> codeColumn;
    @FXML private TableColumn<Course, String> nameColumn;
    @FXML private TableColumn<Course, String> departmentColumn;
    @FXML private TableColumn<Course, String> semesterColumn;
    @FXML private TableColumn<Course, String> creditsColumn;
    @FXML private TableColumn<Course, String> staffColumn;
    
    @FXML private CheckBox checkConflictsCheckBox;
    @FXML private Button assignButton;
    @FXML private Button removeAssignmentButton;
    @FXML private Button viewStaffCoursesButton;
    @FXML private Button backButton;
    @FXML private Label statusLabel;
    
    private ObservableList<Course> coursesList = FXCollections.observableArrayList();
    private ObservableList<User> staffList = FXCollections.observableArrayList();
    private CourseService courseService = new CourseService();
    private StaffCourseAssignmentService assignmentService = new StaffCourseAssignmentService();
    private AuthService authService = AuthService.getInstance();
    
    @FXML
    public void initialize() {
        if (!checkAccess()) {
            disableAllControls();
            return;
        }
        
        setupTableColumns();
        setupRoleComboBox();
        loadStaff();
        loadCourses();
    }
    
    private boolean checkAccess() {
        if (!authService.isLoggedIn()) {
            showError("Access Denied", "Please login to assign staff to courses.");
            return false;
        }
        
        String userType = authService.getCurrentUserType();
        // Allow ADMIN (Academic Administrator)
        if (!"ADMIN".equals(userType)) {
            showError("Access Denied", "Only academic administrators can assign staff to courses.");
            return false;
        }
        return true;
    }
    
    private void setupRoleComboBox() {
        roleComboBox.getItems().addAll("TA", "LAB_ASSISTANT", "TUTOR", "COORDINATOR", "OTHER");
        roleComboBox.setValue("TA");
    }
    
    private void setupTableColumns() {
        codeColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getCode())
        );
        
        nameColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getName())
        );
        
        departmentColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getDepartment())
        );
        
        semesterColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getSemester())
        );
        
        creditsColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(String.valueOf(cellData.getValue().getCredits()))
        );
        
        staffColumn.setCellValueFactory(cellData -> {
            // This would require loading staff assignments - simplified for now
            return new javafx.beans.property.SimpleStringProperty("View details");
        });
    }
    
    private void loadStaff() {
        try {
            List<User> staff = assignmentService.getAllStaff();
            staffList.clear();
            staffList.addAll(staff);
            staffComboBox.setItems(staffList);
        } catch (SQLException e) {
            showError("Database Error", "Failed to load staff members: " + e.getMessage());
        }
    }
    
    private void loadCourses() {
        try {
            List<Course> courses = courseService.getAllCourses(true); // Active courses only
            coursesList.clear();
            coursesList.addAll(courses);
            statusLabel.setText("Loaded " + courses.size() + " course(s)");
        } catch (SQLException e) {
            showError("Database Error", "Failed to load courses: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleAssign() {
        User selectedStaff = staffComboBox.getValue();
        if (selectedStaff == null) {
            showWarning("No Selection", "Please select a staff member.");
            return;
        }
        
        List<Course> selectedCourses = coursesTable.getSelectionModel().getSelectedItems();
        if (selectedCourses.isEmpty()) {
            showWarning("No Selection", "Please select at least one course to assign.");
            return;
        }
        
        String role = roleComboBox.getValue();
        boolean checkConflicts = checkConflictsCheckBox.isSelected();
        
        User currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            showError("Authentication Error", "User session expired. Please login again.");
            return;
        }
        
        List<String> courseIds = new ArrayList<>();
        for (Course course : selectedCourses) {
            courseIds.add(course.getId());
        }
        
        try {
            List<String> assignedCourses = assignmentService.assignMultipleStaffToCourses(
                courseIds, selectedStaff.getId(), role, currentUser.getId(), checkConflicts);
            
            if (assignedCourses.size() == courseIds.size()) {
                showInfo("Success", "Successfully assigned " + assignedCourses.size() + " course(s) to " + 
                         selectedStaff.getUsername() + " as " + role);
            } else {
                showWarning("Partial Success", "Assigned " + assignedCourses.size() + " of " + 
                           courseIds.size() + " course(s). Some assignments may have failed due to conflicts.");
            }
            
            // Refresh courses
            loadCourses();
        } catch (IllegalArgumentException e) {
            showError("Assignment Error", e.getMessage());
        } catch (SQLException e) {
            showError("Database Error", "Failed to assign courses: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleRemoveAssignment() {
        User selectedStaff = staffComboBox.getValue();
        if (selectedStaff == null) {
            showWarning("No Selection", "Please select a staff member.");
            return;
        }
        
        Course selectedCourse = coursesTable.getSelectionModel().getSelectedItem();
        if (selectedCourse == null) {
            showWarning("No Selection", "Please select a course to remove assignment.");
            return;
        }
        
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Remove Assignment");
        confirmDialog.setHeaderText("Are you sure you want to remove this assignment?");
        confirmDialog.setContentText("Staff: " + selectedStaff.getUsername() + "\n" +
                                    "Course: " + selectedCourse.getCode() + " - " + selectedCourse.getName());
        
        confirmDialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    boolean removed = assignmentService.removeStaffAssignment(
                        selectedCourse.getId(), selectedStaff.getId());
                    if (removed) {
                        showInfo("Success", "Assignment removed successfully!");
                        loadCourses();
                    } else {
                        showError("Error", "Failed to remove assignment. Staff may not be assigned to this course.");
                    }
                } catch (SQLException e) {
                    showError("Database Error", "Failed to remove assignment: " + e.getMessage());
                }
            }
        });
    }
    
    @FXML
    private void handleViewStaffCourses() {
        User selectedStaff = staffComboBox.getValue();
        if (selectedStaff == null) {
            showWarning("No Selection", "Please select a staff member.");
            return;
        }
        
        try {
            List<StaffCourseAssignmentService.CourseAssignment> assignments = 
                assignmentService.getStaffAssignedCourses(selectedStaff.getId());
            
            StringBuilder coursesInfo = new StringBuilder();
            coursesInfo.append("Courses assigned to ").append(selectedStaff.getUsername()).append(":\n\n");
            
            if (assignments.isEmpty()) {
                coursesInfo.append("No courses assigned.");
            } else {
                for (StaffCourseAssignmentService.CourseAssignment assignment : assignments) {
                    Course course = assignment.getCourse();
                    coursesInfo.append("- ").append(course.getCode())
                              .append(" - ").append(course.getName())
                              .append(" (").append(course.getSemester()).append(")");
                    if (assignment.getRole() != null) {
                        coursesInfo.append(" - Role: ").append(assignment.getRole());
                    }
                    coursesInfo.append("\n");
                }
            }
            
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Staff Course Assignments");
            alert.setHeaderText(null);
            alert.setContentText(coursesInfo.toString());
            alert.showAndWait();
        } catch (SQLException e) {
            showError("Database Error", "Failed to load staff courses: " + e.getMessage());
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
    
    private void disableAllControls() {
        if (staffComboBox != null) staffComboBox.setDisable(true);
        if (coursesTable != null) coursesTable.setDisable(true);
        if (assignButton != null) assignButton.setDisable(true);
        if (removeAssignmentButton != null) removeAssignmentButton.setDisable(true);
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



