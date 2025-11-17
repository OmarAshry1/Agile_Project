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

import java.io.IOException;
import java.net.URL;

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

        // Load dummy data for testing (replace with database later)
        loadDummyData();

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
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getRoomNumber())
        );

        typeColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getType())
        );

        capacityColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleIntegerProperty(cellData.getValue().getCapacity()).asObject()
        );

        buildingColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getBuilding())
        );

        floorColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getFloor())
        );

        equipmentColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getEquipment())
        );

        statusColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getStatus())
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
                if (!room.getRoomNumber().toLowerCase().contains(lowerCaseFilter) &&
                        !room.getType().toLowerCase().contains(lowerCaseFilter) &&
                        !room.getBuilding().toLowerCase().contains(lowerCaseFilter) &&
                        !room.getEquipment().toLowerCase().contains(lowerCaseFilter)) {
                    return false;
                }
            }

            // Type filter
            String selectedType = typeFilter.getValue();
            if (selectedType != null && !selectedType.equals("All Types") && !selectedType.equals(room.getType())) {
                return false;
            }

            // Status filter
            String selectedStatus = statusFilter.getValue();
            if (selectedStatus != null && !selectedStatus.equals("All Status") && !selectedStatus.equals(room.getStatus())) {
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

            // Get controller and pass the rooms list
            AddRoomController controller = loader.getController();
            @SuppressWarnings("unchecked")
            ObservableList rawList = (ObservableList) roomsList;
            controller.setRoomsList(rawList);

            // Create new stage (window)
            Stage stage = new Stage();
            stage.setTitle("Add New Room");
            stage.initModality(Modality.APPLICATION_MODAL); // Block main window
            stage.setScene(new Scene(root));

            // Set up callback for when window closes
            stage.setOnHidden(event -> {
                // Refresh data after adding new room
                roomsTable.refresh();
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
            System.out.println("Opening Edit Room window for: " + selectedRoom.getRoomNumber());

            // Load the edit room form
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/edit_room.fxml"));
            Parent root = loader.load();

            // Get the controller and pass room data with object reference
            EditRoomController controller = loader.getController();
            controller.setRoomData(
                    selectedRoom,  // Pass the actual Room object
                    selectedRoom.getRoomNumber(),
                    selectedRoom.getType(),
                    selectedRoom.getCapacity(),
                    selectedRoom.getBuilding(),
                    selectedRoom.getFloor(),
                    selectedRoom.getEquipment(),
                    selectedRoom.getStatus()
            );

            // Create new stage
            Stage stage = new Stage();
            stage.setTitle("Edit Room - " + selectedRoom.getRoomNumber());
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));

            // Set up callback for when window closes
            stage.setOnHidden(event -> {
                // Refresh data after editing
                roomsTable.refresh();
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
        confirmDialog.setContentText("Room: " + selectedRoom.getRoomNumber() + " - " + selectedRoom.getType());

        confirmDialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                // TODO: Delete from database
                // roomService.deleteRoom(selectedRoom.getId());

                // Remove from table
                roomsList.remove(selectedRoom);
                updateStatistics();

                showInfo("Success", "Room deleted successfully!");
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
     * Refresh room data from database
     */
    private void refreshRoomData() {
        // TODO: Replace with actual database call
        // roomsList.setAll(roomService.getAllRooms());
        updateStatistics();
        roomsTable.refresh();
    }

    /**
     * Update footer statistics (total, available, booked, maintenance)
     */
    private void updateStatistics() {
        ObservableList<Room> currentList = roomsTable.getItems();
        int total = currentList.size();
        int available = (int) currentList.stream().filter(r -> "Available".equals(r.getStatus())).count();
        int booked = (int) currentList.stream().filter(r -> "Booked".equals(r.getStatus())).count();
        int maintenance = (int) currentList.stream().filter(r -> "Maintenance".equals(r.getStatus())).count();

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

    /**
     * Load dummy data for testing (REMOVE THIS LATER)
     * Your backend team will replace this with database queries
     */
    private void loadDummyData() {
        roomsList.add(new Room("C101", "Classroom", 50, "Main Building", "1st Floor", "Projector, Whiteboard", "Available"));
        roomsList.add(new Room("L201", "Laboratory", 30, "Science Building", "2nd Floor", "Computers, Lab Equipment", "Available"));
        roomsList.add(new Room("C305", "Lecture Hall", 120, "Main Building", "3rd Floor", "Projector, Sound System", "Booked"));
        roomsList.add(new Room("L102", "Laboratory", 25, "Engineering Building", "1st Floor", "3D Printers, Tools", "Maintenance"));
        roomsList.add(new Room("C202", "Classroom", 40, "Main Building", "2nd Floor", "Smart Board", "Available"));
        roomsList.add(new Room("S101", "Seminar Room", 20, "Library Building", "1st Floor", "Projector, Conference Table", "Available"));
        roomsList.add(new Room("CL201", "Computer Lab", 35, "Technology Building", "2nd Floor", "30 Computers, Printer", "Booked"));
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

// ============================================
//  ROOM MODEL (Temporary - Backend will create proper model)
// ============================================

/**
 * Simple Room model class
 * Your backend team will create a proper Room.java class
 */
class Room {
    private String roomNumber;
    private String type;
    private int capacity;
    private String building;
    private String floor;
    private String equipment;
    private String status;

    public Room(String roomNumber, String type, int capacity, String building,
                String floor, String equipment, String status) {
        this.roomNumber = roomNumber;
        this.type = type;
        this.capacity = capacity;
        this.building = building;
        this.floor = floor;
        this.equipment = equipment;
        this.status = status;
    }

    // Getters
    public String getRoomNumber() { return roomNumber; }
    public String getType() { return type; }
    public int getCapacity() { return capacity; }
    public String getBuilding() { return building; }
    public String getFloor() { return floor; }
    public String getEquipment() { return equipment; }
    public String getStatus() { return status; }

    // Setters
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }
    public void setType(String type) { this.type = type; }
    public void setCapacity(int capacity) { this.capacity = capacity; }
    public void setBuilding(String building) { this.building = building; }
    public void setFloor(String floor) { this.floor = floor; }
    public void setEquipment(String equipment) { this.equipment = equipment; }
    public void setStatus(String status) { this.status = status; }
}