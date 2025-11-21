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
        updateUI();
    }

    private void updateUI() {
        if (authService.isLoggedIn()) {
            User currentUser = authService.getCurrentUser();
            userIdLabel.setText(currentUser.getUsername() + " / " + currentUser.getId());
            
            // Show logout button, hide login/register
            if (logoutButton != null) logoutButton.setVisible(true);
            if (loginButton != null) loginButton.setVisible(false);
            if (registerButton != null) registerButton.setVisible(false);
            
            // Enable buttons
            if (roomsButton != null) roomsButton.setDisable(false);
            if (reportButton != null) reportButton.setDisable(false);
            
            // Update button text and behavior based on user role
            String userType = authService.getCurrentUserType();
            if ("ADMIN".equals(userType)) {
                // Admins can view all tickets and assign them
                if (reportButton != null) reportButton.setText("View All Tickets");
            } else {
                // Students, Staff, and Professors can create tickets
                // Staff can also view their assigned tickets from the tickets view
                if (reportButton != null) reportButton.setText("Report Maintenance Issue");
            }
            
            // Add "View My Tickets" button for non-admin users
            if (!"ADMIN".equals(userType) && extraButtonsContainer != null) {
                // Clear existing buttons first
                extraButtonsContainer.getChildren().clear();
                
                javafx.scene.control.Button viewMyTicketsButton = new javafx.scene.control.Button("View My Tickets");
                viewMyTicketsButton.setPrefHeight(40);
                viewMyTicketsButton.setPrefWidth(200);
                viewMyTicketsButton.getStyleClass().add("btn-primary");
                viewMyTicketsButton.setOnAction(e -> {
                    try {
                        javafx.scene.Parent root = javafx.fxml.FXMLLoader.load(getClass().getResource("/fxml/tickets_view.fxml"));
                        javafx.stage.Stage stage = (javafx.stage.Stage) ((javafx.scene.Node) e.getSource()).getScene().getWindow();
                        stage.setScene(new javafx.scene.Scene(root));
                        stage.setTitle("My Tickets");
                        stage.show();
                    } catch (java.io.IOException ex) {
                        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                        alert.setTitle("Navigation Error");
                        alert.setHeaderText(null);
                        alert.setContentText("Unable to open tickets view: " + ex.getMessage());
                        alert.showAndWait();
                    }
                });
                extraButtonsContainer.getChildren().add(viewMyTicketsButton);
            }
        } else {
            userIdLabel.setText("Guest");
            
            // Show login/register buttons, hide logout
            if (logoutButton != null) logoutButton.setVisible(false);
            if (loginButton != null) loginButton.setVisible(true);
            if (registerButton != null) registerButton.setVisible(true);
            
            // Disable feature buttons
            if (roomsButton != null) roomsButton.setDisable(true);
            if (reportButton != null) reportButton.setDisable(true);
        }
    }

    @FXML
    private VBox extraButtonsContainer;

    @FXML
    private Button reportButton;

    @FXML
    private Button roomsButton;

    @FXML
    private Button logoutButton;

    @FXML
    private Button loginButton;

    @FXML
    private Button registerButton;

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
        if ("ADMIN".equals(userType)) {
            // Admins can only view and assign tickets
            navigateTo("/fxml/tickets_view.fxml", event, "Maintenance Tickets");
        } else {
            // Students, Staff, and Professors can create tickets
            navigateTo("/fxml/maintenanceTicket.fxml", event, "Report Maintenance Issue");
        }
    }

    @FXML
    void handleRooms(ActionEvent event) {
        navigateTo("/fxml/rooms.fxml", event, "Room Management");
    }

    @FXML
    void handleLogout(ActionEvent event) {
        authService.logout();
        // Navigate to login page after logout
        navigateTo("/fxml/login.fxml", event, "Login");
    }

    @FXML
    void handleLogin(ActionEvent event) {
        navigateTo("/fxml/login.fxml", event, "Login");
    }

    @FXML
    void handleRegister(ActionEvent event) {
        navigateTo("/fxml/register.fxml", event, "Register");
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
