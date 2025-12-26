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

import java.net.URL;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.ResourceBundle;

/**
 * Controller for viewing benefits information (Staff only)
 * US 3.15 - View Benefits Information
 */
public class ViewBenefitsController implements Initializable {

    @FXML private TableView<BenefitsInformation> benefitsTable;
    @FXML private TableColumn<BenefitsInformation, String> typeColumn;
    @FXML private TableColumn<BenefitsInformation, String> nameColumn;
    @FXML private TableColumn<BenefitsInformation, String> coverageColumn;
    @FXML private TableColumn<BenefitsInformation, String> startDateColumn;
    @FXML private TableColumn<BenefitsInformation, String> endDateColumn;
    @FXML private TableColumn<BenefitsInformation, String> statusColumn;
    @FXML private TableColumn<BenefitsInformation, String> providerColumn;
    
    @FXML private Label benefitTypeLabel;
    @FXML private Label benefitNameLabel;
    @FXML private Label coverageAmountLabel;
    @FXML private Label providerLabel;
    @FXML private Label policyNumberLabel;
    @FXML private Label statusLabel;
    @FXML private Label startDateLabel;
    @FXML private Label endDateLabel;
    @FXML private TextArea coverageDetailsTextArea;
    @FXML private TextArea notesTextArea;
    
    @FXML private Button refreshButton;
    @FXML private ComboBox<String> typeFilterComboBox;
    @FXML private ComboBox<String> statusFilterComboBox;

    private BenefitsService benefitsService;
    private AuthService authService;
    private ObservableList<BenefitsInformation> benefitsList;
    private ObservableList<BenefitsInformation> allBenefitsList;
    private NumberFormat currencyFormat;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        benefitsService = new BenefitsService();
        authService = AuthService.getInstance();
        benefitsList = FXCollections.observableArrayList();
        allBenefitsList = FXCollections.observableArrayList();
        currencyFormat = NumberFormat.getCurrencyInstance();

        // Check access
        if (!authService.isLoggedIn()) {
            showError("Access Denied", "Please login to view benefits information.");
            return;
        }

        String userType = authService.getCurrentUserType();
        if (!"STAFF".equals(userType) && !"PROFESSOR".equals(userType)) {
            showError("Access Denied", "Only staff members can view benefits information.");
            return;
        }

        // Initialize table columns
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("benefitType"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("benefitName"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        providerColumn.setCellValueFactory(new PropertyValueFactory<>("provider"));

        coverageColumn.setCellFactory(column -> new TableCell<BenefitsInformation, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                } else {
                    BenefitsInformation benefit = getTableRow().getItem();
                    if (benefit.getCoverageAmount() != null) {
                        setText(currencyFormat.format(benefit.getCoverageAmount()));
                    } else {
                        setText("N/A");
                    }
                }
            }
        });

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

        endDateColumn.setCellFactory(column -> new TableCell<BenefitsInformation, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                } else {
                    BenefitsInformation benefit = getTableRow().getItem();
                    if (benefit.getEndDate() != null) {
                        setText(benefit.getEndDate().toString());
                    } else {
                        setText("Ongoing");
                    }
                }
            }
        });

        // Initialize filters
        ObservableList<String> typeOptions = FXCollections.observableArrayList(
            "ALL", "HEALTH_INSURANCE", "DENTAL_INSURANCE", "VISION_INSURANCE", 
            "LIFE_INSURANCE", "RETIREMENT", "VACATION_DAYS", "SICK_DAYS", "OTHER"
        );
        typeFilterComboBox.setItems(typeOptions);
        typeFilterComboBox.getSelectionModel().select("ALL");
        typeFilterComboBox.setOnAction(e -> applyFilters());

        ObservableList<String> statusOptions = FXCollections.observableArrayList(
            "ALL", "ACTIVE", "INACTIVE", "EXPIRED", "CANCELLED"
        );
        statusFilterComboBox.setItems(statusOptions);
        statusFilterComboBox.getSelectionModel().select("ALL");
        statusFilterComboBox.setOnAction(e -> applyFilters());

        benefitsTable.setItems(benefitsList);
        benefitsTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldSelection, newSelection) -> {
                if (newSelection != null) {
                    displayBenefitDetails(newSelection);
                }
            }
        );

        loadBenefits();
    }

    private void loadBenefits() {
        try {
            User currentUser = authService.getCurrentUser();
            int staffUserID = Integer.parseInt(currentUser.getId());
            
            allBenefitsList.clear();
            allBenefitsList.addAll(benefitsService.getBenefitsByStaff(staffUserID));
            
            applyFilters();
        } catch (SQLException e) {
            showError("Database Error", "Failed to load benefits information: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void applyFilters() {
        String typeFilter = typeFilterComboBox.getValue();
        String statusFilter = statusFilterComboBox.getValue();

        benefitsList.clear();
        
        for (BenefitsInformation benefit : allBenefitsList) {
            boolean typeMatch = "ALL".equals(typeFilter) || typeFilter.equals(benefit.getBenefitType());
            boolean statusMatch = "ALL".equals(statusFilter) || statusFilter.equals(benefit.getStatus());
            
            if (typeMatch && statusMatch) {
                benefitsList.add(benefit);
            }
        }
    }

    private void displayBenefitDetails(BenefitsInformation benefit) {
        if (benefit == null) {
            clearDetails();
            return;
        }

        benefitTypeLabel.setText("Type: " + benefit.getBenefitType());
        benefitNameLabel.setText("Name: " + benefit.getBenefitName());
        
        if (benefit.getCoverageAmount() != null) {
            coverageAmountLabel.setText("Coverage Amount: " + currencyFormat.format(benefit.getCoverageAmount()));
        } else {
            coverageAmountLabel.setText("Coverage Amount: N/A");
        }
        
        providerLabel.setText("Provider: " + (benefit.getProvider() != null ? benefit.getProvider() : "N/A"));
        policyNumberLabel.setText("Policy Number: " + (benefit.getPolicyNumber() != null ? benefit.getPolicyNumber() : "N/A"));
        statusLabel.setText("Status: " + benefit.getStatus());
        startDateLabel.setText("Start Date: " + (benefit.getStartDate() != null ? benefit.getStartDate().toString() : "N/A"));
        endDateLabel.setText("End Date: " + (benefit.getEndDate() != null ? benefit.getEndDate().toString() : "Ongoing"));
        coverageDetailsTextArea.setText(benefit.getCoverageDetails() != null ? benefit.getCoverageDetails() : "");
        notesTextArea.setText(benefit.getNotes() != null ? benefit.getNotes() : "");
    }

    private void clearDetails() {
        benefitTypeLabel.setText("Type: -");
        benefitNameLabel.setText("Name: -");
        coverageAmountLabel.setText("Coverage Amount: -");
        providerLabel.setText("Provider: -");
        policyNumberLabel.setText("Policy Number: -");
        statusLabel.setText("Status: -");
        startDateLabel.setText("Start Date: -");
        endDateLabel.setText("End Date: -");
        coverageDetailsTextArea.clear();
        notesTextArea.clear();
    }

    @FXML
    private void handleRefresh() {
        loadBenefits();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

