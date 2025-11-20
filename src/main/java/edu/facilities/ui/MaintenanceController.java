package edu.facilities.ui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

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

        confirmationBox.setVisible(true);
        confirmationLabel.setText("Ticket generated successfully!");
        ticketNumberLabel.setText("Ticket ID: #" + System.currentTimeMillis());
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
