package edu.facilities.ui;

import edu.facilities.model.Equipment;
import edu.facilities.service.AuthService;
import edu.facilities.service.EquipmentService;
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

public class AddEquipmentController {

    @FXML
    private ComboBox<String> equipmentTypeComboBox;

    @FXML
    private TextField serialNumberField;

    @FXML
    private TextField locationField;

    @FXML
    private TextArea notesTextArea;

    @FXML
    private Button addButton;

    @FXML
    private Button cancelButton;

    @FXML
    private Label equipmentTypeError;

    @FXML
    private Label statusLabel;

    private EquipmentService equipmentService = new EquipmentService();
    private AuthService authService = AuthService.getInstance();

    @FXML
    public void initialize() {
        // Check if user is logged in and is admin
        if (!authService.isLoggedIn()) {
            showError("Access Denied", "Please login to add equipment.");
            disableAllControls();
            return;
        }

        String userType = authService.getCurrentUserType();
        if (!"ADMIN".equals(userType)) {
            showError("Access Denied", "Only administrators can add equipment.");
            disableAllControls();
            return;
        }

        // Load existing equipment types
        loadEquipmentTypes();
    }

    /**
     * Load existing equipment types into dropdown
     */
    private void loadEquipmentTypes() {
        try {
            java.util.List<String> types = equipmentService.getAllEquipmentTypes();
            equipmentTypeComboBox.getItems().clear();
            equipmentTypeComboBox.getItems().addAll(types);
        } catch (SQLException e) {
            System.err.println("Error loading equipment types: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    void handleAdd(ActionEvent event) {
        clearErrors();
        statusLabel.setVisible(false);

        // Validate inputs
        boolean hasErrors = false;

        if (equipmentTypeComboBox.getValue() == null || equipmentTypeComboBox.getValue().isBlank()) {
            showFieldError(equipmentTypeError, "Please select or enter equipment type");
            hasErrors = true;
        }

        if (hasErrors) {
            return;
        }

        String equipmentTypeName = equipmentTypeComboBox.getValue().trim();
        String serialNumber = serialNumberField.getText() != null ? serialNumberField.getText().trim() : null;
        String location = locationField.getText() != null ? locationField.getText().trim() : null;
        String notes = notesTextArea.getText() != null ? notesTextArea.getText().trim() : null;

        try {
            // Get or create equipment type (will create if doesn't exist)
            int equipmentTypeId = equipmentService.getEquipmentTypeIdByName(equipmentTypeName);
            
            if (equipmentTypeId == -1) {
                showError("Validation Error", "Failed to create or find equipment type. Please try again.");
                return;
            }

            // Add equipment
            Equipment equipment = equipmentService.addEquipment(
                String.valueOf(equipmentTypeId),
                serialNumber,
                location,
                notes
            );

            if (equipment != null) {
                statusLabel.setText("Equipment added successfully! Equipment ID: #" + equipment.getId());
                statusLabel.setVisible(true);
                statusLabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-size: 12px;");

                // Clear form
                equipmentTypeComboBox.setValue(null);
                serialNumberField.clear();
                locationField.clear();
                notesTextArea.clear();

                // Show success alert
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Equipment Added");
                alert.setHeaderText("Equipment added successfully!");
                alert.setContentText("Equipment ID: #" + equipment.getId() + "\n" +
                                    "Type: " + equipment.getEquipmentTypeName() + "\n" +
                                    "Status: " + equipment.getStatus());
                alert.showAndWait();
            } else {
                showError("Add Error", "Failed to add equipment. Please try again.");
            }
        } catch (IllegalArgumentException e) {
            showError("Validation Error", e.getMessage());
        } catch (SQLException e) {
            showError("Database Error", "Failed to add equipment: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            showError("Error", "An unexpected error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    void handleCancel(ActionEvent event) {
        navigateToDashboard(event);
    }

    private void navigateToDashboard(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/dashboard.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("");
            stage.setMaximized(true);
            stage.show();
        } catch (IOException e) {
            showError("Navigation Error", "Unable to return to dashboard: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void clearErrors() {
        showFieldError(equipmentTypeError, null);
    }

    private void showFieldError(Label label, String message) {
        if (label == null) {
            return;
        }
        if (message == null || message.isBlank()) {
            label.setText("");
            label.setVisible(false);
        } else {
            label.setText(message);
            label.setVisible(true);
        }
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void disableAllControls() {
        if (equipmentTypeComboBox != null) equipmentTypeComboBox.setDisable(true);
        if (serialNumberField != null) serialNumberField.setDisable(true);
        if (locationField != null) locationField.setDisable(true);
        if (notesTextArea != null) notesTextArea.setDisable(true);
        if (addButton != null) addButton.setDisable(true);
    }
}

