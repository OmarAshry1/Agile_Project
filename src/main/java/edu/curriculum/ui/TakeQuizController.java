package edu.curriculum.ui;

import edu.curriculum.model.Quiz;
import edu.curriculum.model.QuizAttempt;
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
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Controller for Take Quiz Screen (take_quiz.fxml)
 * US 2.11 - Take Quiz
 */
public class TakeQuizController {

    @FXML private Label quizTitleLabel;
    @FXML private Label courseLabel;
    @FXML private Label dueDateLabel;
    @FXML private Label totalPointsLabel;
    @FXML private Label instructionsLabel;
    @FXML private Label timerLabel;
    @FXML private Label statusLabel;
    @FXML private Button startButton;
    @FXML private Button submitButton;
    @FXML private Button backButton;

    private Quiz quiz;
    private QuizAttempt currentAttempt;
    private QuizService quizService = new QuizService();
    private AuthService authService = AuthService.getInstance();
    private Integer timeLimitMinutes;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public void setQuiz(Quiz quiz) {
        this.quiz = quiz;
        if (quiz != null) {
            loadQuizDetails();
            checkExistingAttempts();
        }
    }

    @FXML
    public void initialize() {
        // Check if user is logged in
        if (!authService.isLoggedIn()) {
            showError("Access Denied", "Please login to take quizzes.");
            disableAllControls();
            return;
        }

        // Check user type - only STUDENT can take quizzes
        String userType = authService.getCurrentUserType();
        if (!"STUDENT".equals(userType)) {
            showError("Access Denied", "Only students can take quizzes.");
            disableAllControls();
            return;
        }

        submitButton.setDisable(true);
    }

    /**
     * Load quiz details into UI
     */
    private void loadQuizDetails() {
        if (quiz == null) return;

        quizTitleLabel.setText(quiz.getTitle());
        courseLabel.setText("Course: " + quiz.getCourse().getName());
        
        if (quiz.getDueDate() != null) {
            dueDateLabel.setText("Due: " + quiz.getDueDate().format(DATE_TIME_FORMATTER));
        }
        
        totalPointsLabel.setText("Total Points: " + quiz.getTotalPoints());
        
        if (quiz.getInstructions() != null && !quiz.getInstructions().isEmpty()) {
            instructionsLabel.setText(quiz.getInstructions());
        } else {
            instructionsLabel.setText("No special instructions.");
        }

        // Check for time limit attribute
        String timeLimitStr = quiz.getAttribute("TimeLimitMinutes");
        if (timeLimitStr != null) {
            try {
                timeLimitMinutes = Integer.parseInt(timeLimitStr);
                timerLabel.setText("Time Limit: " + timeLimitMinutes + " minutes");
            } catch (NumberFormatException e) {
                timerLabel.setText("No time limit");
            }
        } else {
            timerLabel.setText("No time limit");
        }
    }

    /**
     * Check for existing attempts
     */
    private void checkExistingAttempts() {
        try {
            User currentUser = authService.getCurrentUser();
            if (currentUser == null) return;

            List<QuizAttempt> attempts = quizService.getQuizAttempts(quiz.getId(), currentUser.getId());
            
            // Check attempts allowed
            String attemptsAllowedStr = quiz.getAttribute("AttemptsAllowed");
            int attemptsAllowed = attemptsAllowedStr != null ? Integer.parseInt(attemptsAllowedStr) : 1;
            
            if (attempts.size() >= attemptsAllowed) {
                statusLabel.setText("You have reached the maximum number of attempts (" + attemptsAllowed + ")");
                startButton.setDisable(true);
            } else if (attempts.size() > 0) {
                statusLabel.setText("Attempt " + (attempts.size() + 1) + " of " + attemptsAllowed);
            } else {
                statusLabel.setText("Ready to start");
            }
        } catch (SQLException e) {
            showError("Database Error", "Failed to check attempts: " + e.getMessage());
        }
    }

    @FXML
    private void handleStart() {
        try {
            User currentUser = authService.getCurrentUser();
            if (currentUser == null) {
                showError("Authentication Error", "User session expired. Please login again.");
                return;
            }

            // Start quiz attempt
            currentAttempt = quizService.startQuizAttempt(quiz.getId(), currentUser.getId());
            
            if (currentAttempt != null) {
                startButton.setDisable(true);
                submitButton.setDisable(false);
                statusLabel.setText("Quiz in progress...");
                
                // Start timer if time limit is set
                if (timeLimitMinutes != null && timeLimitMinutes > 0) {
                    startTimer();
                }
                
                showSuccess("Quiz started! Click Submit when finished.");
            } else {
                showError("Error", "Failed to start quiz attempt.");
            }
        } catch (SQLException e) {
            showError("Database Error", "Failed to start quiz: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleSubmit() {
        if (currentAttempt == null) {
            showError("Error", "No quiz attempt in progress.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Submit Quiz");
        confirm.setHeaderText("Are you sure you want to submit?");
        confirm.setContentText("You cannot change your answers after submitting.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    // For now, we'll set score to null (can be graded later)
                    // In a full implementation, you would calculate score based on answers
                    boolean completed = quizService.completeQuizAttempt(currentAttempt.getId(), null);
                    
                    if (completed) {
                        showSuccess("Quiz submitted successfully!");
                        handleBack(null);
                    } else {
                        showError("Error", "Failed to submit quiz.");
                    }
                } catch (SQLException e) {
                    showError("Database Error", "Failed to submit quiz: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Start timer for timed quiz
     */
    private void startTimer() {
        // In a full implementation, you would use a JavaFX Timeline or ScheduledService
        // to update the timer label and auto-submit when time expires
        // For now, this is a placeholder
        System.out.println("Timer started for " + timeLimitMinutes + " minutes");
    }

    @FXML
    private void handleBack(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/student_quizzes.fxml"));
            Stage stage;
            if (event != null && event.getSource() instanceof Node) {
                stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            } else {
                stage = (Stage) backButton.getScene().getWindow();
            }
            stage.setScene(new Scene(root));
            stage.setTitle("");
            stage.setMaximized(true);
            stage.show();
        } catch (IOException e) {
            showError("Error", "Unable to return to quizzes page: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void disableAllControls() {
        if (startButton != null) startButton.setDisable(true);
        if (submitButton != null) submitButton.setDisable(true);
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
}

