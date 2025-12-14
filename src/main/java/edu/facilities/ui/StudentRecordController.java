package edu.facilities.ui;

import edu.facilities.model.Student;
import edu.facilities.model.StudentStatus;
import edu.facilities.model.YearLevel;
import edu.facilities.service.AuthService;
import edu.facilities.service.StudentRecordService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
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
import java.time.LocalDate;
import java.util.List;

/**
 * Controller for Student Records Management
 * US 2.1 - Log Student Records
 * Admin-only access for Create/Edit/Delete operations
 */
public class StudentRecordController {

    @FXML private TableView<StudentRecordService.StudentRecord> studentsTable;
    @FXML private TableColumn<StudentRecordService.StudentRecord, String> usernameColumn;
    @FXML private TableColumn<StudentRecordService.StudentRecord, String> studentNumberColumn;
    @FXML private TableColumn<StudentRecordService.StudentRecord, String> emailColumn;
    @FXML private TableColumn<StudentRecordService.StudentRecord, String> majorColumn;
    @FXML private TableColumn<StudentRecordService.StudentRecord, String> departmentColumn;
    @FXML private TableColumn<StudentRecordService.StudentRecord, String> statusColumn;
    @FXML private TableColumn<StudentRecordService.StudentRecord, String> yearLevelColumn;
    @FXML private TableColumn<StudentRecordService.StudentRecord, Double> gpaColumn;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilter;
    @FXML private ComboBox<String> departmentFilter;

    @FXML private Button addButton;
    @FXML private Button editButton;
    @FXML private Button deleteButton;
    @FXML private Button backButton;

    @FXML private Label statusLabel;

    private ObservableList<StudentRecordService.StudentRecord> studentsList = FXCollections.observableArrayList();
    private FilteredList<StudentRecordService.StudentRecord> filteredData;
    private StudentRecordService studentRecordService = new StudentRecordService();
    private AuthService authService = AuthService.getInstance();

    @FXML
    public void initialize() {
        // Check admin access
        if (!checkAdminAccess()) {
            disableAllControls();
            return;
        }

        setupTableColumns();
        populateFilters();
        loadStudentRecords();
        setupSearchAndFilter();
    }

    private boolean checkAdminAccess() {
        if (!authService.isLoggedIn()) {
            showError("Access Denied", "Please login to access this feature.");
            return false;
        }

        String userType = authService.getCurrentUserType();
        if (!"ADMIN".equals(userType)) {
            showError("Access Denied", "Only administrators can manage student records.");
            return false;
        }
        return true;
    }

    private void setupTableColumns() {
        usernameColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().getStudent().getUsername())
        );

        studentNumberColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().getStudentNumber() != null ? cellData.getValue().getStudentNumber() : "")
        );

        emailColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().getEmail() != null ? cellData.getValue().getEmail() : "")
        );

        majorColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().getMajor() != null ? cellData.getValue().getMajor() : "")
        );

        departmentColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().getDepartment() != null ? cellData.getValue().getDepartment() : "")
        );

        statusColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        statusToString(cellData.getValue().getStatus()))
        );

        yearLevelColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().getYearLevel() != null ? 
                                cellData.getValue().getYearLevel().toString() : "")
        );

        gpaColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleObjectProperty<>(
                        cellData.getValue().getGpa())
        );
    }

    private void populateFilters() {
        if (statusFilter != null) {
            statusFilter.getItems().addAll(
                    "All Statuses",
                    "ACTIVE",
                    "INACTIVE",
                    "GRADUATED",
                    "SUSPENDED",
                    "WITHDRAWN"
            );
            statusFilter.setValue("All Statuses");
        }
    }

    private void loadStudentRecords() {
        try {
            List<StudentRecordService.StudentRecord> records = studentRecordService.getAllStudentRecords();
            studentsList.clear();
            studentsList.addAll(records);
            statusLabel.setText("Loaded " + records.size() + " student record(s)");
        } catch (SQLException e) {
            showError("Database Error", "Failed to load student records: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void setupSearchAndFilter() {
        filteredData = new FilteredList<>(studentsList, p -> true);

        // Search filter
        if (searchField != null) {
            searchField.textProperty().addListener((observable, oldValue, newValue) -> {
                filteredData.setPredicate(record -> {
                    if (newValue == null || newValue.isEmpty()) {
                        return true;
                    }
                    String lowerCaseFilter = newValue.toLowerCase();
                    return record.getStudent().getUsername().toLowerCase().contains(lowerCaseFilter) ||
                           (record.getEmail() != null && record.getEmail().toLowerCase().contains(lowerCaseFilter)) ||
                           (record.getStudentNumber() != null && record.getStudentNumber().toLowerCase().contains(lowerCaseFilter)) ||
                           (record.getMajor() != null && record.getMajor().toLowerCase().contains(lowerCaseFilter));
                });
            });
        }

        // Status filter
        if (statusFilter != null) {
            statusFilter.setOnAction(e -> {
                String selectedStatus = statusFilter.getValue();
                if (selectedStatus == null || "All Statuses".equals(selectedStatus)) {
                    filteredData.setPredicate(record -> true);
                } else {
                    StudentStatus status = stringToStatus(selectedStatus);
                    filteredData.setPredicate(record -> record.getStatus() == status);
                }
            });
        }

        // Sort by username
        SortedList<StudentRecordService.StudentRecord> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(studentsTable.comparatorProperty());
        studentsTable.setItems(sortedData);
    }

    @FXML
    void handleAdd(ActionEvent event) {
        showStudentRecordDialog(null);
    }

    @FXML
    void handleEdit(ActionEvent event) {
        StudentRecordService.StudentRecord selected = studentsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("No Selection", "Please select a student record to edit.");
            return;
        }
        showStudentRecordDialog(selected);
    }

    @FXML
    void handleDelete(ActionEvent event) {
        StudentRecordService.StudentRecord selected = studentsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("No Selection", "Please select a student record to delete.");
            return;
        }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Deletion");
        confirmAlert.setHeaderText("Delete Student Record");
        confirmAlert.setContentText("Are you sure you want to delete the student record for: " +
                                   selected.getStudent().getUsername() + "?\n\n" +
                                   "This action cannot be undone.");

        java.util.Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                boolean success = studentRecordService.deleteStudentRecord(selected.getStudent().getId());
                if (success) {
                    showSuccess("Record Deleted", "Student record deleted successfully.");
                    loadStudentRecords();
                } else {
                    showError("Deletion Failed", "Failed to delete student record.");
                }
            } catch (SQLException e) {
                showError("Database Error", "Failed to delete record: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    @FXML
    void handleBack(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/dashboard.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Dashboard");
            stage.show();
        } catch (IOException e) {
            showError("Navigation Error", "Unable to return to dashboard: " + e.getMessage());
        }
    }

    private void showStudentRecordDialog(StudentRecordService.StudentRecord existingRecord) {
        Dialog<StudentRecordData> dialog = new Dialog<>();
        dialog.setTitle(existingRecord == null ? "Add Student Record" : "Edit Student Record");
        dialog.setHeaderText(existingRecord == null ? "Create a new student record" : "Edit student record");

        ButtonType submitButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(submitButtonType, ButtonType.CANCEL);

        // Create form fields
        TextField usernameField = new TextField();
        usernameField.setPromptText("Username (required)");
        usernameField.setDisable(existingRecord != null); // Disable if editing

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password (required for new records)");

        TextField emailField = new TextField();
        emailField.setPromptText("Email");

        TextField studentNumberField = new TextField();
        studentNumberField.setPromptText("Student Number");

        TextField majorField = new TextField();
        majorField.setPromptText("Major");

        TextField departmentField = new TextField();
        departmentField.setPromptText("Department");

        DatePicker enrollmentDatePicker = new DatePicker();
        enrollmentDatePicker.setPromptText("Enrollment Date");

        TextField gpaField = new TextField();
        gpaField.setPromptText("GPA (0.00-4.00)");

        ComboBox<StudentStatus> statusComboBox = new ComboBox<>();
        statusComboBox.getItems().addAll(StudentStatus.values());
        statusComboBox.setPromptText("Status");

        DatePicker admissionDatePicker = new DatePicker();
        admissionDatePicker.setPromptText("Admission Date");

        ComboBox<YearLevel> yearLevelComboBox = new ComboBox<>();
        yearLevelComboBox.getItems().addAll(YearLevel.values());
        yearLevelComboBox.setPromptText("Year Level");

        TextArea notesArea = new TextArea();
        notesArea.setPromptText("Notes");
        notesArea.setPrefRowCount(3);

        // Populate if editing
        if (existingRecord != null) {
            usernameField.setText(existingRecord.getStudent().getUsername());
            emailField.setText(existingRecord.getEmail() != null ? existingRecord.getEmail() : "");
            studentNumberField.setText(existingRecord.getStudentNumber() != null ? existingRecord.getStudentNumber() : "");
            majorField.setText(existingRecord.getMajor() != null ? existingRecord.getMajor() : "");
            departmentField.setText(existingRecord.getDepartment() != null ? existingRecord.getDepartment() : "");
            if (existingRecord.getEnrollmentDate() != null) {
                enrollmentDatePicker.setValue(existingRecord.getEnrollmentDate());
            }
            if (existingRecord.getGpa() != null) {
                gpaField.setText(String.valueOf(existingRecord.getGpa()));
            }
            statusComboBox.setValue(existingRecord.getStatus());
            if (existingRecord.getAdmissionDate() != null) {
                admissionDatePicker.setValue(existingRecord.getAdmissionDate());
            }
            yearLevelComboBox.setValue(existingRecord.getYearLevel());
            notesArea.setText(existingRecord.getNotes() != null ? existingRecord.getNotes() : "");
        }

        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20));

        int row = 0;
        grid.add(new Label("Username:"), 0, row);
        grid.add(usernameField, 1, row++);
        grid.add(new Label("Password:"), 0, row);
        grid.add(passwordField, 1, row++);
        grid.add(new Label("Email:"), 0, row);
        grid.add(emailField, 1, row++);
        grid.add(new Label("Student Number:"), 0, row);
        grid.add(studentNumberField, 1, row++);
        grid.add(new Label("Major:"), 0, row);
        grid.add(majorField, 1, row++);
        grid.add(new Label("Department:"), 0, row);
        grid.add(departmentField, 1, row++);
        grid.add(new Label("Enrollment Date:"), 0, row);
        grid.add(enrollmentDatePicker, 1, row++);
        grid.add(new Label("GPA:"), 0, row);
        grid.add(gpaField, 1, row++);
        grid.add(new Label("Status:"), 0, row);
        grid.add(statusComboBox, 1, row++);
        grid.add(new Label("Admission Date:"), 0, row);
        grid.add(admissionDatePicker, 1, row++);
        grid.add(new Label("Year Level:"), 0, row);
        grid.add(yearLevelComboBox, 1, row++);
        grid.add(new Label("Notes:"), 0, row);
        grid.add(notesArea, 1, row++);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == submitButtonType) {
                StudentRecordData data = new StudentRecordData();
                data.username = usernameField.getText();
                data.password = passwordField.getText();
                data.email = emailField.getText();
                data.studentNumber = studentNumberField.getText();
                data.major = majorField.getText();
                data.department = departmentField.getText();
                data.enrollmentDate = enrollmentDatePicker.getValue();
                try {
                    data.gpa = gpaField.getText().isEmpty() ? null : Double.parseDouble(gpaField.getText());
                } catch (NumberFormatException e) {
                    data.gpa = null;
                }
                data.status = statusComboBox.getValue();
                data.admissionDate = admissionDatePicker.getValue();
                data.yearLevel = yearLevelComboBox.getValue();
                data.notes = notesArea.getText();
                data.existingRecord = existingRecord;
                return data;
            }
            return null;
        });

        java.util.Optional<StudentRecordData> result = dialog.showAndWait();
        result.ifPresent(data -> {
            try {
                if (existingRecord == null) {
                    // Create new record
                    if (data.username.isEmpty() || data.password.isEmpty()) {
                        showError("Validation Error", "Username and password are required.");
                        return;
                    }
                    StudentRecordService.StudentRecord created = studentRecordService.createStudentRecord(
                            data.username, data.password, data.email, data.studentNumber,
                            data.major, data.department, data.enrollmentDate, data.gpa,
                            data.status != null ? data.status : StudentStatus.ACTIVE,
                            data.admissionDate, data.yearLevel, data.notes);
                    if (created != null) {
                        showSuccess("Record Created", "Student record created successfully.");
                        loadStudentRecords();
                    } else {
                        showError("Creation Failed", "Failed to create student record.");
                    }
                } else {
                    // Update existing record
                    StudentRecordService.StudentRecord updated = studentRecordService.updateStudentRecord(
                            existingRecord.getStudent().getId(), data.email, data.studentNumber,
                            data.major, data.department, data.enrollmentDate, data.gpa,
                            data.status, data.admissionDate, data.yearLevel, data.notes);
                    if (updated != null) {
                        showSuccess("Record Updated", "Student record updated successfully.");
                        loadStudentRecords();
                    } else {
                        showError("Update Failed", "Failed to update student record.");
                    }
                }
            } catch (IllegalArgumentException e) {
                showError("Validation Error", e.getMessage());
            } catch (SQLException e) {
                showError("Database Error", "Failed to save record: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    private String statusToString(StudentStatus status) {
        if (status == null) return "UNKNOWN";
        switch (status) {
            case ACTIVE: return "Active";
            case INACTIVE: return "Inactive";
            case GRADUATED: return "Graduated";
            case SUSPENDED: return "Suspended";
            case WITHDRAWN: return "Withdrawn";
            default: return "Unknown";
        }
    }

    private StudentStatus stringToStatus(String statusStr) {
        if (statusStr == null) return null;
        try {
            return StudentStatus.valueOf(statusStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showSuccess(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void disableAllControls() {
        if (studentsTable != null) studentsTable.setDisable(true);
        if (addButton != null) addButton.setDisable(true);
        if (editButton != null) editButton.setDisable(true);
        if (deleteButton != null) deleteButton.setDisable(true);
        if (searchField != null) searchField.setDisable(true);
        if (statusFilter != null) statusFilter.setDisable(true);
    }

    // Helper class for dialog data
    private static class StudentRecordData {
        String username;
        String password;
        String email;
        String studentNumber;
        String major;
        String department;
        LocalDate enrollmentDate;
        Double gpa;
        StudentStatus status;
        LocalDate admissionDate;
        YearLevel yearLevel;
        String notes;
        StudentRecordService.StudentRecord existingRecord;
    }
}


