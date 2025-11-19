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

    }

}
