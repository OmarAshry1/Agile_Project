package edu.facilities.ui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import edu.facilities.model.Room;
import edu.facilities.model.User;
import edu.facilities.service.AuthService;
import edu.facilities.service.MaintenanceService;
import edu.facilities.service.RoomService;

import java.sql.SQLException;

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
    private TextField roomId;

    @FXML
    private Label roomIdError;

    @FXML
    private Label ticketNumberLabel;

    private MaintenanceService maintenanceService = new MaintenanceService();
    private RoomService roomService = new RoomService();
    private AuthService authService = AuthService.getInstance();

    @FXML
    void handleGenerateTicket(ActionEvent event) {
        clearErrors();

        boolean hasErrors = false;

        if (roomId == null || roomId.getText().isBlank()) {
            showFieldError(roomIdError, "Room ID is required");
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

        User reporter = authService.getCurrentUser();
        if (reporter == null) {
            showError("Authentication Error", "User session expired. Please login again.");
            return;
        }


        String roomCode = roomId.getText().trim();
        String description = issueDescription.getText().trim();

        try {

            Room room = roomService.getRoomById(roomCode);
            
            if (room == null) {
                showFieldError(roomIdError, "Room not found. Please check the room code.");
                confirmationBox.setVisible(false);
                return;
            }


            edu.facilities.model.MaintenanceTicket ticket = maintenanceService.createTicket(room, reporter, description);

            if (ticket != null && ticket.getId() != null) {
                confirmationBox.setVisible(true);
                confirmationLabel.setText("Ticket generated successfully!");
                ticketNumberLabel.setText("Ticket ID: #" + ticket.getId());
                

                roomId.clear();
                issueDescription.clear();
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
}
