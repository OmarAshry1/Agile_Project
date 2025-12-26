package edu.facilities.ui;

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
import javafx.stage.Modality;
import javafx.stage.Stage;

import edu.facilities.model.Room;
import edu.facilities.model.RoomStatus;
import edu.facilities.model.RoomType;
import edu.facilities.service.AuthService;
import edu.facilities.service.BookingService;
import edu.facilities.service.RoomService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/**
 * Controller for Room Management Screen (rooms.fxml)
 * Handles UI interactions and connects to backend services
 */
public class RoomsController {

    // ============================================
    //  FXML INJECTED COMPONENTS
    // ============================================

    // Buttons
    @FXML private Button addButton;
    @FXML private Button editButton;
    @FXML private Button deleteButton;
    @FXML private Button backButton;

    // Search and Filters
    @FXML private TextField searchField;
    @FXML private ComboBox<String> typeFilter;
    @FXML private ComboBox<String> statusFilter;
    @FXML private ComboBox<String> capacityFilter;
    @FXML private TextField equipmentFilter;

    // Table and Columns
    @FXML private TableView<Room> roomsTable;
    @FXML private TableColumn<Room, String> roomNumberColumn;
    @FXML private TableColumn<Room, String> typeColumn;
    @FXML private TableColumn<Room, Integer> capacityColumn;
    @FXML private TableColumn<Room, String> buildingColumn;
    @FXML private TableColumn<Room, String> floorColumn;
    @FXML private TableColumn<Room, String> equipmentColumn;
    @FXML private TableColumn<Room, String> statusColumn;

    // Footer Statistics Labels
    @FXML private Label totalRoomsLabel;
    @FXML private Label availableRoomsLabel;
    @FXML private Label bookedRoomsLabel;
    @FXML private Label maintenanceRoomsLabel;

    // Data List
    private ObservableList<Room> roomsList = FXCollections.observableArrayList();
    private FilteredList<Room> filteredData;

    // Backend Services
    private RoomService roomService = new RoomService();
    private BookingService bookingService = new BookingService();
    private AuthService authService = AuthService.getInstance();

    // ============================================
    //  INITIALIZATION
    // ============================================

    /**
     * This method is automatically called after FXML is loaded
     */
    @FXML
    public void initialize() throws SQLException {
        // Check if user is logged in
        if (!authService.isLoggedIn()) {
            showError("Access Denied", "Please login to view rooms.");
            disableAllControls();
            return;
        }

        // Set up table columns
        setupTableColumns();
        // Populate filter dropdowns
        populateFilters();

        // Load data from backend service
        loadRoomData();

        // Setup search and filter functionality
        setupSearchAndFilter();

        // Update statistics
        updateStatistics();

        // REQUIREMENT: Students can view rooms but not create/edit/delete
        // Only admins can create/edit/delete
        String userType = authService.getCurrentUserType();
        if (!"ADMIN".equals(userType)) {
            // Disable create/edit/delete buttons for non-admins
            if (addButton != null) addButton.setDisable(true);
            if (editButton != null) editButton.setDisable(true);
            if (deleteButton != null) deleteButton.setDisable(true);
        }

        System.out.println("RoomsController initialized successfully!");
    }

    // ============================================
    //  TABLE SETUP
    // ============================================

    /**
     * Configure table columns to display Room object properties
     */
    private void setupTableColumns() {
        roomNumberColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getId())
        );

        typeColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(roomTypeToString(cellData.getValue().getType()))
        );

        capacityColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleIntegerProperty(cellData.getValue().getCapacity()).asObject()
        );

        buildingColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(parseLocation(cellData.getValue().getLocation())[0])
        );

        floorColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(parseLocation(cellData.getValue().getLocation())[1])
        );

        equipmentColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(parseLocation(cellData.getValue().getLocation())[2])
        );

        statusColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(roomStatusToString(cellData.getValue().getStatus()))
        );
    }

    /**
     * Setup search and filter functionality
     */
    private void setupSearchAndFilter() {
        // Initialize filtered list
        filteredData = new FilteredList<>(roomsList, p -> true);

        // Bind the sorted list to table
        SortedList<Room> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(roomsTable.comparatorProperty());
        roomsTable.setItems(sortedData);

        // Add listener to search field
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            applyFilters();
        });

        // Add listeners to filter dropdowns
        typeFilter.valueProperty().addListener((observable, oldValue, newValue) -> {
            applyFilters();
        });

        statusFilter.valueProperty().addListener((observable, oldValue, newValue) -> {
            applyFilters();
        });

        capacityFilter.valueProperty().addListener((observable, oldValue, newValue) -> {
            applyFilters();
        });
    }

    /**
     * Apply all active filters and search
     */
    private void applyFilters() {
        filteredData.setPredicate(room -> {
            // Search filter
            String searchText = searchField.getText();
            if (searchText != null && !searchText.isEmpty()) {
                String lowerCaseFilter = searchText.toLowerCase();
                String[] locationParts = parseLocation(room.getLocation());
                if (!room.getId().toLowerCase().contains(lowerCaseFilter) &&
                        !roomTypeToString(room.getType()).toLowerCase().contains(lowerCaseFilter) &&
                        !locationParts[0].toLowerCase().contains(lowerCaseFilter) &&
                        !locationParts[2].toLowerCase().contains(lowerCaseFilter)) {
                    return false;
                }
            }

            // Type filter
            String selectedType = typeFilter.getValue();
            if (selectedType != null && !selectedType.equals("All Types") &&
                    !selectedType.equals(roomTypeToString(room.getType()))) {
                return false;
            }

            // Status filter
            String selectedStatus = statusFilter.getValue();
            if (selectedStatus != null && !selectedStatus.equals("All Status") &&
                    !selectedStatus.equals(roomStatusToString(room.getStatus()))) {
                return false;
            }

            // Capacity filter
            String selectedCapacity = capacityFilter.getValue();
            if (selectedCapacity != null && !selectedCapacity.equals("Any Capacity")) {
                try {
                    // Parse capacity from string like "10+", "20+", "50+", etc.
                    int minCapacity = Integer.parseInt(selectedCapacity.replace("+", "").trim());
                    if (room.getCapacity() < minCapacity) {
                        return false;
                    }
                } catch (NumberFormatException e) {
                    // If parsing fails, ignore this filter
                }
            }

            // Equipment filter
            String equipmentText = equipmentFilter.getText();
            if (equipmentText != null && !equipmentText.isBlank()) {
                String lowerCaseEquipment = equipmentText.toLowerCase();
                String[] locationParts = parseLocation(room.getLocation());
                String equipment = locationParts.length > 2 ? locationParts[2] : "";
                if (!equipment.toLowerCase().contains(lowerCaseEquipment)) {
                    return false;
                }
            }

            return true;
        });
        updateStatistics();
    }

    // ============================================
    //  BUTTON HANDLERS
    // ============================================

    /**
     * Handle "Add Room" button click
     * Opens add_room.fxml in a new window
     */
    @FXML
    private void handleAddRoom() {
        // REQUIREMENT: Only admins can create rooms
        if (!checkAdminAccess()) {
            showError("Access Denied", "Only administrators can create rooms.");
            return;
        }
        
        try {
            System.out.println("Opening Add Room window...");

            // Load the add room form
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/add_room.fxml"));
            Parent root = loader.load();

            // Get controller and pass the room service
            AddRoomController controller = loader.getController();
            controller.setRoomService(roomService);

            // Create new stage (window)
            Stage stage = new Stage();
            stage.setTitle("Add New Room");
            stage.initModality(Modality.APPLICATION_MODAL); // Block main window
            stage.setScene(new Scene(root));

            // Set up callback for when window closes
            stage.setOnHidden(event -> {
                // Refresh data after adding new room
                try {
                    loadRoomData();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
                updateStatistics();
            });

            stage.showAndWait();

        } catch (IOException e) {
            showError("Error", "Could not open Add Room window: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Handle "Edit" button click
     * Opens edit_room.fxml with selected room data
     */
    @FXML
    private void handleEditRoom() {
        // REQUIREMENT: Only admins can update rooms
        if (!checkAdminAccess()) {
            showError("Access Denied", "Only administrators can update rooms.");
            return;
        }
        
        // Get selected room
        Room selectedRoom = roomsTable.getSelectionModel().getSelectedItem();

        if (selectedRoom == null) {
            showWarning("No Selection", "Please select a room to edit");
            return;
        }

        try {
            System.out.println("Opening Edit Room window for: " + selectedRoom.getId());

            // Load the edit room form
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/edit_room.fxml"));
            Parent root = loader.load();

            // Get the controller and pass room data with object reference
            EditRoomController controller = loader.getController();
            String[] locationParts = parseLocation(selectedRoom.getLocation());
            controller.setRoomData(
                    selectedRoom,  // Pass the actual Room object
                    selectedRoom.getId(),
                    roomTypeToString(selectedRoom.getType()),
                    selectedRoom.getCapacity(),
                    locationParts[0],  // building
                    locationParts[1],  // floor
                    locationParts[2],  // equipment
                    roomStatusToString(selectedRoom.getStatus())
            );
            controller.setRoomService(roomService);

            // Create new stage
            Stage stage = new Stage();
            stage.setTitle("Edit Room - " + selectedRoom.getId());
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));

            // Set up callback for when window closes
            stage.setOnHidden(event -> {
                // Refresh data after editing
                try {
                    loadRoomData();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
                updateStatistics();
            });

            stage.showAndWait();

        } catch (IOException e) {
            showError("Error", "Could not open Edit Room window: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            showError("Error", "An unexpected error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Handle "Delete" button click
     * Deletes selected room after confirmation
     */
    @FXML
    private void handleDeleteRoom() {
        // REQUIREMENT: Only admins can delete rooms
        if (!checkAdminAccess()) {
            showError("Access Denied", "Only administrators can delete rooms.");
            return;
        }
        
        // Get selected room
        Room selectedRoom = roomsTable.getSelectionModel().getSelectedItem();

        if (selectedRoom == null) {
            showWarning("No Selection", "Please select a room to delete");
            return;
        }

        // Show confirmation dialog
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Delete Room");
        confirmDialog.setHeaderText("Are you sure you want to delete this room?");
        confirmDialog.setContentText("Room: " + selectedRoom.getId() + " - " + roomTypeToString(selectedRoom.getType()));

        confirmDialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                // REQUIREMENT: Verify room has no future bookings before deletion
                String roomId = selectedRoom.getId();

                try {
                    // Check for future bookings (only if Bookings table exists)
                    try {
                        if (bookingService.hasFutureBookings(roomId)) {
                            int bookingCount = bookingService.getFutureBookingCount(roomId);
                            showError("Cannot Delete Room",
                                    "This room has " + bookingCount + " future booking(s). " +
                                            "Please cancel or reschedule all future bookings before deleting the room.");
                            return;
                        }
                    } catch (SQLException e) {
                        // Bookings table might not exist yet - continue with deletion
                        System.out.println("Note: Could not check bookings (table may not exist): " + e.getMessage());
                    }

                    // Delete from backend service (this will also delete related maintenance tickets)
                    boolean deleted = roomService.deleteRoom(roomId);

                    if (deleted) {
                        // Refresh data from service
                        try {
                            loadRoomData();
                        } catch (SQLException e) {
                            showError("Warning", "Room was deleted but failed to refresh the list: " + e.getMessage());
                            e.printStackTrace();
                        }
                        updateStatistics();
                        showInfo("Success", "Room deleted successfully!");
                    } else {
                        showError("Error", "Failed to delete room. The room may not exist or there was a database error.");
                    }
                } catch (SQLException e) {
                    // Handle SQL errors with user-friendly messages
                    String errorMessage = e.getMessage();
                    String userMessage = "Failed to delete room: ";
                    
                    if (errorMessage != null) {
                        if (errorMessage.contains("foreign key constraint") || 
                            errorMessage.contains("FOREIGN KEY constraint")) {
                            userMessage += "The room cannot be deleted because it has related records (maintenance tickets or bookings). " +
                                         "Please delete or resolve related records first.";
                        } else if (errorMessage.contains("permission denied") || 
                                  errorMessage.contains("access denied")) {
                            userMessage += "You do not have permission to delete this room.";
                        } else {
                            userMessage += errorMessage;
                        }
                    } else {
                        userMessage += "An unknown database error occurred.";
                    }
                    
                    showError("Database Error", userMessage);
                    System.err.println("Error deleting room '" + roomId + "': " + e.getMessage());
                    e.printStackTrace();
                } catch (Exception e) {
                    // Handle any other unexpected errors
                    showError("Error", "An unexpected error occurred while deleting the room: " + e.getMessage());
                    System.err.println("Unexpected error deleting room '" + roomId + "': " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });
    }

    // ============================================
    //  SEARCH & FILTER HANDLERS
    // ============================================

    /**
     * Handle search field input
     * Filters table based on search text
     */
    @FXML
    private void handleSearch() {
        applyFilters();
    }

    /**
     * Handle type filter selection
     */
    @FXML
    private void handleFilterType() {
        applyFilters();
    }

    /**
     * Handle status filter selection
     */
    @FXML
    private void handleFilterStatus() {
        applyFilters();
    }

    /**
     * Handle capacity filter selection
     */
    @FXML
    private void handleFilterCapacity() {
        applyFilters();
    }

    /**
     * Handle equipment filter input
     */
    @FXML
    private void handleFilterEquipment() {
        applyFilters();
    }

    @FXML
    private void handleBack(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/dashboard.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Dashboard");
            stage.setMaximized(true);
            stage.show();
        } catch (IOException e) {
            showError("Navigation Error", "Unable to return to dashboard: " + e.getMessage());
        }
    }

    // ============================================
    //  UTILITY METHODS
    // ============================================

    /**
     * Load room data from backend service
     */
    private void loadRoomData() throws SQLException {
        List<Room> rooms = roomService.getAllRooms();
        roomsList.setAll(rooms);
        updateStatistics();
        roomsTable.refresh();
    }

    /**
     * Update footer statistics (total, available, booked, maintenance)
     */
    private void updateStatistics() {
        ObservableList<Room> currentList = roomsTable.getItems();
        int total = currentList.size();
        int available = (int) currentList.stream().filter(r -> r.getStatus() == RoomStatus.AVAILABLE).count();
        int booked = (int) currentList.stream().filter(r -> r.getStatus() == RoomStatus.OCCUPIED).count();
        int maintenance = (int) currentList.stream().filter(r -> r.getStatus() == RoomStatus.MAINTENANCE).count();

        totalRoomsLabel.setText(String.valueOf(total));
        availableRoomsLabel.setText(String.valueOf(available));
        bookedRoomsLabel.setText(String.valueOf(booked));
        maintenanceRoomsLabel.setText(String.valueOf(maintenance));
    }

    /**
     * Populate ComboBox filters with options
     */
    private void populateFilters() {
        // Populate type filter
        typeFilter.getItems().addAll(
                "All Types",
                "Classroom",
                "Laboratory",
                "Lecture Hall",
                "Seminar Room",
                "Computer Lab"
        );
        typeFilter.setValue("All Types");

        // Populate status filter
        statusFilter.getItems().addAll(
                "All Status",
                "Available",
                "Booked",
                "Maintenance",
                "Unavailable"
        );
        statusFilter.setValue("All Status");

        // Populate capacity filter
        capacityFilter.getItems().addAll(
                "Any Capacity",
                "10+",
                "20+",
                "30+",
                "50+",
                "100+",
                "200+"
        );
        capacityFilter.setValue("Any Capacity");
    }

    // ============================================
    //  HELPER METHODS FOR MAPPING
    // ============================================

    /**
     * Convert RoomType enum to display string
     */
    private String roomTypeToString(RoomType type) {
        if (type == null) return "";
        switch (type) {
            case CLASSROOM: return "Classroom";
            case LAB: return "Laboratory";
            case OFFICE: return "Office";
            case CONFERENCE: return "Conference";
            default: return type.toString();
        }
    }

    /**
     * Convert display string to RoomType enum
     */
    private RoomType stringToRoomType(String typeStr) {
        if (typeStr == null) return RoomType.CLASSROOM;
        switch (typeStr.toLowerCase()) {
            case "classroom": return RoomType.CLASSROOM;
            case "laboratory": case "lab": return RoomType.LAB;
            case "office": return RoomType.OFFICE;
            case "conference": case "seminar room": case "lecture hall": return RoomType.CONFERENCE;
            default: return RoomType.CLASSROOM;
        }
    }

    /**
     * Convert RoomStatus enum to display string
     */
    private String roomStatusToString(RoomStatus status) {
        if (status == null) return "";
        switch (status) {
            case AVAILABLE: return "Available";
            case OCCUPIED: return "Booked";
            case MAINTENANCE: return "Maintenance";
            default: return status.toString();
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

    /**
     * Parse location string back to building, floor, equipment
     * Format: "Building|Floor|Equipment"
     * Returns array: [building, floor, equipment]
     */
    private String[] parseLocation(String location) {
        if (location == null || location.isEmpty()) {
            return new String[]{"", "", ""};
        }
        String[] parts = location.split("\\|", 3);
        return new String[]{
                parts.length > 0 ? parts[0] : "",
                parts.length > 1 ? parts[1] : "",
                parts.length > 2 ? parts[2] : ""
        };
    }

    /**
     * Show error dialog
     */
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Show warning dialog
     */
    private void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Show info dialog
     */
    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // ============================================
    //  ADMIN ACCESS CONTROL
    // ============================================

    /**
     * Check if current user is an admin
     * REQUIREMENT: Admin-only access to room management
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

    /**
     * Disable all controls for non-admin users
     */
    private void disableAllControls() {
        if (addButton != null) addButton.setDisable(true);
        if (editButton != null) editButton.setDisable(true);
        if (deleteButton != null) deleteButton.setDisable(true);
        if (searchField != null) searchField.setDisable(true);
        if (typeFilter != null) typeFilter.setDisable(true);
        if (statusFilter != null) statusFilter.setDisable(true);
        if (capacityFilter != null) capacityFilter.setDisable(true);
        if (equipmentFilter != null) equipmentFilter.setDisable(true);
        if (roomsTable != null) roomsTable.setDisable(true);
    }
}