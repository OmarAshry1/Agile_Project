package edu.facilities.ui;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import edu.facilities.model.RoomType;
import edu.facilities.model.RoomStatus;
import edu.facilities.service.RoomService;

import java.sql.SQLException;


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

    // Reference to the room service
    private RoomService roomService;

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
     * Set the room service reference from RoomsController
     */
    public void setRoomService(RoomService roomService) {
        this.roomService = roomService;
    }

    // ============================================
    //  BUTTON HANDLERS
    // ============================================

    /**
     * Handle Save button click
     * Validates input and saves the new room
     */
    @FXML
    private void handleSave() throws SQLException {
        // Clear previous errors
        clearErrors();

        // Validate inputs
        if (!validateInputs()) {
            return;
        }

        // Get values from form
        String roomId = roomNumberField.getText().trim();
        String typeStr = typeComboBox.getValue();
        int capacity = Integer.parseInt(capacityField.getText().trim());
        String building = buildingField.getText().trim();
        String floor = floorField.getText().trim();
        String equipment = equipmentArea.getText().trim();
        String statusStr = statusComboBox.getValue();

        // Convert to backend enums
        RoomType type = stringToRoomType(typeStr);
        RoomStatus status = stringToRoomStatus(statusStr);

        // Combine building, floor, and equipment into location
        String location = combineLocation(building, floor, equipment);

        // Use room ID as name (or you can use a separate name field if needed)
        String roomName = roomId;

        // Create room using RoomService
        if (roomService != null) {
            roomService.createRoom(roomId, roomName, type, capacity, location, status);

            System.out.println("Room Added:");
            System.out.println("  ID: " + roomId);
            System.out.println("  Name: " + roomName);
            System.out.println("  Type: " + type);
            System.out.println("  Capacity: " + capacity);
            System.out.println("  Location: " + location);
            System.out.println("  Status: " + status);

            // Show success message
            showSuccess("Room added successfully!");

            // Close the window
            closeWindow();
        } else {
            showError("Room service not available. Please contact support.");
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

    // ============================================
    //  HELPER METHODS FOR MAPPING
    // ============================================

    /**
     * Convert display string to RoomType enum
     */
    private RoomType stringToRoomType(String typeStr) {
        if (typeStr == null) return RoomType.CLASSROOM;
        switch (typeStr.toLowerCase()) {
            case "classroom": return RoomType.CLASSROOM;
            case "laboratory": case "lab": return RoomType.LAB;
            case "office": return RoomType.OFFICE;
            case "conference": case "seminar room": case "lecture hall": case "computer lab": return RoomType.CONFERENCE;
            default: return RoomType.CLASSROOM;
        }
    }

    /**
     * Convert display string to RoomStatus enum
     */
    private RoomStatus stringToRoomStatus(String statusStr) {
        if (statusStr == null) return RoomStatus.AVAILABLE;
        switch (statusStr.toLowerCase()) {
            case "available": return RoomStatus.AVAILABLE;
            case "booked": case "occupied": case "unavailable": return RoomStatus.OCCUPIED;
            case "maintenance": return RoomStatus.MAINTENANCE;
            default: return RoomStatus.AVAILABLE;
        }
    }

    /**
     * Combine building, floor, and equipment into location string
     * Format: "Building|Floor|Equipment"
     */
    private String combineLocation(String building, String floor, String equipment) {
        return (building != null ? building : "") + "|" +
                (floor != null ? floor : "") + "|" +
                (equipment != null ? equipment : "");
    }
}