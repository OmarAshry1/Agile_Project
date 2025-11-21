package edu.facilities.ui;

import edu.facilities.model.User;
import edu.facilities.service.AuthService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;

public class dashboardcontroller {

    private AuthService authService;

    @FXML
    public void initialize() {
        authService = AuthService.getInstance();

        if (authService.isLoggedIn()) {
            User currentUser = authService.getCurrentUser();
            userIdLabel.setText(currentUser.getUsername() + " / " + currentUser.getId());
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
        navigateTo("/fxml/maintenanceTicket.fxml", event, "Maintenance Reports");
    }

    @FXML
    void handleRooms(ActionEvent event) {
        navigateTo("/fxml/rooms.fxml", event, "Room Management");
    }

    private void navigateTo(String fxmlPath, ActionEvent event, String title) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle(title);
            stage.show();
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Navigation Error");
            alert.setHeaderText("Unable to open view");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }
}
