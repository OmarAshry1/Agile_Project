package edu.facilities.ui;

import edu.facilities.model.EquipmentAllocation;
import edu.facilities.model.User;
import edu.facilities.service.AuthService;
import edu.facilities.service.EquipmentService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
import java.time.format.DateTimeFormatter;
import java.util.List;

public class MyAllocatedEquipmentController {

    @FXML
    private TableView<EquipmentAllocation> allocationsTable;

    @FXML
    private TableColumn<EquipmentAllocation, String> allocationIdColumn;

    @FXML
    private TableColumn<EquipmentAllocation, String> equipmentTypeColumn;

    @FXML
    private TableColumn<EquipmentAllocation, String> serialNumberColumn;

    @FXML
    private TableColumn<EquipmentAllocation, String> allocationDateColumn;

    @FXML
    private TableColumn<EquipmentAllocation, String> notesColumn;

    @FXML
    private TableColumn<EquipmentAllocation, String> statusColumn;

    @FXML
    private Button refreshButton;

    @FXML
    private Button returnButton;

    @FXML
    private Button backButton;

    @FXML
    private Label statusLabel;

    private EquipmentService equipmentService = new EquipmentService();
    private AuthService authService = AuthService.getInstance();
    private ObservableList<EquipmentAllocation> allocationsList = FXCollections.observableArrayList();

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @FXML
    public void initialize() {
        // Check if user is logged in
        if (!authService.isLoggedIn()) {
            showError("Access Denied", "Please login to view your allocated equipment.");
            disableAllControls();
            return;
        }

        // Check user type - only STAFF can return equipment
        String userType = authService.getCurrentUserType();
        if (!"STAFF".equals(userType) && !"PROFESSOR".equals(userType)) {
            showError("Access Denied", "Only staff and professors can view allocated equipment.");
            disableAllControls();
            return;
        }

        // Setup table columns
        setupTableColumns();

        // Load allocations
        loadAllocations();
    }

    /**
     * Setup table columns to display allocation data
     */
    private void setupTableColumns() {
        allocationIdColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty("#" + cellData.getValue().getId())
        );

        equipmentTypeColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getEquipment().getEquipmentTypeName())
        );

        serialNumberColumn.setCellValueFactory(cellData -> {
            String serial = cellData.getValue().getEquipment().getSerialNumber();
            return new javafx.beans.property.SimpleStringProperty(serial != null ? serial : "");
        });

        allocationDateColumn.setCellValueFactory(cellData -> {
            String dateStr = cellData.getValue().getAllocationDate().format(DATE_FORMATTER);
            return new javafx.beans.property.SimpleStringProperty(dateStr);
        });

        notesColumn.setCellValueFactory(cellData -> {
            String notes = cellData.getValue().getNotes();
            return new javafx.beans.property.SimpleStringProperty(notes != null ? notes : "");
        });

        statusColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getStatus().toString())
        );
    }

    /**
     * Load equipment allocations for the current user
     */
    private void loadAllocations() {
        try {
            User currentUser = authService.getCurrentUser();
            if (currentUser == null) {
                showError("Authentication Error", "User session expired. Please login again.");
                return;
            }

            List<EquipmentAllocation> allocations = equipmentService.getEquipmentByUser(currentUser.getId());
            allocationsList.setAll(allocations);
            allocationsTable.setItems(allocationsList);

            if (allocations.isEmpty()) {
                statusLabel.setText("You have no allocated equipment.");
                statusLabel.setVisible(true);
                statusLabel.setStyle("-fx-text-fill: #757575; -fx-font-size: 12px;");
            } else {
                statusLabel.setVisible(false);
            }

            System.out.println("Loaded " + allocations.size() + " equipment allocations for user: " + currentUser.getUsername());
        } catch (SQLException e) {
            showError("Database Error", "Failed to load allocations: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            showError("Error", "An unexpected error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    void handleRefresh(ActionEvent event) {
        loadAllocations();
        statusLabel.setText("Equipment list refreshed.");
        statusLabel.setVisible(true);
        statusLabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-size: 12px;");
    }

    @FXML
    void handleReturnEquipment(ActionEvent event) {
        EquipmentAllocation selectedAllocation = allocationsTable.getSelectionModel().getSelectedItem();

        if (selectedAllocation == null) {
            showWarning("No Selection", "Please select equipment to return.");
            return;
        }

        if (selectedAllocation.getStatus() != edu.facilities.model.AllocationStatus.ACTIVE) {
            showWarning("Cannot Return", "This equipment has already been returned.");
            return;
        }

        // Show confirmation dialog
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Return Equipment");
        confirmDialog.setHeaderText("Are you sure you want to return this equipment?");
        confirmDialog.setContentText("Allocation ID: #" + selectedAllocation.getId() + "\n" +
                                    "Equipment: " + selectedAllocation.getEquipment().getEquipmentTypeName() + "\n" +
                                    "Serial Number: " + (selectedAllocation.getEquipment().getSerialNumber() != null ? 
                                        selectedAllocation.getEquipment().getSerialNumber() : "N/A"));

        confirmDialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    User currentUser = authService.getCurrentUser();
                    boolean returned = equipmentService.returnEquipment(
                        selectedAllocation.getId(),
                        currentUser.getId()
                    );
                    
                    if (returned) {
                        // Reload allocations
                        loadAllocations();
                        
                        statusLabel.setText("Equipment returned successfully.");
                        statusLabel.setVisible(true);
                        statusLabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-size: 12px;");

                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Equipment Returned");
                        alert.setHeaderText(null);
                        alert.setContentText("Equipment has been returned and is now available for allocation.");
                        alert.showAndWait();
                    } else {
                        showError("Return Error", "Failed to return equipment. Please try again.");
                    }
                } catch (IllegalArgumentException e) {
                    showError("Validation Error", e.getMessage());
                } catch (SQLException e) {
                    showError("Database Error", "Failed to return equipment: " + e.getMessage());
                    e.printStackTrace();
                } catch (Exception e) {
                    showError("Error", "An unexpected error occurred: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });
    }

    @FXML
    void handleBack(ActionEvent event) {
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

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void disableAllControls() {
        if (allocationsTable != null) allocationsTable.setDisable(true);
        if (refreshButton != null) refreshButton.setDisable(true);
        if (returnButton != null) returnButton.setDisable(true);
    }
}

