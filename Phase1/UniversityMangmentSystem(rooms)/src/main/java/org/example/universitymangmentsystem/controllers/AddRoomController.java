package org.example.universitymangmentsystem.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.collections.ObservableList;

/**
 * Controller for Add Room Form (add_room.fxml)
 * Handles the creation of new room records
 */
public class AddRoomController {

    // ============================================
    //  FXML INJECTED COMPONENTS
    // ============================================

    @FXML private TextField roomNumberField;
    @FXML private ComboBox<String> typeComboBox;
    @FXML private TextField capacityField;
    @FXML private TextField buildingField;
    @FXML private TextField floorField;
    @FXML private TextArea equipmentArea;
    @FXML private ComboBox<String> statusComboBox;

    // Error Labels
    @FXML private Label roomNumberError;
    @FXML private Label typeError;
    @FXML private Label capacityError;
    @FXML private Label buildingError;

    // Reference to the rooms list from RoomsController
    private ObservableList<Object> roomsList;

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

        // Set default status
        statusComboBox.setValue("Available");

        System.out.println("AddRoomController initialized");
    }

    /**
     * Set the rooms list reference from RoomsController
     */
    public void setRoomsList(ObservableList<Object> roomsList) {
        this.roomsList = roomsList;
    }

    // ============================================
    //  BUTTON HANDLERS
    // ============================================

    /**
     * Handle Save button click
     * Validates input and saves the new room
     */
    @FXML
    private void handleSave() {
        // Clear previous errors
        clearErrors();

        // Validate inputs
        if (!validateInputs()) {
            return;
        }

        // Get values from form
        String roomNumber = roomNumberField.getText().trim();
        String type = typeComboBox.getValue();
        int capacity = Integer.parseInt(capacityField.getText().trim());
        String building = buildingField.getText().trim();
        String floor = floorField.getText().trim();
        String equipment = equipmentArea.getText().trim();
        String status = statusComboBox.getValue();

        // Create new Room object using reflection (to work with the Room class in RoomsController)
        try {
            // Get the Room class from RoomsController
            Class<?> roomClass = Class.forName("org.example.universitymangmentsystem.controllers.Room");

            // Create new Room instance
            Object newRoom = roomClass.getConstructor(
                    String.class, String.class, int.class, String.class,
                    String.class, String.class, String.class
            ).newInstance(roomNumber, type, capacity, building, floor, equipment, status);

            // Add to the list if we have a reference
            if (roomsList != null) {
                roomsList.add(newRoom);
                System.out.println("Room added to list successfully!");
            } else {
                System.err.println("WARNING: roomsList is null, room not added to table");
            }

            System.out.println("Room Added:");
            System.out.println("  Number: " + roomNumber);
            System.out.println("  Type: " + type);
            System.out.println("  Capacity: " + capacity);
            System.out.println("  Building: " + building);
            System.out.println("  Floor: " + floor);
            System.out.println("  Equipment: " + equipment);
            System.out.println("  Status: " + status);

            // Show success message
            showSuccess("Room added successfully!");

            // Close the window
            closeWindow();

        } catch (Exception e) {
            System.err.println("Error creating Room object: " + e.getMessage());
            e.printStackTrace();
            showError("Could not add room. Please contact support.");
        }
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

        // Validate Room Number
        String roomNumber = roomNumberField.getText().trim();
        if (roomNumber.isEmpty()) {
            roomNumberError.setText("Room number is required");
            roomNumberError.setVisible(true);
            isValid = false;
        } else if (!roomNumber.matches("^[A-Za-z0-9-]+$")) {
            roomNumberError.setText("Room number can only contain letters, numbers, and hyphens");
            roomNumberError.setVisible(true);
            isValid = false;
        }

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
        roomNumberError.setVisible(false);
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