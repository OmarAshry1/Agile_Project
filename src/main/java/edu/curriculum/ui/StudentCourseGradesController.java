package edu.curriculum.ui;

import edu.curriculum.model.*;
import edu.curriculum.service.*;
import edu.facilities.model.Enrollment;
import edu.facilities.model.User;
import edu.facilities.service.AuthService;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for Student Course Grades Screen (student_course_grades.fxml)
 * US: As a student, I want to view all grades for the course so that I know how many marks left in the final for a passing grade.
 */
public class StudentCourseGradesController {

    @FXML private ComboBox<String> courseComboBox;
    @FXML private TableView<GradeEntry> gradesTable;
    @FXML private TableColumn<GradeEntry, String> typeColumn;
    @FXML private TableColumn<GradeEntry, String> titleColumn;
    @FXML private TableColumn<GradeEntry, String> pointsEarnedColumn;
    @FXML private TableColumn<GradeEntry, String> totalPointsColumn;
    @FXML private TableColumn<GradeEntry, String> percentageColumn;
    @FXML private TableColumn<GradeEntry, String> feedbackColumn;
    
    @FXML private Label overallGradeLabel;
    @FXML private Label finalGradeLabel;
    @FXML private Label pointsEarnedLabel;
    @FXML private Label totalPointsLabel;
    @FXML private Button loadGradesButton;
    @FXML private Button refreshButton;
    @FXML private Button backButton;
    @FXML private Label statusLabel;

    private ObservableList<GradeEntry> gradesList = FXCollections.observableArrayList();
    private Map<String, Course> courseMap = new HashMap<>();
    
    private CourseService courseService = new CourseService();
    private AssignmentService assignmentService = new AssignmentService();
    private SubmissionService submissionService = new SubmissionService();
    private QuizService quizService = new QuizService();
    private ExamService examService = new ExamService();
    private EnrollmentService enrollmentService = new EnrollmentService();
    private AuthService authService = AuthService.getInstance();

    @FXML
    public void initialize() {
        // Check if user is logged in
        if (!authService.isLoggedIn()) {
            showError("Access Denied", "Please login to view course grades.");
            disableAllControls();
            return;
        }

        // Check user type - only STUDENT can view grades
        String userType = authService.getCurrentUserType();
        if (!"STUDENT".equals(userType)) {
            showError("Access Denied", "Only students can view course grades.");
            disableAllControls();
            return;
        }

        // Setup table columns
        setupTableColumns();

        // Load courses
        loadCourses();

        System.out.println("StudentCourseGradesController initialized successfully!");
    }

    /**
     * Setup table columns to display grade data
     */
    private void setupTableColumns() {
        typeColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getType())
        );

        titleColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getTitle())
        );

        pointsEarnedColumn.setCellValueFactory(cellData -> {
            Integer points = cellData.getValue().getPointsEarned();
            return new javafx.beans.property.SimpleStringProperty(
                    points != null ? String.valueOf(points) : "N/A"
            );
        });

        totalPointsColumn.setCellValueFactory(cellData -> {
            Integer total = cellData.getValue().getTotalPoints();
            return new javafx.beans.property.SimpleStringProperty(
                    total != null ? String.valueOf(total) : "N/A"
            );
        });

        percentageColumn.setCellValueFactory(cellData -> {
            Double percentage = cellData.getValue().getPercentage();
            return new javafx.beans.property.SimpleStringProperty(
                    percentage != null ? String.format("%.1f%%", percentage) : "N/A"
            );
        });

        feedbackColumn.setCellValueFactory(cellData -> {
            String feedback = cellData.getValue().getFeedback();
            return new javafx.beans.property.SimpleStringProperty(
                    feedback != null && !feedback.isEmpty() ? feedback : "No feedback"
            );
        });
    }

    /**
     * Load enrolled courses for the student
     */
    private void loadCourses() {
        try {
            User currentUser = authService.getCurrentUser();
            if (currentUser == null) {
                return;
            }

            List<Course> courses = courseService.getEnrolledCourses(currentUser.getId());
            courseMap.clear();
            courseComboBox.getItems().clear();

            for (Course course : courses) {
                String displayText = course.getCode() + " - " + course.getName();
                courseComboBox.getItems().add(displayText);
                courseMap.put(displayText, course);
            }

            if (courses.isEmpty()) {
                showStatus("No enrolled courses found", false);
            } else {
                showStatus("Loaded " + courses.size() + " enrolled courses", true);
            }
        } catch (SQLException e) {
            showError("Database Error", "Failed to load courses: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Load grades for the selected course
     */
    @FXML
    private void handleLoadGrades() {
        String selectedCourse = courseComboBox.getSelectionModel().getSelectedItem();
        if (selectedCourse == null) {
            showWarning("No Selection", "Please select a course to view grades");
            return;
        }

        Course course = courseMap.get(selectedCourse);
        if (course == null) {
            showError("Error", "Selected course not found");
            return;
        }

        loadGradesForCourse(course);
    }

    /**
     * Load all grades (assignments, quizzes, exams) for a course
     */
    private void loadGradesForCourse(Course course) {
        try {
            User currentUser = authService.getCurrentUser();
            if (currentUser == null) {
                return;
            }

            gradesList.clear();

            int totalPointsEarned = 0;
            int totalPointsPossible = 0;

            // Load Assignment grades
            List<Assignment> assignments = assignmentService.getAssignmentsByCourse(course.getId());
            for (Assignment assignment : assignments) {
                AssignmentSubmission submission = submissionService.getSubmission(
                        assignment.getId(), currentUser.getId());
                
                Integer pointsEarned = null;
                String feedback = null;
                if (submission != null && submission.getScore() != null) {
                    pointsEarned = submission.getScore();
                    feedback = submission.getFeedback();
                }

                GradeEntry entry = new GradeEntry(
                        "Assignment",
                        assignment.getTitle(),
                        pointsEarned,
                        assignment.getTotalPoints(),
                        feedback
                );
                gradesList.add(entry);

                if (pointsEarned != null) {
                    totalPointsEarned += pointsEarned;
                }
                totalPointsPossible += assignment.getTotalPoints();
            }

            // Load Quiz grades
            List<Quiz> quizzes = quizService.getQuizzesByCourse(course.getId());
            for (Quiz quiz : quizzes) {
                // Get all attempts and find the best one (highest score)
                List<QuizAttempt> attempts = quizService.getQuizAttempts(quiz.getId(), currentUser.getId());
                
                Integer pointsEarned = null;
                String feedback = "No feedback available"; // Quizzes don't have feedback in the model
                
                if (!attempts.isEmpty()) {
                    // Find the best attempt (highest score)
                    QuizAttempt bestAttempt = null;
                    for (QuizAttempt attempt : attempts) {
                        if (attempt.getScore() != null && attempt.getStatus() == QuizAttemptStatus.COMPLETED) {
                            if (bestAttempt == null || 
                                (bestAttempt.getScore() == null || attempt.getScore() > bestAttempt.getScore())) {
                                bestAttempt = attempt;
                            }
                        }
                    }
                    
                    if (bestAttempt != null && bestAttempt.getScore() != null) {
                        pointsEarned = bestAttempt.getScore();
                    }
                }

                GradeEntry entry = new GradeEntry(
                        "Quiz",
                        quiz.getTitle(),
                        pointsEarned,
                        quiz.getTotalPoints(),
                        feedback
                );
                gradesList.add(entry);

                if (pointsEarned != null) {
                    totalPointsEarned += pointsEarned;
                }
                totalPointsPossible += quiz.getTotalPoints();
            }

            // Load Exam grades
            List<Exam> exams = examService.getExamsByCourse(course.getId());
            for (Exam exam : exams) {
                ExamGrade examGrade = examService.getExamGrade(exam.getId(), currentUser.getId());
                
                Integer pointsEarned = null;
                String feedback = null;
                if (examGrade != null && examGrade.getPointsEarned() != null) {
                    pointsEarned = examGrade.getPointsEarned();
                    feedback = examGrade.getComments();
                }

                GradeEntry entry = new GradeEntry(
                        "Exam",
                        exam.getTitle(),
                        pointsEarned,
                        exam.getTotalPoints(),
                        feedback
                );
                gradesList.add(entry);

                if (pointsEarned != null) {
                    totalPointsEarned += pointsEarned;
                }
                totalPointsPossible += exam.getTotalPoints();
            }

            gradesTable.setItems(gradesList);
            updateOverallGrade(totalPointsEarned, totalPointsPossible, course, currentUser);
            
            showStatus("Loaded " + gradesList.size() + " grade entries for " + course.getCode(), true);
            
        } catch (SQLException e) {
            showError("Database Error", "Failed to load grades: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Update overall course grade display
     * Shows the final grade from enrollment (the one professor calculated using weights)
     * Also shows current average for reference
     */
    private void updateOverallGrade(int totalPointsEarned, int totalPointsPossible, Course course, User student) {
        pointsEarnedLabel.setText("Total Points Earned: " + totalPointsEarned);
        totalPointsLabel.setText("Total Points Possible: " + totalPointsPossible);

        // Get final grade from enrollment (the one professor calculated and saved)
        String finalGrade = null;
        try {
            List<Enrollment> enrollments = enrollmentService.getStudentEnrollments(student, true);
            for (Enrollment enrollment : enrollments) {
                if (enrollment.getCourse().getId().equals(course.getId())) {
                    finalGrade = enrollment.getGrade();
                    break;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error loading enrollment grade: " + e.getMessage());
        }

        // Show the final grade (the one professor puts in)
        if (finalGrade != null && !finalGrade.isEmpty()) {
            overallGradeLabel.setText("Final Grade: " + finalGrade);
            overallGradeLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        } else {
            // Calculate simple average as reference if final grade not yet set
            if (totalPointsPossible > 0) {
                double overallPercentage = (totalPointsEarned / (double) totalPointsPossible) * 100.0;
                overallGradeLabel.setText(String.format("Current Average: %.1f%% (Final grade not yet calculated)", overallPercentage));
            } else {
                overallGradeLabel.setText("Final Grade: Not yet calculated (No graded items)");
            }
            overallGradeLabel.setStyle("-fx-font-size: 14px;");
        }
    }

    @FXML
    private void handleRefresh() {
        loadCourses();
        String selectedCourse = courseComboBox.getSelectionModel().getSelectedItem();
        if (selectedCourse != null) {
            Course course = courseMap.get(selectedCourse);
            if (course != null) {
                loadGradesForCourse(course);
            }
        }
        showStatus("Data refreshed successfully", true);
    }

    @FXML
    private void handleBack(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/dashboard.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));        stage.setMaximized(true);stage.show();
        } catch (IOException e) {
            showError("Navigation Error", "Unable to return to dashboard: " + e.getMessage());
        }
    }

    private void disableAllControls() {
        if (courseComboBox != null) courseComboBox.setDisable(true);
        if (loadGradesButton != null) loadGradesButton.setDisable(true);
        if (refreshButton != null) refreshButton.setDisable(true);
        if (gradesTable != null) gradesTable.setDisable(true);
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

    private void showStatus(String message, boolean success) {
        if (statusLabel != null) {
            statusLabel.setText(message);
            statusLabel.setVisible(true);
            statusLabel.setStyle(success ? "-fx-text-fill: #4CAF50;" : "-fx-text-fill: #E53935;");
        }
    }
}

