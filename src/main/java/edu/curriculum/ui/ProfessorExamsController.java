package edu.curriculum.ui;

import edu.curriculum.model.Course;
import edu.curriculum.model.Exam;
import edu.curriculum.service.CourseService;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Controller for Professor Exam Management Screen (professor_exams.fxml)
 * US 2.12 - Create Exam, US 2.13 - Record Exam Grades
 */
public class ProfessorExamsController {

    @FXML private Button backButton;
    @FXML private Button createButton;
    @FXML private Button recordGradesButton;

    @FXML private TableView<Exam> examsTable;
    @FXML private TableColumn<Exam, String> examIdColumn;
    @FXML private TableColumn<Exam, String> courseNameColumn;
    @FXML private TableColumn<Exam, String> titleColumn;
    @FXML private TableColumn<Exam, String> examDateColumn;
    @FXML private TableColumn<Exam, String> locationColumn;
    @FXML private TableColumn<Exam, String> totalPointsColumn;

    private ObservableList<Exam> examsList = FXCollections.observableArrayList();
    private ExamService examService = new ExamService();
    private CourseService courseService = new CourseService();
    private AuthService authService = AuthService.getInstance();

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML
    public void initialize() throws SQLException {
        // Check if user is logged in
        if (!authService.isLoggedIn()) {
            showError("Access Denied", "Please login to view exams.");
            disableAllControls();
            return;
        }

        // Check user type - only PROFESSOR can manage exams
        String userType = authService.getCurrentUserType();
        if (!"PROFESSOR".equals(userType)) {
            showError("Access Denied", "Only professors can manage exams.");
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
        examIdColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getId())
        );

        courseNameColumn.setCellValueFactory(cellData -> {
            Course course = cellData.getValue().getCourse();
            return new javafx.beans.property.SimpleStringProperty(course != null ? course.getName() : "");
        });

        titleColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getTitle())
        );

        examDateColumn.setCellValueFactory(cellData -> {
            LocalDateTime examDate = cellData.getValue().getExamDate();
            String dateStr = examDate != null ? examDate.format(DATE_TIME_FORMATTER) : "";
            return new javafx.beans.property.SimpleStringProperty(dateStr);
        });

        locationColumn.setCellValueFactory(cellData -> {
            String location = cellData.getValue().getLocation();
            return new javafx.beans.property.SimpleStringProperty(location != null ? location : "");
        });

        totalPointsColumn.setCellValueFactory(cellData -> {
            int points = cellData.getValue().getTotalPoints();
            return new javafx.beans.property.SimpleStringProperty(String.valueOf(points));
        });
    }

    /**
     * Load exams for the professor's courses
     */
    private void loadExams() throws SQLException {
        try {
            User currentUser = authService.getCurrentUser();
            if (currentUser == null) {
                return;
            }

            List<Course> courses = courseService.getCoursesByProfessor(currentUser.getId());
            examsList.clear();

            for (Course course : courses) {
                List<Exam> courseExams = examService.getExamsByCourse(course.getId());
                examsList.addAll(courseExams);
            }

            examsTable.setItems(examsList);
            examsTable.refresh();
        } catch (SQLException e) {
            showError("Database Error", "Failed to load exams: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleCreateExam() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/create_exam.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Create New Exam");
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));

            stage.setOnHidden(event -> {
                try {
                    loadExams();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            });

            stage.showAndWait();
        } catch (IOException e) {
            showError("Error", "Could not open Create Exam window: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleRecordGrades() {
        Exam selectedExam = examsTable.getSelectionModel().getSelectedItem();

        if (selectedExam == null) {
            showWarning("No Selection", "Please select an exam to record grades");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/record_exam_grades.fxml"));
            Parent root = loader.load();

            RecordExamGradesController controller = loader.getController();
            if (controller != null) {
                controller.setExam(selectedExam);
            }

            Stage stage = new Stage();
            stage.setTitle("Record Exam Grades - " + selectedExam.getTitle());
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();
        } catch (IOException e) {
            showError("Error", "Could not open Record Grades window: " + e.getMessage());
            e.printStackTrace();
        }
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
        if (createButton != null) createButton.setDisable(true);
        if (recordGradesButton != null) recordGradesButton.setDisable(true);
        if (examsTable != null) examsTable.setDisable(true);
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
}

