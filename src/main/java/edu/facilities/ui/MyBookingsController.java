package edu.facilities.ui;

import edu.facilities.model.Booking;
import edu.facilities.model.User;
import edu.facilities.service.AuthService;
import edu.facilities.service.BookingService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class MyBookingsController {

    @FXML
    private TableView<Booking> bookingsTable;

    @FXML
    private TableColumn<Booking, String> bookingIdColumn;

    @FXML
    private TableColumn<Booking, String> roomColumn;

    @FXML
    private TableColumn<Booking, String> capacityColumn;

    @FXML
    private TableColumn<Booking, String> dateColumn;

    @FXML
    private TableColumn<Booking, String> startTimeColumn;

    @FXML
    private TableColumn<Booking, String> endTimeColumn;

    @FXML
    private TableColumn<Booking, String> purposeColumn;

    @FXML
    private TableColumn<Booking, String> statusColumn;

    @FXML
    private Button refreshButton;

    @FXML
    private Button editBookingButton;

    @FXML
    private Button cancelBookingButton;

    @FXML
    private Button backButton;

    @FXML
    private Label statusLabel;

    private BookingService bookingService = new BookingService();
    private AuthService authService = AuthService.getInstance();
    private ObservableList<Booking> bookingsList = FXCollections.observableArrayList();

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    @FXML
    public void initialize() {
        // Check if user is logged in
        if (!authService.isLoggedIn()) {
            showError("Access Denied", "Please login to view your bookings.");
            disableAllControls();
            return;
        }

        // Check user type - only PROFESSOR and STAFF can have bookings
        String userType = authService.getCurrentUserType();
        if (!"PROFESSOR".equals(userType) && !"STAFF".equals(userType)) {
            showError("Access Denied", "Only professors and staff can view bookings.");
            disableAllControls();
            return;
        }

        // Setup table columns
        setupTableColumns();

        // Load bookings
        loadBookings();
    }

    /**
     * Setup table columns to display booking data
     */
    private void setupTableColumns() {
        bookingIdColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty("#" + cellData.getValue().getId())
        );

        roomColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getRoom().getId())
        );

        capacityColumn.setCellValueFactory(cellData -> {
            int capacity = cellData.getValue().getRoom().getCapacity();
            return new javafx.beans.property.SimpleStringProperty(String.valueOf(capacity));
        });

        dateColumn.setCellValueFactory(cellData -> {
            LocalDateTime bookingDate = cellData.getValue().getBookingDate();
            String dateStr = bookingDate != null ? bookingDate.format(DATE_FORMATTER) : "";
            return new javafx.beans.property.SimpleStringProperty(dateStr);
        });

        startTimeColumn.setCellValueFactory(cellData -> {
            LocalDateTime bookingDate = cellData.getValue().getBookingDate();
            String timeStr = bookingDate != null ? bookingDate.format(TIME_FORMATTER) : "";
            return new javafx.beans.property.SimpleStringProperty(timeStr);
        });

        endTimeColumn.setCellValueFactory(cellData -> {
            LocalDateTime endDate = cellData.getValue().getEndDate();
            String timeStr = endDate != null ? endDate.format(TIME_FORMATTER) : "";
            return new javafx.beans.property.SimpleStringProperty(timeStr);
        });

        purposeColumn.setCellValueFactory(cellData -> {
            String purpose = cellData.getValue().getPurpose();
            return new javafx.beans.property.SimpleStringProperty(purpose != null ? purpose : "");
        });

        statusColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getStatus().toString())
        );
    }

    /**
     * Load bookings for the current user
     */
    private void loadBookings() {
        try {
            User currentUser = authService.getCurrentUser();
            if (currentUser == null) {
                showError("Authentication Error", "User session expired. Please login again.");
                return;
            }

            List<Booking> bookings = bookingService.getBookingsByUser(currentUser);
            bookingsList.setAll(bookings);
            bookingsTable.setItems(bookingsList);

            if (bookings.isEmpty()) {
                statusLabel.setText("You have no bookings.");
                statusLabel.setVisible(true);
                statusLabel.setStyle("-fx-text-fill: #757575; -fx-font-size: 12px;");
            } else {
                statusLabel.setVisible(false);
            }

            System.out.println("Loaded " + bookings.size() + " bookings for user: " + currentUser.getUsername());
        } catch (SQLException e) {
            showError("Database Error", "Failed to load bookings: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            showError("Error", "An unexpected error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    void handleRefresh(ActionEvent event) {
        loadBookings();
        statusLabel.setText("Bookings refreshed.");
        statusLabel.setVisible(true);
        statusLabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-size: 12px;");
    }

    @FXML
    void handleEditBooking(ActionEvent event) {
        Booking selectedBooking = bookingsTable.getSelectionModel().getSelectedItem();

        if (selectedBooking == null) {
            showWarning("No Selection", "Please select a booking to edit.");
            return;
        }

        // Check if booking is in the past
        if (selectedBooking.getBookingDate().isBefore(LocalDateTime.now())) {
            showWarning("Cannot Edit", "Cannot edit past bookings.");
            return;
        }

        // Check if booking is cancelled
        if (selectedBooking.getStatus() != edu.facilities.model.BookingStatus.CONFIRMED) {
            showWarning("Cannot Edit", "Can only edit confirmed bookings.");
            return;
        }

        try {
            // Load the edit booking form
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/edit_booking.fxml"));
            Parent root = loader.load();

            // Get the controller and set booking data
            EditBookingController controller = loader.getController();
            controller.setBookingData(selectedBooking);

            // Create new stage
            Stage stage = new Stage();
            stage.setTitle("Edit Booking - #" + selectedBooking.getId());
            stage.setScene(new Scene(root));
            stage.setResizable(false);

            // Set up callback for when window closes
            stage.setOnHidden(e -> {
                // Refresh bookings after editing
                loadBookings();
            });

            stage.showAndWait();

        } catch (IOException e) {
            showError("Navigation Error", "Unable to open edit booking form: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    void handleCancelBooking(ActionEvent event) {
        Booking selectedBooking = bookingsTable.getSelectionModel().getSelectedItem();

        if (selectedBooking == null) {
            showWarning("No Selection", "Please select a booking to cancel.");
            return;
        }

        // Check if booking is in the past
        if (selectedBooking.getBookingDate().isBefore(LocalDateTime.now())) {
            showWarning("Cannot Cancel", "Cannot cancel past bookings.");
            return;
        }

        // Show confirmation dialog
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Cancel Booking");
        confirmDialog.setHeaderText("Are you sure you want to cancel this booking?");
        confirmDialog.setContentText("Booking ID: #" + selectedBooking.getId() + "\n" +
                                    "Room: " + selectedBooking.getRoom().getId() + "\n" +
                                    "Date: " + selectedBooking.getBookingDate().format(DATE_FORMATTER) + "\n" +
                                    "Time: " + selectedBooking.getBookingDate().format(TIME_FORMATTER) + " - " +
                                    selectedBooking.getEndDate().format(TIME_FORMATTER));

        confirmDialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    boolean cancelled = bookingService.cancelBooking(selectedBooking.getId());
                    if (cancelled) {
                        // Reload bookings
                        loadBookings();
                        
                        statusLabel.setText("Booking #" + selectedBooking.getId() + " cancelled successfully.");
                        statusLabel.setVisible(true);
                        statusLabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-size: 12px;");

                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Booking Cancelled");
                        alert.setHeaderText(null);
                        alert.setContentText("Booking #" + selectedBooking.getId() + " has been cancelled.");
                        alert.showAndWait();
                    } else {
                        showError("Cancellation Error", "Failed to cancel booking. Please try again.");
                    }
                } catch (SQLException e) {
                    showError("Database Error", "Failed to cancel booking: " + e.getMessage());
                    e.printStackTrace();
                } catch (Exception e) {
                    showError("Error", "An unexpected error occurred: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });
    }

    @FXML
    void handleBack(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/dashboard.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Dashboard");
            stage.show();
        } catch (IOException e) {
            showError("Navigation Error", "Unable to return to dashboard: " + e.getMessage());
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

    private void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void disableAllControls() {
        if (bookingsTable != null) bookingsTable.setDisable(true);
        if (refreshButton != null) refreshButton.setDisable(true);
        if (editBookingButton != null) editBookingButton.setDisable(true);
        if (cancelBookingButton != null) cancelBookingButton.setDisable(true);
    }
}

