package edu.curriculum.ui;

import edu.curriculum.model.Exam;
import edu.curriculum.model.ExamGrade;
import edu.curriculum.service.ExamService;
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
 * Controller for Student Exams Screen (student_exams.fxml)
 * Shows exams with course, date, time, location, and grade
 */
public class StudentExamsController {

    @FXML private Button backButton;
    @FXML private TableView<Map<String, Object>> examsTable;
    @FXML private TableColumn<Map<String, Object>, String> courseNameColumn;
    @FXML private TableColumn<Map<String, Object>, String> examTitleColumn;
    @FXML private TableColumn<Map<String, Object>, String> examDateColumn;
    @FXML private TableColumn<Map<String, Object>, String> examTimeColumn;
    @FXML private TableColumn<Map<String, Object>, String> locationColumn;
    @FXML private TableColumn<Map<String, Object>, String> durationColumn;
    @FXML private TableColumn<Map<String, Object>, String> totalPointsColumn;
    @FXML private TableColumn<Map<String, Object>, String> pointsEarnedColumn;
    @FXML private TableColumn<Map<String, Object>, String> percentageColumn;

    private ObservableList<Map<String, Object>> examsList = FXCollections.observableArrayList();
    private ExamService examService = new ExamService();
    private AuthService authService = AuthService.getInstance();

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @FXML
    public void initialize() throws SQLException {
        // Check if user is logged in
        if (!authService.isLoggedIn()) {
            showError("Access Denied", "Please login to view exams.");
            disableAllControls();
            return;
        }

        // Check user type - only STUDENT can view exams
        String userType = authService.getCurrentUserType();
        if (!"STUDENT".equals(userType)) {
            showError("Access Denied", "Only students can view exams.");
            disableAllControls();
            return;
        }

        // Setup table columns
        setupTableColumns();

        // Load exams
        loadExams();
    }

    /**
     * Setup table columns to display exam data
     */
    private void setupTableColumns() {
        courseNameColumn.setCellValueFactory(cellData -> {
            Map<String, Object> exam = cellData.getValue();
            return new javafx.beans.property.SimpleStringProperty(
                exam.get("courseName") != null ? exam.get("courseName").toString() : ""
            );
        });

        examTitleColumn.setCellValueFactory(cellData -> {
            Map<String, Object> exam = cellData.getValue();
            return new javafx.beans.property.SimpleStringProperty(
                exam.get("title") != null ? exam.get("title").toString() : ""
            );
        });

        examDateColumn.setCellValueFactory(cellData -> {
            Map<String, Object> exam = cellData.getValue();
            return new javafx.beans.property.SimpleStringProperty(
                exam.get("examDate") != null ? exam.get("examDate").toString() : ""
            );
        });

        examTimeColumn.setCellValueFactory(cellData -> {
            Map<String, Object> exam = cellData.getValue();
            return new javafx.beans.property.SimpleStringProperty(
                exam.get("examTime") != null ? exam.get("examTime").toString() : ""
            );
        });

        locationColumn.setCellValueFactory(cellData -> {
            Map<String, Object> exam = cellData.getValue();
            return new javafx.beans.property.SimpleStringProperty(
                exam.get("location") != null ? exam.get("location").toString() : ""
            );
        });

        durationColumn.setCellValueFactory(cellData -> {
            Map<String, Object> exam = cellData.getValue();
            return new javafx.beans.property.SimpleStringProperty(
                exam.get("duration") != null ? exam.get("duration").toString() : ""
            );
        });

        totalPointsColumn.setCellValueFactory(cellData -> {
            Map<String, Object> exam = cellData.getValue();
            return new javafx.beans.property.SimpleStringProperty(
                exam.get("totalPoints") != null ? exam.get("totalPoints").toString() : ""
            );
        });

        pointsEarnedColumn.setCellValueFactory(cellData -> {
            Map<String, Object> exam = cellData.getValue();
            return new javafx.beans.property.SimpleStringProperty(
                exam.get("pointsEarned") != null ? exam.get("pointsEarned").toString() : "Not graded"
            );
        });

        percentageColumn.setCellValueFactory(cellData -> {
            Map<String, Object> exam = cellData.getValue();
            return new javafx.beans.property.SimpleStringProperty(
                exam.get("percentage") != null ? exam.get("percentage").toString() : ""
            );
        });
    }

    /**
     * Load exams for the student's enrolled courses
     */
    private void loadExams() throws SQLException {
        try {
            User currentUser = authService.getCurrentUser();
            if (currentUser == null) {
                return;
            }

            List<Exam> exams = examService.getStudentExams(currentUser.getId());
            examsList.clear();

            for (Exam exam : exams) {
                Map<String, Object> examData = new HashMap<>();
                
                examData.put("examId", exam.getId());
                examData.put("courseName", exam.getCourse().getName());
                examData.put("title", exam.getTitle());
                
                // Format date and time
                if (exam.getExamDate() != null) {
                    examData.put("examDate", exam.getExamDate().format(DATE_FORMATTER));
                    examData.put("examTime", exam.getExamDate().format(TIME_FORMATTER));
                } else {
                    examData.put("examDate", "");
                    examData.put("examTime", "");
                }
                
                examData.put("location", exam.getLocation() != null ? exam.getLocation() : "TBA");
                examData.put("duration", exam.getDurationMinutes() + " minutes");
                examData.put("totalPoints", String.valueOf(exam.getTotalPoints()));
                
                // Get grade if available
                ExamGrade grade = examService.getExamGrade(exam.getId(), currentUser.getId());
                if (grade != null && grade.getPointsEarned() != null) {
                    examData.put("pointsEarned", String.valueOf(grade.getPointsEarned()));
                    double percentage = grade.getPercentage();
                    examData.put("percentage", String.format("%.1f%%", percentage));
                } else {
                    examData.put("pointsEarned", null);
                    examData.put("percentage", "");
                }
                
                examsList.add(examData);
            }

            examsTable.setItems(examsList);
            examsTable.refresh();
        } catch (SQLException e) {
            showError("Database Error", "Failed to load exams: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleBack(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/dashboard.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            showError("Navigation Error", "Unable to return to dashboard: " + e.getMessage());
        }
    }

    private void disableAllControls() {
        if (examsTable != null) examsTable.setDisable(true);
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

