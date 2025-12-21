package edu.curriculum.ui;

import edu.curriculum.model.Course;
import edu.curriculum.model.Quiz;
import edu.curriculum.service.CourseService;
import edu.curriculum.service.QuizService;
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
 * Controller for Create Quiz Screen (create_quiz.fxml)
 * US 2.10 - Create Quiz
 */
public class CreateQuizController {

    @FXML private ComboBox<String> courseComboBox;
    @FXML private TextField titleField;
    @FXML private TextArea instructionsArea;
    @FXML private DatePicker dueDatePicker;
    @FXML private Spinner<Integer> hourSpinner;
    @FXML private Spinner<Integer> minuteSpinner;
    @FXML private Spinner<Integer> totalPointsSpinner;
    @FXML private Spinner<Integer> timeLimitSpinner;
    @FXML private Spinner<Integer> attemptsAllowedSpinner;
    @FXML private CheckBox autoGradingCheckBox;
    @FXML private CheckBox shuffleQuestionsCheckBox;
    @FXML private Button backButton;

    @FXML private Label courseError;
    @FXML private Label titleError;
    @FXML private Label dueDateError;
    @FXML private Label pointsError;

    private QuizService quizService = new QuizService();
    private CourseService courseService = new CourseService();
    private AuthService authService = AuthService.getInstance();
    private Map<String, Course> courseMap = new HashMap<>();

    @FXML
    public void initialize() {
        // Check if user is logged in
        if (!authService.isLoggedIn()) {
            showError("Access Denied", "Please login to create quizzes.");
            disableAllControls();
            return;
        }

        // Check user type - only PROFESSOR can create quizzes
        String userType = authService.getCurrentUserType();
        if (!"PROFESSOR".equals(userType)) {
            showError("Access Denied", "Only professors can create quizzes.");
            disableAllControls();
            return;
        }

        // Initialize spinners
        SpinnerValueFactory<Integer> hourFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 23);
        SpinnerValueFactory<Integer> minuteFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 59, 15);
        SpinnerValueFactory<Integer> pointsFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 1000, 100);
        SpinnerValueFactory<Integer> timeLimitFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(5, 300, 60, 5);
        SpinnerValueFactory<Integer> attemptsFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10, 1);

        hourSpinner.setValueFactory(hourFactory);
        minuteSpinner.setValueFactory(minuteFactory);
        totalPointsSpinner.setValueFactory(pointsFactory);
        timeLimitSpinner.setValueFactory(timeLimitFactory);
        attemptsAllowedSpinner.setValueFactory(attemptsFactory);

        // Set default date to tomorrow
        dueDatePicker.setValue(LocalDate.now().plusDays(1));

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
        String instructions = instructionsArea.getText().trim();
        LocalDate dueDate = dueDatePicker.getValue();
        int hour = hourSpinner.getValue();
        int minute = minuteSpinner.getValue();
        int totalPoints = totalPointsSpinner.getValue();

        // Build due date time
        LocalDateTime dueDateTime = LocalDateTime.of(dueDate, LocalTime.of(hour, minute));

        // Validate due date not in past
        if (dueDateTime.isBefore(LocalDateTime.now())) {
            dueDateError.setText("Due date cannot be in the past");
            dueDateError.setVisible(true);
            return;
        }

        // Build EAV attributes
        Map<String, String> attributes = new HashMap<>();
        int timeLimit = timeLimitSpinner.getValue();
        if (timeLimit > 0) {
            attributes.put("TimeLimitMinutes", String.valueOf(timeLimit));
        }
        int attemptsAllowed = attemptsAllowedSpinner.getValue();
        if (attemptsAllowed > 0) {
            attributes.put("AttemptsAllowed", String.valueOf(attemptsAllowed));
        }
        if (autoGradingCheckBox.isSelected()) {
            attributes.put("AutoGrading", "true");
        }
        if (shuffleQuestionsCheckBox.isSelected()) {
            attributes.put("ShuffleQuestions", "true");
        }

        // Create quiz
        try {
            Quiz quiz = quizService.createQuiz(
                    selectedCourse.getId(), title, instructions, totalPoints, dueDateTime, attributes
            );

            if (quiz != null) {
                showSuccess("Quiz created successfully!");
                closeWindow();
            } else {
                showError("Error", "Failed to create quiz.");
            }
        } catch (SQLException e) {
            showError("Database Error", "Failed to create quiz: " + e.getMessage());
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
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/professor_quizzes.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Quiz Management");
            stage.show();
        } catch (IOException e) {
            showError("Error", "Unable to return to quizzes page: " + e.getMessage());
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

