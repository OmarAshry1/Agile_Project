package edu.facilities.ui;

import edu.facilities.model.Course;
import edu.facilities.model.Enrollment;
import edu.facilities.model.User;
import edu.facilities.service.AuthService;
import edu.facilities.service.CourseService;
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
import java.util.List;

/**
 * Controller for Student Course Enrollment
 * US 2.3 - Enroll in Courses
 */
public class StudentEnrollmentController {

    @FXML private TableView<Course> availableCoursesTable;
    @FXML private TableColumn<Course, String> codeColumn;
    @FXML private TableColumn<Course, String> nameColumn;
    @FXML private TableColumn<Course, String> creditsColumn;
    @FXML private TableColumn<Course, String> departmentColumn;
    @FXML private TableColumn<Course, String> seatsColumn;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> departmentFilter;

    @FXML private Button enrollButton;
    @FXML private Button viewCatalogButton;
    @FXML private Button backButton;

    @FXML private Label statusLabel;
    @FXML private Label creditsLabel;

    private ObservableList<Course> coursesList = FXCollections.observableArrayList();
    private CourseService courseService = new CourseService();
    private EnrollmentService enrollmentService = new EnrollmentService();
    private AuthService authService = AuthService.getInstance();

    @FXML
    public void initialize() {
        if (!checkStudentAccess()) {
            disableAllControls();
            return;
        }

        setupTableColumns();
        populateFilters();
        loadAvailableCourses();
        updateCreditsDisplay();
    }

    private boolean checkStudentAccess() {
        if (!authService.isLoggedIn()) {
            showError("Access Denied", "Please login to enroll in courses.");
            return false;
        }

        String userType = authService.getCurrentUserType();
        if (!"STUDENT".equals(userType)) {
            showError("Access Denied", "Only students can enroll in courses.");
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

        creditsColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(String.valueOf(cellData.getValue().getCredits()))
        );

        departmentColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getDepartment())
        );

        seatsColumn.setCellValueFactory(cellData -> {
            Course course = cellData.getValue();
            String seats = course.getAvailableSeats() + " / " + course.getMaxSeats();
            return new javafx.beans.property.SimpleStringProperty(seats);
        });
    }

    private void populateFilters() {
        try {
            List<String> departments = courseService.getAllDepartments();
            departmentFilter.getItems().add("All Departments");
            departmentFilter.getItems().addAll(departments);
            departmentFilter.setValue("All Departments");
        } catch (SQLException e) {
            showError("Database Error", "Failed to load filter options: " + e.getMessage());
        }
    }

    private void loadAvailableCourses() {
        try {
            List<Course> courses = courseService.getAllCourses(true); // Active courses only
            coursesList.clear();
            coursesList.addAll(courses);
            statusLabel.setText("Loaded " + courses.size() + " available course(s)");
        } catch (SQLException e) {
            showError("Database Error", "Failed to load courses: " + e.getMessage());
        }
    }

    private void updateCreditsDisplay() {
        try {
            User student = authService.getCurrentUser();
            int totalCredits = enrollmentService.getTotalEnrolledCredits(student);
            creditsLabel.setText("Total Enrolled Credits: " + totalCredits + " / 18");
        } catch (SQLException e) {
            creditsLabel.setText("Total Enrolled Credits: Error loading");
        }
    }

    @FXML
    private void handleSearch() {
        String keyword = searchField.getText().trim();
        if (keyword.isEmpty()) {
            loadAvailableCourses();
            return;
        }

        try {
            List<Course> courses = courseService.searchCourses(keyword, true);
            coursesList.clear();
            coursesList.addAll(courses);
            statusLabel.setText("Found " + courses.size() + " course(s)");
        } catch (SQLException e) {
            showError("Database Error", "Failed to search courses: " + e.getMessage());
        }
    }

    @FXML
    private void handleFilter() {
        String department = departmentFilter.getValue();

        try {
            List<Course> courses = courseService.filterCourses(
                department != null && !department.equals("All Departments") ? department : null,
                null, null, true
            );
            coursesList.clear();
            coursesList.addAll(courses);
            statusLabel.setText("Filtered: " + courses.size() + " course(s)");
        } catch (SQLException e) {
            showError("Database Error", "Failed to filter courses: " + e.getMessage());
        }
    }

    @FXML
    private void handleEnroll() {
        Course selectedCourse = availableCoursesTable.getSelectionModel().getSelectedItem();
        if (selectedCourse == null) {
            showWarning("No Selection", "Please select a course to enroll in.");
            return;
        }

        User student = authService.getCurrentUser();
        if (student == null) {
            showError("Authentication Error", "User session expired. Please login again.");
            return;
        }

        try {
            Enrollment enrollment = enrollmentService.enrollStudent(student, selectedCourse);

            if (enrollment != null) {
                showInfo("Enrollment Successful", 
                    "You have been successfully enrolled in " + selectedCourse.getCode() + " - " + selectedCourse.getName() + ".\n" +
                    "Credits: " + selectedCourse.getCredits());
                
                // Refresh data
                loadAvailableCourses();
                updateCreditsDisplay();
            } else {
                showError("Enrollment Failed", "Failed to enroll in course. Please try again.");
            }
        } catch (IllegalArgumentException e) {
            showError("Enrollment Error", e.getMessage());
        } catch (SQLException e) {
            showError("Database Error", "Failed to enroll: " + e.getMessage());
        }
    }

    @FXML
    private void handleViewCatalog(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/student_course_catalog.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Course Catalog");
            stage.show();
        } catch (IOException e) {
            showError("Navigation Error", "Could not open course catalog: " + e.getMessage());
        }
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
        if (availableCoursesTable != null) availableCoursesTable.setDisable(true);
        if (enrollButton != null) enrollButton.setDisable(true);
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

