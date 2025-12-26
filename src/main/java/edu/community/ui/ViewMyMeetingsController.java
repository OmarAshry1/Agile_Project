package edu.community.ui;

import edu.community.model.Meeting;
import edu.community.service.MeetingService;
import edu.facilities.service.AuthService;
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
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

/**
 * Controller for students to view their meeting requests.
 */
public class ViewMyMeetingsController implements Initializable {

    @FXML private TableView<Meeting> meetingsTable;
    @FXML private TableColumn<Meeting, String> subjectColumn;
    @FXML private TableColumn<Meeting, String> staffColumn;
    @FXML private TableColumn<Meeting, String> dateColumn;
    @FXML private TableColumn<Meeting, String> timeColumn;
    @FXML private TableColumn<Meeting, String> statusColumn;
    @FXML private TableColumn<Meeting, String> locationColumn;

    @FXML private TextArea detailsArea;
    @FXML private Button cancelButton;
    @FXML private Button refreshButton;
    @FXML private Button backButton;

    private MeetingService meetingService;
    private AuthService authService;
    private ObservableList<Meeting> meetingsList;
    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        meetingService = new MeetingService();
        authService = AuthService.getInstance();
        meetingsList = FXCollections.observableArrayList();

        // Access control - only students
        if (!authService.isLoggedIn()) {
            showAlert(Alert.AlertType.ERROR, "Access Denied", "Please login to view meetings.");
            return;
        }

        String userType = authService.getCurrentUserType();
        if (!"STUDENT".equals(userType)) {
            showAlert(Alert.AlertType.ERROR, "Access Denied", "Only students can view their meetings.");
            return;
        }

        setupTableColumns();
        loadMeetings();
    }

    private void setupTableColumns() {
        subjectColumn.setCellValueFactory(new PropertyValueFactory<>("subject"));
        
        staffColumn.setCellFactory(column -> new TableCell<Meeting, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                } else {
                    Meeting meeting = getTableRow().getItem();
                    setText(meeting.getStaffName() != null ? meeting.getStaffName() : "N/A");
                }
            }
        });

        dateColumn.setCellFactory(column -> new TableCell<Meeting, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                } else {
                    Meeting meeting = getTableRow().getItem();
                    if (meeting.getMeetingDate() != null) {
                        setText(meeting.getMeetingDate().format(dateFormatter));
                    } else {
                        setText("");
                    }
                }
            }
        });

        timeColumn.setCellFactory(column -> new TableCell<Meeting, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                } else {
                    Meeting meeting = getTableRow().getItem();
                    if (meeting.getStartTime() != null && meeting.getEndTime() != null) {
                        setText(meeting.getStartTime().format(timeFormatter) + " - " + 
                               meeting.getEndTime().format(timeFormatter));
                    } else {
                        setText("");
                    }
                }
            }
        });

        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        locationColumn.setCellValueFactory(new PropertyValueFactory<>("location"));

        meetingsTable.setItems(meetingsList);
        meetingsTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldSelection, newSelection) -> {
                if (newSelection != null) {
                    displayMeetingDetails(newSelection);
                }
            }
        );
    }

    private void loadMeetings() {
        try {
            int studentUserID = Integer.parseInt(authService.getCurrentUser().getId());
            meetingsList.clear();
            meetingsList.addAll(meetingService.getMeetingsByStudent(studentUserID));
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to load meetings: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to load meetings: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void displayMeetingDetails(Meeting meeting) {
        StringBuilder details = new StringBuilder();
        details.append("Subject: ").append(meeting.getSubject()).append("\n\n");
        details.append("Staff: ").append(meeting.getStaffName()).append("\n");
        details.append("Date: ").append(meeting.getMeetingDate().format(dateFormatter)).append("\n");
        details.append("Time: ").append(meeting.getStartTime().format(timeFormatter))
               .append(" - ").append(meeting.getEndTime().format(timeFormatter)).append("\n");
        details.append("Location: ").append(meeting.getLocation()).append("\n");
        details.append("Status: ").append(meeting.getStatus()).append("\n\n");
        
        if (meeting.getDescription() != null && !meeting.getDescription().isEmpty()) {
            details.append("Description:\n").append(meeting.getDescription()).append("\n\n");
        }
        
        if (meeting.getResponseNotes() != null && !meeting.getResponseNotes().isEmpty()) {
            details.append("Response Notes:\n").append(meeting.getResponseNotes());
        }

        detailsArea.setText(details.toString());
    }

    @FXML
    private void handleCancelMeeting(ActionEvent event) {
        Meeting selectedMeeting = meetingsTable.getSelectionModel().getSelectedItem();
        if (selectedMeeting == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a meeting to cancel.");
            return;
        }

        if (!"PENDING".equals(selectedMeeting.getStatus()) && !"APPROVED".equals(selectedMeeting.getStatus())) {
            showAlert(Alert.AlertType.WARNING, "Cannot Cancel", 
                     "Only pending or approved meetings can be cancelled.");
            return;
        }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Cancellation");
        confirmAlert.setHeaderText(null);
        confirmAlert.setContentText("Are you sure you want to cancel this meeting?");
        
        if (confirmAlert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                int userID = Integer.parseInt(authService.getCurrentUser().getId());
                boolean success = meetingService.cancelMeeting(selectedMeeting.getMeetingID(), userID);
                
                if (success) {
                    showAlert(Alert.AlertType.INFORMATION, "Success", "Meeting cancelled successfully.");
                    loadMeetings();
                } else {
                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to cancel meeting.");
                }
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to cancel meeting: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void handleRefresh(ActionEvent event) {
        loadMeetings();
    }

    @FXML
    private void handleBack(ActionEvent event) {
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

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

