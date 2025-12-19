package edu.curriculum.ui;

import edu.curriculum.model.CourseMaterial;
import edu.curriculum.model.MaterialType;
import edu.curriculum.service.MaterialService;
import edu.facilities.model.Course;
import edu.facilities.service.CourseService;
import edu.facilities.model.User;
import edu.facilities.service.AuthService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for Upload Course Materials
 * US 2.5 - Upload Course Materials (Professor)
 */
public class UploadMaterialController {

    @FXML private ComboBox<String> courseComboBox;
    @FXML private TextField titleField;
    @FXML private TextArea descriptionArea;
    @FXML private ComboBox<String> materialTypeComboBox;
    @FXML private TextField linkUrlField;
    @FXML private TextField fileNameField;
    @FXML private Button browseFileButton;
    @FXML private Button uploadButton;
    @FXML private Button cancelButton;

    @FXML private Label courseError;
    @FXML private Label titleError;
    @FXML private Label descriptionError;
    @FXML private Label typeError;
    @FXML private Label fileError;
    @FXML private Label linkError;

    @FXML private Label statusLabel;

    private MaterialService materialService = new MaterialService();
    private CourseService courseService = new CourseService();
    private AuthService authService = AuthService.getInstance();
    private Map<String, Course> courseMap = new HashMap<>();
    private File selectedFile;

    @FXML
    public void initialize() {
        // Check if user is logged in
        if (!authService.isLoggedIn()) {
            showError("Access Denied", "Please login to upload materials.");
            disableAllControls();
            return;
        }

        // Check user type - only PROFESSOR can upload materials
        String userType = authService.getCurrentUserType();
        if (!"PROFESSOR".equals(userType)) {
            showError("Access Denied", "Only professors can upload course materials.");
            disableAllControls();
            return;
        }

        // Populate material type dropdown
        materialTypeComboBox.getItems().addAll("LECTURE", "READING", "VIDEO", "LINK");
        materialTypeComboBox.setValue("LECTURE");

        // Add listener to material type to show/hide file/link fields
        materialTypeComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if ("LINK".equals(newVal)) {
                linkUrlField.setVisible(true);
                linkUrlField.setManaged(true);
                browseFileButton.setVisible(false);
                browseFileButton.setManaged(false);
                fileNameField.setVisible(false);
                fileNameField.setManaged(false);
            } else {
                linkUrlField.setVisible(false);
                linkUrlField.setManaged(false);
                browseFileButton.setVisible(true);
                browseFileButton.setManaged(true);
                fileNameField.setVisible(true);
                fileNameField.setManaged(true);
            }
        });

        // Initially hide link field
        linkUrlField.setVisible(false);
        linkUrlField.setManaged(false);

        // Load professor's courses
        loadCourses();
    }

    private void loadCourses() {
        try {
            User currentUser = authService.getCurrentUser();
            if (currentUser == null) {
                showError("Authentication Error", "User session expired. Please login again.");
                return;
            }

            String professorId = currentUser.getId();
            List<Course> courses = courseService.getCoursesByProfessor(professorId);
            courseMap.clear();
            courseComboBox.getItems().clear();

            if (courses.isEmpty()) {
                courseComboBox.setPromptText("No courses available");
                showError("No Courses Found",
                    "No courses were found for your account.\n\n" +
                    "Please ensure courses are assigned to your professor account.");
                return;
            }

            for (Course course : courses) {
                String displayName = course.getCode() + " - " + course.getName();
                courseComboBox.getItems().add(displayName);
                courseMap.put(displayName, course);
            }
        } catch (SQLException e) {
            showError("Database Error", "Failed to load courses: " + e.getMessage());
        }
    }

    @FXML
    private void handleBrowseFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Course Material File");

        // Set file filters based on material type
        String materialType = materialTypeComboBox.getValue();
        if (materialType != null) {
            FileChooser.ExtensionFilter filter;
            switch (materialType) {
                case "LECTURE":
                case "READING":
                    filter = new FileChooser.ExtensionFilter(
                        "Documents", "*.pdf", "*.docx", "*.pptx");
                    fileChooser.getExtensionFilters().add(filter);
                    break;
                case "VIDEO":
                    filter = new FileChooser.ExtensionFilter(
                        "Video Files", "*.mp4", "*.avi", "*.mov");
                    fileChooser.getExtensionFilters().add(filter);
                    break;
            }
        }

        File file = fileChooser.showOpenDialog(browseFileButton.getScene().getWindow());
        if (file != null) {
            selectedFile = file;
            fileNameField.setText(file.getName());

            // Validate file size (50MB max)
            if (file.length() > 50 * 1024 * 1024) {
                showError("File Too Large", 
                    "File size exceeds 50 MB limit. Current size: " + 
                    String.format("%.2f MB", file.length() / (1024.0 * 1024.0)));
                selectedFile = null;
                fileNameField.clear();
            } else {
                statusLabel.setText("Selected: " + file.getName() + 
                                  " (" + String.format("%.2f MB", file.length() / (1024.0 * 1024.0)) + ")");
            }
        }
    }

    @FXML
    private void handleUpload() {
        clearErrors();

        if (!validateInputs()) {
            return;
        }

        try {
            User currentUser = authService.getCurrentUser();
            if (currentUser == null) {
                showError("Authentication Error", "User session expired. Please login again.");
                return;
            }

            // Get selected course
            String selectedCourseDisplay = courseComboBox.getValue();
            if (selectedCourseDisplay == null || !courseMap.containsKey(selectedCourseDisplay)) {
                courseError.setText("Please select a course");
                courseError.setVisible(true);
                return;
            }

            Course selectedCourse = courseMap.get(selectedCourseDisplay);

            // Get form values
            String title = titleField.getText().trim();
            String description = descriptionArea.getText().trim();
            MaterialType materialType = MaterialType.valueOf(materialTypeComboBox.getValue());
            String linkUrl = materialType == MaterialType.LINK ? linkUrlField.getText().trim() : null;

            // Upload material
            CourseMaterial material = materialService.uploadMaterial(
                selectedCourse.getId(), title, description, materialType,
                selectedFile, linkUrl, currentUser.getId()
            );

            if (material != null) {
                showSuccess("Material uploaded successfully!");
                clearForm();
                statusLabel.setText("Upload successful!");
            } else {
                showError("Error", "Failed to upload material.");
            }
        } catch (IllegalArgumentException e) {
            showError("Validation Error", e.getMessage());
        } catch (SQLException e) {
            showError("Database Error", "Failed to upload material: " + e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            showError("File Error", "Failed to save file: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            showError("Error", "An unexpected error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleCancel() {
        closeWindow();
    }

    private boolean validateInputs() {
        boolean isValid = true;

        // Validate course
        if (courseComboBox.getValue() == null) {
            courseError.setText("Please select a course");
            courseError.setVisible(true);
            isValid = false;
        }

        // Validate title
        if (titleField.getText().trim().isEmpty()) {
            titleError.setText("Title is required");
            titleError.setVisible(true);
            isValid = false;
        }

        // Validate description
        if (descriptionArea.getText().trim().isEmpty()) {
            descriptionError.setText("Description is required");
            descriptionError.setVisible(true);
            isValid = false;
        }

        // Validate material type
        if (materialTypeComboBox.getValue() == null) {
            typeError.setText("Material type is required");
            typeError.setVisible(true);
            isValid = false;
        }

        // Validate file or link based on type
        String materialType = materialTypeComboBox.getValue();
        if ("LINK".equals(materialType)) {
            if (linkUrlField.getText().trim().isEmpty()) {
                linkError.setText("Link URL is required");
                linkError.setVisible(true);
                isValid = false;
            }
        } else {
            if (selectedFile == null || !selectedFile.exists()) {
                fileError.setText("Please select a file");
                fileError.setVisible(true);
                isValid = false;
            }
        }

        return isValid;
    }

    private void clearErrors() {
        courseError.setVisible(false);
        titleError.setVisible(false);
        descriptionError.setVisible(false);
        typeError.setVisible(false);
        fileError.setVisible(false);
        linkError.setVisible(false);
    }

    private void clearForm() {
        titleField.clear();
        descriptionArea.clear();
        materialTypeComboBox.setValue("LECTURE");
        fileNameField.clear();
        linkUrlField.clear();
        selectedFile = null;
        statusLabel.setText("");
    }

    private void disableAllControls() {
        if (courseComboBox != null) courseComboBox.setDisable(true);
        if (titleField != null) titleField.setDisable(true);
        if (descriptionArea != null) descriptionArea.setDisable(true);
        if (materialTypeComboBox != null) materialTypeComboBox.setDisable(true);
        if (browseFileButton != null) browseFileButton.setDisable(true);
        if (uploadButton != null) uploadButton.setDisable(true);
    }

    private void closeWindow() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

