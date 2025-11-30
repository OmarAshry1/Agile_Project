package edu.facilities.ui;

import edu.facilities.model.SoftwareLicense;
import edu.facilities.service.AuthService;
import edu.facilities.service.EquipmentService;
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
import java.time.LocalDate;

public class AddSoftwareLicenseController {

    @FXML
    private TextField softwareNameField;

    @FXML
    private TextField vendorField;

    @FXML
    private TextField licenseKeyField;

    @FXML
    private DatePicker purchaseDatePicker;

    @FXML
    private DatePicker expiryDatePicker;

    @FXML
    private TextField costField;

    @FXML
    private Spinner<Integer> quantitySpinner;

    @FXML
    private TextArea notesTextArea;

    @FXML
    private Button addButton;

    @FXML
    private Button cancelButton;

    @FXML
    private Label softwareNameError;

    @FXML
    private Label quantityError;

    @FXML
    private Label statusLabel;

    private EquipmentService equipmentService = new EquipmentService();
    private AuthService authService = AuthService.getInstance();

    @FXML
    public void initialize() {
        // Check if user is logged in and is admin
        if (!authService.isLoggedIn()) {
            showError("Access Denied", "Please login to add software licenses.");
            disableAllControls();
            return;
        }

        String userType = authService.getCurrentUserType();
        if (!"ADMIN".equals(userType)) {
            showError("Access Denied", "Only administrators can add software licenses.");
            disableAllControls();
            return;
        }

        // Initialize quantity spinner
        SpinnerValueFactory<Integer> quantityFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10000, 1);
        quantitySpinner.setValueFactory(quantityFactory);
    }

    @FXML
    void handleAdd(ActionEvent event) {
        clearErrors();
        statusLabel.setVisible(false);

        // Validate inputs
        boolean hasErrors = false;

        if (softwareNameField.getText() == null || softwareNameField.getText().isBlank()) {
            showFieldError(softwareNameError, "Software name is required");
            hasErrors = true;
        }

        if (hasErrors) {
            return;
        }

        String softwareName = softwareNameField.getText().trim();
        String vendor = vendorField.getText() != null && !vendorField.getText().isBlank() ? vendorField.getText().trim() : null;
        String licenseKey = licenseKeyField.getText() != null && !licenseKeyField.getText().isBlank() ? licenseKeyField.getText().trim() : null;
        LocalDate purchaseDate = purchaseDatePicker.getValue();
        LocalDate expiryDate = expiryDatePicker.getValue();
        
        Double cost = null;
        String costText = costField.getText();
        if (costText != null && !costText.isBlank()) {
            try {
                cost = Double.parseDouble(costText.trim());
                if (cost < 0) {
                    showError("Validation Error", "Cost cannot be negative");
                    return;
                }
            } catch (NumberFormatException e) {
                showError("Validation Error", "Invalid cost format. Please enter a valid number.");
                return;
            }
        }
        
        int quantity = quantitySpinner.getValue();
        String notes = notesTextArea.getText() != null && !notesTextArea.getText().isBlank() ? notesTextArea.getText().trim() : null;

        try {
            SoftwareLicense license = equipmentService.addSoftwareLicense(
                softwareName,
                licenseKey,
                vendor,
                purchaseDate,
                expiryDate,
                cost,
                quantity,
                notes
            );

            if (license != null) {
                statusLabel.setText("Software license added successfully! License ID: #" + license.getId());
                statusLabel.setVisible(true);
                statusLabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-size: 12px;");

                // Clear form
                softwareNameField.clear();
                vendorField.clear();
                licenseKeyField.clear();
                purchaseDatePicker.setValue(null);
                expiryDatePicker.setValue(null);
                costField.clear();
                quantitySpinner.getValueFactory().setValue(1);
                notesTextArea.clear();

                // Show success alert
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("License Added");
                alert.setHeaderText("Software license added successfully!");
                alert.setContentText("License ID: #" + license.getId() + "\n" +
                                    "Software: " + license.getSoftwareName() + "\n" +
                                    "Quantity: " + license.getQuantity() + "\n" +
                                    "Status: " + license.getStatus());
                alert.showAndWait();
            } else {
                showError("Add Error", "Failed to add software license. Please try again.");
            }
        } catch (IllegalArgumentException e) {
            showError("Validation Error", e.getMessage());
        } catch (SQLException e) {
            showError("Database Error", "Failed to add software license: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            showError("Error", "An unexpected error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    void handleCancel(ActionEvent event) {
        navigateToSoftwareLicenses(event);
    }

    private void navigateToSoftwareLicenses(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/software_licenses.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Software Licenses");
            stage.show();
        } catch (IOException e) {
            showError("Navigation Error", "Unable to return to software licenses: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void clearErrors() {
        showFieldError(softwareNameError, null);
        showFieldError(quantityError, null);
    }

    private void showFieldError(Label label, String message) {
        if (label == null) {
            return;
        }
        if (message == null || message.isBlank()) {
            label.setText("");
            label.setVisible(false);
        } else {
            label.setText(message);
            label.setVisible(true);
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
        if (softwareNameField != null) softwareNameField.setDisable(true);
        if (vendorField != null) vendorField.setDisable(true);
        if (licenseKeyField != null) licenseKeyField.setDisable(true);
        if (purchaseDatePicker != null) purchaseDatePicker.setDisable(true);
        if (expiryDatePicker != null) expiryDatePicker.setDisable(true);
        if (costField != null) costField.setDisable(true);
        if (quantitySpinner != null) quantitySpinner.setDisable(true);
        if (notesTextArea != null) notesTextArea.setDisable(true);
        if (addButton != null) addButton.setDisable(true);
    }
}

