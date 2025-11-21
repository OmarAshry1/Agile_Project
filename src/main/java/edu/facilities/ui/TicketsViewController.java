package edu.facilities.ui;

import edu.facilities.model.MaintenanceTicket;
import edu.facilities.model.User;
import edu.facilities.service.AuthService;
import edu.facilities.service.MaintenanceService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;

/**
 * Controller for viewing and managing maintenance tickets
 * - Admins can view all tickets and assign them to staff
 * - Staff can view their assigned tickets
 */
public class TicketsViewController {

    @FXML private TableView<MaintenanceTicket> ticketsTable;
    @FXML private TableColumn<MaintenanceTicket, String> ticketIdColumn;
    @FXML private TableColumn<MaintenanceTicket, String> roomColumn;
    @FXML private TableColumn<MaintenanceTicket, String> reporterColumn;
    @FXML private TableColumn<MaintenanceTicket, String> assigneeColumn;
    @FXML private TableColumn<MaintenanceTicket, String> descriptionColumn;
    @FXML private TableColumn<MaintenanceTicket, String> statusColumn;
    @FXML private TableColumn<MaintenanceTicket, String> createdDateColumn;
    
    @FXML private Button assignButton;
    @FXML private Button backButton;
    @FXML private Button refreshButton;
    @FXML private ComboBox<User> staffComboBox;
    @FXML private Label titleLabel;
    @FXML private Label assigneeLabel;
    @FXML private Label staffComboBoxLabel;

    private MaintenanceService maintenanceService = new MaintenanceService();
    private AuthService authService = AuthService.getInstance();
    private ObservableList<MaintenanceTicket> ticketsList = FXCollections.observableArrayList();
    private boolean isAdmin = false;

    @FXML
    public void initialize() {
        // Check user role
        if (authService.isLoggedIn()) {
            String userType = authService.getCurrentUserType();
            isAdmin = "ADMIN".equals(userType);
            
            if (isAdmin) {
                titleLabel.setText("All Maintenance Tickets - Admin View");
                assignButton.setVisible(true);
                assignButton.setDisable(false);
                staffComboBox.setVisible(true);
                staffComboBoxLabel.setVisible(true);
                assigneeLabel.setVisible(true);
                loadAllTickets();
                loadStaffUsers();
            } else if ("STAFF".equals(userType)) {
                titleLabel.setText("My Assigned Tickets");
                assignButton.setVisible(false);
                staffComboBox.setVisible(false);
                staffComboBoxLabel.setVisible(false);
                assigneeLabel.setVisible(false);
                loadAssignedTickets();
            } else {
                // Non-admin, non-staff users shouldn't access this view
                titleLabel.setText("Access Denied");
                ticketsTable.setDisable(true);
                assignButton.setVisible(false);
                staffComboBox.setVisible(false);
                staffComboBoxLabel.setVisible(false);
                assigneeLabel.setVisible(false);
            }
        } else {
            titleLabel.setText("Please login to view tickets");
            ticketsTable.setDisable(true);
        }

        setupTableColumns();
    }

    private void setupTableColumns() {
        ticketIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        roomColumn.setCellValueFactory(cellData -> {
            if (cellData.getValue().getRoom() != null) {
                return new javafx.beans.property.SimpleStringProperty(cellData.getValue().getRoom().getId());
            }
            return new javafx.beans.property.SimpleStringProperty("N/A");
        });
        reporterColumn.setCellValueFactory(cellData -> {
            if (cellData.getValue().getReporter() != null) {
                return new javafx.beans.property.SimpleStringProperty(cellData.getValue().getReporter().getUsername());
            }
            return new javafx.beans.property.SimpleStringProperty("N/A");
        });
        assigneeColumn.setCellValueFactory(cellData -> {
            if (cellData.getValue().getAssignedStaff() != null) {
                return new javafx.beans.property.SimpleStringProperty(cellData.getValue().getAssignedStaff().getUsername());
            }
            return new javafx.beans.property.SimpleStringProperty("Unassigned");
        });
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        statusColumn.setCellValueFactory(cellData -> {
            if (cellData.getValue().getStatus() != null) {
                return new javafx.beans.property.SimpleStringProperty(cellData.getValue().getStatus().toString());
            }
            return new javafx.beans.property.SimpleStringProperty("N/A");
        });
        createdDateColumn.setCellValueFactory(cellData -> {
            if (cellData.getValue().getCreatedAt() != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                return new javafx.beans.property.SimpleStringProperty(
                    cellData.getValue().getCreatedAt().format(formatter)
                );
            }
            return new javafx.beans.property.SimpleStringProperty("N/A");
        });
    }

    private void loadAllTickets() {
        try {
            ticketsList.setAll(maintenanceService.getAllTickets());
            ticketsTable.setItems(ticketsList);
        } catch (SQLException e) {
            showError("Database Error", "Failed to load tickets: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadAssignedTickets() {
        if (!authService.isLoggedIn()) {
            return;
        }
        
        User currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            showError("Authentication Error", "User session expired. Please login again.");
            return;
        }

        try {
            ticketsList.setAll(maintenanceService.getTicketsByAssignee(currentUser.getId()));
            ticketsTable.setItems(ticketsList);
        } catch (SQLException e) {
            showError("Database Error", "Failed to load assigned tickets: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadStaffUsers() {
        try {
            var staffUsers = maintenanceService.getStaffUsers();
            staffComboBox.setItems(FXCollections.observableArrayList(staffUsers));
            
            // Set cell factory to display username
            staffComboBox.setCellFactory(param -> new ListCell<User>() {
                @Override
                protected void updateItem(User item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(item.getUsername() + " (" + item.getId() + ")");
                    }
                }
            });
            
            // Set button cell to display username
            staffComboBox.setButtonCell(new ListCell<User>() {
                @Override
                protected void updateItem(User item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText("Select Staff Member");
                    } else {
                        setText(item.getUsername() + " (" + item.getId() + ")");
                    }
                }
            });
        } catch (SQLException e) {
            showError("Database Error", "Failed to load staff users: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleAssignTicket() {
        MaintenanceTicket selectedTicket = ticketsTable.getSelectionModel().getSelectedItem();
        User selectedStaff = staffComboBox.getSelectionModel().getSelectedItem();

        if (selectedTicket == null) {
            showWarning("No Selection", "Please select a ticket to assign.");
            return;
        }

        if (selectedStaff == null) {
            showWarning("No Staff Selected", "Please select a staff member to assign the ticket to.");
            return;
        }

        // Confirm assignment
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Assign Ticket");
        confirmDialog.setHeaderText("Assign ticket to staff member?");
        confirmDialog.setContentText("Ticket #" + selectedTicket.getId() + 
                                    " will be assigned to " + selectedStaff.getUsername());

        confirmDialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    boolean success = maintenanceService.assignTicketToStaff(
                        selectedTicket.getId(), 
                        selectedStaff.getId()
                    );

                    if (success) {
                        showInfo("Success", "Ticket #" + selectedTicket.getId() + 
                                " has been assigned to " + selectedStaff.getUsername());
                        // Refresh the ticket list
                        loadAllTickets();
                        // Clear selection
                        staffComboBox.getSelectionModel().clearSelection();
                    } else {
                        showError("Error", "Failed to assign ticket. Please try again.");
                    }
                } catch (SQLException e) {
                    showError("Database Error", "Failed to assign ticket: " + e.getMessage());
                    e.printStackTrace();
                } catch (IllegalArgumentException e) {
                    showError("Validation Error", e.getMessage());
                }
            }
        });
    }

    @FXML
    private void handleRefresh() {
        if (isAdmin) {
            loadAllTickets();
        } else {
            loadAssignedTickets();
        }
    }

    @FXML
    private void handleBack(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/dashboard.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            showError("Navigation Error", "Unable to return to dashboard: " + e.getMessage());
        }
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

