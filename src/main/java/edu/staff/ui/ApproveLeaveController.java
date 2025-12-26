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
 * Controller for approving/rejecting leave requests (HR Admin only)
 * US 3.12 - Approve or Reject Leave
 */
public class ApproveLeaveController implements Initializable {

    @FXML private TableView<LeaveRequest> leaveRequestsTable;
    @FXML private TableColumn<LeaveRequest, Integer> idColumn;
    @FXML private TableColumn<LeaveRequest, String> staffColumn;
    @FXML private TableColumn<LeaveRequest, String> typeColumn;
    @FXML private TableColumn<LeaveRequest, String> startDateColumn;
    @FXML private TableColumn<LeaveRequest, String> endDateColumn;
    @FXML private TableColumn<LeaveRequest, Integer> daysColumn;
    @FXML private TableColumn<LeaveRequest, String> statusColumn;
    @FXML private TableColumn<LeaveRequest, String> submittedDateColumn;
    
    @FXML private TextArea reasonTextArea;
    @FXML private TextArea rejectionReasonTextArea;
    @FXML private Button approveButton;
    @FXML private Button rejectButton;
    @FXML private Button refreshButton;
    @FXML private ComboBox<String> statusFilterComboBox;

    private LeaveService leaveService;
    private AuthService authService;
    private ObservableList<LeaveRequest> leaveRequestsList;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        leaveService = new LeaveService();
        authService = AuthService.getInstance();
        leaveRequestsList = FXCollections.observableArrayList();

        // Check access
        if (!authService.isLoggedIn()) {
            showError("Access Denied", "Please login to manage leave requests.");
            return;
        }

        String userType = authService.getCurrentUserType();
        if (!"ADMIN".equals(userType)) {
            showError("Access Denied", "Only HR administrators can approve/reject leave requests.");
            return;
        }

        // Initialize table columns
        idColumn.setCellValueFactory(new PropertyValueFactory<>("leaveRequestID"));
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("leaveType"));
        daysColumn.setCellValueFactory(new PropertyValueFactory<>("numberOfDays"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        
        // Staff column (from JOIN)
        staffColumn.setCellFactory(column -> new TableCell<LeaveRequest, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                } else {
                    LeaveRequest request = getTableRow().getItem();
                    setText(request.getStaffUsername() != null ? request.getStaffUsername() : 
                           "User " + request.getStaffUserID());
                }
            }
        });
        
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

        // Initialize status filter
        ObservableList<String> statusOptions = FXCollections.observableArrayList(
            "ALL", "PENDING", "APPROVED", "REJECTED", "CANCELLED"
        );
        statusFilterComboBox.setItems(statusOptions);
        statusFilterComboBox.getSelectionModel().select("PENDING");
        statusFilterComboBox.setOnAction(e -> loadLeaveRequests());

        leaveRequestsTable.setItems(leaveRequestsList);
        leaveRequestsTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldSelection, newSelection) -> {
                if (newSelection != null) {
                    displayLeaveRequestDetails(newSelection);
                }
            }
        );

        loadLeaveRequests();
    }

    private void loadLeaveRequests() {
        try {
            String statusFilter = statusFilterComboBox.getValue();
            if ("ALL".equals(statusFilter)) {
                statusFilter = null;
            }

            leaveRequestsList.clear();
            leaveRequestsList.addAll(leaveService.getAllLeaveRequests(statusFilter));
        } catch (SQLException e) {
            showError("Database Error", "Failed to load leave requests: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void displayLeaveRequestDetails(LeaveRequest request) {
        if (request == null) {
            reasonTextArea.clear();
            rejectionReasonTextArea.clear();
            return;
        }

        reasonTextArea.setText(request.getReason() != null ? request.getReason() : "");
        
        if (request.getRejectionReason() != null) {
            rejectionReasonTextArea.setText(request.getRejectionReason());
        } else {
            rejectionReasonTextArea.clear();
        }

        // Enable/disable buttons based on status
        boolean isPending = "PENDING".equals(request.getStatus());
        approveButton.setDisable(!isPending);
        rejectButton.setDisable(!isPending);
    }

    @FXML
    private void handleApprove() {
        LeaveRequest selected = leaveRequestsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Selection Error", "Please select a leave request to approve.");
            return;
        }

        if (!"PENDING".equals(selected.getStatus())) {
            showError("Invalid Status", "Only pending leave requests can be approved.");
            return;
        }

        Alert confirmDialog = new Alert(AlertType.CONFIRMATION);
        confirmDialog.setTitle("Approve Leave Request");
        confirmDialog.setHeaderText("Approve Leave Request?");
        confirmDialog.setContentText("Are you sure you want to approve this leave request?");
        
        confirmDialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    User currentUser = authService.getCurrentUser();
                    leaveService.approveLeaveRequest(selected.getLeaveRequestID(), 
                                                     Integer.parseInt(currentUser.getId()));
                    
                    showInfo("Success", "Leave request approved successfully.");
                    loadLeaveRequests();
                    leaveRequestsTable.getSelectionModel().clearSelection();
                } catch (Exception e) {
                    showError("Error", "Failed to approve leave request: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });
    }

    @FXML
    private void handleReject() {
        LeaveRequest selected = leaveRequestsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Selection Error", "Please select a leave request to reject.");
            return;
        }

        if (!"PENDING".equals(selected.getStatus())) {
            showError("Invalid Status", "Only pending leave requests can be rejected.");
            return;
        }

        // Show dialog to get rejection reason
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Reject Leave Request");
        dialog.setHeaderText("Rejection Reason Required");
        dialog.setContentText("Please provide a reason for rejection:");

        dialog.showAndWait().ifPresent(rejectionReason -> {
            if (rejectionReason == null || rejectionReason.trim().isEmpty()) {
                showError("Validation Error", "Rejection reason is required.");
                return;
            }

            try {
                User currentUser = authService.getCurrentUser();
                leaveService.rejectLeaveRequest(selected.getLeaveRequestID(),
                                               Integer.parseInt(currentUser.getId()),
                                               rejectionReason.trim());
                
                showInfo("Success", "Leave request rejected successfully.");
                loadLeaveRequests();
                leaveRequestsTable.getSelectionModel().clearSelection();
            } catch (Exception e) {
                showError("Error", "Failed to reject leave request: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    @FXML
    private void handleRefresh() {
        loadLeaveRequests();
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

