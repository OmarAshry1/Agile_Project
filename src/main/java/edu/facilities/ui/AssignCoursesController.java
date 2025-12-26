package edu.facilities.ui;

import edu.facilities.model.Course;
import edu.facilities.model.User;
import edu.facilities.service.AuthService;
import edu.facilities.service.CourseAssignmentService;
import edu.facilities.service.CourseService;
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
 * Controller for Assigning Courses to Professors
 * US 3.3 - Assign Courses to Professors
 */
public class AssignCoursesController {

    @FXML private ComboBox<User> professorComboBox;
    @FXML private TableView<Course> coursesTable;
    @FXML private TableColumn<Course, String> codeColumn;
    @FXML private TableColumn<Course, String> nameColumn;
    @FXML private TableColumn<Course, String> departmentColumn;
    @FXML private TableColumn<Course, String> semesterColumn;
    @FXML private TableColumn<Course, String> creditsColumn;
    @FXML private TableColumn<Course, String> professorsColumn;
    
    @FXML private CheckBox checkConflictsCheckBox;
    @FXML private Button assignButton;
    @FXML private Button removeAssignmentButton;
    @FXML private Button viewProfessorCoursesButton;
    @FXML private Button backButton;
    @FXML private Label statusLabel;
    
    private ObservableList<Course> coursesList = FXCollections.observableArrayList();
    private ObservableList<User> professorsList = FXCollections.observableArrayList();
    private CourseService courseService = new CourseService();
    private CourseAssignmentService assignmentService = new CourseAssignmentService();
    private AuthService authService = AuthService.getInstance();
    
    @FXML
    public void initialize() {
        if (!checkAccess()) {
            disableAllControls();
            return;
        }
        
        setupTableColumns();
        loadProfessors();
        loadCourses();
    }
    
    private boolean checkAccess() {
        if (!authService.isLoggedIn()) {
            showError("Access Denied", "Please login to assign courses.");
            return false;
        }
        
        String userType = authService.getCurrentUserType();
        // Allow ADMIN and potentially DEPARTMENT_HEAD (for now, just ADMIN)
        if (!"ADMIN".equals(userType)) {
            showError("Access Denied", "Only administrators can assign courses to professors.");
            return false;
        }
        return true;
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
        
        professorsColumn.setCellValueFactory(cellData -> {
            List<User> professors = cellData.getValue().getProfessors();
            if (professors != null && !professors.isEmpty()) {
                StringBuilder profNames = new StringBuilder();
                for (User prof : professors) {
                    if (profNames.length() > 0) profNames.append(", ");
                    profNames.append(prof.getUsername());
                }
                return new javafx.beans.property.SimpleStringProperty(profNames.toString());
            }
            return new javafx.beans.property.SimpleStringProperty("None");
        });
    }
    
    private void loadProfessors() {
        try {
            List<User> professors = assignmentService.getAllProfessors();
            professorsList.clear();
            professorsList.addAll(professors);
            professorComboBox.setItems(professorsList);
        } catch (SQLException e) {
            showError("Database Error", "Failed to load professors: " + e.getMessage());
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
        User selectedProfessor = professorComboBox.getValue();
        if (selectedProfessor == null) {
            showWarning("No Selection", "Please select a professor.");
            return;
        }
        
        List<Course> selectedCourses = coursesTable.getSelectionModel().getSelectedItems();
        if (selectedCourses.isEmpty()) {
            showWarning("No Selection", "Please select at least one course to assign.");
            return;
        }
        
        List<String> courseIds = new ArrayList<>();
        for (Course course : selectedCourses) {
            courseIds.add(course.getId());
        }
        
        boolean checkConflicts = checkConflictsCheckBox.isSelected();
        
        try {
            List<String> assignedCourses = assignmentService.assignMultipleCoursesToProfessor(
                courseIds, selectedProfessor.getId(), checkConflicts);
            
            if (assignedCourses.size() == courseIds.size()) {
                showInfo("Success", "Successfully assigned " + assignedCourses.size() + " course(s) to " + 
                         selectedProfessor.getUsername());
            } else {
                showWarning("Partial Success", "Assigned " + assignedCourses.size() + " of " + 
                           courseIds.size() + " course(s). Some assignments may have failed due to conflicts.");
            }
            
            // Refresh courses to show updated professor assignments
            loadCourses();
        } catch (IllegalArgumentException e) {
            showError("Assignment Error", e.getMessage());
        } catch (SQLException e) {
            showError("Database Error", "Failed to assign courses: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleRemoveAssignment() {
        User selectedProfessor = professorComboBox.getValue();
        if (selectedProfessor == null) {
            showWarning("No Selection", "Please select a professor.");
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
        confirmDialog.setContentText("Professor: " + selectedProfessor.getUsername() + "\n" +
                                    "Course: " + selectedCourse.getCode() + " - " + selectedCourse.getName());
        
        confirmDialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    boolean removed = assignmentService.removeCourseAssignment(
                        selectedCourse.getId(), selectedProfessor.getId());
                    if (removed) {
                        showInfo("Success", "Assignment removed successfully!");
                        loadCourses();
                    } else {
                        showError("Error", "Failed to remove assignment. Professor may not be assigned to this course.");
                    }
                } catch (SQLException e) {
                    showError("Database Error", "Failed to remove assignment: " + e.getMessage());
                }
            }
        });
    }
    
    @FXML
    private void handleViewProfessorCourses() {
        User selectedProfessor = professorComboBox.getValue();
        if (selectedProfessor == null) {
            showWarning("No Selection", "Please select a professor.");
            return;
        }
        
        try {
            List<Course> professorCourses = assignmentService.getProfessorCourses(selectedProfessor.getId());
            
            StringBuilder coursesInfo = new StringBuilder();
            coursesInfo.append("Courses assigned to ").append(selectedProfessor.getUsername()).append(":\n\n");
            
            if (professorCourses.isEmpty()) {
                coursesInfo.append("No courses assigned.");
            } else {
                for (Course course : professorCourses) {
                    coursesInfo.append("- ").append(course.getCode())
                              .append(" - ").append(course.getName())
                              .append(" (").append(course.getSemester()).append(")\n");
                }
            }
            
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Professor Courses");
            alert.setHeaderText(null);
            alert.setContentText(coursesInfo.toString());
            alert.showAndWait();
        } catch (SQLException e) {
            showError("Database Error", "Failed to load professor courses: " + e.getMessage());
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
        if (professorComboBox != null) professorComboBox.setDisable(true);
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



