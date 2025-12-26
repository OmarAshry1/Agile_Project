package edu.community.ui;

import edu.community.model.Event;
import edu.community.service.EventService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controller for viewing events calendar.
 * US 4.12 - View Events Calendar
 */
public class EventsCalendarController implements Initializable {

    @FXML private DatePicker monthPicker;
    @FXML private TableView<Event> eventsTable;
    @FXML private TableColumn<Event, String> titleColumn;
    @FXML private TableColumn<Event, String> dateColumn;
    @FXML private TableColumn<Event, String> timeColumn;
    @FXML private TableColumn<Event, String> locationColumn;
    @FXML private TableColumn<Event, String> typeColumn;

    @FXML private Label eventTitleLabel;
    @FXML private Label eventDescriptionLabel;
    @FXML private Label eventDetailsLabel;
    @FXML private Button refreshButton;
    @FXML private Button backButton;

    private EventService eventService;
    private ObservableList<Event> events;
    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        eventService = new EventService();
        events = FXCollections.observableArrayList();

        monthPicker.setValue(LocalDate.now());
        setupTable();
        loadEvents();
    }

    private void setupTable() {
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        
        dateColumn.setCellFactory(column -> new TableCell<Event, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                } else {
                    Event event = getTableRow().getItem();
                    if (event.getEventDate() != null) {
                        setText(event.getEventDate().format(dateFormatter));
                    } else {
                        setText("");
                    }
                }
            }
        });
        
        timeColumn.setCellFactory(column -> new TableCell<Event, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                } else {
                    Event event = getTableRow().getItem();
                    if (event.getStartTime() != null) {
                        String timeStr = event.getStartTime().format(timeFormatter);
                        if (event.getEndTime() != null) {
                            timeStr += " - " + event.getEndTime().format(timeFormatter);
                        }
                        setText(timeStr);
                    } else {
                        setText("");
                    }
                }
            }
        });
        
        locationColumn.setCellValueFactory(new PropertyValueFactory<>("location"));
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("eventType"));

        eventsTable.setItems(events);
        eventsTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldSelection, newSelection) -> {
                if (newSelection != null) {
                    showEventDetails(newSelection);
                }
            }
        );
    }

    private void loadEvents() {
        try {
            LocalDate selectedDate = monthPicker.getValue();
            if (selectedDate == null) {
                selectedDate = LocalDate.now();
            }

            LocalDate startDate = selectedDate.withDayOfMonth(1);
            LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

            List<Event> eventList = eventService.getEvents(startDate, endDate, true);
            events.setAll(eventList);
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to load events: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showEventDetails(Event event) {
        eventTitleLabel.setText("Title: " + event.getTitle());
        eventDescriptionLabel.setText("Description: " + (event.getDescription() != null ? event.getDescription() : "N/A"));
        
        StringBuilder details = new StringBuilder();
        details.append("Date: ").append(event.getEventDate().format(dateFormatter)).append("\n");
        details.append("Time: ").append(event.getStartTime().format(timeFormatter));
        if (event.getEndTime() != null) {
            details.append(" - ").append(event.getEndTime().format(timeFormatter));
        }
        details.append("\n");
        details.append("Location: ").append(event.getLocation()).append("\n");
        details.append("Type: ").append(event.getEventType());
        
        eventDetailsLabel.setText(details.toString());
    }

    @FXML
    private void handleMonthChanged(ActionEvent event) {
        loadEvents();
    }

    @FXML
    private void handleRefresh(ActionEvent event) {
        loadEvents();
    }

    @FXML
    private void handleBack(ActionEvent event) {
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

