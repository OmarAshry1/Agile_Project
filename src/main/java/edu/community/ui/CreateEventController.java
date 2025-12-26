package edu.community.ui;

import edu.community.model.Event;
import edu.community.service.EventService;
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
import java.time.LocalTime;
import java.util.ResourceBundle;

/**
 * Controller for creating events.
 * US 4.11 - Create Event
 */
public class CreateEventController implements Initializable {

    @FXML private TextField titleField;
    @FXML private TextArea descriptionArea;
    @FXML private DatePicker eventDatePicker;
    @FXML private TextField startTimeField;
    @FXML private TextField endTimeField;
    @FXML private TextField locationField;
    @FXML private ComboBox<String> eventTypeComboBox;
    @FXML private CheckBox isPublicCheckBox;
    @FXML private CheckBox isRecurringCheckBox;
    @FXML private ComboBox<String> recurrencePatternComboBox;
    @FXML private DatePicker recurrenceEndDatePicker;
    @FXML private Button createButton;
    @FXML private Button cancelButton;

    private EventService eventService;
    private AuthService authService;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        eventService = new EventService();
        authService = AuthService.getInstance();

        // Access control - only admin
        if (!authService.isLoggedIn()) {
            showAlert(Alert.AlertType.ERROR, "Access Denied", "Please login to create events.");
            return;
        }

        String userType = authService.getCurrentUserType();
        if (!"ADMIN".equals(userType)) {
            showAlert(Alert.AlertType.ERROR, "Access Denied", "Only administrators can create events.");
            return;
        }

        eventTypeComboBox.setItems(FXCollections.observableArrayList(eventService.getEventTypes()));
        eventTypeComboBox.getSelectionModel().select("GENERAL");

        recurrencePatternComboBox.setItems(FXCollections.observableArrayList("DAILY", "WEEKLY", "MONTHLY", "YEARLY"));
        isPublicCheckBox.setSelected(true);
    }

    @FXML
    private void handleCreate(ActionEvent event) {
        if (titleField.getText() == null || titleField.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Title is required.");
            return;
        }

        if (eventDatePicker.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Event date is required.");
            return;
        }

        if (startTimeField.getText() == null || startTimeField.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Start time is required.");
            return;
        }

        if (locationField.getText() == null || locationField.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Location is required.");
            return;
        }

        try {
            LocalTime startTime = LocalTime.parse(startTimeField.getText().trim());
            LocalTime endTime = null;
            if (endTimeField.getText() != null && !endTimeField.getText().trim().isEmpty()) {
                endTime = LocalTime.parse(endTimeField.getText().trim());
            }

            Event newEvent = new Event();
            newEvent.setCreatedByUserID(Integer.parseInt(authService.getCurrentUser().getId()));
            newEvent.setTitle(titleField.getText().trim());
            newEvent.setDescription(descriptionArea.getText());
            newEvent.setEventDate(eventDatePicker.getValue());
            newEvent.setStartTime(startTime);
            newEvent.setEndTime(endTime);
            newEvent.setLocation(locationField.getText().trim());
            newEvent.setEventType(eventTypeComboBox.getValue());
            newEvent.setPublic(isPublicCheckBox.isSelected());
            newEvent.setRecurring(isRecurringCheckBox.isSelected());
            
            if (isRecurringCheckBox.isSelected()) {
                newEvent.setRecurrencePattern(recurrencePatternComboBox.getValue());
                newEvent.setRecurrenceEndDate(recurrenceEndDatePicker.getValue());
            }

            int eventID = eventService.createEvent(newEvent);
            showAlert(Alert.AlertType.INFORMATION, "Success", "Event created successfully! ID: " + eventID);
            navigateToDashboard(event);
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to create event: " + e.getMessage());
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
            stage.setTitle("");
            stage.setMaximized(true);
            stage.show();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Navigation Error", "Could not load dashboard: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

