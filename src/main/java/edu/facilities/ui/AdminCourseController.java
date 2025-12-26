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
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/**
 * Controller for Course Catalog Management
 * US 2.1 - Manage Course Catalog (Admin)
 */
public class AdminCourseController {

    @FXML private TableView<Course> coursesTable;
    @FXML private TableColumn<Course, String> codeColumn;
    @FXML private TableColumn<Course, String> nameColumn;
    @FXML private TableColumn<Course, String> departmentColumn;
    @FXML private TableColumn<Course, String> semesterColumn;
    @FXML private TableColumn<Course, String> typeColumn;
    @FXML private TableColumn<Course, String> creditsColumn;
    @FXML private TableColumn<Course, String> seatsColumn;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> departmentFilter;
    @FXML private ComboBox<String> semesterFilter;
    @FXML private ComboBox<String> typeFilter;

    @FXML private Button addButton;
    @FXML private Button editButton;
    @FXML private Button deleteButton;
    @FXML private Button manageAttributesButton;
    @FXML private Button backButton;

    @FXML private Label statusLabel;

    private ObservableList<Course> coursesList = FXCollections.observableArrayList();
    private CourseService courseService = new CourseService();
    private AuthService authService = AuthService.getInstance();

    @FXML
    public void initialize() {
        if (!checkAdminAccess()) {
            disableAllControls();
            return;
        }

        setupTableColumns();
        populateFilters();
        loadCourses();
    }

    private boolean checkAdminAccess() {
        if (!authService.isLoggedIn()) {
            showError("Access Denied", "Please login to access this feature.");
            return false;
        }

        String userType = authService.getCurrentUserType();
        if (!"ADMIN".equals(userType)) {
            showError("Access Denied", "Only administrators can manage courses.");
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
            List<Course> courses = courseService.getAllCourses(false); // Show all including inactive
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
            List<Course> courses = courseService.searchCourses(keyword, false);
            coursesList.clear();
            coursesList.addAll(courses);
        } catch (SQLException e) {
            showError("Database Error", "Failed to search courses: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleFilterDepartment() {
        handleFilter();
    }
    
    @FXML
    private void handleFilterSemester() {
        handleFilter();
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
                false
            );
            coursesList.clear();
            coursesList.addAll(courses);
            statusLabel.setText("Filtered: " + courses.size() + " course(s)");
        } catch (SQLException e) {
            showError("Database Error", "Failed to filter courses: " + e.getMessage());
        }
    }

    @FXML
    private void handleAddCourse() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/add_course.fxml"));
            Parent root = loader.load();

            AddCourseController controller = loader.getController();
            controller.setCourseService(courseService);

            Stage stage = new Stage();
            stage.setTitle("Add New Course");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));

            stage.setOnHidden(event -> {
                loadCourses();
            });

            stage.showAndWait();
        } catch (IOException e) {
            showError("Error", "Could not open Add Course window: " + e.getMessage());
        }
    }

    @FXML
    private void handleEditCourse() {
        Course selectedCourse = coursesTable.getSelectionModel().getSelectedItem();
        if (selectedCourse == null) {
            showWarning("No Selection", "Please select a course to edit.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/edit_course.fxml"));
            Parent root = loader.load();

            EditCourseController controller = loader.getController();
            controller.setCourseService(courseService);
            controller.setCourse(selectedCourse);

            Stage stage = new Stage();
            stage.setTitle("Edit Course - " + selectedCourse.getCode());
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));

            stage.setOnHidden(event -> {
                loadCourses();
            });

            stage.showAndWait();
        } catch (IOException e) {
            showError("Error", "Could not open Edit Course window: " + e.getMessage());
        }
    }

    @FXML
    private void handleDeleteCourse() {
        Course selectedCourse = coursesTable.getSelectionModel().getSelectedItem();
        if (selectedCourse == null) {
            showWarning("No Selection", "Please select a course to delete.");
            return;
        }

        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Delete Course");
        confirmDialog.setHeaderText("Are you sure you want to delete this course?");
        confirmDialog.setContentText("Course: " + selectedCourse.getCode() + " - " + selectedCourse.getName());

        confirmDialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    boolean deleted = courseService.deleteCourse(selectedCourse.getId());
                    if (deleted) {
                        loadCourses();
                        showInfo("Success", "Course deleted successfully!");
                    } else {
                        showError("Error", "Failed to delete course.");
                    }
                } catch (IllegalArgumentException e) {
                    showError("Cannot Delete Course", e.getMessage());
                } catch (SQLException e) {
                    showError("Database Error", "Failed to delete course: " + e.getMessage());
                }
            }
        });
    }

    @FXML
    private void handleManageAttributes() {
        Course selectedCourse = coursesTable.getSelectionModel().getSelectedItem();
        if (selectedCourse == null) {
            showWarning("No Selection", "Please select a course to manage attributes.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/course_attributes.fxml"));
            Parent root = loader.load();

            CourseAttributesController controller = loader.getController();
            controller.setCourseService(courseService);
            controller.setCourse(selectedCourse);

            Stage stage = new Stage();
            stage.setTitle("Manage Attributes - " + selectedCourse.getCode());
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));

            stage.showAndWait();
        } catch (IOException e) {
            showError("Error", "Could not open Attributes window: " + e.getMessage());
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
        if (coursesTable != null) coursesTable.setDisable(true);
        if (addButton != null) addButton.setDisable(true);
        if (editButton != null) editButton.setDisable(true);
        if (deleteButton != null) deleteButton.setDisable(true);
        if (manageAttributesButton != null) manageAttributesButton.setDisable(true);
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

