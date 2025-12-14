package edu.facilities.ui;

import edu.facilities.model.Course;
import edu.facilities.model.CourseAttribute;
import edu.facilities.model.CourseAttributeType;
import edu.facilities.service.AuthService;
import edu.facilities.service.CourseService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.sql.SQLException;

/**
 * Controller for managing course attributes (EAV pattern)
 * US 2.1 - Flexible EAV attributes
 */
public class CourseAttributesController {

    @FXML private Label courseLabel;
    @FXML private TableView<CourseAttribute> attributesTable;
    @FXML private TableColumn<CourseAttribute, String> nameColumn;
    @FXML private TableColumn<CourseAttribute, String> valueColumn;
    @FXML private TableColumn<CourseAttribute, String> typeColumn;

    @FXML private TextField attributeNameField;
    @FXML private TextArea attributeValueArea;
    @FXML private ComboBox<String> attributeTypeComboBox;

    @FXML private Button addButton;
    @FXML private Button updateButton;
    @FXML private Button deleteButton;
    @FXML private Button closeButton;

    @FXML private Label statusLabel;

    private CourseService courseService;
    private Course course;
    private ObservableList<CourseAttribute> attributesList = FXCollections.observableArrayList();
    private AuthService authService = AuthService.getInstance();

    @FXML
    public void initialize() {
        attributeTypeComboBox.getItems().addAll("TEXT", "NUMBER", "BOOLEAN", "DATE", "JSON");
        attributeTypeComboBox.setValue("TEXT");

        setupTableColumns();
    }

    public void setCourseService(CourseService courseService) {
        this.courseService = courseService;
    }

    public void setCourse(Course course) {
        this.course = course;
        if (course != null) {
            courseLabel.setText("Course: " + course.getCode() + " - " + course.getName());
            loadAttributes();
        }
    }

    private void setupTableColumns() {
        nameColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getAttributeName())
        );

        valueColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getAttributeValue())
        );

        typeColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getAttributeType().toString())
        );
    }

    private void loadAttributes() {
        if (course == null) return;

        try {
            List<CourseAttribute> attributes = course.getAttributes();
            attributesList.clear();
            attributesList.addAll(attributes);
            attributesTable.setItems(attributesList);
            statusLabel.setText("Loaded " + attributes.size() + " attribute(s)");
        } catch (Exception e) {
            showError("Error", "Failed to load attributes: " + e.getMessage());
        }
    }

    @FXML
    private void handleAdd() {
        if (!validateInputs()) {
            return;
        }

        try {
            String name = attributeNameField.getText().trim();
            String value = attributeValueArea.getText().trim();
            CourseAttributeType type = CourseAttributeType.valueOf(attributeTypeComboBox.getValue());

            boolean success = courseService.setCourseAttribute(course.getId(), name, value, type);

            if (success) {
                // Reload course to get updated attributes
                Course updatedCourse = courseService.getCourseById(course.getId());
                if (updatedCourse != null) {
                    course = updatedCourse;
                    loadAttributes();
                    clearFields();
                    showInfo("Success", "Attribute added successfully!");
                }
            } else {
                showError("Error", "Failed to add attribute.");
            }
        } catch (SQLException e) {
            if (e.getMessage().contains("UNIQUE")) {
                showError("Error", "Attribute name already exists for this course.");
            } else {
                showError("Database Error", "Failed to add attribute: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleUpdate() {
        CourseAttribute selected = attributesTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("No Selection", "Please select an attribute to update.");
            return;
        }

        if (!validateInputs()) {
            return;
        }

        try {
            String name = selected.getAttributeName(); // Keep original name
            String value = attributeValueArea.getText().trim();
            CourseAttributeType type = CourseAttributeType.valueOf(attributeTypeComboBox.getValue());

            boolean success = courseService.setCourseAttribute(course.getId(), name, value, type);

            if (success) {
                Course updatedCourse = courseService.getCourseById(course.getId());
                if (updatedCourse != null) {
                    course = updatedCourse;
                    loadAttributes();
                    clearFields();
                    showInfo("Success", "Attribute updated successfully!");
                }
            } else {
                showError("Error", "Failed to update attribute.");
            }
        } catch (SQLException e) {
            showError("Database Error", "Failed to update attribute: " + e.getMessage());
        }
    }

    @FXML
    private void handleDelete() {
        CourseAttribute selected = attributesTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("No Selection", "Please select an attribute to delete.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Attribute");
        confirm.setHeaderText("Are you sure you want to delete this attribute?");
        confirm.setContentText("Attribute: " + selected.getAttributeName());

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    boolean deleted = courseService.deleteCourseAttribute(course.getId(), selected.getAttributeName());
                    if (deleted) {
                        Course updatedCourse = courseService.getCourseById(course.getId());
                        if (updatedCourse != null) {
                            course = updatedCourse;
                            loadAttributes();
                            clearFields();
                            showInfo("Success", "Attribute deleted successfully!");
                        }
                    } else {
                        showError("Error", "Failed to delete attribute.");
                    }
                } catch (SQLException e) {
                    showError("Database Error", "Failed to delete attribute: " + e.getMessage());
                }
            }
        });
    }

    @FXML
    private void handleTableSelection() {
        CourseAttribute selected = attributesTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            attributeNameField.setText(selected.getAttributeName());
            attributeValueArea.setText(selected.getAttributeValue());
            attributeTypeComboBox.setValue(selected.getAttributeType().toString());
        }
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }

    private boolean validateInputs() {
        if (attributeNameField.getText().trim().isEmpty()) {
            showError("Validation Error", "Attribute name is required.");
            return false;
        }

        if (attributeTypeComboBox.getValue() == null) {
            showError("Validation Error", "Attribute type is required.");
            return false;
        }

        return true;
    }

    private void clearFields() {
        attributeNameField.clear();
        attributeValueArea.clear();
        attributeTypeComboBox.setValue("TEXT");
        attributesTable.getSelectionModel().clearSelection();
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

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

