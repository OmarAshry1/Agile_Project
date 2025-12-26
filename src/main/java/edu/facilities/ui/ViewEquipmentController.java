package edu.facilities.ui;

import edu.facilities.model.Equipment;
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
import java.util.List;

public class ViewEquipmentController {

    @FXML
    private TextField equipmentTypeField;

    @FXML
    private TableView<Equipment> equipmentTable;

    @FXML
    private TableColumn<Equipment, String> equipmentIdColumn;

    @FXML
    private TableColumn<Equipment, String> equipmentTypeColumn;

    @FXML
    private TableColumn<Equipment, String> serialNumberColumn;

    @FXML
    private TableColumn<Equipment, String> statusColumn;

    @FXML
    private TableColumn<Equipment, String> locationColumn;

    @FXML
    private TableColumn<Equipment, String> notesColumn;

    @FXML
    private Button searchButton;

    @FXML
    private Button refreshButton;

    @FXML
    private Button backButton;

    @FXML
    private Label statusLabel;

    private EquipmentService equipmentService = new EquipmentService();
    private AuthService authService = AuthService.getInstance();
    private ObservableList<Equipment> equipmentList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Check if user is logged in
        if (!authService.isLoggedIn()) {
            showError("Access Denied", "Please login to view equipment.");
            disableAllControls();
            return;
        }

        // Check user type - only PROFESSOR and STAFF can view equipment
        String userType = authService.getCurrentUserType();
        if (!"PROFESSOR".equals(userType) && !"STAFF".equals(userType)) {
            showError("Access Denied", "Only professors and staff can view equipment.");
            disableAllControls();
            return;
        }

        // Setup table columns
        setupTableColumns();

        // Load initial equipment (empty search shows all available)
        loadEquipment(null);
    }

    /**
     * Setup table columns to display equipment data
     */
    private void setupTableColumns() {
        equipmentIdColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty("#" + cellData.getValue().getId())
        );

        equipmentTypeColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getEquipmentTypeName())
        );

        serialNumberColumn.setCellValueFactory(cellData -> {
            String serial = cellData.getValue().getSerialNumber();
            return new javafx.beans.property.SimpleStringProperty(serial != null ? serial : "");
        });

        statusColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getStatus().toString())
        );

        locationColumn.setCellValueFactory(cellData -> {
            String location = cellData.getValue().getLocation();
            return new javafx.beans.property.SimpleStringProperty(location != null ? location : "");
        });

        notesColumn.setCellValueFactory(cellData -> {
            String notes = cellData.getValue().getNotes();
            return new javafx.beans.property.SimpleStringProperty(notes != null ? notes : "");
        });
    }

    /**
     * Load equipment based on search criteria
     */
    private void loadEquipment(String equipmentTypeName) {
        try {
            User currentUser = authService.getCurrentUser();
            if (currentUser == null) {
                showError("Authentication Error", "User session expired. Please login again.");
                return;
            }

            // Get user's department
            String department = getUserDepartment(currentUser);
            
            List<Equipment> equipment = equipmentService.getAvailableEquipment(equipmentTypeName, department);
            equipmentList.setAll(equipment);
            equipmentTable.setItems(equipmentList);

            if (equipment.isEmpty()) {
                statusLabel.setText("No available equipment found matching your search.");
                statusLabel.setVisible(true);
                statusLabel.setStyle("-fx-text-fill: #757575; -fx-font-size: 12px;");
            } else {
                statusLabel.setVisible(false);
            }

            System.out.println("Loaded " + equipment.size() + " equipment items for user: " + currentUser.getUsername());
        } catch (SQLException e) {
            showError("Database Error", "Failed to load equipment: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            showError("Error", "An unexpected error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Get user's department from database
     */
    private String getUserDepartment(User user) {
        try {
            String userType = user.getUserType();
            int userId = Integer.parseInt(user.getId());
            
            if ("PROFESSOR".equals(userType)) {
                String sql = "SELECT Department FROM Professors WHERE UserID = ?";
                try (java.sql.Connection conn = edu.facilities.service.DatabaseConnection.getConnection();
                     java.sql.PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setInt(1, userId);
                    try (java.sql.ResultSet rs = pstmt.executeQuery()) {
                        if (rs.next()) {
                            return rs.getString("Department");
                        }
                    }
                }
            } else if ("STAFF".equals(userType)) {
                String sql = "SELECT Department FROM Staff WHERE UserID = ?";
                try (java.sql.Connection conn = edu.facilities.service.DatabaseConnection.getConnection();
                     java.sql.PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setInt(1, userId);
                    try (java.sql.ResultSet rs = pstmt.executeQuery()) {
                        if (rs.next()) {
                            return rs.getString("Department");
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error getting user department: " + e.getMessage());
        }
        return null; // Return null to show all available equipment if department not found
    }

    @FXML
    void handleSearch(ActionEvent event) {
        String equipmentTypeName = equipmentTypeField.getText();
        if (equipmentTypeName != null && equipmentTypeName.isBlank()) {
            equipmentTypeName = null;
        }
        loadEquipment(equipmentTypeName);
    }

    @FXML
    void handleRefresh(ActionEvent event) {
        String equipmentTypeName = equipmentTypeField.getText();
        if (equipmentTypeName != null && equipmentTypeName.isBlank()) {
            equipmentTypeName = null;
        }
        loadEquipment(equipmentTypeName);
        statusLabel.setText("Equipment list refreshed.");
        statusLabel.setVisible(true);
        statusLabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-size: 12px;");
    }

    @FXML
    void handleBack(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/dashboard.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Dashboard");
            stage.setMaximized(true);
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

    private void disableAllControls() {
        if (equipmentTable != null) equipmentTable.setDisable(true);
        if (equipmentTypeField != null) equipmentTypeField.setDisable(true);
        if (searchButton != null) searchButton.setDisable(true);
        if (refreshButton != null) refreshButton.setDisable(true);
    }
}

