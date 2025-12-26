package edu.facilities.ui;

import edu.facilities.model.MaintenanceTicket;
import edu.facilities.model.TicketStatus;
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
import java.util.List;

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
    private boolean isStaff = false;

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
                isStaff = true;
                titleLabel.setText("My Assigned Tickets");
                assignButton.setVisible(false);
                staffComboBox.setVisible(false);
                staffComboBoxLabel.setVisible(false);
                assigneeLabel.setVisible(false);
                loadAssignedTickets();
            } else {
                // Students, Professors, and other users can view tickets they created
                titleLabel.setText("My Created Tickets");
                assignButton.setVisible(false);
                staffComboBox.setVisible(false);
                staffComboBoxLabel.setVisible(false);
                assigneeLabel.setVisible(false);
                loadMyCreatedTickets();
            }
        } else {
            titleLabel.setText("Please login to view tickets");
            ticketsTable.setDisable(true);
        }

        setupTableColumns();
    }
    
    /**
     * Custom TableCell for status column with ComboBox for staff users
     */
    private class StatusComboBoxTableCell extends TableCell<MaintenanceTicket, String> {
        private ComboBox<String> comboBox;

        public StatusComboBoxTableCell() {
            comboBox = new ComboBox<>();
            comboBox.getItems().addAll("NEW", "IN_PROGRESS", "RESOLVED");
            comboBox.setEditable(false);
            
            comboBox.setOnAction(e -> {
                if (getTableRow() != null && getTableRow().getItem() != null) {
                    MaintenanceTicket ticket = getTableRow().getItem();
                    String newStatusStr = comboBox.getValue();
                    if (newStatusStr != null && !newStatusStr.equals(ticket.getStatus().toString())) {
                        try {
                            TicketStatus newStatus = TicketStatus.valueOf(newStatusStr);
                            boolean success = maintenanceService.updateTicketStatus(ticket.getId(), newStatus);
                            if (success) {
                                // Refresh the table to show updated status
                                if (isStaff) {
                                    loadAssignedTickets();
                                } else {
                                    loadAllTickets();
                                }
                            } else {
                                showError("Error", "Failed to update ticket status");
                            }
                        } catch (Exception ex) {
                            showError("Error", "Failed to update ticket status: " + ex.getMessage());
                            ex.printStackTrace();
                        }
                    }
                }
            });
        }

        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            
            if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                setGraphic(null);
                setText(null);
            } else {
                MaintenanceTicket ticket = getTableRow().getItem();
                if (ticket != null && ticket.getStatus() != null) {
                    comboBox.setValue(ticket.getStatus().toString());
                    setGraphic(comboBox);
                    setText(null);
                } else {
                    setGraphic(null);
                    setText("N/A");
                }
            }
        }
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
        
        // Make status column editable for staff users
        if (isStaff) {
            statusColumn.setCellFactory(column -> new StatusComboBoxTableCell());
        }
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
            System.out.println("Loading all tickets for admin view...");
            List<MaintenanceTicket> tickets = maintenanceService.getAllTickets();
            System.out.println("Retrieved " + tickets.size() + " tickets from service");
            ticketsList.setAll(tickets);
            ticketsTable.setItems(ticketsList);
            System.out.println("Table updated with " + ticketsList.size() + " tickets");
        } catch (SQLException e) {
            System.err.println("Error loading tickets: " + e.getMessage());
            e.printStackTrace();
            showError("Database Error", "Failed to load tickets: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Unexpected error loading tickets: " + e.getMessage());
            e.printStackTrace();
            showError("Error", "An unexpected error occurred while loading tickets: " + e.getMessage());
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
    
    private void loadMyCreatedTickets() {
        if (!authService.isLoggedIn()) {
            return;
        }

        User currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            showError("Authentication Error", "User session expired. Please login again.");
            return;
        }

        try {
            System.out.println("Loading tickets created by user: " + currentUser.getId());
            ticketsList.setAll(maintenanceService.getTicketsByReporter(currentUser.getId()));
            ticketsTable.setItems(ticketsList);
            System.out.println("Loaded " + ticketsList.size() + " tickets created by user");
        } catch (SQLException e) {
            System.err.println("Error loading created tickets: " + e.getMessage());
            e.printStackTrace();
            showError("Database Error", "Failed to load your tickets: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Unexpected error loading created tickets: " + e.getMessage());
            e.printStackTrace();
            showError("Error", "An unexpected error occurred while loading your tickets: " + e.getMessage());
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
        } else if (isStaff) {
            loadAssignedTickets();
        } else {
            loadMyCreatedTickets();
        }
    }

    @FXML
    private void handleBack(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/dashboard.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));        stage.setMaximized(true);stage.show();
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
