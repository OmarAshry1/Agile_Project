package edu.curriculum.ui;

import edu.curriculum.model.Course;
import edu.curriculum.model.Quiz;
import edu.curriculum.model.QuizQuestion;
import edu.curriculum.model.QuizQuestionOption;
import edu.curriculum.model.QuestionType;
import edu.curriculum.service.CourseService;
import edu.curriculum.service.QuizService;
import edu.facilities.model.User;
import edu.facilities.service.AuthService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for Create Quiz Screen (create_quiz.fxml)
 * US 2.10 - Create Quiz with Questions
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
    @FXML private Spinner<Integer> numberOfQuestionsSpinner;
    @FXML private CheckBox autoGradingCheckBox;
    @FXML private CheckBox shuffleQuestionsCheckBox;
    @FXML private Button backButton;
    @FXML private VBox questionsContainer;

    @FXML private Label courseError;
    @FXML private Label titleError;
    @FXML private Label dueDateError;
    @FXML private Label pointsError;
    @FXML private Label questionsError;

    private QuizService quizService = new QuizService();
    private CourseService courseService = new CourseService();
    private AuthService authService = AuthService.getInstance();
    private Map<String, Course> courseMap = new HashMap<>();
    private List<QuestionInput> questionInputs = new ArrayList<>();

    // Inner class to hold question input fields
    private static class QuestionInput {
        int questionNumber;
        TextArea questionTextArea;
        ComboBox<String> questionTypeCombo;
        VBox optionsContainer;
        List<TextField> optionFields;
        List<CheckBox> correctCheckBoxes;
    }

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
        SpinnerValueFactory<Integer> questionsFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 15, 5);

        hourSpinner.setValueFactory(hourFactory);
        minuteSpinner.setValueFactory(minuteFactory);
        totalPointsSpinner.setValueFactory(pointsFactory);
        timeLimitSpinner.setValueFactory(timeLimitFactory);
        attemptsAllowedSpinner.setValueFactory(attemptsFactory);
        numberOfQuestionsSpinner.setValueFactory(questionsFactory);

        // Set default date to tomorrow
        dueDatePicker.setValue(LocalDate.now().plusDays(1));

        // Load professor's courses
        loadCourses();

        // Initialize with default number of questions
        updateQuestionsUI();
        
        // Listen for changes in number of questions
        numberOfQuestionsSpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                updateQuestionsUI();
            }
        });
    }

    /**
     * Update the questions UI based on number of questions selected
     */
    private void updateQuestionsUI() {
        questionsContainer.getChildren().clear();
        questionInputs.clear();

        int numQuestions = numberOfQuestionsSpinner.getValue();
        
        for (int i = 1; i <= numQuestions; i++) {
            createQuestionInput(i);
        }
    }

    /**
     * Create a question input section
     */
    private void createQuestionInput(int questionNumber) {
        QuestionInput qInput = new QuestionInput();
        qInput.questionNumber = questionNumber;
        qInput.optionFields = new ArrayList<>();
        qInput.correctCheckBoxes = new ArrayList<>();

        // Question container
        VBox questionBox = new VBox(10);
        questionBox.setStyle("-fx-background-color: #f5f5f5; -fx-padding: 15; -fx-border-color: #ddd; -fx-border-width: 1; -fx-border-radius: 5;");
        questionBox.setPadding(new Insets(15));

        // Question header
        Label questionLabel = new Label("Question " + questionNumber + " *");
        questionLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        // Question text area
        Label questionTextLabel = new Label("Question Text:");
        TextArea questionTextArea = new TextArea();
        questionTextArea.setPromptText("Enter your question here...");
        questionTextArea.setPrefRowCount(3);
        questionTextArea.setWrapText(true);
        qInput.questionTextArea = questionTextArea;

        // Question type selector
        HBox typeBox = new HBox(10);
        Label typeLabel = new Label("Question Type:");
        ComboBox<String> typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll("MCQ", "WRITTEN");
        typeCombo.setValue("MCQ");
        qInput.questionTypeCombo = typeCombo;
        typeBox.getChildren().addAll(typeLabel, typeCombo);

        // Options container (for MCQ)
        VBox optionsContainer = new VBox(5);
        qInput.optionsContainer = optionsContainer;
        optionsContainer.setVisible(true);
        optionsContainer.setManaged(true);

        // Add option button (for MCQ)
        Button addOptionBtn = new Button("+ Add Option");
        addOptionBtn.setOnAction(e -> addOptionToQuestion(qInput));

        // Add initial 4 options for MCQ
        for (int i = 1; i <= 4; i++) {
            addOptionToQuestion(qInput);
        }

        // Show/hide options based on question type
        typeCombo.setOnAction(e -> {
            boolean isMCQ = "MCQ".equals(typeCombo.getValue());
            optionsContainer.setVisible(isMCQ);
            optionsContainer.setManaged(isMCQ);
            addOptionBtn.setVisible(isMCQ);
            addOptionBtn.setManaged(isMCQ);
        });

        // Assemble question box
        questionBox.getChildren().addAll(
            questionLabel,
            questionTextLabel,
            questionTextArea,
            typeBox,
            optionsContainer,
            addOptionBtn
        );

        questionsContainer.getChildren().add(questionBox);
        questionInputs.add(qInput);
    }

    /**
     * Add an option field to an MCQ question
     */
    private void addOptionToQuestion(QuestionInput qInput) {
        HBox optionBox = new HBox(10);
        optionBox.setPadding(new Insets(5, 0, 5, 0));

        TextField optionField = new TextField();
        optionField.setPromptText("Option text");
        optionField.setPrefWidth(300);

        CheckBox correctCheckBox = new CheckBox("Correct Answer");
        correctCheckBox.setPrefWidth(150);

        Button removeBtn = new Button("Remove");
        removeBtn.setOnAction(e -> {
            qInput.optionsContainer.getChildren().remove(optionBox);
            qInput.optionFields.remove(optionField);
            qInput.correctCheckBoxes.remove(correctCheckBox);
        });

        optionBox.getChildren().addAll(optionField, correctCheckBox, removeBtn);
        qInput.optionsContainer.getChildren().add(optionBox);
        qInput.optionFields.add(optionField);
        qInput.correctCheckBoxes.add(correctCheckBox);
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
                // Save questions
                saveQuestions(quiz.getId());
                showSuccess("Quiz created successfully with " + questionInputs.size() + " questions!");
                closeWindow();
            } else {
                showError("Error", "Failed to create quiz.");
            }
        } catch (SQLException e) {
            showError("Database Error", "Failed to create quiz: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Save questions to database
     */
    private void saveQuestions(String quizId) throws SQLException {
        // This will be implemented in QuizService
        // For now, we'll store questions in a JSON-like format in attributes
        // In a full implementation, you'd use the QuizQuestions table
        
        List<QuizQuestion> questions = new ArrayList<>();
        for (QuestionInput qInput : questionInputs) {
            String questionText = qInput.questionTextArea.getText().trim();
            if (questionText.isEmpty()) {
                continue; // Skip empty questions
            }

            QuestionType type = "MCQ".equals(qInput.questionTypeCombo.getValue()) 
                ? QuestionType.MCQ : QuestionType.WRITTEN;

            List<QuizQuestionOption> options = new ArrayList<>();
            if (type == QuestionType.MCQ) {
                for (int i = 0; i < qInput.optionFields.size(); i++) {
                    String optionText = qInput.optionFields.get(i).getText().trim();
                    if (!optionText.isEmpty()) {
                        boolean isCorrect = qInput.correctCheckBoxes.get(i).isSelected();
                        options.add(new QuizQuestionOption(
                            String.valueOf(i + 1), 
                            String.valueOf(qInput.questionNumber), 
                            optionText, 
                            isCorrect, 
                            i + 1
                        ));
                    }
                }
            }

            questions.add(new QuizQuestion(
                String.valueOf(qInput.questionNumber),
                quizId,
                qInput.questionNumber,
                questionText,
                type,
                1, // Default points per question
                options
            ));
        }

        // Save questions using QuizService
        quizService.saveQuizQuestions(quizId, questions);
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

        // Validate questions
        boolean hasValidQuestion = false;
        for (QuestionInput qInput : questionInputs) {
            if (!qInput.questionTextArea.getText().trim().isEmpty()) {
                hasValidQuestion = true;
                // Validate MCQ has at least 2 options and one correct
                if ("MCQ".equals(qInput.questionTypeCombo.getValue())) {
                    int validOptions = 0;
                    boolean hasCorrect = false;
                    for (int i = 0; i < qInput.optionFields.size(); i++) {
                        if (!qInput.optionFields.get(i).getText().trim().isEmpty()) {
                            validOptions++;
                            if (qInput.correctCheckBoxes.get(i).isSelected()) {
                                hasCorrect = true;
                            }
                        }
                    }
                    if (validOptions < 2) {
                        questionsError.setText("MCQ questions must have at least 2 options");
                        questionsError.setVisible(true);
                        isValid = false;
                    }
                    if (!hasCorrect) {
                        questionsError.setText("MCQ questions must have at least one correct answer");
                        questionsError.setVisible(true);
                        isValid = false;
                    }
                }
            }
        }

        if (!hasValidQuestion) {
            questionsError.setText("At least one question is required");
            questionsError.setVisible(true);
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
        questionsError.setVisible(false);
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
