package edu.facilities.ui;

import edu.facilities.model.Equipment;
import edu.facilities.model.EquipmentAllocation;
import edu.facilities.model.User;
import edu.facilities.service.AuthService;
import edu.facilities.service.EquipmentService;
import edu.facilities.service.MaintenanceService;
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
import java.util.List;

public class AllocateEquipmentController {

    @FXML
    private ComboBox<String> equipmentComboBox;

    @FXML
    private ToggleGroup allocationTypeGroup;

    @FXML
    private RadioButton userRadio;

    @FXML
    private RadioButton departmentRadio;

    @FXML
    private VBox userSelectionBox;

    @FXML
    private ComboBox<String> userComboBox;

    @FXML
    private VBox departmentSelectionBox;

    @FXML
    private TextField departmentField;

    @FXML
    private TextArea notesTextArea;

    @FXML
    private Button allocateButton;

    @FXML
    private Button cancelButton;

    @FXML
    private Label equipmentError;

    @FXML
    private Label userError;

    @FXML
    private Label departmentError;

    @FXML
    private Label statusLabel;

    private EquipmentService equipmentService = new EquipmentService();
    private MaintenanceService maintenanceService = new MaintenanceService();
    private AuthService authService = AuthService.getInstance();

    @FXML
    public void initialize() {
        // Check if user is logged in and is admin
        if (!authService.isLoggedIn()) {
            showError("Access Denied", "Please login to allocate equipment.");
            disableAllControls();
            return;
        }

        String userType = authService.getCurrentUserType();
        if (!"ADMIN".equals(userType)) {
            showError("Access Denied", "Only administrators can allocate equipment.");
            disableAllControls();
            return;
        }

        // Setup radio button listeners
        userRadio.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                userSelectionBox.setVisible(true);
                userSelectionBox.setManaged(true);
                departmentSelectionBox.setVisible(false);
                departmentSelectionBox.setManaged(false);
            }
        });

        departmentRadio.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                userSelectionBox.setVisible(false);
                userSelectionBox.setManaged(false);
                departmentSelectionBox.setVisible(true);
                departmentSelectionBox.setManaged(true);
            }
        });

        // Load available equipment
        loadAvailableEquipment();
        
        // Load staff users
        loadStaffUsers();
    }

    /**
     * Load available equipment into dropdown
     */
    private void loadAvailableEquipment() {
        try {
            List<Equipment> equipment = equipmentService.getAvailableEquipment(null, null);
            equipmentComboBox.getItems().clear();
            for (Equipment eq : equipment) {
                String display = eq.getEquipmentTypeName() + " (ID: #" + eq.getId() + ")";
                equipmentComboBox.getItems().add(display);
            }

            if (equipmentComboBox.getItems().isEmpty()) {
                equipmentComboBox.setPromptText("No available equipment");
            } else {
                equipmentComboBox.setPromptText("Select equipment (" + equipmentComboBox.getItems().size() + " available)");
            }
        } catch (SQLException e) {
            showError("Database Error", "Failed to load equipment: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Load staff users into dropdown
     */
    private void loadStaffUsers() {
        try {
            List<User> staffUsers = maintenanceService.getStaffUsers();
            userComboBox.getItems().clear();
            for (User user : staffUsers) {
                userComboBox.getItems().add(user.getUsername() + " (ID: " + user.getId() + ")");
            }

            if (userComboBox.getItems().isEmpty()) {
                userComboBox.setPromptText("No staff users available");
            }
        } catch (SQLException e) {
            showError("Database Error", "Failed to load staff users: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    void handleAllocate(ActionEvent event) {
        clearErrors();
        statusLabel.setVisible(false);

        // Validate inputs
        boolean hasErrors = false;

        if (equipmentComboBox.getValue() == null || equipmentComboBox.getValue().isBlank()) {
            showFieldError(equipmentError, "Please select equipment");
            hasErrors = true;
        }

        if (hasErrors) {
            return;
        }

        // Get equipment ID from selection
        String equipmentDisplay = equipmentComboBox.getValue();
        String equipmentId = extractIdFromDisplay(equipmentDisplay);

        String allocatedToUserId = null;
        String department = null;

        if (userRadio.isSelected()) {
            if (userComboBox.getValue() == null || userComboBox.getValue().isBlank()) {
                showFieldError(userError, "Please select a user");
                return;
            }
            String userDisplay = userComboBox.getValue();
            allocatedToUserId = extractIdFromDisplay(userDisplay);
        } else {
            if (departmentField.getText() == null || departmentField.getText().isBlank()) {
                showFieldError(departmentError, "Please enter department name");
                return;
            }
            department = departmentField.getText().trim();
        }

        // Get current admin user
        User adminUser = authService.getCurrentUser();
        if (adminUser == null) {
            showError("Authentication Error", "User session expired. Please login again.");
            return;
        }

        String notes = notesTextArea.getText() != null ? notesTextArea.getText().trim() : "";

        try {
            EquipmentAllocation allocation = equipmentService.allocateEquipment(
                equipmentId,
                allocatedToUserId,
                department,
                adminUser.getId(),
                notes
            );

            if (allocation != null) {
                statusLabel.setText("Equipment allocated successfully! Allocation ID: #" + allocation.getId());
                statusLabel.setVisible(true);
                statusLabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-size: 12px;");

                // Clear form
                equipmentComboBox.setValue(null);
                userComboBox.setValue(null);
                departmentField.clear();
                notesTextArea.clear();

                // Reload available equipment
                loadAvailableEquipment();

                // Show success alert
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Equipment Allocated");
                alert.setHeaderText("Equipment allocated successfully!");
                alert.setContentText("Equipment has been allocated " + 
                                   (allocatedToUserId != null ? "to user" : "to department: " + department));
                alert.showAndWait();
            } else {
                showError("Allocation Error", "Failed to allocate equipment. Please try again.");
            }
        } catch (IllegalArgumentException e) {
            showError("Validation Error", e.getMessage());
        } catch (SQLException e) {
            showError("Database Error", "Failed to allocate equipment: " + e.getMessage());
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

    private String extractIdFromDisplay(String display) {
        // Extract ID from display string like "Equipment Type (ID: #123)" or "Username (ID: 123)"
        if (display == null || display.isBlank()) {
            return null;
        }
        int startIdx = display.indexOf("ID: ");
        if (startIdx == -1) {
            return null;
        }
        startIdx += 4;
        int endIdx = display.indexOf(")", startIdx);
        if (endIdx == -1) {
            endIdx = display.length();
        }
        String idStr = display.substring(startIdx, endIdx).trim();
        // Remove # if present
        if (idStr.startsWith("#")) {
            idStr = idStr.substring(1);
        }
        return idStr;
    }

    private void navigateToDashboard(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/dashboard.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Dashboard");
            stage.show();
        } catch (IOException e) {
            showError("Navigation Error", "Unable to return to dashboard: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void clearErrors() {
        showFieldError(equipmentError, null);
        showFieldError(userError, null);
        showFieldError(departmentError, null);
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
        if (equipmentComboBox != null) equipmentComboBox.setDisable(true);
        if (userComboBox != null) userComboBox.setDisable(true);
        if (departmentField != null) departmentField.setDisable(true);
        if (notesTextArea != null) notesTextArea.setDisable(true);
        if (allocateButton != null) allocateButton.setDisable(true);
    }
}

