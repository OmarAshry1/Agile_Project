package edu.community.ui;

import edu.community.model.ForumComment;
import edu.community.model.ForumPost;
import edu.community.service.ForumService;
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
 * Controller for forum posts and comments.
 * US 4.6 - Create Discussion Post
 * US 4.7 - Comment on Post
 */
public class ForumController implements Initializable {

    @FXML private TableView<ForumPost> postsTable;
    @FXML private TableColumn<ForumPost, String> titleColumn;
    @FXML private TableColumn<ForumPost, String> authorColumn;
    @FXML private TableColumn<ForumPost, String> topicColumn;
    @FXML private TableColumn<ForumPost, String> dateColumn;
    @FXML private TableColumn<ForumPost, Integer> viewsColumn;
    @FXML private TableColumn<ForumPost, Integer> repliesColumn;

    @FXML private TextArea postContentArea;
    @FXML private ListView<ForumComment> commentsList;
    @FXML private TextArea commentArea;
    @FXML private Button createPostButton;
    @FXML private Button addCommentButton;
    @FXML private Button refreshButton;
    @FXML private Button backButton;

    @FXML private ComboBox<String> topicFilterComboBox;
    @FXML private ComboBox<Integer> courseFilterComboBox;

    private ForumService forumService;
    private AuthService authService;
    private ObservableList<ForumPost> posts;
    private ObservableList<ForumComment> comments;
    private ForumPost selectedPost;
    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        forumService = new ForumService();
        authService = AuthService.getInstance();
        posts = FXCollections.observableArrayList();
        comments = FXCollections.observableArrayList();

        setupTable();
        loadTopics();
        loadPosts();
    }

    private void setupTable() {
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        authorColumn.setCellValueFactory(new PropertyValueFactory<>("authorName"));
        topicColumn.setCellValueFactory(new PropertyValueFactory<>("topic"));
        
        dateColumn.setCellFactory(column -> new TableCell<ForumPost, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                } else {
                    ForumPost post = getTableRow().getItem();
                    if (post.getCreatedDate() != null) {
                        setText(post.getCreatedDate().format(dateFormatter));
                    } else {
                        setText("");
                    }
                }
            }
        });
        
        viewsColumn.setCellValueFactory(new PropertyValueFactory<>("viewCount"));
        repliesColumn.setCellValueFactory(new PropertyValueFactory<>("replyCount"));

        postsTable.setItems(posts);
        postsTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldSelection, newSelection) -> {
                if (newSelection != null) {
                    selectedPost = newSelection;
                    loadPostDetails(newSelection);
                }
            }
        );

        commentsList.setCellFactory(param -> new ListCell<ForumComment>() {
            @Override
            protected void updateItem(ForumComment comment, boolean empty) {
                super.updateItem(comment, empty);
                if (empty || comment == null) {
                    setText(null);
                } else {
                    String author = comment.getAuthorName() != null ? comment.getAuthorName() : "Unknown";
                    String date = comment.getCreatedDate() != null 
                        ? comment.getCreatedDate().format(dateFormatter) 
                        : "";
                    setText(String.format("[%s] %s: %s", date, author, comment.getContent()));
                }
            }
        });
        commentsList.setItems(comments);
    }

    private void loadTopics() {
        try {
            List<String> topics = forumService.getDistinctTopics();
            topicFilterComboBox.setItems(FXCollections.observableArrayList(topics));
            topicFilterComboBox.getItems().add(0, "All Topics");
            topicFilterComboBox.getSelectionModel().select(0);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadPosts() {
        try {
            String selectedTopic = topicFilterComboBox.getValue();
            Integer courseID = courseFilterComboBox.getValue();
            
            List<ForumPost> postList = forumService.getAllPosts(
                courseID, 
                "All Topics".equals(selectedTopic) ? null : selectedTopic
            );
            posts.setAll(postList);
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to load posts: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadPostDetails(ForumPost post) {
        try {
            ForumPost fullPost = forumService.getPostById(post.getPostID());
            if (fullPost != null) {
                postContentArea.setText(fullPost.getContent());
                List<ForumComment> commentList = forumService.getPostComments(fullPost.getPostID());
                comments.setAll(commentList);
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to load post details: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleCreatePost(ActionEvent event) {
        navigateToCreatePost(event);
    }

    @FXML
    private void handleAddComment(ActionEvent event) {
        if (selectedPost == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a post to comment on.");
            return;
        }

        if (commentArea.getText() == null || commentArea.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Comment content is required.");
            return;
        }

        try {
            int authorUserID = Integer.parseInt(authService.getCurrentUser().getId());
            ForumComment comment = new ForumComment();
            comment.setPostID(selectedPost.getPostID());
            comment.setAuthorUserID(authorUserID);
            comment.setContent(commentArea.getText().trim());

            forumService.addComment(comment);
            showAlert(Alert.AlertType.INFORMATION, "Success", "Comment added successfully!");
            commentArea.clear();
            loadPostDetails(selectedPost);
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to add comment: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleRefresh(ActionEvent event) {
        loadPosts();
    }

    @FXML
    private void handleBack(ActionEvent event) {
        navigateToDashboard(event);
    }

    private void navigateToCreatePost(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/create_forum_post.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("");
            stage.setMaximized(true);
            stage.show();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Navigation Error", "Could not load create post view: " + e.getMessage());
            e.printStackTrace();
        }
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

