package edu.facilities.ui;

import edu.facilities.model.Student;
import edu.facilities.model.TranscriptRequest;
import edu.facilities.model.TranscriptStatus;
import edu.facilities.model.User;
import edu.facilities.service.AuthService;
import edu.facilities.service.TranscriptService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
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

/**
 * Controller for Student Transcript Management
 * US 2.2 - Student Request Transcript
 * US 2.3 - Student Request Transcript Status
 */
public class StudentTranscriptController {

    @FXML private TableView<TranscriptRequest> requestsTable;
    @FXML private TableColumn<TranscriptRequest, String> idColumn;
    @FXML private TableColumn<TranscriptRequest, String> requestDateColumn;
    @FXML private TableColumn<TranscriptRequest, String> statusColumn;
    @FXML private TableColumn<TranscriptRequest, String> purposeColumn;
    @FXML private TableColumn<TranscriptRequest, String> processedDateColumn;

    @FXML private ComboBox<String> statusFilter;
    @FXML private Button requestTranscriptButton;
    @FXML private Button viewDetailsButton;
    @FXML private Button backButton;

    @FXML private Label statusLabel;

    private ObservableList<TranscriptRequest> requestsList = FXCollections.observableArrayList();
    private FilteredList<TranscriptRequest> filteredData;
    private TranscriptService transcriptService = new TranscriptService();
    private AuthService authService = AuthService.getInstance();

    @FXML
    public void initialize() {
        // Check student access
        if (!checkStudentAccess()) {
            disableAllControls();
            return;
        }

        setupTableColumns();
        populateFilters();
        loadTranscriptRequests();
        setupSearchAndFilter();
    }

    private boolean checkStudentAccess() {
        if (!authService.isLoggedIn()) {
            showError("Access Denied", "Please login to access this feature.");
            return false;
        }

        String userType = authService.getCurrentUserType();
        if (!"STUDENT".equals(userType)) {
            showError("Access Denied", "Only students can request transcripts.");
            return false;
        }
        return true;
    }

    private void setupTableColumns() {
        idColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getId())
        );

        requestDateColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().getRequestDate().toString())
        );

        statusColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        statusToString(cellData.getValue().getStatus()))
        );

        purposeColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().getPurpose() != null ? cellData.getValue().getPurpose() : "")
        );

        processedDateColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().getProcessedDate() != null ? 
                                cellData.getValue().getProcessedDate().toString() : "Not processed")
        );
    }

    private void populateFilters() {
        if (statusFilter != null) {
            statusFilter.getItems().addAll(
                    "All Statuses",
                    "PENDING",
                    "IN_PROGRESS",
                    "READY_FOR_PICKUP",
                    "COMPLETED",
                    "CANCELLED"
            );
            statusFilter.setValue("All Statuses");
        }
    }

    private void loadTranscriptRequests() {
        try {
            User currentUser = authService.getCurrentUser();
            if (currentUser == null) {
                showError("Authentication Error", "User session expired. Please login again.");
                return;
            }
            if (currentUser instanceof Student) {
                List<TranscriptRequest> requests = transcriptService.getTranscriptRequestsByStudent((Student) currentUser);
                requestsList.clear();
                requestsList.addAll(requests);
                statusLabel.setText("Loaded " + requests.size() + " transcript request(s)");
            } else {
                showError("Access Error", "Current user is not a student.");
            }
        } catch (SQLException e) {
            showError("Database Error", "Failed to load transcript requests: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void setupSearchAndFilter() {
        filteredData = new FilteredList<>(requestsList, p -> true);

        // Status filter
        if (statusFilter != null) {
            statusFilter.setOnAction(e -> {
                String selectedStatus = statusFilter.getValue();
                if (selectedStatus == null || "All Statuses".equals(selectedStatus)) {
                    filteredData.setPredicate(request -> true);
                } else {
                    TranscriptStatus status = stringToStatus(selectedStatus);
                    filteredData.setPredicate(request -> request.getStatus() == status);
                }
            });
        }

        // Sort by request date (newest first)
        SortedList<TranscriptRequest> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(requestsTable.comparatorProperty());
        requestsTable.setItems(sortedData);
    }

    @FXML
    void handleRequestTranscript(ActionEvent event) {
        // Show dialog to request transcript
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Request Transcript");
        dialog.setHeaderText("Request a new transcript");

        ButtonType submitButtonType = new ButtonType("Submit Request", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(submitButtonType, ButtonType.CANCEL);

        TextArea purposeArea = new TextArea();
        purposeArea.setPromptText("Purpose of transcript request (optional)");
        purposeArea.setPrefRowCount(3);

        javafx.scene.layout.VBox vbox = new javafx.scene.layout.VBox(10);
        vbox.getChildren().addAll(
                new Label("Purpose:"),
                purposeArea
        );
        vbox.setPadding(new javafx.geometry.Insets(20));
        dialog.getDialogPane().setContent(vbox);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == submitButtonType) {
                return purposeArea.getText();
            }
            return null;
        });

        java.util.Optional<String> result = dialog.showAndWait();
        result.ifPresent(purpose -> {
            try {
                User currentUser = authService.getCurrentUser();
                if (currentUser == null) {
                    showError("Authentication Error", "User session expired. Please login again.");
                    return;
                }
                
                // Verify user is a student
                String userType = authService.getCurrentUserType();
                if (!"STUDENT".equals(userType)) {
                    showError("Access Error", "Only students can request transcripts. Current user type: " + 
                            (userType != null ? userType : "Unknown"));
                    return;
                }
                
                if (!(currentUser instanceof Student)) {
                    showError("Access Error", "Current user is not a student instance. Please contact support.");
                    System.err.println("User type mismatch: Expected Student, got " + currentUser.getClass().getName());
                    return;
                }
                
                System.out.println("Creating transcript request for student ID: " + currentUser.getId());
                TranscriptRequest request = transcriptService.createTranscriptRequest(
                        (Student) currentUser, purpose);

                if (request != null) {
                    showSuccess("Request Submitted", 
                            "Your transcript request has been submitted successfully!\n" +
                            "Request ID: #" + request.getId() + "\n" +
                            "Status: " + statusToString(request.getStatus()));
                    loadTranscriptRequests();
                } else {
                    showError("Request Failed", "Failed to submit transcript request. " +
                            "The request was not created in the database. Please try again or contact support.");
                    System.err.println("Transcript request creation returned null for student: " + currentUser.getId());
                }
            } catch (IllegalArgumentException e) {
                showError("Validation Error", "Invalid input: " + e.getMessage());
                e.printStackTrace();
            } catch (SQLException e) {
                String errorMsg = "Failed to submit transcript request due to database error.\n\n";
                if (e.getMessage() != null) {
                    if (e.getMessage().contains("foreign key")) {
                        errorMsg += "Error: Student record not found in database.\n";
                        errorMsg += "Please ensure your student profile is properly set up.";
                    } else if (e.getMessage().contains("constraint")) {
                        errorMsg += "Error: Database constraint violation.\n";
                        errorMsg += "Details: " + e.getMessage();
                    } else {
                        errorMsg += "Details: " + e.getMessage();
                    }
                } else {
                    errorMsg += "Unknown database error occurred.";
                }
                showError("Database Error", errorMsg);
                e.printStackTrace();
            } catch (Exception e) {
                showError("Unexpected Error", "An unexpected error occurred while submitting your request: " + 
                        e.getMessage() + "\n\nPlease contact support if this issue persists.");
                e.printStackTrace();
            }
        });
    }

    @FXML
    void handleViewDetails(ActionEvent event) {
        TranscriptRequest selected = requestsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("No Selection", "Please select a transcript request to view details.");
            return;
        }

        // Show details dialog
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Transcript Request Details");
        dialog.setHeaderText("Request #" + selected.getId());

        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        javafx.scene.layout.VBox vbox = new javafx.scene.layout.VBox(10);
        vbox.getChildren().addAll(
                createDetailLabel("Request ID:", selected.getId()),
                createDetailLabel("Status:", statusToString(selected.getStatus())),
                createDetailLabel("Request Date:", selected.getRequestDate().toString()),
                createDetailLabel("Purpose:", selected.getPurpose() != null ? selected.getPurpose() : "N/A"),
                createDetailLabel("Processed Date:", selected.getProcessedDate() != null ? 
                        selected.getProcessedDate().toString() : "Not processed"),
                createDetailLabel("Completed Date:", selected.getCompletedDate() != null ? 
                        selected.getCompletedDate().toString() : "Not completed"),
                createDetailLabel("Pickup Date:", selected.getPickupDate() != null ? 
                        selected.getPickupDate().toString() : "Not picked up"),
                createDetailLabel("Processed By:", selected.getProcessedBy() != null ? 
                        selected.getProcessedBy().getUsername() : "N/A"),
                createDetailLabel("Notes:", selected.getNotes() != null ? selected.getNotes() : "No notes"),
                createDetailLabel("PDF Path:", selected.getPdfPath() != null ? selected.getPdfPath() : "Not generated")
        );
        vbox.setPadding(new javafx.geometry.Insets(20));
        dialog.getDialogPane().setContent(vbox);
        dialog.showAndWait();
    }

    private Label createDetailLabel(String label, String value) {
        Label detailLabel = new Label(label + " " + value);
        detailLabel.setWrapText(true);
        return detailLabel;
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
        }
    }

    private String statusToString(TranscriptStatus status) {
        if (status == null) return "UNKNOWN";
        switch (status) {
            case PENDING: return "Pending";
            case IN_PROGRESS: return "In Progress";
            case READY_FOR_PICKUP: return "Ready for Pickup";
            case COMPLETED: return "Completed";
            case CANCELLED: return "Cancelled";
            default: return "Unknown";
        }
    }

    private TranscriptStatus stringToStatus(String statusStr) {
        if (statusStr == null) return null;
        try {
            return TranscriptStatus.valueOf(statusStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showSuccess(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void disableAllControls() {
        if (requestsTable != null) requestsTable.setDisable(true);
        if (requestTranscriptButton != null) requestTranscriptButton.setDisable(true);
        if (viewDetailsButton != null) viewDetailsButton.setDisable(true);
        if (statusFilter != null) statusFilter.setDisable(true);
    }
}

