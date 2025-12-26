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
import javafx.scene.control.cell.PropertyValueFactory;

import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

/**
 * Controller for viewing leave history (Staff only)
 * US 3.13 - View Leave History
 */
public class ViewLeaveHistoryController implements Initializable {

    @FXML private TableView<LeaveRequest> leaveHistoryTable;
    @FXML private TableColumn<LeaveRequest, Integer> idColumn;
    @FXML private TableColumn<LeaveRequest, String> typeColumn;
    @FXML private TableColumn<LeaveRequest, String> startDateColumn;
    @FXML private TableColumn<LeaveRequest, String> endDateColumn;
    @FXML private TableColumn<LeaveRequest, Integer> daysColumn;
    @FXML private TableColumn<LeaveRequest, String> statusColumn;
    @FXML private TableColumn<LeaveRequest, String> submittedDateColumn;
    @FXML private TableColumn<LeaveRequest, String> reviewedDateColumn;
    
    @FXML private TextArea reasonTextArea;
    @FXML private TextArea rejectionReasonTextArea;
    @FXML private Label statusLabel;
    @FXML private Button refreshButton;
    @FXML private Button cancelButton;
    @FXML private ComboBox<String> statusFilterComboBox;

    private LeaveService leaveService;
    private AuthService authService;
    private ObservableList<LeaveRequest> leaveHistoryList;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        leaveService = new LeaveService();
        authService = AuthService.getInstance();
        leaveHistoryList = FXCollections.observableArrayList();

        // Check access
        if (!authService.isLoggedIn()) {
            showError("Access Denied", "Please login to view leave history.");
            return;
        }

        String userType = authService.getCurrentUserType();
        if (!"STAFF".equals(userType) && !"PROFESSOR".equals(userType)) {
            showError("Access Denied", "Only staff members can view their leave history.");
            return;
        }

        // Initialize table columns
        idColumn.setCellValueFactory(new PropertyValueFactory<>("leaveRequestID"));
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("leaveType"));
        daysColumn.setCellValueFactory(new PropertyValueFactory<>("numberOfDays"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        
        // Custom cell factories for dates
        startDateColumn.setCellFactory(column -> new TableCell<LeaveRequest, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                } else {
                    LeaveRequest request = getTableRow().getItem();
                    if (request.getStartDate() != null) {
                        setText(request.getStartDate().toString());
                    } else {
                        setText("");
                    }
                }
            }
        });

        endDateColumn.setCellFactory(column -> new TableCell<LeaveRequest, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                } else {
                    LeaveRequest request = getTableRow().getItem();
                    if (request.getEndDate() != null) {
                        setText(request.getEndDate().toString());
                    } else {
                        setText("");
                    }
                }
            }
        });

        submittedDateColumn.setCellFactory(column -> new TableCell<LeaveRequest, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                } else {
                    LeaveRequest request = getTableRow().getItem();
                    if (request.getSubmittedDate() != null) {
                        setText(request.getSubmittedDate().toString());
                    } else {
                        setText("");
                    }
                }
            }
        });

        reviewedDateColumn.setCellFactory(column -> new TableCell<LeaveRequest, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                } else {
                    LeaveRequest request = getTableRow().getItem();
                    if (request.getReviewedDate() != null) {
                        setText(request.getReviewedDate().toString());
                    } else {
                        setText("N/A");
                    }
                }
            }
        });

        // Initialize status filter
        ObservableList<String> statusOptions = FXCollections.observableArrayList(
            "ALL", "PENDING", "APPROVED", "REJECTED", "CANCELLED"
        );
        statusFilterComboBox.setItems(statusOptions);
        statusFilterComboBox.getSelectionModel().select("ALL");
        statusFilterComboBox.setOnAction(e -> loadLeaveHistory());

        leaveHistoryTable.setItems(leaveHistoryList);
        leaveHistoryTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldSelection, newSelection) -> {
                if (newSelection != null) {
                    displayLeaveRequestDetails(newSelection);
                }
            }
        );

        loadLeaveHistory();
    }

    private void loadLeaveHistory() {
        try {
            User currentUser = authService.getCurrentUser();
            int staffUserID = Integer.parseInt(currentUser.getId());
            
            leaveHistoryList.clear();
            leaveHistoryList.addAll(leaveService.getLeaveRequestsByStaff(staffUserID));
            
            // Apply status filter
            String statusFilter = statusFilterComboBox.getValue();
            if (!"ALL".equals(statusFilter)) {
                leaveHistoryList.removeIf(request -> !statusFilter.equals(request.getStatus()));
            }
        } catch (SQLException e) {
            showError("Database Error", "Failed to load leave history: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void displayLeaveRequestDetails(LeaveRequest request) {
        if (request == null) {
            reasonTextArea.clear();
            rejectionReasonTextArea.clear();
            statusLabel.setText("Status: -");
            return;
        }

        reasonTextArea.setText(request.getReason() != null ? request.getReason() : "");
        statusLabel.setText("Status: " + request.getStatus());
        
        if (request.getRejectionReason() != null && !request.getRejectionReason().isEmpty()) {
            rejectionReasonTextArea.setText("Rejection Reason: " + request.getRejectionReason());
        } else {
            rejectionReasonTextArea.clear();
        }

        // Enable cancel button only for pending requests
        boolean isPending = "PENDING".equals(request.getStatus());
        cancelButton.setDisable(!isPending);
    }

    @FXML
    private void handleCancel() {
        LeaveRequest selected = leaveHistoryTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Selection Error", "Please select a leave request to cancel.");
            return;
        }

        if (!"PENDING".equals(selected.getStatus())) {
            showError("Invalid Status", "Only pending leave requests can be cancelled.");
            return;
        }

        Alert confirmDialog = new Alert(AlertType.CONFIRMATION);
        confirmDialog.setTitle("Cancel Leave Request");
        confirmDialog.setHeaderText("Cancel Leave Request?");
        confirmDialog.setContentText("Are you sure you want to cancel this leave request?");
        
        confirmDialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    User currentUser = authService.getCurrentUser();
                    leaveService.cancelLeaveRequest(selected.getLeaveRequestID(),
                                                   Integer.parseInt(currentUser.getId()));
                    
                    showInfo("Success", "Leave request cancelled successfully.");
                    loadLeaveHistory();
                    leaveHistoryTable.getSelectionModel().clearSelection();
                } catch (Exception e) {
                    showError("Error", "Failed to cancel leave request: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });
    }

    @FXML
    private void handleRefresh() {
        loadLeaveHistory();
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

