package edu.staff.ui;

import edu.staff.model.BenefitsInformation;
import edu.staff.service.BenefitsService;
import edu.facilities.model.User;
import edu.facilities.service.AuthService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.cell.PropertyValueFactory;

import java.math.BigDecimal;
import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

/**
 * Controller for managing benefits information (HR Admin only)
 * US 3.15 - Add or Update Benefits Information
 */
public class ManageBenefitsController implements Initializable {

    @FXML private ComboBox<User> staffComboBox;
    @FXML private TableView<BenefitsInformation> benefitsTable;
    @FXML private TableColumn<BenefitsInformation, String> typeColumn;
    @FXML private TableColumn<BenefitsInformation, String> nameColumn;
    @FXML private TableColumn<BenefitsInformation, String> statusColumn;
    @FXML private TableColumn<BenefitsInformation, String> startDateColumn;
    
    @FXML private ComboBox<String> benefitTypeComboBox;
    @FXML private TextField benefitNameField;
    @FXML private TextField coverageAmountField;
    @FXML private TextArea coverageDetailsTextArea;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private ComboBox<String> statusComboBox;
    @FXML private TextField providerField;
    @FXML private TextField policyNumberField;
    @FXML private TextArea notesTextArea;
    
    @FXML private Button addButton;
    @FXML private Button updateButton;
    @FXML private Button deleteButton;
    @FXML private Button refreshButton;
    @FXML private Button clearButton;

    private BenefitsService benefitsService;
    private AuthService authService;
    private ObservableList<BenefitsInformation> benefitsList;
    private ObservableList<User> staffList;
    private BenefitsInformation currentBenefit;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        benefitsService = new BenefitsService();
        authService = AuthService.getInstance();
        benefitsList = FXCollections.observableArrayList();
        staffList = FXCollections.observableArrayList();

        // Check access
        if (!authService.isLoggedIn()) {
            showError("Access Denied", "Please login to manage benefits information.");
            return;
        }

        String userType = authService.getCurrentUserType();
        if (!"ADMIN".equals(userType)) {
            showError("Access Denied", "Only HR administrators can manage benefits information.");
            return;
        }

        // Initialize benefit type combo box
        ObservableList<String> benefitTypes = FXCollections.observableArrayList(
            "HEALTH_INSURANCE", "DENTAL_INSURANCE", "VISION_INSURANCE", 
            "LIFE_INSURANCE", "RETIREMENT", "VACATION_DAYS", "SICK_DAYS", "OTHER"
        );
        benefitTypeComboBox.setItems(benefitTypes);
        benefitTypeComboBox.getSelectionModel().selectFirst();

        // Initialize status combo box
        ObservableList<String> statuses = FXCollections.observableArrayList(
            "ACTIVE", "INACTIVE", "EXPIRED", "CANCELLED"
        );
        statusComboBox.setItems(statuses);
        statusComboBox.getSelectionModel().select("ACTIVE");

        // Initialize staff combo box
        staffComboBox.setCellFactory(param -> new ListCell<User>() {
            @Override
            protected void updateItem(User item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getUsername() + " (ID: " + item.getId() + ")");
                }
            }
        });
        staffComboBox.setButtonCell(new ListCell<User>() {
            @Override
            protected void updateItem(User item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getUsername() + " (ID: " + item.getId() + ")");
                }
            }
        });
        staffComboBox.setOnAction(e -> loadBenefitsForStaff());

        // Initialize table columns
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("benefitType"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("benefitName"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));

        startDateColumn.setCellFactory(column -> new TableCell<BenefitsInformation, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                } else {
                    BenefitsInformation benefit = getTableRow().getItem();
                    if (benefit.getStartDate() != null) {
                        setText(benefit.getStartDate().toString());
                    } else {
                        setText("");
                    }
                }
            }
        });

        benefitsTable.setItems(benefitsList);
        benefitsTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldSelection, newSelection) -> {
                if (newSelection != null) {
                    populateForm(newSelection);
                    currentBenefit = newSelection;
                    updateButton.setDisable(false);
                    deleteButton.setDisable(false);
                }
            }
        );

        loadStaffList();
        clearForm();
    }

    private void loadStaffList() {
        try {
            staffList.clear();
            staffList.addAll(benefitsService.getAllStaff());
            staffComboBox.setItems(staffList);
        } catch (SQLException e) {
            showError("Database Error", "Failed to load staff list: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadBenefitsForStaff() {
        User selectedStaff = staffComboBox.getValue();
        if (selectedStaff == null) {
            benefitsList.clear();
            return;
        }

        try {
            int staffUserID = Integer.parseInt(selectedStaff.getId());
            benefitsList.clear();
            benefitsList.addAll(benefitsService.getBenefitsByStaff(staffUserID));
        } catch (SQLException e) {
            showError("Database Error", "Failed to load benefits: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void populateForm(BenefitsInformation benefit) {
        if (benefit == null) {
            clearForm();
            return;
        }

        benefitTypeComboBox.setValue(benefit.getBenefitType());
        benefitNameField.setText(benefit.getBenefitName());
        if (benefit.getCoverageAmount() != null) {
            coverageAmountField.setText(benefit.getCoverageAmount().toString());
        } else {
            coverageAmountField.clear();
        }
        coverageDetailsTextArea.setText(benefit.getCoverageDetails() != null ? benefit.getCoverageDetails() : "");
        startDatePicker.setValue(benefit.getStartDate());
        endDatePicker.setValue(benefit.getEndDate());
        statusComboBox.setValue(benefit.getStatus());
        providerField.setText(benefit.getProvider() != null ? benefit.getProvider() : "");
        policyNumberField.setText(benefit.getPolicyNumber() != null ? benefit.getPolicyNumber() : "");
        notesTextArea.setText(benefit.getNotes() != null ? benefit.getNotes() : "");
    }

    @FXML
    private void handleAdd() {
        try {
            // Validate inputs
            if (staffComboBox.getValue() == null) {
                showError("Validation Error", "Please select a staff member.");
                return;
            }

            if (benefitTypeComboBox.getValue() == null) {
                showError("Validation Error", "Please select a benefit type.");
                return;
            }

            if (benefitNameField.getText() == null || benefitNameField.getText().trim().isEmpty()) {
                showError("Validation Error", "Benefit Name is required.");
                return;
            }

            if (startDatePicker.getValue() == null) {
                showError("Validation Error", "Start Date is required.");
                return;
            }

            // Create benefit object
            BenefitsInformation benefit = new BenefitsInformation();
            benefit.setStaffUserID(Integer.parseInt(staffComboBox.getValue().getId()));
            benefit.setBenefitType(benefitTypeComboBox.getValue());
            benefit.setBenefitName(benefitNameField.getText().trim());
            
            String coverageAmountText = coverageAmountField.getText();
            if (coverageAmountText != null && !coverageAmountText.trim().isEmpty()) {
                try {
                    benefit.setCoverageAmount(new BigDecimal(coverageAmountText.trim()));
                } catch (NumberFormatException e) {
                    benefit.setCoverageAmount(null);
                }
            }
            
            benefit.setCoverageDetails(coverageDetailsTextArea.getText());
            benefit.setStartDate(startDatePicker.getValue());
            benefit.setEndDate(endDatePicker.getValue());
            benefit.setStatus(statusComboBox.getValue());
            benefit.setProvider(providerField.getText().trim());
            benefit.setPolicyNumber(policyNumberField.getText().trim());
            benefit.setNotes(notesTextArea.getText());

            // Add benefit
            User currentUser = authService.getCurrentUser();
            benefitsService.addBenefit(benefit, Integer.parseInt(currentUser.getId()));

            showInfo("Success", "Benefit added successfully.");
            loadBenefitsForStaff();
            clearForm();

        } catch (Exception e) {
            showError("Error", "Failed to add benefit: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleUpdate() {
        if (currentBenefit == null) {
            showError("Selection Error", "Please select a benefit to update.");
            return;
        }

        try {
            // Validate inputs
            if (benefitTypeComboBox.getValue() == null) {
                showError("Validation Error", "Please select a benefit type.");
                return;
            }

            if (benefitNameField.getText() == null || benefitNameField.getText().trim().isEmpty()) {
                showError("Validation Error", "Benefit Name is required.");
                return;
            }

            if (startDatePicker.getValue() == null) {
                showError("Validation Error", "Start Date is required.");
                return;
            }

            // Update benefit object
            currentBenefit.setBenefitType(benefitTypeComboBox.getValue());
            currentBenefit.setBenefitName(benefitNameField.getText().trim());
            
            String coverageAmountText = coverageAmountField.getText();
            if (coverageAmountText != null && !coverageAmountText.trim().isEmpty()) {
                try {
                    currentBenefit.setCoverageAmount(new BigDecimal(coverageAmountText.trim()));
                } catch (NumberFormatException e) {
                    currentBenefit.setCoverageAmount(null);
                }
            } else {
                currentBenefit.setCoverageAmount(null);
            }
            
            currentBenefit.setCoverageDetails(coverageDetailsTextArea.getText());
            currentBenefit.setStartDate(startDatePicker.getValue());
            currentBenefit.setEndDate(endDatePicker.getValue());
            currentBenefit.setStatus(statusComboBox.getValue());
            currentBenefit.setProvider(providerField.getText().trim());
            currentBenefit.setPolicyNumber(policyNumberField.getText().trim());
            currentBenefit.setNotes(notesTextArea.getText());

            // Update benefit
            User currentUser = authService.getCurrentUser();
            benefitsService.updateBenefit(currentBenefit, Integer.parseInt(currentUser.getId()));

            showInfo("Success", "Benefit updated successfully.");
            loadBenefitsForStaff();
            clearForm();

        } catch (Exception e) {
            showError("Error", "Failed to update benefit: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleDelete() {
        if (currentBenefit == null) {
            showError("Selection Error", "Please select a benefit to delete.");
            return;
        }

        Alert confirmDialog = new Alert(AlertType.CONFIRMATION);
        confirmDialog.setTitle("Delete Benefit");
        confirmDialog.setHeaderText("Delete Benefit?");
        confirmDialog.setContentText("Are you sure you want to delete this benefit? This action cannot be undone.");
        
        confirmDialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    benefitsService.deleteBenefit(currentBenefit.getBenefitID());
                    showInfo("Success", "Benefit deleted successfully.");
                    loadBenefitsForStaff();
                    clearForm();
                } catch (Exception e) {
                    showError("Error", "Failed to delete benefit: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });
    }

    @FXML
    private void handleClear() {
        clearForm();
    }

    @FXML
    private void handleRefresh() {
        loadBenefitsForStaff();
    }

    private void clearForm() {
        benefitTypeComboBox.getSelectionModel().selectFirst();
        benefitNameField.clear();
        coverageAmountField.clear();
        coverageDetailsTextArea.clear();
        startDatePicker.setValue(null);
        endDatePicker.setValue(null);
        statusComboBox.getSelectionModel().select("ACTIVE");
        providerField.clear();
        policyNumberField.clear();
        notesTextArea.clear();
        currentBenefit = null;
        updateButton.setDisable(true);
        deleteButton.setDisable(true);
        benefitsTable.getSelectionModel().clearSelection();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

