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
import java.util.List;

public class BookingController {

    @FXML
    private ComboBox<String> roomComboBox;

    @FXML
    private ComboBox<String> capacityFilter;

    @FXML
    private TextField equipmentFilter;

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
    private Button submitButton;

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

    @FXML
    public void initialize() {
        // Check if user is logged in and has permission
        if (!authService.isLoggedIn()) {
            showError("Access Denied", "Please login to book a room.");
            disableAllControls();
            return;
        }

        String userType = authService.getCurrentUserType();
        if (!"PROFESSOR".equals(userType) && !"STAFF".equals(userType)) {
            showError("Access Denied", "Only professors and staff can book rooms.");
            disableAllControls();
            return;
        }

        // Set default date to today
        datePicker.setValue(LocalDate.now());

        // Initialize time spinners
        SpinnerValueFactory<Integer> startHourFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 9);
        SpinnerValueFactory<Integer> startMinuteFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0, 15);
        SpinnerValueFactory<Integer> endHourFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 10);
        SpinnerValueFactory<Integer> endMinuteFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0, 15);

        startHourSpinner.setValueFactory(startHourFactory);
        startMinuteSpinner.setValueFactory(startMinuteFactory);
        endHourSpinner.setValueFactory(endHourFactory);
        endMinuteSpinner.setValueFactory(endMinuteFactory);

        // Populate capacity filter
        if (capacityFilter != null) {
            capacityFilter.getItems().addAll(
                    "Any Capacity",
                    "10+",
                    "20+",
                    "30+",
                    "50+",
                    "100+",
                    "200+"
            );
            capacityFilter.setValue("Any Capacity");
            capacityFilter.setOnAction(e -> filterRooms());
        }

        // Add listener to equipment filter
        if (equipmentFilter != null) {
            equipmentFilter.textProperty().addListener((observable, oldValue, newValue) -> {
                filterRooms();
            });
        }

        // Load available rooms
        loadAvailableRooms();
    }

    /**
     * Load available rooms into the dropdown
     */
    private void loadAvailableRooms() {
        filterRooms();
    }

    /**
     * Filter rooms based on capacity and equipment filters
     */
    private void filterRooms() {
        try {
            List<Room> availableRooms = roomService.getAvailableRooms();
            System.out.println("Loading " + availableRooms.size() + " available rooms into dropdown");

            // Apply capacity filter
            String selectedCapacity = capacityFilter != null ? capacityFilter.getValue() : null;
            if (selectedCapacity != null && !selectedCapacity.equals("Any Capacity")) {
                try {
                    int minCapacity = Integer.parseInt(selectedCapacity.replace("+", "").trim());
                    availableRooms = availableRooms.stream()
                            .filter(room -> room.getCapacity() >= minCapacity)
                            .collect(java.util.stream.Collectors.toList());
                } catch (NumberFormatException e) {
                    // If parsing fails, ignore this filter
                }
            }

            // Apply equipment filter
            String equipmentText = equipmentFilter != null ? equipmentFilter.getText() : null;
            if (equipmentText != null && !equipmentText.isBlank()) {
                String lowerCaseEquipment = equipmentText.toLowerCase();
                availableRooms = availableRooms.stream()
                        .filter(room -> {
                            String location = room.getLocation();
                            if (location == null || location.isEmpty()) {
                                return false;
                            }
                            String[] locationParts = location.split("\\|", 3);
                            String equipment = locationParts.length > 2 ? locationParts[2] : "";
                            return equipment.toLowerCase().contains(lowerCaseEquipment);
                        })
                        .collect(java.util.stream.Collectors.toList());
            }

            roomComboBox.getItems().clear();
            for (Room room : availableRooms) {
                String roomCode = room.getId();
                roomComboBox.getItems().add(roomCode);
                System.out.println("Added room to dropdown: " + roomCode);
            }

            if (roomComboBox.getItems().isEmpty()) {
                roomComboBox.setPromptText("No available rooms match filters");
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
    void handleSubmit(ActionEvent event) {
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

        // Get current user
        if (!authService.isLoggedIn()) {
            showError("Authentication Error", "You must be logged in to book a room.");
            return;
        }

        User user = authService.getCurrentUser();
        if (user == null) {
            showError("Authentication Error", "User session expired. Please login again.");
            return;
        }

        // Verify user type again
        String userType = authService.getCurrentUserType();
        if (!"PROFESSOR".equals(userType) && !"STAFF".equals(userType)) {
            showError("Access Denied", "Only professors and staff can book rooms.");
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

            // Create booking
            Booking booking = bookingService.createBooking(room, user, startDateTime, endDateTime, purpose);

            if (booking != null) {
                // Show success message
                statusLabel.setText("Room booked successfully! Booking ID: #" + booking.getId());
                statusLabel.setVisible(true);
                statusLabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-size: 12px;");

                // Clear form
                roomComboBox.setValue(null);
                datePicker.setValue(LocalDate.now());
                startHourSpinner.getValueFactory().setValue(9);
                startMinuteSpinner.getValueFactory().setValue(0);
                endHourSpinner.getValueFactory().setValue(10);
                endMinuteSpinner.getValueFactory().setValue(0);
                purposeTextArea.clear();

                // Reload available rooms
                loadAvailableRooms();

                // Show success alert
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Booking Confirmed");
                alert.setHeaderText("Room booked successfully!");
                alert.setContentText("Booking ID: #" + booking.getId() + "\n" +
                                    "Room: " + room.getId() + "\n" +
                                    "Date: " + selectedDate + "\n" +
                                    "Time: " + startTime + " - " + endTime);
                alert.showAndWait();
            } else {
                showError("Booking Error", "Failed to create booking. Please try again.");
            }
        } catch (IllegalArgumentException e) {
            showError("Validation Error", e.getMessage());
        } catch (SQLException e) {
            showError("Database Error", "Failed to create booking: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            showError("Error", "An unexpected error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    void handleCancel(ActionEvent event) {
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
        if (submitButton != null) submitButton.setDisable(true);
    }
}

