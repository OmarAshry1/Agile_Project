package edu.facilities.ui;

import edu.facilities.model.SoftwareLicense;
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

public class SoftwareLicensesController {

    @FXML
    private TableView<SoftwareLicense> licensesTable;

    @FXML
    private TableColumn<SoftwareLicense, String> licenseIdColumn;

    @FXML
    private TableColumn<SoftwareLicense, String> softwareNameColumn;

    @FXML
    private TableColumn<SoftwareLicense, String> vendorColumn;

    @FXML
    private TableColumn<SoftwareLicense, String> quantityColumn;

    @FXML
    private TableColumn<SoftwareLicense, String> usedQuantityColumn;

    @FXML
    private TableColumn<SoftwareLicense, String> availableQuantityColumn;

    @FXML
    private TableColumn<SoftwareLicense, String> expiryDateColumn;

    @FXML
    private TableColumn<SoftwareLicense, String> costColumn;

    @FXML
    private TableColumn<SoftwareLicense, String> statusColumn;

    @FXML
    private Label totalExpensesLabel;

    @FXML
    private Label nearExpiryCountLabel;

    @FXML
    private Label totalLicensesLabel;

    @FXML
    private Button refreshButton;

    @FXML
    private Button addLicenseButton;

    @FXML
    private Button backButton;

    @FXML
    private Label statusLabel;

    private EquipmentService equipmentService = new EquipmentService();
    private AuthService authService = AuthService.getInstance();
    private ObservableList<SoftwareLicense> licensesList = FXCollections.observableArrayList();

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @FXML
    public void initialize() {
        // Check if user is logged in and is admin
        if (!authService.isLoggedIn()) {
            showError("Access Denied", "Please login to view software licenses.");
            disableAllControls();
            return;
        }

        String userType = authService.getCurrentUserType();
        if (!"ADMIN".equals(userType)) {
            showError("Access Denied", "Only administrators can view software licenses.");
            disableAllControls();
            return;
        }

        // Setup table columns
        setupTableColumns();

        // Load licenses
        loadLicenses();
    }

    /**
     * Setup table columns to display license data
     */
    private void setupTableColumns() {
        licenseIdColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty("#" + cellData.getValue().getId())
        );

        softwareNameColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getSoftwareName())
        );

        vendorColumn.setCellValueFactory(cellData -> {
            String vendor = cellData.getValue().getVendor();
            return new javafx.beans.property.SimpleStringProperty(vendor != null ? vendor : "");
        });

        quantityColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(String.valueOf(cellData.getValue().getQuantity()))
        );

        usedQuantityColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(String.valueOf(cellData.getValue().getUsedQuantity()))
        );

        availableQuantityColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(String.valueOf(cellData.getValue().getAvailableQuantity()))
        );

        expiryDateColumn.setCellValueFactory(cellData -> {
            java.time.LocalDate expiry = cellData.getValue().getExpiryDate();
            String dateStr = expiry != null ? expiry.format(DATE_FORMATTER) : "Perpetual";
            return new javafx.beans.property.SimpleStringProperty(dateStr);
        });

        costColumn.setCellValueFactory(cellData -> {
            Double cost = cellData.getValue().getCost();
            if (cost == null) {
                return new javafx.beans.property.SimpleStringProperty("N/A");
            }
            return new javafx.beans.property.SimpleStringProperty(String.format("$%.2f", cost));
        });

        statusColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getStatus().toString())
        );
    }

    /**
     * Load software licenses and update statistics
     */
    private void loadLicenses() {
        try {
            List<SoftwareLicense> licenses = equipmentService.getAllSoftwareLicenses();
            licensesList.setAll(licenses);
            licensesTable.setItems(licensesList);

            // Update statistics
            updateStatistics(licenses);

            if (licenses.isEmpty()) {
                statusLabel.setText("No software licenses found.");
                statusLabel.setVisible(true);
                statusLabel.setStyle("-fx-text-fill: #757575; -fx-font-size: 12px;");
            } else {
                statusLabel.setVisible(false);
            }

            System.out.println("Loaded " + licenses.size() + " software licenses");
        } catch (SQLException e) {
            showError("Database Error", "Failed to load licenses: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            showError("Error", "An unexpected error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Update statistics labels
     */
    private void updateStatistics(List<SoftwareLicense> licenses) {
        try {
            // Total expenses
            Double totalExpenses = equipmentService.getTotalLicenseExpenses();
            totalExpensesLabel.setText(String.format("$%.2f", totalExpenses));

            // Near expiry count
            List<SoftwareLicense> nearExpiry = equipmentService.getLicensesNearExpiry();
            nearExpiryCountLabel.setText(String.valueOf(nearExpiry.size()));

            // Total licenses
            totalLicensesLabel.setText(String.valueOf(licenses.size()));

            // Highlight near-expiring licenses in table
            licensesTable.setRowFactory(tv -> new TableRow<SoftwareLicense>() {
                @Override
                protected void updateItem(SoftwareLicense license, boolean empty) {
                    super.updateItem(license, empty);
                    if (empty || license == null) {
                        setStyle("");
                    } else {
                        if (license.isNearExpiry() && !license.isExpired()) {
                            setStyle("-fx-background-color: #FFF3E0;"); // Orange background for near expiry
                        } else if (license.isExpired()) {
                            setStyle("-fx-background-color: #FFEBEE;"); // Red background for expired
                        } else {
                            setStyle("");
                        }
                    }
                }
            });
        } catch (SQLException e) {
            System.err.println("Error updating statistics: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    void handleRefresh(ActionEvent event) {
        loadLicenses();
        statusLabel.setText("Licenses refreshed.");
        statusLabel.setVisible(true);
        statusLabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-size: 12px;");
    }

    @FXML
    void handleAddLicense(ActionEvent event) {
        try {
            // Load the add software license form
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/add_software_license.fxml"));
            Parent root = loader.load();

            // Create new stage
            Stage stage = new Stage();
            stage.setTitle("Add Software License");
            stage.setScene(new Scene(root));
            stage.setResizable(false);

            // Set up callback for when window closes
            stage.setOnHidden(e -> {
                // Refresh licenses after adding
                loadLicenses();
            });

            stage.showAndWait();

        } catch (IOException e) {
            showError("Navigation Error", "Unable to open add license form: " + e.getMessage());
            e.printStackTrace();
        }
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
        if (licensesTable != null) licensesTable.setDisable(true);
        if (refreshButton != null) refreshButton.setDisable(true);
    }
}

