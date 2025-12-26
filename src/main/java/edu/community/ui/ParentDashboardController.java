package edu.community.ui;

import edu.community.service.ParentTeacherMessageService;
import edu.curriculum.model.TranscriptEntry;
import edu.curriculum.service.TranscriptViewService;
import edu.facilities.model.Course;
import edu.facilities.model.Enrollment;
import edu.facilities.model.User;
import edu.facilities.service.AuthService;
import edu.facilities.service.EnrollmentService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controller for parents to view their child's academic progress.
 */
public class ParentDashboardController implements Initializable {

    @FXML private ComboBox<User> studentComboBox;
    @FXML private Label studentNameLabel;
    @FXML private Label studentIDLabel;
    @FXML private Label gpaLabel;
    @FXML private Label totalCreditsLabel;
    
    @FXML private TableView<Enrollment> currentCoursesTable;
    @FXML private TableColumn<Enrollment, String> courseCodeColumn;
    @FXML private TableColumn<Enrollment, String> courseNameColumn;
    @FXML private TableColumn<Enrollment, String> creditsColumn;
    @FXML private TableColumn<Enrollment, String> professorColumn;
    @FXML private TableColumn<Enrollment, String> semesterColumn;
    @FXML private TableColumn<Enrollment, String> gradeColumn;
    
    @FXML private TableView<TranscriptEntry> transcriptTable;
    @FXML private TableColumn<TranscriptEntry, String> transcriptCourseCodeColumn;
    @FXML private TableColumn<TranscriptEntry, String> transcriptCourseNameColumn;
    @FXML private TableColumn<TranscriptEntry, String> transcriptCreditsColumn;
    @FXML private TableColumn<TranscriptEntry, String> transcriptGradeColumn;
    @FXML private TableColumn<TranscriptEntry, String> transcriptSemesterColumn;
    
    @FXML private Button refreshButton;
    @FXML private Button backButton;
    @FXML private Tab currentCoursesTab;
    @FXML private Tab transcriptTab;

    private ParentTeacherMessageService parentService;
    private EnrollmentService enrollmentService;
    private TranscriptViewService transcriptService;
    private AuthService authService;
    private ObservableList<Enrollment> enrollmentsList;
    private ObservableList<TranscriptEntry> transcriptList;
    private User selectedStudent;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        parentService = new ParentTeacherMessageService();
        enrollmentService = new EnrollmentService();
        transcriptService = new TranscriptViewService();
        authService = AuthService.getInstance();
        enrollmentsList = FXCollections.observableArrayList();
        transcriptList = FXCollections.observableArrayList();

        // Access control - only parents
        if (!authService.isLoggedIn()) {
            showAlert(Alert.AlertType.ERROR, "Access Denied", "Please login to view student progress.");
            return;
        }

        String userType = authService.getCurrentUserType();
        if (!"PARENT".equals(userType)) {
            showAlert(Alert.AlertType.ERROR, "Access Denied", "Only parents can access this dashboard.");
            return;
        }

        setupTableColumns();
        loadLinkedStudents();
    }

    private void setupTableColumns() {
        // Current courses table
        courseCodeColumn.setCellValueFactory(cellData -> {
            Course course = cellData.getValue().getCourse();
            return new javafx.beans.property.SimpleStringProperty(course != null ? course.getCode() : "");
        });
        
        courseNameColumn.setCellValueFactory(cellData -> {
            Course course = cellData.getValue().getCourse();
            return new javafx.beans.property.SimpleStringProperty(course != null ? course.getName() : "");
        });
        
        creditsColumn.setCellValueFactory(cellData -> {
            Course course = cellData.getValue().getCourse();
            return new javafx.beans.property.SimpleStringProperty(course != null ? String.valueOf(course.getCredits()) : "");
        });
        
        professorColumn.setCellValueFactory(cellData -> {
            Course course = cellData.getValue().getCourse();
            if (course != null && course.getProfessors() != null && !course.getProfessors().isEmpty()) {
                return new javafx.beans.property.SimpleStringProperty(course.getProfessors().get(0).getUsername());
            }
            return new javafx.beans.property.SimpleStringProperty("TBA");
        });
        
        semesterColumn.setCellValueFactory(cellData -> {
            Course course = cellData.getValue().getCourse();
            return new javafx.beans.property.SimpleStringProperty(course != null ? course.getSemester() : "");
        });
        
        gradeColumn.setCellValueFactory(cellData -> {
            String grade = cellData.getValue().getGrade();
            return new javafx.beans.property.SimpleStringProperty(grade != null && !grade.isEmpty() ? grade : "In Progress");
        });

        currentCoursesTable.setItems(enrollmentsList);

        // Transcript table
        transcriptCourseCodeColumn.setCellValueFactory(new PropertyValueFactory<>("courseCode"));
        transcriptCourseNameColumn.setCellValueFactory(new PropertyValueFactory<>("courseName"));
        transcriptCreditsColumn.setCellValueFactory(cellData -> {
            return new javafx.beans.property.SimpleStringProperty(
                String.valueOf(cellData.getValue().getCredits()));
        });
        transcriptGradeColumn.setCellValueFactory(new PropertyValueFactory<>("grade"));
        transcriptSemesterColumn.setCellValueFactory(new PropertyValueFactory<>("semester"));

        transcriptTable.setItems(transcriptList);
    }

    private void loadLinkedStudents() {
        try {
            int parentUserID = Integer.parseInt(authService.getCurrentUser().getId());
            List<User> students = parentService.getParentStudents(parentUserID);

            if (students.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "No Students Linked",
                    "You don't have any students linked to your account. " +
                    "Please contact the administrator to link your child(ren) to your account.");
                studentComboBox.setItems(FXCollections.observableArrayList());
                studentComboBox.setDisable(true);
                return;
            }

            studentComboBox.setItems(FXCollections.observableArrayList(students));
            studentComboBox.setCellFactory(param -> new ListCell<User>() {
                @Override
                protected void updateItem(User item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(item.getUsername() + " (ID: " + item.getId() + ")");
                    }
                }
            });

            studentComboBox.setOnAction(e -> {
                User selected = studentComboBox.getValue();
                if (selected != null) {
                    selectedStudent = selected;
                    loadStudentData(selected);
                }
            });

            // Auto-select first student if only one
            if (students.size() == 1) {
                studentComboBox.getSelectionModel().select(0);
                selectedStudent = students.get(0);
                loadStudentData(selectedStudent);
            }

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to load linked students: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to load linked students: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadStudentData(User student) {
        try {
            // Update student info labels
            studentNameLabel.setText("Student: " + student.getUsername());
            studentIDLabel.setText("Student ID: " + student.getId());

            // Load current enrollments
            List<Enrollment> enrollments = enrollmentService.getStudentEnrollments(student, true);
            enrollmentsList.clear();
            enrollmentsList.addAll(enrollments);

            // Load transcript and calculate GPA
            List<TranscriptEntry> transcriptEntries = transcriptService.getTranscriptEntries(student);
            transcriptList.clear();
            transcriptList.addAll(transcriptEntries);

            double gpa = transcriptService.calculateGPA(transcriptEntries);
            int totalCredits = transcriptService.calculateTotalCredits(transcriptEntries);

            gpaLabel.setText(String.format("GPA: %.2f", gpa));
            totalCreditsLabel.setText("Total Credits: " + totalCredits);

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to load student data: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to load student data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleRefresh(ActionEvent event) {
        if (selectedStudent != null) {
            loadStudentData(selectedStudent);
        } else if (studentComboBox.getValue() != null) {
            selectedStudent = studentComboBox.getValue();
            loadStudentData(selectedStudent);
        }
    }

    @FXML
    private void handleBack(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/dashboard.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Dashboard");
            stage.setMaximized(true);
            stage.show();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Navigation Error", "Could not load dashboard: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

