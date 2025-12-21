package edu.curriculum.ui;

import edu.curriculum.model.Course;
import edu.curriculum.model.Quiz;
import edu.curriculum.service.CourseService;
import edu.curriculum.service.QuizService;
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
 * Controller for Professor Quiz Management Screen (professor_quizzes.fxml)
 * US 2.10 - Create Quiz
 */
public class ProfessorQuizzesController {

    @FXML private Button backButton;
    @FXML private Button createButton;
    @FXML private Button deleteButton;

    @FXML private TableView<Quiz> quizzesTable;
    @FXML private TableColumn<Quiz, String> quizIdColumn;
    @FXML private TableColumn<Quiz, String> courseNameColumn;
    @FXML private TableColumn<Quiz, String> titleColumn;
    @FXML private TableColumn<Quiz, String> dueDateColumn;
    @FXML private TableColumn<Quiz, String> totalPointsColumn;

    private ObservableList<Quiz> quizzesList = FXCollections.observableArrayList();
    private QuizService quizService = new QuizService();
    private CourseService courseService = new CourseService();
    private AuthService authService = AuthService.getInstance();

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML
    public void initialize() throws SQLException {
        // Check if user is logged in
        if (!authService.isLoggedIn()) {
            showError("Access Denied", "Please login to view quizzes.");
            disableAllControls();
            return;
        }

        // Check user type - only PROFESSOR can manage quizzes
        String userType = authService.getCurrentUserType();
        if (!"PROFESSOR".equals(userType)) {
            showError("Access Denied", "Only professors can manage quizzes.");
            disableAllControls();
            return;
        }

        // Setup table columns
        setupTableColumns();

        // Load quizzes
        loadQuizzes();
    }

    /**
     * Setup table columns to display quiz data
     */
    private void setupTableColumns() {
        quizIdColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getId())
        );

        courseNameColumn.setCellValueFactory(cellData -> {
            Course course = cellData.getValue().getCourse();
            return new javafx.beans.property.SimpleStringProperty(course != null ? course.getName() : "");
        });

        titleColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getTitle())
        );

        dueDateColumn.setCellValueFactory(cellData -> {
            LocalDateTime dueDate = cellData.getValue().getDueDate();
            String dateStr = dueDate != null ? dueDate.format(DATE_TIME_FORMATTER) : "";
            return new javafx.beans.property.SimpleStringProperty(dateStr);
        });

        totalPointsColumn.setCellValueFactory(cellData -> {
            int points = cellData.getValue().getTotalPoints();
            return new javafx.beans.property.SimpleStringProperty(String.valueOf(points));
        });
    }

    /**
     * Load quizzes for the professor's courses
     */
    private void loadQuizzes() throws SQLException {
        try {
            User currentUser = authService.getCurrentUser();
            if (currentUser == null) {
                return;
            }

            List<Course> courses = courseService.getCoursesByProfessor(currentUser.getId());
            quizzesList.clear();

            for (Course course : courses) {
                List<Quiz> courseQuizzes = quizService.getQuizzesByCourse(course.getId());
                quizzesList.addAll(courseQuizzes);
            }

            quizzesTable.setItems(quizzesList);
            quizzesTable.refresh();
        } catch (SQLException e) {
            showError("Database Error", "Failed to load quizzes: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleCreateQuiz() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/create_quiz.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Create New Quiz");
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));

            stage.setOnHidden(event -> {
                try {
                    loadQuizzes();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            });

            stage.showAndWait();
        } catch (IOException e) {
            showError("Error", "Could not open Create Quiz window: " + e.getMessage());
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
        if (createButton != null) createButton.setDisable(true);
        if (deleteButton != null) deleteButton.setDisable(true);
        if (quizzesTable != null) quizzesTable.setDisable(true);
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

