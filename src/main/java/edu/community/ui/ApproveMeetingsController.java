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
 * Controller for staff/professors to view and approve/reject meeting requests.
 */
public class ApproveMeetingsController implements Initializable {

    @FXML private TableView<Meeting> meetingsTable;
    @FXML private TableColumn<Meeting, String> subjectColumn;
    @FXML private TableColumn<Meeting, String> studentColumn;
    @FXML private TableColumn<Meeting, String> dateColumn;
    @FXML private TableColumn<Meeting, String> timeColumn;
    @FXML private TableColumn<Meeting, String> statusColumn;
    @FXML private TableColumn<Meeting, String> locationColumn;

    @FXML private TextArea detailsArea;
    @FXML private TextArea responseNotesArea;
    @FXML private Button approveButton;
    @FXML private Button rejectButton;
    @FXML private Button refreshButton;
    @FXML private Button backButton;
    @FXML private ComboBox<String> statusFilterComboBox;

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

        // Access control - only staff/professors
        if (!authService.isLoggedIn()) {
            showAlert(Alert.AlertType.ERROR, "Access Denied", "Please login to manage meetings.");
            return;
        }

        String userType = authService.getCurrentUserType();
        if (!"PROFESSOR".equals(userType) && !"STAFF".equals(userType)) {
            showAlert(Alert.AlertType.ERROR, "Access Denied", 
                     "Only staff and professors can manage meeting requests.");
            return;
        }

        setupTableColumns();
        setupStatusFilter();
        loadMeetings();
    }

    private void setupTableColumns() {
        subjectColumn.setCellValueFactory(new PropertyValueFactory<>("subject"));
        
        studentColumn.setCellFactory(column -> new TableCell<Meeting, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                } else {
                    Meeting meeting = getTableRow().getItem();
                    setText(meeting.getStudentName() != null ? meeting.getStudentName() : "N/A");
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

    private void setupStatusFilter() {
        ObservableList<String> statusOptions = FXCollections.observableArrayList(
            "ALL", "PENDING", "APPROVED", "REJECTED", "CANCELLED", "COMPLETED"
        );
        statusFilterComboBox.setItems(statusOptions);
        statusFilterComboBox.getSelectionModel().select("PENDING");
        statusFilterComboBox.setOnAction(e -> loadMeetings());
    }

    private void loadMeetings() {
        try {
            int staffUserID = Integer.parseInt(authService.getCurrentUser().getId());
            String filter = statusFilterComboBox.getSelectionModel().getSelectedItem();
            
            meetingsList.clear();
            if ("PENDING".equals(filter)) {
                meetingsList.addAll(meetingService.getPendingMeetingsByStaff(staffUserID));
            } else {
                meetingsList.addAll(meetingService.getMeetingsByStaff(staffUserID));
                // Filter by status if not ALL
                if (!"ALL".equals(filter)) {
                    meetingsList.removeIf(m -> !filter.equals(m.getStatus()));
                }
            }
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
        details.append("Student: ").append(meeting.getStudentName()).append("\n");
        details.append("Date: ").append(meeting.getMeetingDate().format(dateFormatter)).append("\n");
        details.append("Time: ").append(meeting.getStartTime().format(timeFormatter))
               .append(" - ").append(meeting.getEndTime().format(timeFormatter)).append("\n");
        details.append("Location: ").append(meeting.getLocation()).append("\n");
        details.append("Status: ").append(meeting.getStatus()).append("\n\n");
        
        if (meeting.getDescription() != null && !meeting.getDescription().isEmpty()) {
            details.append("Description:\n").append(meeting.getDescription()).append("\n\n");
        }
        
        if (meeting.getResponseNotes() != null && !meeting.getResponseNotes().isEmpty()) {
            details.append("Previous Response Notes:\n").append(meeting.getResponseNotes());
        }

        detailsArea.setText(details.toString());
        
        // Enable/disable buttons based on status
        boolean isPending = "PENDING".equals(meeting.getStatus());
        approveButton.setDisable(!isPending);
        rejectButton.setDisable(!isPending);
    }

    @FXML
    private void handleApprove(ActionEvent event) {
        Meeting selectedMeeting = meetingsTable.getSelectionModel().getSelectedItem();
        if (selectedMeeting == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a meeting to approve.");
            return;
        }

        if (!"PENDING".equals(selectedMeeting.getStatus())) {
            showAlert(Alert.AlertType.WARNING, "Invalid Status", "Only pending meetings can be approved.");
            return;
        }

        try {
            int staffUserID = Integer.parseInt(authService.getCurrentUser().getId());
            String responseNotes = responseNotesArea.getText() != null ? responseNotesArea.getText().trim() : "";
            
            boolean success = meetingService.respondToMeeting(
                selectedMeeting.getMeetingID(), "APPROVED", responseNotes, staffUserID);

            if (success) {
                showAlert(Alert.AlertType.INFORMATION, "Success", "Meeting approved successfully.");
                responseNotesArea.clear();
                loadMeetings();
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to approve meeting.");
            }
        } catch (IllegalArgumentException e) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", e.getMessage());
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to approve meeting: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleReject(ActionEvent event) {
        Meeting selectedMeeting = meetingsTable.getSelectionModel().getSelectedItem();
        if (selectedMeeting == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a meeting to reject.");
            return;
        }

        if (!"PENDING".equals(selectedMeeting.getStatus())) {
            showAlert(Alert.AlertType.WARNING, "Invalid Status", "Only pending meetings can be rejected.");
            return;
        }

        String responseNotes = responseNotesArea.getText();
        if (responseNotes == null || responseNotes.trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", 
                     "Please provide a reason for rejection.");
            return;
        }

        try {
            int staffUserID = Integer.parseInt(authService.getCurrentUser().getId());
            
            boolean success = meetingService.respondToMeeting(
                selectedMeeting.getMeetingID(), "REJECTED", responseNotes.trim(), staffUserID);

            if (success) {
                showAlert(Alert.AlertType.INFORMATION, "Success", "Meeting rejected.");
                responseNotesArea.clear();
                loadMeetings();
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to reject meeting.");
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to reject meeting: " + e.getMessage());
            e.printStackTrace();
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

