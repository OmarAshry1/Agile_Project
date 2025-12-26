package edu.facilities.ui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;

import edu.facilities.model.Room;
import edu.facilities.model.User;
import edu.facilities.service.AuthService;
import edu.facilities.service.MaintenanceService;
import edu.facilities.service.RoomService;

import java.sql.SQLException;
import java.util.List;

public class MaintenanceController {

    @FXML
    private VBox confirmationBox;

    @FXML
    private Label confirmationLabel;

    @FXML
    private Button generateTicketButton;

    @FXML
    private TextArea issueDescription;

    @FXML
    private Label issueError;

    @FXML
    private ComboBox<String> roomComboBox;

    @FXML
    private Label roomIdError;

    @FXML
    private Label ticketNumberLabel;

    @FXML
    private Button backButton;

    @FXML
    private Button viewMyTicketsButton;

    private MaintenanceService maintenanceService = new MaintenanceService();
    private RoomService roomService = new RoomService();
    private AuthService authService = AuthService.getInstance();

    @FXML
    public void initialize() {
        loadAvailableRooms();
        
        // Show "View My Tickets" button for staff users
        if (authService.isLoggedIn() && "STAFF".equals(authService.getCurrentUserType())) {
            if (viewMyTicketsButton != null) {
                viewMyTicketsButton.setVisible(true);
            }
        } else {
            if (viewMyTicketsButton != null) {
                viewMyTicketsButton.setVisible(false);
            }
        }
    }
    
    /**
     * Load available rooms into the dropdown
     */
    private void loadAvailableRooms() {
        try {
            List<Room> availableRooms = roomService.getAvailableRooms();
            System.out.println("Loading " + availableRooms.size() + " available rooms into dropdown");
            
            roomComboBox.getItems().clear();
            for (Room room : availableRooms) {
                String roomCode = room.getId();
                roomComboBox.getItems().add(roomCode);
                System.out.println("Added room to dropdown: " + roomCode);
            }
            
            if (roomComboBox.getItems().isEmpty()) {
                roomComboBox.setPromptText("No available rooms");
            } else {
                roomComboBox.setPromptText("Select a room (" + roomComboBox.getItems().size() + " available)");
            }
        } catch (SQLException e) {
            System.err.println("Error loading available rooms: " + e.getMessage());
            e.printStackTrace();
            showError("Database Error", "Failed to load available rooms: " + e.getMessage());
        }
    }

    @FXML
    void handleGenerateTicket(ActionEvent event) {
        clearErrors();

        boolean hasErrors = false;

        if (roomComboBox == null || roomComboBox.getValue() == null || roomComboBox.getValue().isBlank()) {
            showFieldError(roomIdError, "Please select a room");
            hasErrors = true;
        }

        if (issueDescription == null || issueDescription.getText().isBlank()) {
            showFieldError(issueError, "Please describe the issue");
            hasErrors = true;
        }

        if (hasErrors) {
            confirmationBox.setVisible(false);
            return;
        }


        if (!authService.isLoggedIn()) {
            showError("Authentication Error", "You must be logged in to create a maintenance ticket.");
            return;
        }

        // REQUIREMENT: Admins cannot create maintenance tickets
        String userType = authService.getCurrentUserType();
        if ("ADMIN".equals(userType)) {
            showError("Access Denied", "Administrators cannot create maintenance tickets. Only students, staff, and professors can create tickets.");
            confirmationBox.setVisible(false);
            return;
        }

        User reporter = authService.getCurrentUser();
        if (reporter == null) {
            showError("Authentication Error", "User session expired. Please login again.");
            return;
        }


        String roomCode = roomComboBox.getValue().trim();
        String description = issueDescription.getText().trim();

        try {
            Room room = roomService.getRoomById(roomCode);
            
            if (room == null) {
                showFieldError(roomIdError, "Room not found. Please select a valid room.");
                confirmationBox.setVisible(false);
                return;
            }

            edu.facilities.model.MaintenanceTicket ticket = maintenanceService.createTicket(room, reporter, description);

            if (ticket != null && ticket.getId() != null) {
                confirmationBox.setVisible(true);
                confirmationLabel.setText("Ticket generated successfully!");
                ticketNumberLabel.setText("Ticket ID: #" + ticket.getId());
                
                // Clear form
                roomComboBox.setValue(null);
                issueDescription.clear();
                
                // Reload available rooms (in case status changed)
                loadAvailableRooms();
            } else {
                showError("Database Error", "Failed to create ticket. Please try again.");
                confirmationBox.setVisible(false);
            }
        } catch (SQLException e) {
            showError("Database Error", "Failed to create maintenance ticket: " + e.getMessage());
            confirmationBox.setVisible(false);
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            showError("Validation Error", e.getMessage());
            confirmationBox.setVisible(false);
        } catch (RuntimeException e) {
            showError("Error", "An error occurred: " + e.getMessage());
            confirmationBox.setVisible(false);
            e.printStackTrace();
        }
    }


    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void clearErrors() {
        showFieldError(roomIdError, null);
        showFieldError(issueError, null);
    }

    private void showFieldError(Label label, String message) {
        if (label == null) {
            return;
        }
        if (message == null || message.isBlank()) {
            label.setText("");
            label.setVisible(false);
        } else {
            label.setText(message);
            label.setVisible(true);
        }
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
            e.printStackTrace();
        }
    }

    @FXML
    void handleViewMyTickets(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/tickets_view.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("");
            stage.setMaximized(true);
            stage.show();
        } catch (IOException e) {
            showError("Navigation Error", "Unable to open tickets view: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
