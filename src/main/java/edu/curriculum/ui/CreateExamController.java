package edu.curriculum.ui;

import edu.curriculum.model.Course;
import edu.curriculum.model.Exam;
import edu.curriculum.service.CourseService;
import edu.curriculum.service.ExamService;
import edu.facilities.model.User;
import edu.facilities.service.AuthService;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for Create Exam Screen (create_exam.fxml)
 * US 2.12 - Create Exam
 */
public class CreateExamController {

    @FXML private ComboBox<String> courseComboBox;
    @FXML private TextField titleField;
    @FXML private DatePicker examDatePicker;
    @FXML private Spinner<Integer> hourSpinner;
    @FXML private Spinner<Integer> minuteSpinner;
    @FXML private Spinner<Integer> durationSpinner;
    @FXML private TextField locationField;
    @FXML private Spinner<Integer> totalPointsSpinner;
    @FXML private TextArea instructionsArea;
    @FXML private CheckBox openBookCheckBox;
    @FXML private CheckBox calculatorAllowedCheckBox;
    @FXML private CheckBox onlineProctoredCheckBox;
    @FXML private Button backButton;

    @FXML private Label courseError;
    @FXML private Label titleError;
    @FXML private Label examDateError;
    @FXML private Label durationError;
    @FXML private Label pointsError;

    private ExamService examService = new ExamService();
    private CourseService courseService = new CourseService();
    private AuthService authService = AuthService.getInstance();
    private Map<String, Course> courseMap = new HashMap<>();

    @FXML
    public void initialize() {
        // Check if user is logged in
        if (!authService.isLoggedIn()) {
            showError("Access Denied", "Please login to create exams.");
            disableAllControls();
            return;
        }

        // Check user type - only PROFESSOR can create exams
        String userType = authService.getCurrentUserType();
        if (!"PROFESSOR".equals(userType)) {
            showError("Access Denied", "Only professors can create exams.");
            disableAllControls();
            return;
        }

        // Initialize spinners
        SpinnerValueFactory<Integer> hourFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 9);
        SpinnerValueFactory<Integer> minuteFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0, 15);
        SpinnerValueFactory<Integer> durationFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(30, 480, 120, 15);
        SpinnerValueFactory<Integer> pointsFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 1000, 100);

        hourSpinner.setValueFactory(hourFactory);
        minuteSpinner.setValueFactory(minuteFactory);
        durationSpinner.setValueFactory(durationFactory);
        totalPointsSpinner.setValueFactory(pointsFactory);

        // Set default date to tomorrow
        examDatePicker.setValue(LocalDate.now().plusDays(1));

        // Load professor's courses
        loadCourses();
    }

    /**
     * Load courses for the current professor
     */
    private void loadCourses() {
        try {
            User currentUser = authService.getCurrentUser();
            if (currentUser == null) {
                showError("Authentication Error", "User session expired. Please login again.");
                return;
            }

            String professorId = currentUser.getId();
            List<Course> courses = courseService.getCoursesByProfessor(professorId);
            courseMap.clear();
            courseComboBox.getItems().clear();

            if (courses.isEmpty()) {
                courseComboBox.setPromptText("No courses available - Please create courses first");
                showError("No Courses Found", 
                    "No courses were found for your account.\n\n" +
                    "Please ensure courses are assigned to your professor account.");
                return;
            }

            for (Course course : courses) {
                String displayName = course.getCode() + " - " + course.getName();
                courseComboBox.getItems().add(displayName);
                courseMap.put(displayName, course);
            }
        } catch (SQLException e) {
            showError("Database Error", "Failed to load courses: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleCreate() throws SQLException {
        // Clear previous errors
        clearErrors();

        // Validate inputs
        if (!validateInputs()) {
            return;
        }

        // Get selected course
        String selectedCourseDisplay = courseComboBox.getValue();
        if (selectedCourseDisplay == null || !courseMap.containsKey(selectedCourseDisplay)) {
            courseError.setText("Please select a course");
            courseError.setVisible(true);
            return;
        }

        Course selectedCourse = courseMap.get(selectedCourseDisplay);

        // Get form values
        String title = titleField.getText().trim();
        LocalDate examDate = examDatePicker.getValue();
        int hour = hourSpinner.getValue();
        int minute = minuteSpinner.getValue();
        int durationMinutes = durationSpinner.getValue();
        String location = locationField.getText().trim();
        int totalPoints = totalPointsSpinner.getValue();
        String instructions = instructionsArea.getText().trim();

        // Build exam date time
        LocalDateTime examDateTime = LocalDateTime.of(examDate, LocalTime.of(hour, minute));

        // Build EAV attributes
        Map<String, String> attributes = new HashMap<>();
        if (openBookCheckBox.isSelected()) {
            attributes.put("OpenBook", "true");
        }
        if (calculatorAllowedCheckBox.isSelected()) {
            attributes.put("CalculatorAllowed", "true");
        }
        if (onlineProctoredCheckBox.isSelected()) {
            attributes.put("OnlineProctored", "true");
        }

        // Create exam
        try {
            Exam exam = examService.createExam(
                    selectedCourse.getId(), title, examDateTime, durationMinutes,
                    location, totalPoints, instructions, attributes
            );

            if (exam != null) {
                showSuccess("Exam created successfully!");
                closeWindow();
            } else {
                showError("Error", "Failed to create exam.");
            }
        } catch (SQLException e) {
            showError("Database Error", "Failed to create exam: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleCancel() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Cancel");
        confirm.setHeaderText("Are you sure you want to cancel?");
        confirm.setContentText("Any unsaved changes will be lost.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                closeWindow();
            }
        });
    }

    @FXML
    private void handleBack(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/professor_exams.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("");
            stage.setMaximized(true);
            stage.show();
        } catch (IOException e) {
            showError("Error", "Unable to return to exams page: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Validate all form inputs
     */
    private boolean validateInputs() {
        boolean isValid = true;

        // Validate course
        if (courseComboBox.getValue() == null) {
            courseError.setText("Please select a course");
            courseError.setVisible(true);
            isValid = false;
        }

        // Validate title
        if (titleField.getText().trim().isEmpty()) {
            titleError.setText("Title is required");
            titleError.setVisible(true);
            isValid = false;
        }

        // Validate exam date
        if (examDatePicker.getValue() == null) {
            examDateError.setText("Exam date is required");
            examDateError.setVisible(true);
            isValid = false;
        }

        // Validate duration
        if (durationSpinner.getValue() <= 0) {
            durationError.setText("Duration must be greater than 0");
            durationError.setVisible(true);
            isValid = false;
        }

        // Validate points
        if (totalPointsSpinner.getValue() <= 0) {
            pointsError.setText("Total points must be greater than 0");
            pointsError.setVisible(true);
            isValid = false;
        }

        return isValid;
    }

    /**
     * Clear all error messages
     */
    private void clearErrors() {
        courseError.setVisible(false);
        titleError.setVisible(false);
        examDateError.setVisible(false);
        durationError.setVisible(false);
        pointsError.setVisible(false);
    }

    /**
     * Close the current window
     */
    private void closeWindow() {
        Stage stage = (Stage) backButton.getScene().getWindow();
        stage.close();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
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

    private void disableAllControls() {
        if (courseComboBox != null) courseComboBox.setDisable(true);
        if (titleField != null) titleField.setDisable(true);
        if (examDatePicker != null) examDatePicker.setDisable(true);
        if (durationSpinner != null) durationSpinner.setDisable(true);
        if (totalPointsSpinner != null) totalPointsSpinner.setDisable(true);
    }
}

