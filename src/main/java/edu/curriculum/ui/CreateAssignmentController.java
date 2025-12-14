package edu.curriculum.ui;

import edu.curriculum.model.Assignment;
import edu.curriculum.model.Course;
import edu.curriculum.model.SubmissionType;
import edu.curriculum.service.AssignmentService;
import edu.curriculum.service.CourseService;
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
 * Controller for Create Assignment Screen (create_assignment.fxml)
 */
public class CreateAssignmentController {

    @FXML private ComboBox<String> courseComboBox;
    @FXML private TextField titleField;
    @FXML private TextArea instructionsArea;
    @FXML private DatePicker dueDatePicker;
    @FXML private Spinner<Integer> hourSpinner;
    @FXML private Spinner<Integer> minuteSpinner;
    @FXML private Spinner<Integer> totalPointsSpinner;
    @FXML private ComboBox<String> submissionTypeComboBox;
    @FXML private CheckBox allowLateCheckBox;
    @FXML private TextField maxFileSizeField;
    @FXML private Button backButton;

    @FXML private Label courseError;
    @FXML private Label titleError;
    @FXML private Label instructionsError;
    @FXML private Label dueDateError;
    @FXML private Label pointsError;

    private AssignmentService assignmentService = new AssignmentService();
    private CourseService courseService = new CourseService();
    private AuthService authService = AuthService.getInstance();
    private Map<String, Course> courseMap = new HashMap<>();

    @FXML
    public void initialize() {
        // Check if user is logged in
        if (!authService.isLoggedIn()) {
            showError("Access Denied", "Please login to create assignments.");
            disableAllControls();
            return;
        }

        // Check user type - only PROFESSOR can create assignments
        String userType = authService.getCurrentUserType();
        if (!"PROFESSOR".equals(userType)) {
            showError("Access Denied", "Only professors can create assignments.");
            disableAllControls();
            return;
        }

        // Populate submission type dropdown
        submissionTypeComboBox.getItems().addAll("FILE", "TEXT", "BOTH");
        submissionTypeComboBox.setValue("TEXT");

        // Initialize spinners
        SpinnerValueFactory<Integer> hourFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 23);
        SpinnerValueFactory<Integer> minuteFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 59, 15);
        SpinnerValueFactory<Integer> pointsFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 1000, 100);

        hourSpinner.setValueFactory(hourFactory);
        minuteSpinner.setValueFactory(minuteFactory);
        totalPointsSpinner.setValueFactory(pointsFactory);

        // Set default date to tomorrow
        dueDatePicker.setValue(LocalDate.now().plusDays(1));

        // Load professor's courses
        loadCourses();

        System.out.println("CreateAssignmentController initialized");
    }

    /**
     * Load courses for the current professor
     */
    private void loadCourses() {
        try {
            User currentUser = authService.getCurrentUser();
            if (currentUser == null) {
                System.err.println("ERROR: Current user is null in CreateAssignmentController.loadCourses()");
                showError("Authentication Error", "User session expired. Please login again.");
                return;
            }

            String professorId = currentUser.getId();
            String professorUsername = currentUser.getUsername();
            System.out.println("Loading courses for professor - ID: " + professorId + ", Username: " + professorUsername);

            List<Course> courses = courseService.getCoursesByProfessor(professorId);
            courseMap.clear();
            courseComboBox.getItems().clear();

            System.out.println("Found " + courses.size() + " courses for professor ID: " + professorId);

            if (courses.isEmpty()) {
                System.out.println("WARNING: No courses found for professor ID: " + professorId);
                System.out.println("This may mean:");
                System.out.println("  1. No courses have been created in the database");
                System.out.println("  2. Courses exist but ProfessorUserID doesn't match this professor's UserID");
                System.out.println("  3. Database connection issue");
                
                // Show helpful message to user
                courseComboBox.setPromptText("No courses available - Please create courses first");
                showError("No Courses Found", 
                    "No courses were found for your account.\n\n" +
                    "Please ensure:\n" +
                    "1. Courses have been created in the system\n" +
                    "2. Courses are assigned to your professor account (UserID: " + professorId + ")\n" +
                    "3. Contact administrator if courses should be available");
                return;
            }

            for (Course course : courses) {
                String displayName = course.getCode() + " - " + course.getName();
                courseComboBox.getItems().add(displayName);
                courseMap.put(displayName, course);
                System.out.println("  - Added course: " + displayName + " (ID: " + course.getId() + ")");
            }

            System.out.println("Successfully loaded " + courses.size() + " courses into dropdown");
        } catch (SQLException e) {
            System.err.println("SQLException in loadCourses(): " + e.getMessage());
            e.printStackTrace();
            showError("Database Error", 
                "Failed to load courses from database.\n\n" +
                "Error: " + e.getMessage() + "\n\n" +
                "Please check:\n" +
                "1. Database connection is working\n" +
                "2. Courses table exists\n" +
                "3. Your professor account is properly configured");
        } catch (Exception e) {
            System.err.println("Unexpected error in loadCourses(): " + e.getMessage());
            e.printStackTrace();
            showError("Error", "An unexpected error occurred while loading courses: " + e.getMessage());
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
        String instructions = instructionsArea.getText().trim();
        LocalDate dueDate = dueDatePicker.getValue();
        int hour = hourSpinner.getValue();
        int minute = minuteSpinner.getValue();
        int totalPoints = totalPointsSpinner.getValue();
        String submissionTypeStr = submissionTypeComboBox.getValue();

        // Build due date time
        LocalDateTime dueDateTime = LocalDateTime.of(dueDate, LocalTime.of(hour, minute));

        // Validate due date not in past
        if (dueDateTime.isBefore(LocalDateTime.now())) {
            dueDateError.setText("Due date cannot be in the past");
            dueDateError.setVisible(true);
            return;
        }

        // Convert submission type
        SubmissionType submissionType = stringToSubmissionType(submissionTypeStr);

        // Build EAV attributes
        Map<String, String> attributes = new HashMap<>();
        if (allowLateCheckBox.isSelected()) {
            attributes.put("AllowLateSubmissions", "true");
        }
        if (maxFileSizeField.getText() != null && !maxFileSizeField.getText().trim().isEmpty()) {
            attributes.put("MaxFileSizeMB", maxFileSizeField.getText().trim());
        }

        // Create assignment
        try {
            Assignment assignment = assignmentService.createAssignment(
                    selectedCourse.getId(), title, instructions, dueDateTime,
                    totalPoints, submissionType, attributes
            );

            if (assignment != null) {
                showSuccess("Assignment created successfully!");
                closeWindow();
            } else {
                showError("Error", "Failed to create assignment.");
            }
        } catch (SQLException e) {
            showError("Database Error", "Failed to create assignment: " + e.getMessage());
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
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/professor_assignments.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Assignment Management");
            stage.show();
        } catch (IOException e) {
            showError("Error", "Unable to return to assignments page: " + e.getMessage());
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

        // Validate instructions
        if (instructionsArea.getText().trim().isEmpty()) {
            instructionsError.setText("Instructions are required");
            instructionsError.setVisible(true);
            isValid = false;
        }

        // Validate due date
        if (dueDatePicker.getValue() == null) {
            dueDateError.setText("Due date is required");
            dueDateError.setVisible(true);
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
        instructionsError.setVisible(false);
        dueDateError.setVisible(false);
        pointsError.setVisible(false);
    }

    /**
     * Close the current window
     */
    private void closeWindow() {
        Stage stage = (Stage) backButton.getScene().getWindow();
        stage.close();
    }

    /**
     * Convert string to SubmissionType enum
     */
    private SubmissionType stringToSubmissionType(String typeStr) {
        if (typeStr == null) return SubmissionType.TEXT;
        switch (typeStr.toUpperCase()) {
            case "FILE": return SubmissionType.FILE;
            case "TEXT": return SubmissionType.TEXT;
            case "BOTH": return SubmissionType.BOTH;
            default: return SubmissionType.TEXT;
        }
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
        if (instructionsArea != null) instructionsArea.setDisable(true);
        if (dueDatePicker != null) dueDatePicker.setDisable(true);
        if (totalPointsSpinner != null) totalPointsSpinner.setDisable(true);
    }
}

