package edu.curriculum.ui;

import edu.curriculum.model.Course;
import edu.curriculum.model.CourseGradeWeights;
import edu.curriculum.service.CourseService;
import edu.curriculum.service.GradeCalculationService;
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
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for Calculate Final Grades Screen (calculate_final_grades.fxml)
 * US: As a professor/system, I want to calculate final grades using weight distributions
 */
public class CalculateFinalGradesController {

    @FXML private ComboBox<String> courseComboBox;
    @FXML private Spinner<Double> assignmentsWeightSpinner;
    @FXML private Spinner<Double> quizzesWeightSpinner;
    @FXML private Spinner<Double> examsWeightSpinner;
    @FXML private Label totalWeightLabel;
    @FXML private Button saveWeightsButton;
    @FXML private Button calculateButton;
    @FXML private Button saveAllButton;
    
    @FXML private TableView<GradeCalculationService.StudentFinalGrade> gradesTable;
    @FXML private TableColumn<GradeCalculationService.StudentFinalGrade, String> studentNameColumn;
    @FXML private TableColumn<GradeCalculationService.StudentFinalGrade, String> studentIdColumn;
    @FXML private TableColumn<GradeCalculationService.StudentFinalGrade, String> calculatedPercentageColumn;
    @FXML private TableColumn<GradeCalculationService.StudentFinalGrade, String> calculatedGradeColumn;
    @FXML private TableColumn<GradeCalculationService.StudentFinalGrade, String> currentGradeColumn;
    @FXML private TableColumn<GradeCalculationService.StudentFinalGrade, String> finalGradeColumn;
    @FXML private TableColumn<GradeCalculationService.StudentFinalGrade, String> saveButtonColumn;
    
    @FXML private Button refreshButton;
    @FXML private Button backButton;
    @FXML private Label statusLabel;

    private ObservableList<GradeCalculationService.StudentFinalGrade> gradesList = FXCollections.observableArrayList();
    private Map<String, Course> courseMap = new HashMap<>();
    private Map<String, String> overrideGrades = new HashMap<>(); // enrollmentId -> grade
    
    private CourseService courseService = new CourseService();
    private GradeCalculationService gradeCalculationService = new GradeCalculationService();
    private AuthService authService = AuthService.getInstance();
    
    private Course currentCourse;

    @FXML
    public void initialize() {
        // Check if user is logged in
        if (!authService.isLoggedIn()) {
            showError("Access Denied", "Please login to calculate final grades.");
            disableAllControls();
            return;
        }

        // Check user type - only PROFESSOR can calculate grades
        String userType = authService.getCurrentUserType();
        if (!"PROFESSOR".equals(userType)) {
            showError("Access Denied", "Only professors can calculate final grades.");
            disableAllControls();
            return;
        }

        // Setup spinners for weight input
        setupWeightSpinners();

        // Setup table columns
        setupTableColumns();

        // Load courses
        loadCourses();

        System.out.println("CalculateFinalGradesController initialized successfully!");
    }

    /**
     * Setup weight spinners with validation
     */
    private void setupWeightSpinners() {
        // Initialize spinners with 0-100 range, step 1, default 0
        SpinnerValueFactory<Double> assignmentsFactory = new SpinnerValueFactory.DoubleSpinnerValueFactory(0, 100, 0, 1);
        SpinnerValueFactory<Double> quizzesFactory = new SpinnerValueFactory.DoubleSpinnerValueFactory(0, 100, 0, 1);
        SpinnerValueFactory<Double> examsFactory = new SpinnerValueFactory.DoubleSpinnerValueFactory(0, 100, 0, 1);
        
        assignmentsWeightSpinner.setValueFactory(assignmentsFactory);
        quizzesWeightSpinner.setValueFactory(quizzesFactory);
        examsWeightSpinner.setValueFactory(examsFactory);

        // Add listeners to update total weight
        assignmentsWeightSpinner.valueProperty().addListener((obs, oldVal, newVal) -> updateTotalWeight());
        quizzesWeightSpinner.valueProperty().addListener((obs, oldVal, newVal) -> updateTotalWeight());
        examsWeightSpinner.valueProperty().addListener((obs, oldVal, newVal) -> updateTotalWeight());
        
        updateTotalWeight();
    }

    /**
     * Update total weight label
     */
    private void updateTotalWeight() {
        double total = assignmentsWeightSpinner.getValue() + 
                      quizzesWeightSpinner.getValue() + 
                      examsWeightSpinner.getValue();
        totalWeightLabel.setText(String.format("Total: %.1f%%", total));
        
        // Change color if not 100%
        if (Math.abs(total - 100.0) < 0.01) {
            totalWeightLabel.setStyle("-fx-text-fill: #4CAF50;");
            saveWeightsButton.setDisable(false);
        } else {
            totalWeightLabel.setStyle("-fx-text-fill: #E53935;");
            saveWeightsButton.setDisable(true);
        }
    }

    /**
     * Setup table columns
     */
    private void setupTableColumns() {
        studentNameColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getStudentName())
        );

        studentIdColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getStudentId())
        );

        calculatedPercentageColumn.setCellValueFactory(cellData -> {
            Double percentage = cellData.getValue().getCalculatedPercentage();
            return new javafx.beans.property.SimpleStringProperty(
                    percentage != null ? String.format("%.1f%%", percentage) : "N/A"
            );
        });

        calculatedGradeColumn.setCellValueFactory(cellData -> {
            String grade = cellData.getValue().getCalculatedGrade();
            return new javafx.beans.property.SimpleStringProperty(grade != null ? grade : "N/A");
        });

        currentGradeColumn.setCellValueFactory(cellData -> {
            String grade = cellData.getValue().getCurrentGrade();
            return new javafx.beans.property.SimpleStringProperty(grade != null ? grade : "Not Set");
        });

        // Final grade column - editable for override
        finalGradeColumn.setCellValueFactory(cellData -> {
            String enrollmentId = cellData.getValue().getEnrollmentId();
            String override = overrideGrades.get(enrollmentId);
            if (override != null) {
                return new javafx.beans.property.SimpleStringProperty(override);
            }
            String calculated = cellData.getValue().getCalculatedGrade();
            return new javafx.beans.property.SimpleStringProperty(calculated != null ? calculated : "");
        });

        finalGradeColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        finalGradeColumn.setOnEditCommit(event -> {
            GradeCalculationService.StudentFinalGrade grade = event.getRowValue();
            String newGrade = event.getNewValue();
            if (newGrade != null && !newGrade.trim().isEmpty()) {
                overrideGrades.put(grade.getEnrollmentId(), newGrade.trim().toUpperCase());
            } else {
                overrideGrades.remove(grade.getEnrollmentId());
            }
            gradesTable.refresh();
        });

        // Save button column
        saveButtonColumn.setCellFactory(column -> new TableCell<GradeCalculationService.StudentFinalGrade, String>() {
            private final Button saveButton = new Button("Save");

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    GradeCalculationService.StudentFinalGrade grade = getTableView().getItems().get(getIndex());
                    saveButton.setOnAction(e -> handleSaveIndividualGrade(grade));
                    setGraphic(saveButton);
                }
            }
        });
    }

    /**
     * Load courses for the professor
     */
    private void loadCourses() {
        try {
            User currentUser = authService.getCurrentUser();
            if (currentUser == null) {
                return;
            }

            List<Course> courses = courseService.getCoursesByProfessor(currentUser.getId());
            courseMap.clear();
            courseComboBox.getItems().clear();

            for (Course course : courses) {
                String displayText = course.getCode() + " - " + course.getName();
                courseComboBox.getItems().add(displayText);
                courseMap.put(displayText, course);
            }

            if (courses.isEmpty()) {
                showStatus("No courses found", false);
            } else {
                showStatus("Loaded " + courses.size() + " courses", true);
            }
        } catch (SQLException e) {
            showError("Database Error", "Failed to load courses: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Load course data when selected
     */
    @FXML
    private void handleLoadCourse() {
        String selectedCourse = courseComboBox.getSelectionModel().getSelectedItem();
        if (selectedCourse == null) {
            showWarning("No Selection", "Please select a course");
            return;
        }

        currentCourse = courseMap.get(selectedCourse);
        if (currentCourse == null) {
            showError("Error", "Selected course not found");
            return;
        }

        // Load existing weights
        loadGradeWeights();

        // Load student grades
        loadStudentGrades();
    }

    /**
     * Load grade weights for the current course
     */
    private void loadGradeWeights() {
        try {
            CourseGradeWeights weights = gradeCalculationService.getGradeWeights(currentCourse.getId());
            if (weights != null) {
                assignmentsWeightSpinner.getValueFactory().setValue(weights.getAssignmentsWeight());
                quizzesWeightSpinner.getValueFactory().setValue(weights.getQuizzesWeight());
                examsWeightSpinner.getValueFactory().setValue(weights.getExamsWeight());
                showStatus("Loaded existing grade weights", true);
            } else {
                // Reset to defaults
                assignmentsWeightSpinner.getValueFactory().setValue(0.0);
                quizzesWeightSpinner.getValueFactory().setValue(0.0);
                examsWeightSpinner.getValueFactory().setValue(0.0);
                showStatus("No weights set. Please configure grade weights.", false);
            }
        } catch (SQLException e) {
            showError("Database Error", "Failed to load grade weights: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Save grade weights
     */
    @FXML
    private void handleSaveWeights() {
        if (currentCourse == null) {
            showWarning("No Course", "Please select a course first");
            return;
        }

        try {
            CourseGradeWeights weights = new CourseGradeWeights(
                    currentCourse.getId(),
                    assignmentsWeightSpinner.getValue(),
                    quizzesWeightSpinner.getValue(),
                    examsWeightSpinner.getValue()
            );

            if (!weights.isValid()) {
                showError("Invalid Weights", "Grade weights must sum to exactly 100%");
                return;
            }

            boolean success = gradeCalculationService.saveGradeWeights(weights);
            if (success) {
                showStatus("Grade weights saved successfully", true);
            } else {
                showError("Error", "Failed to save grade weights");
            }
        } catch (SQLException e) {
            showError("Database Error", "Failed to save grade weights: " + e.getMessage());
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            showError("Invalid Input", e.getMessage());
        }
    }

    /**
     * Load student grades
     */
    private void loadStudentGrades() {
        try {
            if (currentCourse == null) {
                return;
            }

            List<GradeCalculationService.StudentFinalGrade> grades = 
                    gradeCalculationService.getStudentFinalGrades(currentCourse.getId());
            
            gradesList.setAll(grades);
            gradesTable.setItems(gradesList);
            
            // Clear overrides
            overrideGrades.clear();
            
            showStatus("Loaded " + grades.size() + " student grades", true);
        } catch (SQLException e) {
            showError("Database Error", "Failed to load student grades: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Calculate all final grades
     */
    @FXML
    private void handleCalculateGrades() {
        if (currentCourse == null) {
            showWarning("No Course", "Please select a course first");
            return;
        }

        // Verify weights are set
        try {
            CourseGradeWeights weights = gradeCalculationService.getGradeWeights(currentCourse.getId());
            if (weights == null) {
                showError("No Weights", "Please set and save grade weights first");
                return;
            }
        } catch (SQLException e) {
            showError("Database Error", "Failed to check grade weights: " + e.getMessage());
            return;
        }

        // Reload grades (this will recalculate)
        loadStudentGrades();
        showStatus("Grades calculated successfully", true);
    }

    /**
     * Save all grades (both calculated and overridden)
     */
    @FXML
    private void handleSaveAllGrades() {
        if (currentCourse == null) {
            showWarning("No Course", "Please select a course first");
            return;
        }

        try {
            int savedCount = 0;
            for (GradeCalculationService.StudentFinalGrade grade : gradesList) {
                String finalGrade;
                
                // Check if overridden
                String override = overrideGrades.get(grade.getEnrollmentId());
                if (override != null && !override.isEmpty()) {
                    finalGrade = override;
                } else {
                    finalGrade = grade.getCalculatedGrade();
                }

                if (finalGrade != null && !finalGrade.isEmpty()) {
                    boolean success = gradeCalculationService.updateFinalGrade(
                            grade.getEnrollmentId(), finalGrade);
                    if (success) {
                        savedCount++;
                    }
                }
            }

            if (savedCount > 0) {
                showStatus("Saved " + savedCount + " final grades successfully", true);
                // Reload to refresh current grades
                loadStudentGrades();
            } else {
                showWarning("No Grades", "No grades to save");
            }
        } catch (SQLException e) {
            showError("Database Error", "Failed to save grades: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Save individual grade
     */
    private void handleSaveIndividualGrade(GradeCalculationService.StudentFinalGrade grade) {
        try {
            String finalGrade;
            
            // Check if overridden
            String override = overrideGrades.get(grade.getEnrollmentId());
            if (override != null && !override.isEmpty()) {
                finalGrade = override;
            } else {
                finalGrade = grade.getCalculatedGrade();
            }

            if (finalGrade == null || finalGrade.isEmpty()) {
                showWarning("No Grade", "No grade to save for " + grade.getStudentName());
                return;
            }

            boolean success = gradeCalculationService.updateFinalGrade(
                    grade.getEnrollmentId(), finalGrade);
            if (success) {
                showStatus("Saved final grade for " + grade.getStudentName(), true);
                // Reload to refresh
                loadStudentGrades();
            } else {
                showError("Error", "Failed to save grade");
            }
        } catch (SQLException e) {
            showError("Database Error", "Failed to save grade: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleRefresh() {
        if (currentCourse != null) {
            loadStudentGrades();
        }
        showStatus("Data refreshed", true);
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
        if (saveWeightsButton != null) saveWeightsButton.setDisable(true);
        if (calculateButton != null) calculateButton.setDisable(true);
        if (saveAllButton != null) saveAllButton.setDisable(true);
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

