package edu.community.ui;

import edu.community.model.Message;
import edu.facilities.model.User;
import edu.facilities.service.AuthService;
import edu.community.service.MessageService;
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
import java.util.List;

public class ComposeMessageController {

    @FXML
    private ComboBox<User> recipientComboBox;
    @FXML
    private TextField subjectField;
    @FXML
    private TextArea messageBodyArea;
    @FXML
    private Button sendButton;
    @FXML
    private Button cancelButton;

    private MessageService messageService;
    private AuthService authService;
    private Integer parentMessageID = null;

    @FXML
    public void initialize() {
        messageService = new MessageService();
        authService = AuthService.getInstance();

        setupRecipientComboBox();
    }

    private void setupRecipientComboBox() {
        List<User> staff = messageService.getAllStaffMembers();
        recipientComboBox.setItems(FXCollections.observableArrayList(staff));

        recipientComboBox.setCellFactory(param -> new ListCell<User>() {
            @Override
            protected void updateItem(User user, boolean empty) {
                super.updateItem(user, empty);
                if (empty || user == null) {
                    setText(null);
                } else {
                    setText(user.getUsername() + " (" + user.getUserType() + ")");
                }
            }
        });
        recipientComboBox.setButtonCell(recipientComboBox.getCellFactory().call(null));
    }

    public void setReplyTo(Message message) {
        this.parentMessageID = message.getMessageID();
        String subject = message.getSubject();
        if (!subject.startsWith("Re: ")) {
            subject = "Re: " + subject;
        }
        subjectField.setText(subject);

        // Find the sender in the list
        for (User user : recipientComboBox.getItems()) {
            if (user.getId().equals(String.valueOf(message.getSenderUserID()))) {
                recipientComboBox.getSelectionModel().select(user);
                break;
            }
        }

        messageBodyArea.setText("\n\n--- Original Message ---\n" + message.getMessageBody());
    }

    @FXML
    private void handleSend(ActionEvent event) {
        User selectedRecipient = recipientComboBox.getSelectionModel().getSelectedItem();
        String subject = subjectField.getText().trim();
        String body = messageBodyArea.getText().trim();

        if (selectedRecipient == null) {
            showAlert("Recipient required", "Please select a staff member to send the message to.");
            return;
        }

        if (subject.isEmpty()) {
            showAlert("Subject required", "Please enter a subject.");
            return;
        }

        if (subject.length() > 200) {
            showAlert("Subject too long", "Subject cannot exceed 200 characters.");
            return;
        }

        if (body.isEmpty()) {
            showAlert("Message body required", "Please enter a message body.");
            return;
        }

        try {
            int senderID = Integer.parseInt(authService.getCurrentUser().getId());
            int receiverID = Integer.parseInt(selectedRecipient.getId());

            Message message = new Message(senderID, receiverID, subject, body);
            message.setParentMessageID(parentMessageID);

            if (messageService.sendMessage(message)) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Success");
                alert.setHeaderText(null);
                alert.setContentText("Message sent successfully!");
                alert.showAndWait();

                handleBack(event);
            } else {
                showAlert("Error", "Could not send message. Please try again.");
            }
        } catch (NumberFormatException e) {
            showAlert("Error", "Invalid user ID format.");
        }
    }

    @FXML
    private void handleCancel(ActionEvent event) {
        handleBack(event);
    }

    private void handleBack(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/messages-inbox.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Messages");
            stage.setMaximized(true);
            stage.show();
        } catch (IOException e) {
            showAlert("Error", "Navigation Error: " + e.getMessage());
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
