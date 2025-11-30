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
                // Hide and disable "View My Tickets" button for admins
                if (viewMyTicketsButton != null) {
                    viewMyTicketsButton.setVisible(false);
                    viewMyTicketsButton.setManaged(false);
                    viewMyTicketsButton.setDisable(true);
                }
                // Hide booking buttons for admins
                if (bookRoomButton != null) {
                    bookRoomButton.setVisible(false);
                    bookRoomButton.setManaged(false);
                    bookRoomButton.setDisable(true);
                }
                if (viewMyBookingsButton != null) {
                    viewMyBookingsButton.setVisible(false);
                    viewMyBookingsButton.setManaged(false);
                    viewMyBookingsButton.setDisable(true);
                }
            } else {
                // Students, Staff, and Professors can create tickets and view their own
                // Staff can also view their assigned tickets from the tickets view
                if (reportButton != null) reportButton.setText("Report Maintenance Issue");
                // Show and enable "View My Tickets" button for Students, Professors, and Staff only
                if (viewMyTicketsButton != null) {
                    viewMyTicketsButton.setVisible(true);
                    viewMyTicketsButton.setManaged(true);
                    viewMyTicketsButton.setDisable(false);
                }
                
                // Show booking buttons only for PROFESSOR and STAFF
                if ("PROFESSOR".equals(userType) || "STAFF".equals(userType)) {
                    if (bookRoomButton != null) {
                        bookRoomButton.setVisible(true);
                        bookRoomButton.setManaged(true);
                        bookRoomButton.setDisable(false);
                    }
                    if (viewMyBookingsButton != null) {
                        viewMyBookingsButton.setVisible(true);
                        viewMyBookingsButton.setManaged(true);
                        viewMyBookingsButton.setDisable(false);
                    }
                } else {
                    // Hide booking buttons for STUDENT
                    if (bookRoomButton != null) {
                        bookRoomButton.setVisible(false);
                        bookRoomButton.setManaged(false);
                        bookRoomButton.setDisable(true);
                    }
                    if (viewMyBookingsButton != null) {
                        viewMyBookingsButton.setVisible(false);
                        viewMyBookingsButton.setManaged(false);
                        viewMyBookingsButton.setDisable(true);
                    }
                }
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
            if (viewMyTicketsButton != null) {
                viewMyTicketsButton.setVisible(false);
                viewMyTicketsButton.setManaged(false);
            }
            if (bookRoomButton != null) {
                bookRoomButton.setVisible(false);
                bookRoomButton.setManaged(false);
                bookRoomButton.setDisable(true);
            }
            if (viewMyBookingsButton != null) {
                viewMyBookingsButton.setVisible(false);
                viewMyBookingsButton.setManaged(false);
                viewMyBookingsButton.setDisable(true);
            }
        }
    }
    
    @FXML
    void handleViewMyTickets(ActionEvent event) {
        if (!authService.isLoggedIn()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Not Logged In");
            alert.setHeaderText(null);
            alert.setContentText("Please login to view your tickets.");
            alert.showAndWait();
            return;
        }
        
        // Only allow Students, Professors, and Staff to view their tickets
        // Admins should use "View All Tickets" instead
        String userType = authService.getCurrentUserType();
        if ("ADMIN".equals(userType)) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Access Denied");
            alert.setHeaderText(null);
            alert.setContentText("Administrators cannot view individual tickets. Please use 'View All Tickets' to see all tickets.");
            alert.showAndWait();
            return;
        }
        
        navigateTo("/fxml/tickets_view.fxml", event, "My Tickets");
    }

    @FXML
    private VBox extraButtonsContainer;

    @FXML
    private Button reportButton;

    @FXML
    private Button roomsButton;
    
    @FXML
    private Button viewMyTicketsButton;

    @FXML
    private Button bookRoomButton;

    @FXML
    private Button viewMyBookingsButton;

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
    void handleBookRoom(ActionEvent event) {
        if (!authService.isLoggedIn()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Not Logged In");
            alert.setHeaderText(null);
            alert.setContentText("Please login to book a room.");
            alert.showAndWait();
            return;
        }
        
        String userType = authService.getCurrentUserType();
        if (!"PROFESSOR".equals(userType) && !"STAFF".equals(userType)) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Access Denied");
            alert.setHeaderText(null);
            alert.setContentText("Only professors and staff can book rooms.");
            alert.showAndWait();
            return;
        }
        
        navigateTo("/fxml/booking.fxml", event, "Book a Room");
    }

    @FXML
    void handleViewMyBookings(ActionEvent event) {
        if (!authService.isLoggedIn()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Not Logged In");
            alert.setHeaderText(null);
            alert.setContentText("Please login to view your bookings.");
            alert.showAndWait();
            return;
        }
        
        String userType = authService.getCurrentUserType();
        if (!"PROFESSOR".equals(userType) && !"STAFF".equals(userType)) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Access Denied");
            alert.setHeaderText(null);
            alert.setContentText("Only professors and staff can view bookings.");
            alert.showAndWait();
            return;
        }
        
        navigateTo("/fxml/my_bookings.fxml", event, "My Bookings");
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
