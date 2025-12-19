package edu.facilities.ui;

import edu.facilities.model.Course;
import edu.facilities.model.CourseType;
import edu.facilities.service.AuthService;
import edu.facilities.service.CourseService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.sql.SQLException;

/**
 * Controller for Add Course Form
 */
public class AddCourseController {

    @FXML private TextField codeField;
    @FXML private TextField nameField;
    @FXML private TextArea descriptionArea;
    @FXML private TextField creditsField;
    @FXML private TextField departmentField;
    @FXML private TextField semesterField;
    @FXML private ComboBox<String> typeComboBox;
    @FXML private TextField maxSeatsField;

    @FXML private Label codeError;
    @FXML private Label nameError;
    @FXML private Label creditsError;
    @FXML private Label departmentError;
    @FXML private Label semesterError;
    @FXML private Label typeError;
    @FXML private Label seatsError;

    @FXML private Button saveButton;
    @FXML private Button cancelButton;

    private CourseService courseService;
    private AuthService authService = AuthService.getInstance();

    @FXML
    public void initialize() {
        typeComboBox.getItems().addAll("CORE", "ELECTIVE");
        typeComboBox.setValue("CORE");
    }

    public void setCourseService(CourseService courseService) {
        this.courseService = courseService;
    }

    @FXML
    private void handleSave() {
        if (!checkAdminAccess()) {
            return;
        }

        clearErrors();

        if (!validateInputs()) {
            return;
        }

        try {
            String code = codeField.getText().trim();
            String name = nameField.getText().trim();
            String description = descriptionArea.getText().trim();
            int credits = Integer.parseInt(creditsField.getText().trim());
            String department = departmentField.getText().trim();
            String semester = semesterField.getText().trim();
            CourseType type = CourseType.valueOf(typeComboBox.getValue());
            int maxSeats = Integer.parseInt(maxSeatsField.getText().trim());

            Course course = courseService.createCourse(code, name, description, credits, department, semester, type, maxSeats);

            if (course != null) {
                showInfo("Success", "Course created successfully!");
                closeWindow();
            } else {
                showError("Error", "Failed to create course.");
            }
        } catch (SQLException e) {
            if (e.getMessage().contains("UNIQUE") || e.getMessage().contains("duplicate")) {
                showFieldError(codeError, "Course code already exists.");
            } else {
                showError("Database Error", "Failed to create course: " + e.getMessage());
            }
        } catch (Exception e) {
            showError("Error", "An error occurred: " + e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        closeWindow();
    }

    private boolean checkAdminAccess() {
        if (!authService.isLoggedIn() || !"ADMIN".equals(authService.getCurrentUserType())) {
            showError("Access Denied", "Only administrators can create courses.");
            return false;
        }
        return true;
    }

    private boolean validateInputs() {
        boolean isValid = true;

        if (codeField.getText().trim().isEmpty()) {
            showFieldError(codeError, "Course code is required");
            isValid = false;
        }

        if (nameField.getText().trim().isEmpty()) {
            showFieldError(nameError, "Course name is required");
            isValid = false;
        }

        try {
            int credits = Integer.parseInt(creditsField.getText().trim());
            if (credits <= 0 || credits > 6) {
                showFieldError(creditsError, "Credits must be between 1 and 6");
                isValid = false;
            }
        } catch (NumberFormatException e) {
            showFieldError(creditsError, "Credits must be a number");
            isValid = false;
        }

        if (departmentField.getText().trim().isEmpty()) {
            showFieldError(departmentError, "Department is required");
            isValid = false;
        }

        if (semesterField.getText().trim().isEmpty()) {
            showFieldError(semesterError, "Semester is required");
            isValid = false;
        }

        if (typeComboBox.getValue() == null) {
            showFieldError(typeError, "Course type is required");
            isValid = false;
        }

        try {
            int maxSeats = Integer.parseInt(maxSeatsField.getText().trim());
            if (maxSeats <= 0) {
                showFieldError(seatsError, "Max seats must be greater than 0");
                isValid = false;
            }
        } catch (NumberFormatException e) {
            showFieldError(seatsError, "Max seats must be a number");
            isValid = false;
        }

        return isValid;
    }

    private void clearErrors() {
        if (codeError != null) codeError.setVisible(false);
        if (nameError != null) nameError.setVisible(false);
        if (creditsError != null) creditsError.setVisible(false);
        if (departmentError != null) departmentError.setVisible(false);
        if (semesterError != null) semesterError.setVisible(false);
        if (typeError != null) typeError.setVisible(false);
        if (seatsError != null) seatsError.setVisible(false);
    }

    private void showFieldError(Label errorLabel, String message) {
        if (errorLabel != null) {
            errorLabel.setText(message);
            errorLabel.setVisible(true);
        }
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
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

    private void closeWindow() {
        Stage stage = (Stage) saveButton.getScene().getWindow();
        stage.close();
    }
}

