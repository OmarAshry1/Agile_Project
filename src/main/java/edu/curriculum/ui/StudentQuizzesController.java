package edu.curriculum.ui;

import edu.curriculum.model.Quiz;
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
 * Controller for Student Quizzes Screen (student_quizzes.fxml)
 * US 2.11 - Take Quiz
 */
public class StudentQuizzesController {

    @FXML private Button backButton;
    @FXML private Button takeQuizButton;

    @FXML private TableView<Quiz> quizzesTable;
    @FXML private TableColumn<Quiz, String> courseNameColumn;
    @FXML private TableColumn<Quiz, String> titleColumn;
    @FXML private TableColumn<Quiz, String> dueDateColumn;
    @FXML private TableColumn<Quiz, String> totalPointsColumn;

    private ObservableList<Quiz> quizzesList = FXCollections.observableArrayList();
    private QuizService quizService = new QuizService();
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

        // Check user type - only STUDENT can view quizzes
        String userType = authService.getCurrentUserType();
        if (!"STUDENT".equals(userType)) {
            showError("Access Denied", "Only students can view quizzes.");
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
        courseNameColumn.setCellValueFactory(cellData -> {
            edu.curriculum.model.Course course = cellData.getValue().getCourse();
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
     * Load quizzes for the student's enrolled courses
     */
    private void loadQuizzes() throws SQLException {
        try {
            User currentUser = authService.getCurrentUser();
            if (currentUser == null) {
                return;
            }

            List<Quiz> quizzes = quizService.getStudentQuizzes(currentUser.getId());
            quizzesList.clear();
            quizzesList.addAll(quizzes);

            quizzesTable.setItems(quizzesList);
            quizzesTable.refresh();
        } catch (SQLException e) {
            showError("Database Error", "Failed to load quizzes: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleTakeQuiz() {
        Quiz selectedQuiz = quizzesTable.getSelectionModel().getSelectedItem();

        if (selectedQuiz == null) {
            showWarning("No Selection", "Please select a quiz to take");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/take_quiz.fxml"));
            Parent root = loader.load();

            TakeQuizController controller = loader.getController();
            if (controller != null) {
                controller.setQuiz(selectedQuiz);
            }

            Stage stage = new Stage();
            stage.setTitle("Take Quiz - " + selectedQuiz.getTitle());
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();

            // Refresh list after quiz is taken
            try {
                loadQuizzes();
            } catch (SQLException e) {
                showError("Database Error", "Failed to refresh quizzes: " + e.getMessage());
            }
        } catch (IOException e) {
            showError("Error", "Could not open Take Quiz window: " + e.getMessage());
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
        if (takeQuizButton != null) takeQuizButton.setDisable(true);
        if (quizzesTable != null) quizzesTable.setDisable(true);
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

