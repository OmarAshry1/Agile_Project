package org.example.universitymangmentsystem.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;

import edu.facilities.model.Room;
import edu.facilities.model.RoomType;
import edu.facilities.model.RoomStatus;
import edu.facilities.service.RoomService;

import java.io.IOException;
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

    // Search and Filters
    @FXML private TextField searchField;
    @FXML private ComboBox<String> typeFilter;
    @FXML private ComboBox<String> statusFilter;

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
    
    // Backend Service
    private RoomService roomService = new RoomService();

    // ============================================
    //  INITIALIZATION
    // ============================================

    /**
     * This method is automatically called after FXML is loaded
     */
    @FXML
    public void initialize() {
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
                loadRoomData();
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
                loadRoomData();
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
                // Delete from backend service
                boolean deleted = roomService.deleteRoom(selectedRoom.getId());
                
                if (deleted) {
                    // Refresh data from service
                    loadRoomData();
                    updateStatistics();
                    showInfo("Success", "Room deleted successfully!");
                } else {
                    showError("Error", "Failed to delete room. Please try again.");
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

    // ============================================
    //  UTILITY METHODS
    // ============================================

    /**
     * Load room data from backend service
     */
    private void loadRoomData() {
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
}