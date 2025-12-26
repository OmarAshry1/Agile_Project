package edu.facilities.ui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;

import edu.facilities.model.Room;
import edu.facilities.model.RoomType;
import edu.facilities.model.RoomStatus;
import edu.facilities.service.AuthService;
import edu.facilities.service.RoomService;

import java.sql.SQLException;

/**
 * Controller for Edit Room Form (edit_room.fxml)
 * Handles editing existing room records
 */
public class EditRoomController {

    // ============================================
    //  FXML INJECTED COMPONENTS
    // ============================================

    @FXML
    private Label roomInfoLabel;
    @FXML
    private TextField roomNumberField;
    @FXML
    private ComboBox<String> typeComboBox;
    @FXML
    private TextField capacityField;
    @FXML
    private TextField buildingField;
    @FXML
    private TextField floorField;
    @FXML
    private TextArea equipmentArea;
    @FXML
    private ComboBox<String> statusComboBox;

    // Error Labels
    @FXML
    private Label typeError;
    @FXML
    private Label capacityError;
    @FXML
    private Label buildingError;

    @FXML private Button backButton;

    // Store the original room object to update it directly
    private Room roomObject;

    // Store the original room ID/code to use in WHERE clauses
    private String originalRoomId;

    // Reference to the room service
    private RoomService roomService;

    // Reference to auth service for authorization checks
    private AuthService authService = AuthService.getInstance();

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
        // Store the original room ID/code to use in WHERE clauses for updates
        this.originalRoomId = roomNumber;

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
    private void handleUpdate() throws SQLException {
        // REQUIREMENT: Only admins can update rooms
        if (!checkAdminAccess()) {
            showError("Access Denied", "Only administrators can update rooms.");
            return;
        }

        // Clear previous errors
        clearErrors();

        // Validate inputs
        if (!validateInputs()) {
            return;
        }

        // Get updated values from form
        String newRoomId = roomNumberField.getText().trim();
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

        // Update the room using RoomService - update all attributes in one call
        // IMPORTANT: Use originalRoomId for WHERE clause to ensure we only update the specific room
        if (roomObject != null && roomService != null && originalRoomId != null) {
            // Use the comprehensive updateRoom method to update all fields at once
            roomService.updateRoom(originalRoomId, newRoomId, newRoomId, type, capacity, location, status);
            
            // Update the room object to reflect changes
            if (!originalRoomId.equals(newRoomId)) {
                roomObject.setName(newRoomId);
            }
            roomObject.setLocation(location);
            roomObject.setType(type);
            roomObject.setCapacity(capacity);
            roomObject.setStatus(status);

            System.out.println("Room Updated Successfully:");
            System.out.println("  Original ID: " + originalRoomId);
            System.out.println("  New ID: " + newRoomId);
            System.out.println("  Type: " + type);
            System.out.println("  Capacity: " + capacity);
            System.out.println("  Location: " + location);
            System.out.println("  Status: " + status);

            // Show success message
            showSuccess("Room updated successfully!");
        } else {
            showError("Error", "Room service not available. Please contact support.");
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

    @FXML
    private void handleBack(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/rooms.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("");
            stage.setMaximized(true);
            stage.show();
        } catch (IOException e) {
            showError("Error", "Unable to return to rooms page: " + e.getMessage());
            e.printStackTrace();
        }
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
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
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
            case "classroom":
                return RoomType.CLASSROOM;
            case "laboratory":
            case "lab":
                return RoomType.LAB;
            case "office":
                return RoomType.OFFICE;
            case "conference":
            case "seminar room":
            case "lecture hall":
            case "computer lab":
                return RoomType.CONFERENCE;
            default:
                return RoomType.CLASSROOM;
        }
    }

    /**
     * Convert display string to RoomStatus enum
     */
    private RoomStatus stringToRoomStatus(String statusStr) {
        if (statusStr == null) return RoomStatus.AVAILABLE;
        switch (statusStr.toLowerCase()) {
            case "available":
                return RoomStatus.AVAILABLE;
            case "booked":
            case "occupied":
            case "unavailable":
                return RoomStatus.OCCUPIED;
            case "maintenance":
                return RoomStatus.MAINTENANCE;
            default:
                return RoomStatus.AVAILABLE;
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

    /**
     * Check if current user is an admin
     * REQUIREMENT: Admin-only access to room updates
     *
     * @return true if user is admin, false otherwise
     */
    private boolean checkAdminAccess() {
        if (authService == null) {
            System.err.println("AuthService is not initialized");
            return false;
        }

        if (!authService.isLoggedIn()) {
            System.out.println("User is not logged in");
            return false;
        }

        String userType = authService.getCurrentUserType();
        boolean isAdmin = "ADMIN".equals(userType);

        if (!isAdmin) {
            System.out.println("Access denied: User type is " + userType + ", ADMIN required");
        }

        return isAdmin;
    }
}