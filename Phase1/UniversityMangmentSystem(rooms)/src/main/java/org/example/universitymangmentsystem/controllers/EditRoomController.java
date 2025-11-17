package org.example.universitymangmentsystem.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

/**
 * Controller for Edit Room Form (edit_room.fxml)
 * Handles editing existing room records
 */
public class EditRoomController {

    // ============================================
    //  FXML INJECTED COMPONENTS
    // ============================================

    @FXML private Label roomInfoLabel;
    @FXML private TextField roomNumberField;
    @FXML private ComboBox<String> typeComboBox;
    @FXML private TextField capacityField;
    @FXML private TextField buildingField;
    @FXML private TextField floorField;
    @FXML private TextArea equipmentArea;
    @FXML private ComboBox<String> statusComboBox;

    // Error Labels
    @FXML private Label typeError;
    @FXML private Label capacityError;
    @FXML private Label buildingError;

    // Store the original room object to update it directly
    private Object roomObject;

    // ============================================
    //  INITIALIZATION
    // ============================================

    /**
     * Initialize the form with dropdown options
     */
    @FXML
    public void initialize() {
        // Populate Type dropdown
        typeComboBox.getItems().addAll(
                "Classroom",
                "Laboratory",
                "Lecture Hall",
                "Seminar Room",
                "Computer Lab"
        );

        // Populate Status dropdown
        statusComboBox.getItems().addAll(
                "Available",
                "Booked",
                "Maintenance",
                "Unavailable"
        );

        System.out.println("EditRoomController initialized");
    }

    /**
     * Set room data to edit (with Room object reference)
     * This method will be called from RoomsController
     */
    public void setRoomData(Object room, String roomNumber, String type, int capacity,
                            String building, String floor, String equipment, String status) {
        this.roomObject = room;

        // Populate fields with existing data
        roomNumberField.setText(roomNumber);
        typeComboBox.setValue(type);
        capacityField.setText(String.valueOf(capacity));
        buildingField.setText(building);
        floorField.setText(floor);
        equipmentArea.setText(equipment);
        statusComboBox.setValue(status);

        // Update header label
        roomInfoLabel.setText("Editing room: " + roomNumber);
    }

    /**
     * Set room data to edit (backward compatible)
     */
    public void setRoomData(String roomNumber, String type, int capacity,
                            String building, String floor, String equipment, String status) {
        // Populate fields with existing data
        roomNumberField.setText(roomNumber);
        typeComboBox.setValue(type);
        capacityField.setText(String.valueOf(capacity));
        buildingField.setText(building);
        floorField.setText(floor);
        equipmentArea.setText(equipment);
        statusComboBox.setValue(status);

        // Update header label
        roomInfoLabel.setText("Editing room: " + roomNumber);
    }

    // ============================================
    //  BUTTON HANDLERS
    // ============================================

    /**
     * Handle Update button click
     * Validates input and updates the room
     */
    @FXML
    private void handleUpdate() {
        // Clear previous errors
        clearErrors();

        // Validate inputs
        if (!validateInputs()) {
            return;
        }

        // Get updated values from form
        String roomNumber = roomNumberField.getText().trim();
        String type = typeComboBox.getValue();
        int capacity = Integer.parseInt(capacityField.getText().trim());
        String building = buildingField.getText().trim();
        String floor = floorField.getText().trim();
        String equipment = equipmentArea.getText().trim();
        String status = statusComboBox.getValue();

        // Update the room object directly if we have a reference
        if (roomObject != null) {
            try {
                // Use reflection to update the Room object
                Class<?> roomClass = roomObject.getClass();
                roomClass.getMethod("setType", String.class).invoke(roomObject, type);
                roomClass.getMethod("setCapacity", int.class).invoke(roomObject, capacity);
                roomClass.getMethod("setBuilding", String.class).invoke(roomObject, building);
                roomClass.getMethod("setFloor", String.class).invoke(roomObject, floor);
                roomClass.getMethod("setEquipment", String.class).invoke(roomObject, equipment);
                roomClass.getMethod("setStatus", String.class).invoke(roomObject, status);

                System.out.println("Room Updated Successfully:");
                System.out.println("  Number: " + roomNumber);
                System.out.println("  Type: " + type);
                System.out.println("  Capacity: " + capacity);
                System.out.println("  Building: " + building);
                System.out.println("  Floor: " + floor);
                System.out.println("  Equipment: " + equipment);
                System.out.println("  Status: " + status);
            } catch (Exception e) {
                System.err.println("Error updating room object: " + e.getMessage());
                e.printStackTrace();
            }
        }

        // Show success message
        showSuccess("Room updated successfully!");

        // Close the window
        closeWindow();
    }

    /**
     * Handle Cancel button click
     * Closes the form without saving
     */
    @FXML
    private void handleCancel() {
        // Show confirmation
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Cancel");
        confirm.setHeaderText("Are you sure you want to cancel?");
        confirm.setContentText("Any unsaved changes will be lost.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                closeWindow();
            }
        });
    }

    // ============================================
    //  VALIDATION
    // ============================================

    /**
     * Validate all form inputs
     * Returns true if valid, false otherwise
     */
    private boolean validateInputs() {
        boolean isValid = true;

        // Validate Type
        if (typeComboBox.getValue() == null) {
            typeError.setText("Please select a room type");
            typeError.setVisible(true);
            isValid = false;
        }

        // Validate Capacity
        String capacityText = capacityField.getText().trim();
        if (capacityText.isEmpty()) {
            capacityError.setText("Capacity is required");
            capacityError.setVisible(true);
            isValid = false;
        } else {
            try {
                int capacity = Integer.parseInt(capacityText);
                if (capacity <= 0) {
                    capacityError.setText("Capacity must be greater than 0");
                    capacityError.setVisible(true);
                    isValid = false;
                } else if (capacity > 1000) {
                    capacityError.setText("Capacity seems too large. Please verify");
                    capacityError.setVisible(true);
                    isValid = false;
                }
            } catch (NumberFormatException e) {
                capacityError.setText("Capacity must be a valid number");
                capacityError.setVisible(true);
                isValid = false;
            }
        }

        // Validate Building
        if (buildingField.getText().trim().isEmpty()) {
            buildingError.setText("Building is required");
            buildingError.setVisible(true);
            isValid = false;
        }

        return isValid;
    }

    /**
     * Clear all error messages
     */
    private void clearErrors() {
        typeError.setVisible(false);
        capacityError.setVisible(false);
        buildingError.setVisible(false);
    }

    // ============================================
    //  UTILITY METHODS
    // ============================================

    /**
     * Close the current window
     */
    private void closeWindow() {
        Stage stage = (Stage) roomNumberField.getScene().getWindow();
        stage.close();
    }

    /**
     * Show success message
     */
    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Show error message
     */
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}