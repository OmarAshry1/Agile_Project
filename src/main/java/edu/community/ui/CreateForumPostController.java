package edu.community.ui;

import edu.community.model.ForumPost;
import edu.community.service.ForumService;
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
 * Controller for creating forum posts.
 * US 4.6 - Create Discussion Post
 */
public class CreateForumPostController implements Initializable {

    @FXML private TextField titleField;
    @FXML private TextField topicField;
    @FXML private TextArea contentArea;
    @FXML private Button createButton;
    @FXML private Button cancelButton;

    private ForumService forumService;
    private AuthService authService;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        forumService = new ForumService();
        authService = AuthService.getInstance();
    }

    @FXML
    private void handleCreate(ActionEvent event) {
        if (titleField.getText() == null || titleField.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Title is required.");
            return;
        }

        if (contentArea.getText() == null || contentArea.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Content is required.");
            return;
        }

        try {
            ForumPost post = new ForumPost();
            post.setAuthorUserID(Integer.parseInt(authService.getCurrentUser().getId()));
            post.setTitle(titleField.getText().trim());
            post.setTopic(topicField.getText() != null && !topicField.getText().trim().isEmpty() 
                ? topicField.getText().trim() : null);
            post.setContent(contentArea.getText().trim());

            int postID = forumService.createPost(post);
            showAlert(Alert.AlertType.INFORMATION, "Success", "Post created successfully! ID: " + postID);
            navigateToForum(event);
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to create post: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleCancel(ActionEvent event) {
        navigateToForum(event);
    }

    private void navigateToForum(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/forum.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("");
            stage.setMaximized(true);
            stage.show();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Navigation Error", "Could not load forum: " + e.getMessage());
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

