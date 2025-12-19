package edu.facilities.ui;

import edu.facilities.model.Course;
import edu.facilities.model.CourseType;
import edu.facilities.service.AuthService;
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
import java.util.List;

/**
 * Controller for Student Course Catalog View
 * US 2.2 - View Course Catalog (Student)
 */
public class StudentCourseCatalogController {

    @FXML private TableView<Course> coursesTable;
    @FXML private TableColumn<Course, String> codeColumn;
    @FXML private TableColumn<Course, String> nameColumn;
    @FXML private TableColumn<Course, String> descriptionColumn;
    @FXML private TableColumn<Course, String> departmentColumn;
    @FXML private TableColumn<Course, String> semesterColumn;
    @FXML private TableColumn<Course, String> typeColumn;
    @FXML private TableColumn<Course, String> creditsColumn;
    @FXML private TableColumn<Course, String> seatsColumn;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> departmentFilter;
    @FXML private ComboBox<String> semesterFilter;
    @FXML private ComboBox<String> typeFilter;

    @FXML private Button viewDetailsButton;
    @FXML private Button enrollButton;
    @FXML private Button backButton;

    @FXML private Label statusLabel;

    private ObservableList<Course> coursesList = FXCollections.observableArrayList();
    private CourseService courseService = new CourseService();
    private AuthService authService = AuthService.getInstance();

    @FXML
    public void initialize() {
        if (!checkStudentAccess()) {
            disableAllControls();
            return;
        }

        setupTableColumns();
        populateFilters();
        loadCourses();
    }

    private boolean checkStudentAccess() {
        if (!authService.isLoggedIn()) {
            showError("Access Denied", "Please login to view courses.");
            return false;
        }

        String userType = authService.getCurrentUserType();
        if (!"STUDENT".equals(userType)) {
            showError("Access Denied", "Only students can view the course catalog.");
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

        descriptionColumn.setCellValueFactory(cellData -> {
            String desc = cellData.getValue().getDescription();
            if (desc != null && desc.length() > 50) {
                desc = desc.substring(0, 50) + "...";
            }
            return new javafx.beans.property.SimpleStringProperty(desc != null ? desc : "");
        });

        departmentColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getDepartment())
        );

        semesterColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getSemester())
        );

        typeColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getType().toString())
        );

        creditsColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(String.valueOf(cellData.getValue().getCredits()))
        );

        seatsColumn.setCellValueFactory(cellData -> {
            Course course = cellData.getValue();
            String seats = course.getCurrentSeats() + " / " + course.getMaxSeats();
            return new javafx.beans.property.SimpleStringProperty(seats);
        });
    }

    private void populateFilters() {
        try {
            List<String> departments = courseService.getAllDepartments();
            departmentFilter.getItems().add("All Departments");
            departmentFilter.getItems().addAll(departments);
            departmentFilter.setValue("All Departments");

            List<String> semesters = courseService.getAllSemesters();
            semesterFilter.getItems().add("All Semesters");
            semesterFilter.getItems().addAll(semesters);
            semesterFilter.setValue("All Semesters");

            typeFilter.getItems().addAll("All Types", "CORE", "ELECTIVE");
            typeFilter.setValue("All Types");
        } catch (SQLException e) {
            showError("Database Error", "Failed to load filter options: " + e.getMessage());
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
    private void handleSearch() {
        String keyword = searchField.getText().trim();
        if (keyword.isEmpty()) {
            loadCourses();
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
        String semester = semesterFilter.getValue();
        String typeStr = typeFilter.getValue();

        CourseType type = null;
        if (typeStr != null && !typeStr.equals("All Types")) {
            type = CourseType.valueOf(typeStr);
        }

        try {
            List<Course> courses = courseService.filterCourses(
                department != null && !department.equals("All Departments") ? department : null,
                semester != null && !semester.equals("All Semesters") ? semester : null,
                type,
                true
            );
            coursesList.clear();
            coursesList.addAll(courses);
            statusLabel.setText("Filtered: " + courses.size() + " course(s)");
        } catch (SQLException e) {
            showError("Database Error", "Failed to filter courses: " + e.getMessage());
        }
    }

    @FXML
    private void handleViewDetails() {
        Course selectedCourse = coursesTable.getSelectionModel().getSelectedItem();
        if (selectedCourse == null) {
            showWarning("No Selection", "Please select a course to view details.");
            return;
        }

        // Show course details in a dialog
        StringBuilder details = new StringBuilder();
        details.append("Course Code: ").append(selectedCourse.getCode()).append("\n");
        details.append("Course Name: ").append(selectedCourse.getName()).append("\n");
        details.append("Description: ").append(selectedCourse.getDescription() != null ? selectedCourse.getDescription() : "N/A").append("\n");
        details.append("Credits: ").append(selectedCourse.getCredits()).append("\n");
        details.append("Department: ").append(selectedCourse.getDepartment()).append("\n");
        details.append("Semester: ").append(selectedCourse.getSemester()).append("\n");
        details.append("Type: ").append(selectedCourse.getType()).append("\n");
        details.append("Available Seats: ").append(selectedCourse.getAvailableSeats()).append(" / ").append(selectedCourse.getMaxSeats()).append("\n");

        if (!selectedCourse.getPrerequisites().isEmpty()) {
            details.append("\nPrerequisites: ");
            for (Course prereq : selectedCourse.getPrerequisites()) {
                details.append(prereq.getCode()).append(" ");
            }
        }

        if (!selectedCourse.getProfessors().isEmpty()) {
            details.append("\nProfessors: ");
            for (edu.facilities.model.User prof : selectedCourse.getProfessors()) {
                details.append(prof.getUsername()).append(" ");
            }
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Course Details");
        alert.setHeaderText(selectedCourse.getCode() + " - " + selectedCourse.getName());
        alert.setContentText(details.toString());
        alert.showAndWait();
    }

    @FXML
    private void handleEnroll() {
        Course selectedCourse = coursesTable.getSelectionModel().getSelectedItem();
        if (selectedCourse == null) {
            showWarning("No Selection", "Please select a course to enroll in.");
            return;
        }

        // Navigate to enrollment page (or handle enrollment here)
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/student_enrollment.fxml"));
            Stage stage = (Stage) enrollButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Enroll in Courses");
            stage.show();
        } catch (IOException e) {
            showError("Navigation Error", "Could not open enrollment page: " + e.getMessage());
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
        if (coursesTable != null) coursesTable.setDisable(true);
        if (viewDetailsButton != null) viewDetailsButton.setDisable(true);
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
}

