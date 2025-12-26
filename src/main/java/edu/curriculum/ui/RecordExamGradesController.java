package edu.curriculum.ui;

import edu.curriculum.model.Exam;
import edu.curriculum.model.ExamGrade;
import edu.curriculum.service.ExamService;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for Record Exam Grades Screen (record_exam_grades.fxml)
 * US 2.13 - Record Exam Grades
 */
public class RecordExamGradesController {

    @FXML private Label examTitleLabel;
    @FXML private Label courseLabel;
    @FXML private Label totalPointsLabel;
    @FXML private TableView<Map<String, Object>> studentsTable;
    @FXML private TableColumn<Map<String, Object>, String> studentIdColumn;
    @FXML private TableColumn<Map<String, Object>, String> studentNameColumn;
    @FXML private TableColumn<Map<String, Object>, String> pointsEarnedColumn;
    @FXML private TableColumn<Map<String, Object>, String> percentageColumn;
    @FXML private TableColumn<Map<String, Object>, String> commentsColumn;
    @FXML private Button saveButton;
    @FXML private Button backButton;

    private Exam exam;
    private ExamService examService = new ExamService();
    private AuthService authService = AuthService.getInstance();
    private ObservableList<Map<String, Object>> studentsList = FXCollections.observableArrayList();

    public void setExam(Exam exam) {
        this.exam = exam;
        if (exam != null) {
            loadExamDetails();
            loadStudents();
        }
    }

    @FXML
    public void initialize() {
        // Check if user is logged in
        if (!authService.isLoggedIn()) {
            showError("Access Denied", "Please login to record exam grades.");
            disableAllControls();
            return;
        }

        // Check user type - only PROFESSOR can record grades
        String userType = authService.getCurrentUserType();
        if (!"PROFESSOR".equals(userType)) {
            showError("Access Denied", "Only professors can record exam grades.");
            disableAllControls();
            return;
        }

        setupTableColumns();
    }

    /**
     * Load exam details into UI
     */
    private void loadExamDetails() {
        if (exam == null) return;

        examTitleLabel.setText(exam.getTitle());
        courseLabel.setText("Course: " + exam.getCourse().getName());
        totalPointsLabel.setText("Total Points: " + exam.getTotalPoints());
    }

    /**
     * Setup table columns
     */
    private void setupTableColumns() {
        studentIdColumn.setCellValueFactory(cellData -> {
            Map<String, Object> student = cellData.getValue();
            return new javafx.beans.property.SimpleStringProperty(
                student.get("studentId") != null ? student.get("studentId").toString() : ""
            );
        });

        studentNameColumn.setCellValueFactory(cellData -> {
            Map<String, Object> student = cellData.getValue();
            return new javafx.beans.property.SimpleStringProperty(
                student.get("studentName") != null ? student.get("studentName").toString() : ""
            );
        });

        pointsEarnedColumn.setCellFactory(column -> new TableCell<Map<String, Object>, String>() {
            private final TextField textField = new TextField();

            {
                textField.setPromptText("Points");
                textField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
                    if (!isNowFocused) {
                        commitEdit();
                    }
                });
            }

            @Override
            public void startEdit() {
                super.startEdit();
                Map<String, Object> student = getTableView().getItems().get(getIndex());
                String currentPoints = student.get("pointsEarned") != null ? 
                    student.get("pointsEarned").toString() : "";
                textField.setText(currentPoints);
                setGraphic(textField);
                textField.selectAll();
                textField.requestFocus();
            }

            @Override
            public void cancelEdit() {
                super.cancelEdit();
                setGraphic(null);
            }

            @Override
            public void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Map<String, Object> student = getTableView().getItems().get(getIndex());
                    if (isEditing()) {
                        textField.setText(item);
                        setGraphic(textField);
                    } else {
                        String points = student.get("pointsEarned") != null ? 
                            student.get("pointsEarned").toString() : "";
                        setText(points);
                        setGraphic(null);
                    }
                }
            }

            private void commitEdit() {
                if (isEditing()) {
                    Map<String, Object> student = getTableView().getItems().get(getIndex());
                    String newValue = textField.getText();
                    try {
                        if (newValue != null && !newValue.trim().isEmpty()) {
                            int points = Integer.parseInt(newValue.trim());
                            student.put("pointsEarned", points);
                            updatePercentage(student);
                        } else {
                            student.put("pointsEarned", null);
                            student.put("percentage", "");
                        }
                        commitEdit(newValue);
                    } catch (NumberFormatException e) {
                        cancelEdit();
                    }
                }
            }
        });

        pointsEarnedColumn.setCellValueFactory(cellData -> {
            Map<String, Object> student = cellData.getValue();
            return new javafx.beans.property.SimpleStringProperty(
                student.get("pointsEarned") != null ? student.get("pointsEarned").toString() : ""
            );
        });

        percentageColumn.setCellValueFactory(cellData -> {
            Map<String, Object> student = cellData.getValue();
            return new javafx.beans.property.SimpleStringProperty(
                student.get("percentage") != null ? student.get("percentage").toString() : ""
            );
        });

        commentsColumn.setCellFactory(column -> new TableCell<Map<String, Object>, String>() {
            private final TextField textField = new TextField();

            {
                textField.setPromptText("Comments");
                textField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
                    if (!isNowFocused) {
                        commitEdit();
                    }
                });
            }

            @Override
            public void startEdit() {
                super.startEdit();
                Map<String, Object> student = getTableView().getItems().get(getIndex());
                String currentComments = student.get("comments") != null ? 
                    student.get("comments").toString() : "";
                textField.setText(currentComments);
                setGraphic(textField);
                textField.selectAll();
                textField.requestFocus();
            }

            @Override
            public void cancelEdit() {
                super.cancelEdit();
                setGraphic(null);
            }

            @Override
            public void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Map<String, Object> student = getTableView().getItems().get(getIndex());
                    if (isEditing()) {
                        textField.setText(item);
                        setGraphic(textField);
                    } else {
                        String comments = student.get("comments") != null ? 
                            student.get("comments").toString() : "";
                        setText(comments);
                        setGraphic(null);
                    }
                }
            }

            private void commitEdit() {
                if (isEditing()) {
                    Map<String, Object> student = getTableView().getItems().get(getIndex());
                    String newValue = textField.getText();
                    student.put("comments", newValue != null ? newValue : "");
                    commitEdit(newValue);
                }
            }
        });

        commentsColumn.setCellValueFactory(cellData -> {
            Map<String, Object> student = cellData.getValue();
            return new javafx.beans.property.SimpleStringProperty(
                student.get("comments") != null ? student.get("comments").toString() : ""
            );
        });

        studentsTable.setEditable(true);
    }

    /**
     * Load enrolled students for the exam
     */
    private void loadStudents() {
        try {
            if (exam == null) return;

            List<String> studentIds = examService.getEnrolledStudentIds(exam.getId());
            studentsList.clear();

            // Load existing grades
            List<ExamGrade> existingGrades = examService.getExamGrades(exam.getId());
            Map<String, ExamGrade> gradesMap = new HashMap<>();
            for (ExamGrade grade : existingGrades) {
                gradesMap.put(grade.getStudentUserId(), grade);
            }

            // Get student names from database
            for (String studentId : studentIds) {
                Map<String, Object> studentData = new HashMap<>();
                studentData.put("studentId", studentId);

                // Get student name from database
                try {
                    String studentName = getStudentName(studentId);
                    studentData.put("studentName", studentName != null ? studentName : "Unknown");
                } catch (SQLException e) {
                    studentData.put("studentName", "Unknown");
                }

                // Get existing grade if any
                ExamGrade grade = gradesMap.get(studentId);
                if (grade != null) {
                    studentData.put("pointsEarned", grade.getPointsEarned());
                    studentData.put("comments", grade.getComments() != null ? grade.getComments() : "");
                    updatePercentage(studentData);
                } else {
                    studentData.put("pointsEarned", null);
                    studentData.put("comments", "");
                    studentData.put("percentage", "");
                }

                studentsList.add(studentData);
            }

            studentsTable.setItems(studentsList);
        } catch (SQLException e) {
            showError("Database Error", "Failed to load students: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Update percentage for a student
     */
    private void updatePercentage(Map<String, Object> student) {
        if (exam == null) return;

        Object pointsObj = student.get("pointsEarned");
        if (pointsObj != null && exam.getTotalPoints() > 0) {
            try {
                int points = Integer.parseInt(pointsObj.toString());
                double percentage = (points * 100.0) / exam.getTotalPoints();
                student.put("percentage", String.format("%.1f%%", percentage));
            } catch (NumberFormatException e) {
                student.put("percentage", "");
            }
        } else {
            student.put("percentage", "");
        }
    }

    @FXML
    private void handleSave() {
        try {
            if (exam == null) {
                showError("Error", "No exam selected.");
                return;
            }

            int savedCount = 0;
            for (Map<String, Object> student : studentsList) {
                String studentId = student.get("studentId").toString();
                Object pointsObj = student.get("pointsEarned");
                Integer pointsEarned = null;
                
                if (pointsObj != null) {
                    try {
                        pointsEarned = Integer.parseInt(pointsObj.toString());
                    } catch (NumberFormatException e) {
                        // Skip invalid entries
                        continue;
                    }
                }

                String comments = student.get("comments") != null ? 
                    student.get("comments").toString() : "";

                ExamGrade grade = examService.recordExamGrade(
                    exam.getId(), studentId, pointsEarned, comments
                );

                if (grade != null) {
                    savedCount++;
                }
            }

            showSuccess("Saved grades for " + savedCount + " student(s)!");
            loadStudents(); // Refresh to show updated data
        } catch (SQLException e) {
            showError("Database Error", "Failed to save grades: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleBack(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/professor_exams.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("");
            stage.setMaximized(true);
            stage.show();
        } catch (IOException e) {
            showError("Error", "Unable to return to exams page: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void disableAllControls() {
        if (saveButton != null) saveButton.setDisable(true);
        if (studentsTable != null) studentsTable.setDisable(true);
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Get student name by ID
     */
    private String getStudentName(String studentId) throws SQLException {
        int studentIdInt;
        try {
            studentIdInt = Integer.parseInt(studentId);
        } catch (NumberFormatException e) {
            return "Unknown";
        }

        String sql = "SELECT Username FROM Users WHERE UserID = ?";
        try (java.sql.Connection conn = edu.facilities.service.DatabaseConnection.getConnection();
             java.sql.PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, studentIdInt);
            
            try (java.sql.ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("Username");
                }
            }
        }
        return "Unknown";
    }

    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

