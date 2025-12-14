package edu.facilities.ui;

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
 * Controller for Admin Transcript Management
 * US 2.4 - Generate Student Transcript
 */
public class AdminTranscriptController {

    @FXML private TableView<TranscriptRequest> requestsTable;
    @FXML private TableColumn<TranscriptRequest, String> idColumn;
    @FXML private TableColumn<TranscriptRequest, String> studentColumn;
    @FXML private TableColumn<TranscriptRequest, String> requestDateColumn;
    @FXML private TableColumn<TranscriptRequest, String> statusColumn;
    @FXML private TableColumn<TranscriptRequest, String> purposeColumn;
    @FXML private TableColumn<TranscriptRequest, String> processedByColumn;

    @FXML private ComboBox<String> statusFilter;
    @FXML private TextField searchField;
    @FXML private Button processButton;
    @FXML private Button generateButton;
    @FXML private Button viewDetailsButton;
    @FXML private Button backButton;

    @FXML private Label statusLabel;

    private ObservableList<TranscriptRequest> requestsList = FXCollections.observableArrayList();
    private FilteredList<TranscriptRequest> filteredData;
    private TranscriptService transcriptService = new TranscriptService();
    private AuthService authService = AuthService.getInstance();

    @FXML
    public void initialize() {
        // Check admin access
        if (!checkAdminAccess()) {
            disableAllControls();
            return;
        }

        setupTableColumns();
        populateFilters();
        loadTranscriptRequests();
        setupSearchAndFilter();
    }

    private boolean checkAdminAccess() {
        if (!authService.isLoggedIn()) {
            showError("Access Denied", "Please login to access this feature.");
            return false;
        }

        String userType = authService.getCurrentUserType();
        if (!"ADMIN".equals(userType)) {
            showError("Access Denied", "Only administrators can manage transcript requests.");
            return false;
        }
        return true;
    }

    private void setupTableColumns() {
        idColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getId())
        );

        studentColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().getStudent().getUsername())
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

        processedByColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().getProcessedBy() != null ? 
                                cellData.getValue().getProcessedBy().getUsername() : "Not assigned")
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
            List<TranscriptRequest> requests = transcriptService.getAllTranscriptRequests();
            requestsList.clear();
            requestsList.addAll(requests);
            statusLabel.setText("Loaded " + requests.size() + " transcript request(s)");
        } catch (SQLException e) {
            showError("Database Error", "Failed to load transcript requests: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void setupSearchAndFilter() {
        filteredData = new FilteredList<>(requestsList, p -> true);

        // Search filter
        if (searchField != null) {
            searchField.textProperty().addListener((observable, oldValue, newValue) -> {
                filteredData.setPredicate(request -> {
                    if (newValue == null || newValue.isEmpty()) {
                        return true;
                    }
                    String lowerCaseFilter = newValue.toLowerCase();
                    return request.getStudent().getUsername().toLowerCase().contains(lowerCaseFilter) ||
                           request.getId().toLowerCase().contains(lowerCaseFilter) ||
                           (request.getPurpose() != null && request.getPurpose().toLowerCase().contains(lowerCaseFilter));
                });
            });
        }

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
    void handleProcess(ActionEvent event) {
        TranscriptRequest selected = requestsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("No Selection", "Please select a transcript request to process.");
            return;
        }

        if (selected.getStatus() != TranscriptStatus.PENDING) {
            showError("Invalid Status", "Only pending requests can be processed.");
            return;
        }

        // Update status to IN_PROGRESS
        try {
            User currentUser = authService.getCurrentUser();
            boolean success = transcriptService.updateTranscriptRequestStatus(
                    selected.getId(), TranscriptStatus.IN_PROGRESS, currentUser, null, null);

            if (success) {
                showSuccess("Request Processed", "Transcript request is now in progress.");
                loadTranscriptRequests();
            } else {
                showError("Update Failed", "Failed to update request status.");
            }
        } catch (SQLException e) {
            showError("Database Error", "Failed to update status: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    void handleGenerate(ActionEvent event) {
        TranscriptRequest selected = requestsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("No Selection", "Please select a transcript request to generate.");
            return;
        }

        if (selected.getStatus() != TranscriptStatus.IN_PROGRESS && 
            selected.getStatus() != TranscriptStatus.PENDING) {
            showError("Invalid Status", "Only pending or in-progress requests can be generated.");
            return;
        }

        // Show confirmation dialog
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Generate Transcript");
        confirmAlert.setHeaderText("Generate Transcript for Request #" + selected.getId());
        confirmAlert.setContentText("Student: " + selected.getStudent().getUsername() + "\n\n" +
                                   "This will generate a PDF transcript and mark the request as ready for pickup.");

        java.util.Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                // Generate transcript PDF
                String pdfPath = transcriptService.generateTranscriptPDF(selected);
                
                showSuccess("Transcript Generated", 
                        "Transcript has been generated successfully!\n" +
                        "PDF Path: " + pdfPath + "\n" +
                        "Status: Ready for Pickup");
                loadTranscriptRequests();
            } catch (SQLException e) {
                showError("Database Error", "Failed to generate transcript: " + e.getMessage());
                e.printStackTrace();
            } catch (Exception e) {
                showError("Generation Error", "Failed to generate transcript: " + e.getMessage());
                e.printStackTrace();
            }
        }
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
                createDetailLabel("Student:", selected.getStudent().getUsername()),
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
        if (processButton != null) processButton.setDisable(true);
        if (generateButton != null) generateButton.setDisable(true);
        if (viewDetailsButton != null) viewDetailsButton.setDisable(true);
        if (searchField != null) searchField.setDisable(true);
        if (statusFilter != null) statusFilter.setDisable(true);
    }
}


