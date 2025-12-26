package edu.curriculum.ui;

import edu.curriculum.model.Assignment;
import edu.curriculum.model.AssignmentSubmission;
import edu.curriculum.model.Course;
import edu.curriculum.service.AssignmentService;
import edu.curriculum.service.CourseService;
import edu.curriculum.service.SubmissionService;
import edu.facilities.model.User;
import edu.facilities.service.AuthService;
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
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for Gradebook Screen (gradebook.fxml)
 * Shows all courses and their student grades for a professor
 */
public class GradebookController {

    @FXML private ComboBox<String> courseComboBox;
    @FXML private TableView<GradebookEntry> gradesTable;
    @FXML private TableColumn<GradebookEntry, String> studentNameColumn;
    @FXML private TableColumn<GradebookEntry, String> studentIdColumn;
    @FXML private TableColumn<GradebookEntry, String> assignmentTitleColumn;
    @FXML private TableColumn<GradebookEntry, String> scoreColumn;
    @FXML private TableColumn<GradebookEntry, String> maxPointsColumn;
    @FXML private TableColumn<GradebookEntry, String> percentageColumn;
    @FXML private TableColumn<GradebookEntry, String> statusColumn;
    @FXML private TableColumn<GradebookEntry, String> submittedDateColumn;
    
    @FXML private Label totalStudentsLabel;
    @FXML private Label totalSubmissionsLabel;
    @FXML private Label averageScoreLabel;
    @FXML private Label statusLabel;
    
    @FXML private Button refreshButton;
    @FXML private Button backButton;

    private ObservableList<GradebookEntry> gradesList = FXCollections.observableArrayList();
    private Map<String, Course> courseMap = new HashMap<>();
    
    private CourseService courseService = new CourseService();
    private AssignmentService assignmentService = new AssignmentService();
    private SubmissionService submissionService = new SubmissionService();
    private AuthService authService = AuthService.getInstance();

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML
    public void initialize() {
        // Check if user is logged in
        if (!authService.isLoggedIn()) {
            showError("Access Denied", "Please login to view gradebook.");
            disableAllControls();
            return;
        }

        // Check user type - only PROFESSOR can view gradebook
        String userType = authService.getCurrentUserType();
        if (!"PROFESSOR".equals(userType)) {
            showError("Access Denied", "Only professors can view gradebook.");
            disableAllControls();
            return;
        }

        // Setup table columns
        setupTableColumns();

        // Load courses
        loadCourses();

        System.out.println("GradebookController initialized successfully!");
    }

    /**
     * Setup table columns
     */
    private void setupTableColumns() {
        studentNameColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().studentName));
        
        studentIdColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().studentId));
        
        assignmentTitleColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().assignmentTitle));
        
        scoreColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().score));
        
        maxPointsColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().maxPoints));
        
        percentageColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().percentage));
        
        statusColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().status));
        
        submittedDateColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().submittedDate));
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

            List<Course> courses = courseService.getCoursesByProfessor(currentUser.getId());
            courseMap.clear();
            courseComboBox.getItems().clear();

            if (courses.isEmpty()) {
                courseComboBox.setPromptText("No courses available");
                statusLabel.setText("No courses found for your account. Please create courses first.");
                statusLabel.setVisible(true);
                return;
            }

            for (Course course : courses) {
                String displayName = course.getCode() + " - " + course.getName();
                courseComboBox.getItems().add(displayName);
                courseMap.put(displayName, course);
            }

            System.out.println("Loaded " + courses.size() + " courses into gradebook");
        } catch (SQLException e) {
            showError("Database Error", "Failed to load courses: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Handle course selection
     */
    @FXML
    private void handleCourseSelection() {
        String selectedCourseDisplay = courseComboBox.getValue();
        if (selectedCourseDisplay == null || !courseMap.containsKey(selectedCourseDisplay)) {
            gradesList.clear();
            updateStatistics();
            return;
        }

        Course selectedCourse = courseMap.get(selectedCourseDisplay);
        loadGradesForCourse(selectedCourse);
    }

    /**
     * Load grades for selected course
     */
    private void loadGradesForCourse(Course course) {
        try {
            gradesList.clear();

            // Get all assignments for this course
            List<Assignment> assignments = assignmentService.getAssignmentsByCourse(course.getId());
            
            if (assignments.isEmpty()) {
                statusLabel.setText("No assignments found for " + course.getCode());
                statusLabel.setVisible(true);
                updateStatistics();
                return;
            }

            // Get all submissions for all assignments
            for (Assignment assignment : assignments) {
                List<AssignmentSubmission> submissions = submissionService.getSubmissionsByAssignment(assignment.getId());
                
                for (AssignmentSubmission submission : submissions) {
                    GradebookEntry entry = new GradebookEntry();
                    entry.studentName = submission.getStudent().getUsername();
                    entry.studentId = submission.getStudent().getId();
                    entry.assignmentTitle = assignment.getTitle();
                    entry.maxPoints = String.valueOf(assignment.getTotalPoints());
                    
                    if (submission.getScore() != null) {
                        entry.score = String.valueOf(submission.getScore());
                        double percentage = (submission.getScore().doubleValue() / assignment.getTotalPoints()) * 100;
                        entry.percentage = String.format("%.1f%%", percentage);
                    } else {
                        entry.score = "Not Graded";
                        entry.percentage = "N/A";
                    }
                    
                    entry.status = submission.getStatus().toString();
                    
                    if (submission.getSubmittedDate() != null) {
                        entry.submittedDate = submission.getSubmittedDate().format(DATE_TIME_FORMATTER);
                    } else {
                        entry.submittedDate = "Not Submitted";
                    }
                    
                    gradesList.add(entry);
                }
            }

            gradesTable.setItems(gradesList);
            updateStatistics();
            
            statusLabel.setText("Loaded " + gradesList.size() + " grade entries for " + course.getCode());
            statusLabel.setVisible(true);
            
        } catch (SQLException e) {
            showError("Database Error", "Failed to load grades: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Update summary statistics
     */
    private void updateStatistics() {
        // Count unique students
        long uniqueStudents = gradesList.stream()
                .map(entry -> entry.studentId)
                .distinct()
                .count();
        totalStudentsLabel.setText(String.valueOf(uniqueStudents));

        // Count total submissions
        long totalSubmissions = gradesList.size();
        totalSubmissionsLabel.setText(String.valueOf(totalSubmissions));

        // Calculate average score
        double totalScore = 0;
        int gradedCount = 0;
        for (GradebookEntry entry : gradesList) {
            try {
                if (!entry.score.equals("Not Graded") && !entry.percentage.equals("N/A")) {
                    double percentage = Double.parseDouble(entry.percentage.replace("%", ""));
                    totalScore += percentage;
                    gradedCount++;
                }
            } catch (NumberFormatException e) {
                // Skip invalid entries
            }
        }
        
        if (gradedCount > 0) {
            double average = totalScore / gradedCount;
            averageScoreLabel.setText(String.format("%.1f%%", average));
        } else {
            averageScoreLabel.setText("N/A");
        }
    }

    @FXML
    private void handleRefresh() {
        loadCourses();
        if (courseComboBox.getValue() != null) {
            handleCourseSelection();
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
            showError("Navigation Error", "Unable to return to dashboard: " + e.getMessage());
        }
    }

    private void disableAllControls() {
        if (courseComboBox != null) courseComboBox.setDisable(true);
        if (gradesTable != null) gradesTable.setDisable(true);
        if (refreshButton != null) refreshButton.setDisable(true);
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Data class for gradebook entries
     */
    public static class GradebookEntry {
        String studentName;
        String studentId;
        String assignmentTitle;
        String score;
        String maxPoints;
        String percentage;
        String status;
        String submittedDate;
    }
}

