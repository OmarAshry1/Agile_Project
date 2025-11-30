package edu.facilities.ui;

import edu.facilities.model.Booking;
import edu.facilities.model.Room;
import edu.facilities.model.User;
import edu.facilities.service.AuthService;
import edu.facilities.service.BookingService;
import edu.facilities.service.RoomService;
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
import java.time.LocalTime;

public class EditBookingController {

    @FXML
    private Label bookingIdLabel;

    @FXML
    private Label currentRoomInfo;

    @FXML
    private ComboBox<String> roomComboBox;

    @FXML
    private DatePicker datePicker;

    @FXML
    private Spinner<Integer> startHourSpinner;

    @FXML
    private Spinner<Integer> startMinuteSpinner;

    @FXML
    private Spinner<Integer> endHourSpinner;

    @FXML
    private Spinner<Integer> endMinuteSpinner;

    @FXML
    private TextArea purposeTextArea;

    @FXML
    private Button updateButton;

    @FXML
    private Button cancelButton;

    @FXML
    private Label roomError;

    @FXML
    private Label dateError;

    @FXML
    private Label timeError;

    @FXML
    private Label statusLabel;

    private BookingService bookingService = new BookingService();
    private RoomService roomService = new RoomService();
    private AuthService authService = AuthService.getInstance();
    private Booking currentBooking;

    @FXML
    public void initialize() {
        // Check if user is logged in and has permission
        if (!authService.isLoggedIn()) {
            showError("Access Denied", "Please login to edit bookings.");
            disableAllControls();
            return;
        }

        String userType = authService.getCurrentUserType();
        if (!"PROFESSOR".equals(userType) && !"STAFF".equals(userType)) {
            showError("Access Denied", "Only professors and staff can edit bookings.");
            disableAllControls();
            return;
        }

        // Initialize time spinners
        SpinnerValueFactory<Integer> startHourFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 9);
        SpinnerValueFactory<Integer> startMinuteFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0, 15);
        SpinnerValueFactory<Integer> endHourFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 10);
        SpinnerValueFactory<Integer> endMinuteFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0, 15);

        startHourSpinner.setValueFactory(startHourFactory);
        startMinuteSpinner.setValueFactory(startMinuteFactory);
        endHourSpinner.setValueFactory(endHourFactory);
        endMinuteSpinner.setValueFactory(endMinuteFactory);

        // Load available rooms
        loadAvailableRooms();
    }

    /**
     * Set the booking data to edit
     */
    public void setBookingData(Booking booking) {
        if (booking == null) {
            showError("Error", "No booking data provided.");
            return;
        }

        this.currentBooking = booking;

        // Set booking ID label
        bookingIdLabel.setText("Booking ID: #" + booking.getId());

        // Display current room information with capacity and equipment
        Room room = booking.getRoom();
        String roomInfo = String.format("Room: %s (%s)\nCapacity: %d\nLocation: %s",
                room.getId(),
                room.getType(),
                room.getCapacity(),
                room.getLocation() != null && !room.getLocation().isEmpty() ? room.getLocation() : "Not specified");
        currentRoomInfo.setText(roomInfo);

        // Set current values
        roomComboBox.setValue(room.getId());
        datePicker.setValue(booking.getBookingDate().toLocalDate());
        
        LocalTime startTime = booking.getBookingDate().toLocalTime();
        LocalTime endTime = booking.getEndDate().toLocalTime();
        
        startHourSpinner.getValueFactory().setValue(startTime.getHour());
        startMinuteSpinner.getValueFactory().setValue(startTime.getMinute());
        endHourSpinner.getValueFactory().setValue(endTime.getHour());
        endMinuteSpinner.getValueFactory().setValue(endTime.getMinute());
        
        if (booking.getPurpose() != null) {
            purposeTextArea.setText(booking.getPurpose());
        }
    }

    /**
     * Load available rooms into the dropdown
     */
    private void loadAvailableRooms() {
        try {
            java.util.List<Room> availableRooms = roomService.getAvailableRooms();
            // Also include the current room even if it's not available (for editing)
            if (currentBooking != null) {
                Room currentRoom = currentBooking.getRoom();
                boolean found = false;
                for (Room room : availableRooms) {
                    if (room.getId().equals(currentRoom.getId())) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    availableRooms.add(currentRoom);
                }
            }

            roomComboBox.getItems().clear();
            for (Room room : availableRooms) {
                String roomCode = room.getId();
                roomComboBox.getItems().add(roomCode);
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
    void handleUpdate(ActionEvent event) {
        clearErrors();
        statusLabel.setVisible(false);

        // Validate inputs
        boolean hasErrors = false;

        if (roomComboBox.getValue() == null || roomComboBox.getValue().isBlank()) {
            showFieldError(roomError, "Please select a room");
            hasErrors = true;
        }

        if (datePicker.getValue() == null) {
            showFieldError(dateError, "Please select a date");
            hasErrors = true;
        }

        if (hasErrors) {
            return;
        }

        // Validate time
        LocalTime startTime = LocalTime.of(startHourSpinner.getValue(), startMinuteSpinner.getValue());
        LocalTime endTime = LocalTime.of(endHourSpinner.getValue(), endMinuteSpinner.getValue());

        if (endTime.isBefore(startTime) || endTime.equals(startTime)) {
            showFieldError(timeError, "End time must be after start time");
            return;
        }

        // Check if booking is in the past
        LocalDate selectedDate = datePicker.getValue();
        LocalDateTime startDateTime = LocalDateTime.of(selectedDate, startTime);
        LocalDateTime endDateTime = LocalDateTime.of(selectedDate, endTime);

        if (startDateTime.isBefore(LocalDateTime.now())) {
            showFieldError(timeError, "Cannot book rooms in the past");
            return;
        }

        if (currentBooking == null) {
            showError("Error", "No booking selected for editing.");
            return;
        }

        // Get current user
        if (!authService.isLoggedIn()) {
            showError("Authentication Error", "You must be logged in to edit a booking.");
            return;
        }

        User user = authService.getCurrentUser();
        if (user == null) {
            showError("Authentication Error", "User session expired. Please login again.");
            return;
        }

        // Verify user owns this booking
        if (!user.getId().equals(currentBooking.getUser().getId())) {
            showError("Access Denied", "You can only edit your own bookings.");
            return;
        }

        // Get room
        String roomCode = roomComboBox.getValue().trim();
        String purpose = purposeTextArea.getText() != null ? purposeTextArea.getText().trim() : "";

        try {
            Room room = roomService.getRoomById(roomCode);
            if (room == null) {
                showFieldError(roomError, "Room not found. Please select a valid room.");
                return;
            }

            // Update booking
            Booking updatedBooking = bookingService.updateBooking(
                currentBooking.getId(),
                room,
                startDateTime,
                endDateTime,
                purpose
            );

            if (updatedBooking != null) {
                // Show success message with updated room info
                String updatedRoomInfo = String.format("Room: %s (%s)\nCapacity: %d\nLocation: %s",
                        updatedBooking.getRoom().getId(),
                        updatedBooking.getRoom().getType(),
                        updatedBooking.getRoom().getCapacity(),
                        updatedBooking.getRoom().getLocation() != null && !updatedBooking.getRoom().getLocation().isEmpty() 
                            ? updatedBooking.getRoom().getLocation() : "Not specified");

                statusLabel.setText("Booking updated successfully!");
                statusLabel.setVisible(true);
                statusLabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-size: 12px;");

                // Update current room info display
                currentRoomInfo.setText(updatedRoomInfo);

                // Show success alert with updated information
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Booking Updated");
                alert.setHeaderText("Booking updated successfully!");
                alert.setContentText("Booking ID: #" + updatedBooking.getId() + "\n" +
                                    "Room: " + updatedBooking.getRoom().getId() + " (" + updatedBooking.getRoom().getType() + ")\n" +
                                    "Capacity: " + updatedBooking.getRoom().getCapacity() + "\n" +
                                    "Location: " + (updatedBooking.getRoom().getLocation() != null && !updatedBooking.getRoom().getLocation().isEmpty() 
                                        ? updatedBooking.getRoom().getLocation() : "Not specified") + "\n" +
                                    "Date: " + selectedDate + "\n" +
                                    "Time: " + startTime + " - " + endTime);
                alert.showAndWait();

                // Navigate back to my bookings
                navigateToMyBookings(event);
            } else {
                showError("Update Error", "Failed to update booking. Please try again.");
            }
        } catch (IllegalArgumentException e) {
            showError("Validation Error", e.getMessage());
        } catch (SQLException e) {
            showError("Database Error", "Failed to update booking: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            showError("Error", "An unexpected error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    void handleCancel(ActionEvent event) {
        navigateToMyBookings(event);
    }

    private void navigateToMyBookings(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/my_bookings.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("My Bookings");
            stage.show();
        } catch (IOException e) {
            showError("Navigation Error", "Unable to return to bookings: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void clearErrors() {
        showFieldError(roomError, null);
        showFieldError(dateError, null);
        showFieldError(timeError, null);
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

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void disableAllControls() {
        if (roomComboBox != null) roomComboBox.setDisable(true);
        if (datePicker != null) datePicker.setDisable(true);
        if (startHourSpinner != null) startHourSpinner.setDisable(true);
        if (startMinuteSpinner != null) startMinuteSpinner.setDisable(true);
        if (endHourSpinner != null) endHourSpinner.setDisable(true);
        if (endMinuteSpinner != null) endMinuteSpinner.setDisable(true);
        if (purposeTextArea != null) purposeTextArea.setDisable(true);
        if (updateButton != null) updateButton.setDisable(true);
    }
}

