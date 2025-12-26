package edu.community.ui;

import edu.community.model.Message;
import edu.community.service.ParentTeacherMessageService;
import edu.facilities.service.AuthService;
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
import java.util.ResourceBundle;

/**
 * Controller for teachers to reply to parent messages.
 * US 4.2 - Reply to Parent Message
 */
public class TeacherReplyController implements Initializable {

    @FXML private Label originalSenderLabel;
    @FXML private Label originalSubjectLabel;
    @FXML private TextArea originalMessageArea;
    @FXML private TextArea replyArea;
    @FXML private Button sendReplyButton;
    @FXML private Button cancelButton;

    private ParentTeacherMessageService messageService;
    private AuthService authService;
    private Message originalMessage;

    public void setOriginalMessage(Message message) {
        this.originalMessage = message;
        if (message != null) {
            originalSenderLabel.setText("From: " + message.getSenderName());
            originalSubjectLabel.setText("Subject: " + message.getSubject());
            originalMessageArea.setText(message.getMessageBody());
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        messageService = new ParentTeacherMessageService();
        authService = AuthService.getInstance();
        originalMessageArea.setEditable(false);
    }

    @FXML
    private void handleSendReply(ActionEvent event) {
        if (originalMessage == null) {
            showAlert(Alert.AlertType.ERROR, "Error", "No original message selected.");
            return;
        }

        if (replyArea.getText() == null || replyArea.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Reply content is required.");
            return;
        }

        try {
            int teacherUserID = Integer.parseInt(authService.getCurrentUser().getId());
            String replyContent = replyArea.getText().trim();

            boolean success = messageService.replyToParentMessage(
                teacherUserID, originalMessage.getMessageID(), replyContent
            );

            if (success) {
                showAlert(Alert.AlertType.INFORMATION, "Success", "Reply sent successfully!");
                navigateBack(event);
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to send reply.");
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to send reply: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleCancel(ActionEvent event) {
        navigateBack(event);
    }

    private void navigateBack(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/messages-inbox.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("");
            stage.setMaximized(true);
            stage.show();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Navigation Error", "Could not navigate back: " + e.getMessage());
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

