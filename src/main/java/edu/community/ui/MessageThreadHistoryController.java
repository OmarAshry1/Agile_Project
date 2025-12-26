package edu.community.ui;

import edu.community.model.Message;
import edu.community.model.MessageThread;
import edu.community.service.ParentTeacherMessageService;
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
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controller for viewing parent-teacher message thread history.
 * US 4.3 - View Parent-Teacher History
 */
public class MessageThreadHistoryController implements Initializable {

    @FXML private TableView<MessageThread> threadsTable;
    @FXML private TableColumn<MessageThread, String> subjectColumn;
    @FXML private TableColumn<MessageThread, String> teacherColumn;
    @FXML private TableColumn<MessageThread, String> studentColumn;
    @FXML private TableColumn<MessageThread, String> lastMessageColumn;
    @FXML private TableColumn<MessageThread, Integer> messageCountColumn;

    @FXML private ListView<Message> messagesList;
    @FXML private Label threadSubjectLabel;
    @FXML private Label threadParticipantsLabel;
    @FXML private Button refreshButton;
    @FXML private Button backButton;

    private ParentTeacherMessageService messageService;
    private AuthService authService;
    private ObservableList<MessageThread> threads;
    private ObservableList<Message> messages;
    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        messageService = new ParentTeacherMessageService();
        authService = AuthService.getInstance();
        threads = FXCollections.observableArrayList();
        messages = FXCollections.observableArrayList();

        setupTable();
        loadThreads();
    }

    private void setupTable() {
        subjectColumn.setCellValueFactory(new PropertyValueFactory<>("subject"));
        teacherColumn.setCellValueFactory(new PropertyValueFactory<>("teacherName"));
        studentColumn.setCellValueFactory(new PropertyValueFactory<>("studentName"));
        
        lastMessageColumn.setCellFactory(column -> new TableCell<MessageThread, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                } else {
                    MessageThread thread = getTableRow().getItem();
                    if (thread.getLastMessageDate() != null) {
                        setText(thread.getLastMessageDate().format(dateFormatter));
                    } else {
                        setText("");
                    }
                }
            }
        });
        
        messageCountColumn.setCellValueFactory(new PropertyValueFactory<>("messageCount"));

        threadsTable.setItems(threads);
        threadsTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldSelection, newSelection) -> {
                if (newSelection != null) {
                    loadThreadMessages(newSelection.getThreadID());
                }
            }
        );

        // Setup messages list
        messagesList.setCellFactory(param -> new ListCell<Message>() {
            @Override
            protected void updateItem(Message message, boolean empty) {
                super.updateItem(message, empty);
                if (empty || message == null) {
                    setText(null);
                } else {
                    String sender = message.getSenderName() != null ? message.getSenderName() : "Unknown";
                    String date = message.getSentDate() != null 
                        ? message.getSentDate().format(dateFormatter) 
                        : "";
                    setText(String.format("[%s] %s: %s", date, sender, message.getMessageBody()));
                }
            }
        });
        messagesList.setItems(messages);
    }

    private void loadThreads() {
        try {
            int userID = Integer.parseInt(authService.getCurrentUser().getId());
            String userType = authService.getCurrentUserType();

            List<MessageThread> threadList;
            if ("PROFESSOR".equals(userType) || "STAFF".equals(userType)) {
                threadList = messageService.getTeacherThreads(userID);
            } else if ("PARENT".equals(userType)) {
                threadList = messageService.getParentThreads(userID);
            } else {
                showAlert(Alert.AlertType.WARNING, "Access Denied", 
                    "Only parents, professors, and staff can view message threads.");
                return;
            }

            threads.setAll(threadList);
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to load threads: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadThreadMessages(int threadID) {
        try {
            int userID = Integer.parseInt(authService.getCurrentUser().getId());
            List<Message> messageList = messageService.getThreadHistory(threadID, userID);
            messages.setAll(messageList);

            MessageThread selectedThread = threadsTable.getSelectionModel().getSelectedItem();
            if (selectedThread != null) {
                threadSubjectLabel.setText("Subject: " + selectedThread.getSubject());
                threadParticipantsLabel.setText(String.format("Parent: %s | Teacher: %s | Student: %s",
                    selectedThread.getParentName(), selectedThread.getTeacherName(), selectedThread.getStudentName()));
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to load messages: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleRefresh(ActionEvent event) {
        loadThreads();
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

