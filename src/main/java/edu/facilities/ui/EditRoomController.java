package edu.facilities.ui;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import edu.facilities.model.Room;
import edu.facilities.model.RoomType;
import edu.facilities.model.RoomStatus;
import edu.facilities.service.RoomService;

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
    private Room roomObject;

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

        System.out.println("EditRoomController initialized");
    }

    /**
     * Set room data to edit (with Room object reference)
     * This method will be called from RoomsController
     */
    public void setRoomData(Room room, String roomNumber, String type, int capacity,
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
     * Set room service reference
     */
    public void setRoomService(RoomService roomService) {
        this.roomService = roomService;
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

        // Update the room using RoomService
        if (roomObject != null && roomService != null) {
            // Update name if changed
            if (!roomObject.getName().equals(roomId)) {
                roomObject.setName(roomId);
            }

            // Update location if changed
            if (!roomObject.getLocation().equals(location)) {
                roomObject.setLocation(location);
            }

            // Update type using service
            roomService.updateRoomType(roomId, type);

            // Update capacity using service
            roomService.updateRoomCapacity(roomId, capacity);

            // Update status using service
            roomService.updateRoomStatus(roomId, status);

            System.out.println("Room Updated Successfully:");
            System.out.println("  ID: " + roomId);
            System.out.println("  Type: " + type);
            System.out.println("  Capacity: " + capacity);
            System.out.println("  Location: " + location);
            System.out.println("  Status: " + status);

            // Show success message
            showSuccess("Room updated successfully!");
        } else {
            showError("Room service not available. Please contact support.");
            return;
        }

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