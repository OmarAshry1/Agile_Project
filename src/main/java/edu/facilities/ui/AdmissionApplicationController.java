package edu.facilities.ui;

import edu.facilities.model.AdmissionApplication;
import edu.facilities.model.ApplicationStatus;
import edu.facilities.model.User;
import edu.facilities.service.AdmissionService;
import edu.facilities.service.AuthService;
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
 * Controller for Admission Application Management
 * US 2.5 - Admission Application Management
 * Admin-only access
 */
public class AdmissionApplicationController {

    @FXML private TableView<AdmissionApplication> applicationsTable;
    @FXML private TableColumn<AdmissionApplication, String> idColumn;
    @FXML private TableColumn<AdmissionApplication, String> nameColumn;
    @FXML private TableColumn<AdmissionApplication, String> emailColumn;
    @FXML private TableColumn<AdmissionApplication, String> programColumn;
    @FXML private TableColumn<AdmissionApplication, String> statusColumn;
    @FXML private TableColumn<AdmissionApplication, String> submittedDateColumn;

    @FXML private ComboBox<String> statusFilter;
    @FXML private TextField searchField;
    @FXML private Button updateStatusButton;
    @FXML private Button viewDetailsButton;
    @FXML private Button backButton;

    @FXML private Label statusLabel;

    private ObservableList<AdmissionApplication> applicationsList = FXCollections.observableArrayList();
    private FilteredList<AdmissionApplication> filteredData;
    private AdmissionService admissionService = new AdmissionService();
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
        loadApplications();
        setupSearchAndFilter();
    }

    private boolean checkAdminAccess() {
        if (!authService.isLoggedIn()) {
            showError("Access Denied", "Please login to access this feature.");
            return false;
        }

        String userType = authService.getCurrentUserType();
        if (!"ADMIN".equals(userType)) {
            showError("Access Denied", "Only administrators can manage admission applications.");
            return false;
        }
        return true;
    }

    private void setupTableColumns() {
        idColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getId())
        );

        nameColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getFullName())
        );

        emailColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getEmail())
        );

        programColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().getProgram() != null ? cellData.getValue().getProgram() : "")
        );

        statusColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        statusToString(cellData.getValue().getStatus()))
        );

        submittedDateColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().getSubmittedDate().toString())
        );
    }

    private void populateFilters() {
        if (statusFilter != null) {
            statusFilter.getItems().addAll(
                    "All Statuses",
                    "SUBMITTED",
                    "UNDER_REVIEW",
                    "ACCEPTED",
                    "REJECTED"
            );
            statusFilter.setValue("All Statuses");
        }
    }

    private void loadApplications() {
        try {
            List<AdmissionApplication> applications = admissionService.getAllApplications();
            applicationsList.clear();
            applicationsList.addAll(applications);
            statusLabel.setText("Loaded " + applications.size() + " application(s)");
        } catch (SQLException e) {
            showError("Database Error", "Failed to load applications: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void setupSearchAndFilter() {
        filteredData = new FilteredList<>(applicationsList, p -> true);

        // Search filter
        if (searchField != null) {
            searchField.textProperty().addListener((observable, oldValue, newValue) -> {
                filteredData.setPredicate(application -> {
                    if (newValue == null || newValue.isEmpty()) {
                        return true;
                    }
                    String lowerCaseFilter = newValue.toLowerCase();
                    return application.getFullName().toLowerCase().contains(lowerCaseFilter) ||
                           (application.getEmail() != null && application.getEmail().toLowerCase().contains(lowerCaseFilter)) ||
                           (application.getProgram() != null && application.getProgram().toLowerCase().contains(lowerCaseFilter));
                });
            });
        }

        // Status filter
        if (statusFilter != null) {
            statusFilter.setOnAction(e -> {
                String selectedStatus = statusFilter.getValue();
                if (selectedStatus == null || "All Statuses".equals(selectedStatus)) {
                    filteredData.setPredicate(application -> true);
                } else {
                    ApplicationStatus status = stringToStatus(selectedStatus);
                    filteredData.setPredicate(application -> application.getStatus() == status);
                }
            });
        }

        // Sort by submitted date (newest first)
        SortedList<AdmissionApplication> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(applicationsTable.comparatorProperty());
        applicationsTable.setItems(sortedData);
    }

    @FXML
    void handleUpdateStatus(ActionEvent event) {
        AdmissionApplication selected = applicationsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("No Selection", "Please select an application to update.");
            return;
        }

        // Show dialog to update status
        Dialog<ApplicationStatus> dialog = new Dialog<>();
        dialog.setTitle("Update Application Status");
        dialog.setHeaderText("Update status for: " + selected.getFullName());

        ButtonType submitButtonType = new ButtonType("Update", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(submitButtonType, ButtonType.CANCEL);

        ComboBox<ApplicationStatus> statusComboBox = new ComboBox<>();
        statusComboBox.getItems().addAll(ApplicationStatus.values());
        statusComboBox.setValue(selected.getStatus());

        TextArea notesArea = new TextArea();
        notesArea.setPromptText("Notes (optional)");
        notesArea.setPrefRowCount(3);
        if (selected.getNotes() != null) {
            notesArea.setText(selected.getNotes());
        }

        javafx.scene.layout.VBox vbox = new javafx.scene.layout.VBox(10);
        vbox.getChildren().addAll(
                new Label("New Status:"),
                statusComboBox,
                new Label("Notes:"),
                notesArea
        );
        vbox.setPadding(new javafx.geometry.Insets(20));
        dialog.getDialogPane().setContent(vbox);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == submitButtonType) {
                return statusComboBox.getValue();
            }
            return null;
        });

        java.util.Optional<ApplicationStatus> result = dialog.showAndWait();
        result.ifPresent(newStatus -> {
            try {
                User currentUser = authService.getCurrentUser();
                String notes = notesArea.getText();
                boolean success = admissionService.updateApplicationStatus(
                        selected.getId(), newStatus, currentUser, notes);

                if (success) {
                    showSuccess("Status Updated", "Application status updated successfully.");
                    loadApplications();
                } else {
                    showError("Update Failed", "Failed to update application status.");
                }
            } catch (SQLException e) {
                showError("Database Error", "Failed to update status: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    @FXML
    void handleViewDetails(ActionEvent event) {
        AdmissionApplication selected = applicationsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("No Selection", "Please select an application to view details.");
            return;
        }

        // Show details dialog
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Application Details");
        dialog.setHeaderText("Application #" + selected.getId() + " - " + selected.getFullName());

        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        javafx.scene.layout.VBox vbox = new javafx.scene.layout.VBox(10);
        vbox.getChildren().addAll(
                createDetailLabel("ID:", selected.getId()),
                createDetailLabel("Name:", selected.getFullName()),
                createDetailLabel("Email:", selected.getEmail() != null ? selected.getEmail() : "N/A"),
                createDetailLabel("Phone:", selected.getPhoneNumber() != null ? selected.getPhoneNumber() : "N/A"),
                createDetailLabel("Program:", selected.getProgram() != null ? selected.getProgram() : "N/A"),
                createDetailLabel("Status:", statusToString(selected.getStatus())),
                createDetailLabel("Submitted:", selected.getSubmittedDate().toString()),
                createDetailLabel("Reviewed:", selected.getReviewedDate() != null ? selected.getReviewedDate().toString() : "Not reviewed"),
                createDetailLabel("Reviewed By:", selected.getReviewedBy() != null ? selected.getReviewedBy().getUsername() : "N/A"),
                createDetailLabel("Notes:", selected.getNotes() != null ? selected.getNotes() : "No notes")
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
            stage.setTitle("");
            stage.setMaximized(true);
            stage.show();
        } catch (IOException e) {
            showError("Navigation Error", "Unable to return to dashboard: " + e.getMessage());
        }
    }

    private String statusToString(ApplicationStatus status) {
        if (status == null) return "UNKNOWN";
        switch (status) {
            case SUBMITTED: return "Submitted";
            case UNDER_REVIEW: return "Under Review";
            case ACCEPTED: return "Accepted";
            case REJECTED: return "Rejected";
            default: return "Unknown";
        }
    }

    private ApplicationStatus stringToStatus(String statusStr) {
        if (statusStr == null) return null;
        try {
            return ApplicationStatus.valueOf(statusStr.toUpperCase());
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
        if (applicationsTable != null) applicationsTable.setDisable(true);
        if (updateStatusButton != null) updateStatusButton.setDisable(true);
        if (viewDetailsButton != null) viewDetailsButton.setDisable(true);
        if (searchField != null) searchField.setDisable(true);
        if (statusFilter != null) statusFilter.setDisable(true);
    }
}


