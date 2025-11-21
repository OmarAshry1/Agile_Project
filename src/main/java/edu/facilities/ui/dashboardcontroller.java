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
            
            // Update button text and behavior based on user role
            String userType = authService.getCurrentUserType();
            if ("ADMIN".equals(userType)) {
                // Admins can view all tickets and assign them
                reportButton.setText("View All Tickets");
            } else if ("STAFF".equals(userType)) {
                // Staff can view their assigned tickets
                reportButton.setText("My Assigned Tickets");
            } else {
                // Students/Professors can create tickets
                reportButton.setText("Report Maintenance Issue");
            }
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
        if (!authService.isLoggedIn()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Not Logged In");
            alert.setHeaderText(null);
            alert.setContentText("Please login to access this feature.");
            alert.showAndWait();
            return;
        }
        
        String userType = authService.getCurrentUserType();
        if ("ADMIN".equals(userType) || "STAFF".equals(userType)) {
            // Admins and staff go to tickets view
            navigateTo("/fxml/tickets_view.fxml", event, "Maintenance Tickets");
        } else {
            // Students/Professors go to ticket creation
            navigateTo("/fxml/maintenanceTicket.fxml", event, "Report Maintenance Issue");
        }
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
