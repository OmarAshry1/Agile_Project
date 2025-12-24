package edu.community.ui;

import edu.community.model.Message;
import edu.community.service.MessageService;
import edu.facilities.service.AuthService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class MessagesInboxController {

    @FXML
    private TabPane messageTabPane;
    @FXML
    private TableView<Message> inboxTable;
    @FXML
    private TableView<Message> sentTable;

    @FXML
    private TableColumn<Message, String> inboxFromCol;
    @FXML
    private TableColumn<Message, String> inboxSubjectCol;
    @FXML
    private TableColumn<Message, String> inboxDateCol;
    @FXML
    private TableColumn<Message, String> inboxStatusCol;

    @FXML
    private TableColumn<Message, String> sentToCol;
    @FXML
    private TableColumn<Message, String> sentSubjectCol;
    @FXML
    private TableColumn<Message, String> sentDateCol;

    @FXML
    private Button viewMessageButton;

    private MessageService messageService;
    private AuthService authService;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML
    public void initialize() {
        messageService = new MessageService();
        authService = AuthService.getInstance();

        setupTableColumns();
        loadMessages();

        // Enable view button only when a message is selected
        viewMessageButton.setDisable(true);
        inboxTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null)
                viewMessageButton.setDisable(false);
        });
        sentTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null)
                viewMessageButton.setDisable(false);
        });

        // Double click to open
        inboxTable.setRowFactory(tv -> {
            TableRow<Message> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    openMessage(row.getItem());
                }
            });
            return row;
        });

        sentTable.setRowFactory(tv -> {
            TableRow<Message> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    openMessage(row.getItem());
                }
            });
            return row;
        });
    }

    private void setupTableColumns() {
        // Inbox Columns
        inboxFromCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getSenderName()));
        inboxSubjectCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getSubject()));
        inboxDateCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getSentDate().format(DATE_FORMATTER)));
        inboxStatusCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().isRead() ? "Read" : "Unread"));

        // Sent Columns
        sentToCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getReceiverName()));
        sentSubjectCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getSubject()));
        sentDateCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getSentDate().format(DATE_FORMATTER)));

        // Bold unread messages in inbox
        inboxTable.setRowFactory(tv -> new TableRow<Message>() {
            @Override
            protected void updateItem(Message item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setStyle("");
                } else if (!item.isRead()) {
                    setStyle("-fx-font-weight: bold; -fx-background-color: #f0f7ff;");
                } else {
                    setStyle("");
                }
            }
        });
    }

    private void loadMessages() {
        int currentUserID = Integer.parseInt(authService.getCurrentUser().getId());

        List<Message> inboxMessages = messageService.getInboxMessages(currentUserID);
        inboxTable.setItems(FXCollections.observableArrayList(inboxMessages));

        List<Message> sentMessages = messageService.getSentMessages(currentUserID);
        sentTable.setItems(FXCollections.observableArrayList(sentMessages));
    }

    @FXML
    private void handleCompose(ActionEvent event) {
        navigateTo("/fxml/compose-message.fxml", event, "Compose Message");
    }

    @FXML
    private void handleViewMessage(ActionEvent event) {
        Message selected = getSelectedMessage();
        if (selected != null) {
            openMessage(selected);
        }
    }

    private Message getSelectedMessage() {
        if (messageTabPane.getSelectionModel().getSelectedIndex() == 0) {
            return inboxTable.getSelectionModel().getSelectedItem();
        } else {
            return sentTable.getSelectionModel().getSelectedItem();
        }
    }

    private void openMessage(Message message) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/view-message.fxml"));
            Parent root = loader.load();

            ViewMessageController controller = loader.getController();
            controller.setMessage(message);

            Stage stage = (Stage) inboxTable.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("View Message - " + message.getSubject());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleBackToDashboard(ActionEvent event) {
        navigateTo("/fxml/dashboard.fxml", event, "Dashboard");
    }

    private void navigateTo(String fxmlPath, ActionEvent event, String title) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle(title);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
