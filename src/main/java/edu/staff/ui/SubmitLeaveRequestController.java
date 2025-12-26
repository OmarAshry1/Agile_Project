package edu.staff.ui;

import edu.staff.model.LeaveRequest;
import edu.staff.service.LeaveService;
import edu.facilities.model.User;
import edu.facilities.service.AuthService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;

import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;

/**
 * Controller for submitting leave requests (Staff only)
 * US 3.11 - Submit Leave Request
 */
public class SubmitLeaveRequestController implements Initializable {

    @FXML private ComboBox<String> leaveTypeComboBox;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private TextArea reasonTextArea;
    @FXML private Label numberOfDaysLabel;
    @FXML private Button submitButton;
    @FXML private Button cancelButton;

    private LeaveService leaveService;
    private AuthService authService;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        leaveService = new LeaveService();
        authService = AuthService.getInstance();

        // Check access
        if (!authService.isLoggedIn()) {
            showError("Access Denied", "Please login to submit leave requests.");
            return;
        }

        String userType = authService.getCurrentUserType();
        if (!"STAFF".equals(userType) && !"PROFESSOR".equals(userType)) {
            showError("Access Denied", "Only staff members can submit leave requests.");
            return;
        }

        // Initialize leave type combo box
        ObservableList<String> leaveTypes = FXCollections.observableArrayList(
            "SICK", "VACATION", "PERSONAL", "MATERNITY", "PATERNITY", "BEREAVEMENT", "OTHER"
        );
        leaveTypeComboBox.setItems(leaveTypes);
        leaveTypeComboBox.getSelectionModel().selectFirst();

        // Set minimum date to today
        startDatePicker.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isBefore(LocalDate.now()));
            }
        });

        endDatePicker.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                LocalDate startDate = startDatePicker.getValue();
                if (startDate != null) {
                    setDisable(empty || date.isBefore(startDate));
                } else {
                    setDisable(empty || date.isBefore(LocalDate.now()));
                }
            }
        });

        // Update number of days when dates change
        startDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> updateNumberOfDays());
        endDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> updateNumberOfDays());
    }

    private void updateNumberOfDays() {
        LocalDate start = startDatePicker.getValue();
        LocalDate end = endDatePicker.getValue();
        
        if (start != null && end != null && !end.isBefore(start)) {
            long days = java.time.temporal.ChronoUnit.DAYS.between(start, end) + 1;
            numberOfDaysLabel.setText("Number of Days: " + days);
        } else {
            numberOfDaysLabel.setText("Number of Days: -");
        }
    }

    @FXML
    private void handleSubmit() {
        try {
            // Validate inputs
            if (leaveTypeComboBox.getValue() == null) {
                showError("Validation Error", "Please select a leave type.");
                return;
            }

            LocalDate startDate = startDatePicker.getValue();
            LocalDate endDate = endDatePicker.getValue();

            if (startDate == null || endDate == null) {
                showError("Validation Error", "Please select both start and end dates.");
                return;
            }

            if (endDate.isBefore(startDate)) {
                showError("Validation Error", "End date must be after or equal to start date.");
                return;
            }

            String reason = reasonTextArea.getText();
            if (reason == null || reason.trim().isEmpty()) {
                showError("Validation Error", "Please provide a reason for the leave request.");
                return;
            }

            // Create leave request
            User currentUser = authService.getCurrentUser();
            LeaveRequest leaveRequest = new LeaveRequest();
            leaveRequest.setStaffUserID(Integer.parseInt(currentUser.getId()));
            leaveRequest.setLeaveType(leaveTypeComboBox.getValue());
            leaveRequest.setStartDate(startDate);
            leaveRequest.setEndDate(endDate);
            leaveRequest.setReason(reason.trim());

            // Submit leave request
            leaveService.submitLeaveRequest(leaveRequest);

            showInfo("Success", "Leave request submitted successfully. Status: PENDING");
            
            // Clear form
            clearForm();

        } catch (Exception e) {
            showError("Error", "Failed to submit leave request: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleCancel() {
        clearForm();
    }

    private void clearForm() {
        leaveTypeComboBox.getSelectionModel().selectFirst();
        startDatePicker.setValue(null);
        endDatePicker.setValue(null);
        reasonTextArea.clear();
        numberOfDaysLabel.setText("Number of Days: -");
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

