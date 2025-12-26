package edu.community.ui;

import edu.community.model.Meeting;
import edu.community.service.MeetingService;
import edu.facilities.model.User;
import edu.facilities.service.AuthService;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controller for students to request meetings with staff/professors.
 */
public class RequestMeetingController implements Initializable {

    @FXML private ComboBox<User> staffComboBox;
    @FXML private TextField subjectField;
    @FXML private TextArea descriptionArea;
    @FXML private DatePicker meetingDatePicker;
    @FXML private Spinner<Integer> startHourSpinner;
    @FXML private Spinner<Integer> startMinuteSpinner;
    @FXML private Spinner<Integer> endHourSpinner;
    @FXML private Spinner<Integer> endMinuteSpinner;
    @FXML private TextField locationField;
    @FXML private Button submitButton;
    @FXML private Button cancelButton;
    @FXML private Label statusLabel;

    private MeetingService meetingService;
    private AuthService authService;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        meetingService = new MeetingService();
        authService = AuthService.getInstance();

        // Access control - only students
        if (!authService.isLoggedIn()) {
            showAlert(Alert.AlertType.ERROR, "Access Denied", "Please login to request a meeting.");
            disableControls();
            return;
        }

        String userType = authService.getCurrentUserType();
        if (!"STUDENT".equals(userType)) {
            showAlert(Alert.AlertType.ERROR, "Access Denied", "Only students can request meetings.");
            disableControls();
            return;
        }

        // Set default date to today
        meetingDatePicker.setValue(LocalDate.now());

        // Initialize time spinners
        SpinnerValueFactory<Integer> startHourFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 9);
        SpinnerValueFactory<Integer> startMinuteFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0, 15);
        SpinnerValueFactory<Integer> endHourFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 10);
        SpinnerValueFactory<Integer> endMinuteFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0, 15);

        startHourSpinner.setValueFactory(startHourFactory);
        startMinuteSpinner.setValueFactory(startMinuteFactory);
        endHourSpinner.setValueFactory(endHourFactory);
        endMinuteSpinner.setValueFactory(endMinuteFactory);

        loadStaffMembers();
    }

    private void loadStaffMembers() {
        try {
            List<User> staff = meetingService.getAvailableStaff();
            staffComboBox.setItems(FXCollections.observableArrayList(staff));

            staffComboBox.setCellFactory(param -> new ListCell<User>() {
                @Override
                protected void updateItem(User item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(item.getUsername() + " (" + item.getUserType() + ")");
                    }
                }
            });
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to load staff members: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleSubmit(ActionEvent event) {
        // Validation
        if (staffComboBox.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Please select a staff member.");
            return;
        }

        if (subjectField.getText() == null || subjectField.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Subject is required.");
            return;
        }

        if (meetingDatePicker.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Meeting date is required.");
            return;
        }

        LocalDate meetingDate = meetingDatePicker.getValue();
        if (meetingDate.isBefore(LocalDate.now())) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Meeting date cannot be in the past.");
            return;
        }

        LocalTime startTime = LocalTime.of(startHourSpinner.getValue(), startMinuteSpinner.getValue());
        LocalTime endTime = LocalTime.of(endHourSpinner.getValue(), endMinuteSpinner.getValue());

        if (endTime.isBefore(startTime) || endTime.equals(startTime)) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "End time must be after start time.");
            return;
        }

        try {
            Meeting meeting = new Meeting();
            meeting.setStudentUserID(Integer.parseInt(authService.getCurrentUser().getId()));
            meeting.setStaffUserID(Integer.parseInt(staffComboBox.getValue().getId()));
            meeting.setSubject(subjectField.getText().trim());
            meeting.setDescription(descriptionArea.getText() != null ? descriptionArea.getText().trim() : "");
            meeting.setMeetingDate(meetingDate);
            meeting.setStartTime(startTime);
            meeting.setEndTime(endTime);
            meeting.setLocation(locationField.getText() != null ? locationField.getText().trim() : "");

            int meetingID = meetingService.requestMeeting(meeting);

            showAlert(Alert.AlertType.INFORMATION, "Success", 
                     "Meeting request submitted successfully! Meeting ID: #" + meetingID);
            navigateToDashboard(event);

        } catch (IllegalArgumentException e) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", e.getMessage());
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to submit meeting request: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleCancel(ActionEvent event) {
        navigateToDashboard(event);
    }

    private void navigateToDashboard(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/dashboard.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Dashboard");
            stage.setMaximized(true);
            stage.show();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Navigation Error", "Could not load dashboard: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void disableControls() {
        staffComboBox.setDisable(true);
        subjectField.setDisable(true);
        descriptionArea.setDisable(true);
        meetingDatePicker.setDisable(true);
        startHourSpinner.setDisable(true);
        startMinuteSpinner.setDisable(true);
        endHourSpinner.setDisable(true);
        endMinuteSpinner.setDisable(true);
        locationField.setDisable(true);
        submitButton.setDisable(true);
        cancelButton.setDisable(false); // Allow going back
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

