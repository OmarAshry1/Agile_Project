package org.example.universitymangmentsystem.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class dashboardcontroller {

    public void initialize() {
        authService = new AuthService();

        if (authService.isLoggedIn()) {
            User currentUser = authService.getCurrentUser();
            userIdLabel.setText(currentUser.getId());
        } else {
            userIdLabel.setText("Guest");
        }

    }

    @FXML
    private VBox extraButtonsContainer;

    @FXML
    private Button reportButton;

    @FXML
    private Button roomsButton;

    @FXML
    private Label userIdLabel;

    @FXML
    void handleReport(ActionEvent event) {

    }

    @FXML
    void handleRooms(ActionEvent event) {

    }

}
