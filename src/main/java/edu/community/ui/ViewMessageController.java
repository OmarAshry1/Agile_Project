package edu.community.ui;

import edu.community.model.Message;
import edu.community.service.MessageService;
import edu.facilities.service.AuthService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.format.DateTimeFormatter;

public class ViewMessageController {

    @FXML
    private Label fromLabel;
    @FXML
    private Label toLabel;
    @FXML
    private Label dateLabel;
    @FXML
    private Label subjectLabel;
    @FXML
    private TextArea messageBodyArea;
    @FXML
    private Button replyButton;
    @FXML
    private Button closeButton;

    private Message currentMessage;
    private MessageService messageService;
    private AuthService authService;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm:ss");

    @FXML
    public void initialize() {
        messageService = new MessageService();
        authService = AuthService.getInstance();
    }

    public void setMessage(Message message) {
        this.currentMessage = message;

        fromLabel.setText(message.getSenderName());
        toLabel.setText(message.getReceiverName());
        dateLabel.setText(message.getSentDate() != null ? message.getSentDate().format(DATE_FORMATTER) : "N/A");
        subjectLabel.setText(message.getSubject());
        messageBodyArea.setText(message.getMessageBody());

        // Mark as read if receiver is current user
        int currentUserID = Integer.parseInt(authService.getCurrentUser().getId());
        if (message.getReceiverUserID() == currentUserID && !message.isRead()) {
            messageService.markAsRead(message.getMessageID());
        }
    }

    @FXML
    private void handleReply(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/compose-message.fxml"));
            Parent root = loader.load();

            ComposeMessageController controller = loader.getController();
            controller.setReplyTo(currentMessage);

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Reply to Message - " + currentMessage.getSubject());
        } catch (IOException e) {
            showAlert("Error", "Could not open compose screen: " + e.getMessage());
        }
    }

    @FXML
    private void handleClose(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/messages-inbox.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Messages");
        } catch (IOException e) {
            showAlert("Error", "Navigation Error: " + e.getMessage());
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
