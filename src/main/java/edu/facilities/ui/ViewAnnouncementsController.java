package edu.facilities.ui;

import edu.facilities.model.Announcement;
import edu.facilities.model.User;
import edu.facilities.service.AuthService;
import edu.facilities.service.AnnouncementService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Controller for Viewing Announcements
 * US 4.4 - View Announcements
 */
public class ViewAnnouncementsController {

    @FXML private TableView<Announcement> announcementsTable;
    @FXML private TableColumn<Announcement, String> titleColumn;
    @FXML private TableColumn<Announcement, String> contentColumn;
    @FXML private TableColumn<Announcement, String> targetRoleColumn;
    @FXML private TableColumn<Announcement, String> priorityColumn;
    @FXML private TableColumn<Announcement, String> createdDateColumn;
    @FXML private TableColumn<Announcement, String> readStatusColumn;
    
    @FXML private ComboBox<String> roleFilterComboBox;
    @FXML private CheckBox showReadCheckBox;
    @FXML private Button markAsReadButton;
    @FXML private Button refreshButton;
    @FXML private Button backButton;
    @FXML private Button createButton;
    @FXML private Button editButton;
    @FXML private Button deleteButton;
    @FXML private Button archiveButton;
    @FXML private Label statusLabel;
    @FXML private Label unreadCountLabel;
    
    @FXML private TextArea announcementDetailsArea;
    @FXML private ListView<String> attachmentsList;
    @FXML private ListView<String> linksList;
    
    private ObservableList<Announcement> announcementsList = FXCollections.observableArrayList();
    private AnnouncementService announcementService = new AnnouncementService();
    private AuthService authService = AuthService.getInstance();
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    
    @FXML
    public void initialize() {
        if (!checkAccess()) {
            disableAllControls();
            return;
        }
        
        setupTableColumns();
        setupFilters();
        setupAdminButtons();
        loadAnnouncements();
        updateUnreadCount();
    }
    
    private void setupAdminButtons() {
        String userType = authService.getCurrentUserType();
        boolean isAdminOrStaff = "ADMIN".equals(userType) || "STAFF".equals(userType);
        
        if (createButton != null) {
            createButton.setVisible(isAdminOrStaff);
            createButton.setManaged(isAdminOrStaff);
        }
        if (editButton != null) {
            editButton.setVisible(isAdminOrStaff);
            editButton.setManaged(isAdminOrStaff);
        }
        if (deleteButton != null) {
            deleteButton.setVisible("ADMIN".equals(userType));
            deleteButton.setManaged("ADMIN".equals(userType));
        }
        if (archiveButton != null) {
            archiveButton.setVisible("ADMIN".equals(userType));
            archiveButton.setManaged("ADMIN".equals(userType));
        }
    }
    
    private boolean checkAccess() {
        if (!authService.isLoggedIn()) {
            showError("Access Denied", "Please login to view announcements.");
            return false;
        }
        return true;
    }
    
    private void setupTableColumns() {
        titleColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getTitle())
        );
        
        contentColumn.setCellValueFactory(cellData -> {
            String content = cellData.getValue().getContent();
            if (content != null && content.length() > 50) {
                content = content.substring(0, 50) + "...";
            }
            return new javafx.beans.property.SimpleStringProperty(content != null ? content : "");
        });
        
        targetRoleColumn.setCellValueFactory(cellData -> {
            String role = cellData.getValue().getTargetRole();
            return new javafx.beans.property.SimpleStringProperty(role != null ? role : "ALL");
        });
        
        priorityColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getPriority())
        );
        
        createdDateColumn.setCellValueFactory(cellData -> {
            if (cellData.getValue().getCreatedDate() != null) {
                return new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().getCreatedDate().format(DATE_FORMATTER));
            }
            return new javafx.beans.property.SimpleStringProperty("");
        });
        
        readStatusColumn.setCellValueFactory(cellData -> {
            boolean isRead = cellData.getValue().isRead();
            return new javafx.beans.property.SimpleStringProperty(isRead ? "Read" : "Unread");
        });
        
        // Add row selection listener to show details
        announcementsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                showAnnouncementDetails(newSelection);
            }
        });
    }
    
    private void setupFilters() {
        roleFilterComboBox.getItems().addAll("ALL", "STUDENT", "PROFESSOR", "STAFF", "ADMIN");
        roleFilterComboBox.setValue("ALL");
        roleFilterComboBox.setOnAction(e -> loadAnnouncements());
        
        showReadCheckBox.setSelected(false);
        showReadCheckBox.setOnAction(e -> loadAnnouncements());
    }
    
    private void loadAnnouncements() {
        try {
            User currentUser = authService.getCurrentUser();
            if (currentUser == null) {
                showError("Authentication Error", "User session expired. Please login again.");
                return;
            }
            
            String userType = authService.getCurrentUserType();
            List<Announcement> announcements;
            
            // Admin/Staff see all announcements including drafts
            if ("ADMIN".equals(userType) || "STAFF".equals(userType)) {
                announcements = announcementService.getAllAnnouncementsForAdmin(false); // Exclude archived
            } else {
                // Regular users only see published announcements
                announcements = announcementService.getAnnouncementsForUser(
                    currentUser, showReadCheckBox.isSelected());
            }
            
            // Apply role filter
            String selectedRole = roleFilterComboBox.getValue();
            if (selectedRole != null && !"ALL".equals(selectedRole)) {
                announcements.removeIf(a -> {
                    String targetRole = a.getTargetRole();
                    return targetRole != null && !targetRole.equals(selectedRole);
                });
            }
            
            announcementsList.clear();
            announcementsList.addAll(announcements);
            
            statusLabel.setText("Loaded " + announcements.size() + " announcement(s)");
            updateUnreadCount();
        } catch (SQLException e) {
            showError("Database Error", "Failed to load announcements: " + e.getMessage());
        }
    }
    
    private void updateUnreadCount() {
        try {
            User currentUser = authService.getCurrentUser();
            if (currentUser != null) {
                int unreadCount = announcementService.getUnreadCount(currentUser);
                unreadCountLabel.setText("Unread: " + unreadCount);
            }
        } catch (SQLException e) {
            // Silently fail - not critical
        }
    }
    
    private void showAnnouncementDetails(Announcement announcement) {
        if (announcementDetailsArea != null) {
            StringBuilder details = new StringBuilder();
            details.append("Title: ").append(announcement.getTitle()).append("\n\n");
            details.append("Content:\n").append(announcement.getContent()).append("\n\n");
            details.append("Priority: ").append(announcement.getPriority()).append("\n");
            details.append("Target Role: ").append(announcement.getTargetRole() != null ? announcement.getTargetRole() : "ALL").append("\n");
            details.append("Status: ").append(announcement.getStatus() != null ? announcement.getStatus() : "PUBLISHED").append("\n");
            if (announcement.getPublishDate() != null) {
                details.append("Published: ").append(announcement.getPublishDate().format(DATE_FORMATTER)).append("\n");
            }
            if (announcement.getCreatedDate() != null) {
                details.append("Created: ").append(announcement.getCreatedDate().format(DATE_FORMATTER)).append("\n");
            }
            if (announcement.getExpiryDate() != null) {
                details.append("Expires: ").append(announcement.getExpiryDate().format(DATE_FORMATTER)).append("\n");
            }
            details.append("Read Status: ").append(announcement.isRead() ? "Read" : "Unread");
            
            announcementDetailsArea.setText(details.toString());
        }
        
        // Show attachments
        if (attachmentsList != null) {
            attachmentsList.getItems().clear();
            for (Announcement.AnnouncementAttachment attachment : announcement.getAttachments()) {
                attachmentsList.getItems().add(attachment.getFileName() + " (" + attachment.getFilePath() + ")");
            }
        }
        
        // Show links
        if (linksList != null) {
            linksList.getItems().clear();
            for (Announcement.AnnouncementLink link : announcement.getLinks()) {
                linksList.getItems().add(link.getLinkText() + " - " + link.getLinkUrl());
            }
        }
    }
    
    @FXML
    private void handleCreate() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/create_announcement.fxml"));
            Stage stage = (Stage) createButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Create Announcement");
            stage.show();
        } catch (IOException e) {
            showError("Navigation Error", "Could not open create announcement page: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleEdit() {
        Announcement selectedAnnouncement = announcementsTable.getSelectionModel().getSelectedItem();
        if (selectedAnnouncement == null) {
            showWarning("No Selection", "Please select an announcement to edit.");
            return;
        }
        
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/edit_announcement.fxml"));
            Parent root = loader.load();
            EditAnnouncementController controller = loader.getController();
            controller.loadAnnouncement(selectedAnnouncement.getId());
            
            Stage stage = (Stage) editButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Edit Announcement");
            stage.show();
        } catch (IOException e) {
            showError("Navigation Error", "Could not open edit announcement page: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleDelete() {
        Announcement selectedAnnouncement = announcementsTable.getSelectionModel().getSelectedItem();
        if (selectedAnnouncement == null) {
            showWarning("No Selection", "Please select an announcement to delete.");
            return;
        }
        
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Delete Announcement");
        confirmDialog.setHeaderText("Are you sure you want to permanently delete this announcement?");
        confirmDialog.setContentText("This action cannot be undone.\n\nTitle: " + selectedAnnouncement.getTitle());
        
        confirmDialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    boolean deleted = announcementService.deleteAnnouncement(selectedAnnouncement.getId());
                    if (deleted) {
                        showInfo("Success", "Announcement deleted successfully!");
                        loadAnnouncements();
                    } else {
                        showError("Error", "Failed to delete announcement.");
                    }
                } catch (SQLException e) {
                    showError("Database Error", "Failed to delete announcement: " + e.getMessage());
                }
            }
        });
    }
    
    @FXML
    private void handleArchive() {
        Announcement selectedAnnouncement = announcementsTable.getSelectionModel().getSelectedItem();
        if (selectedAnnouncement == null) {
            showWarning("No Selection", "Please select an announcement to archive.");
            return;
        }
        
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Archive Announcement");
        confirmDialog.setHeaderText("Are you sure you want to archive this announcement?");
        confirmDialog.setContentText("Archived announcements will be hidden from users but can be restored later.\n\nTitle: " + selectedAnnouncement.getTitle());
        
        confirmDialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    boolean archived = announcementService.archiveAnnouncement(selectedAnnouncement.getId());
                    if (archived) {
                        showInfo("Success", "Announcement archived successfully!");
                        loadAnnouncements();
                    } else {
                        showError("Error", "Failed to archive announcement.");
                    }
                } catch (SQLException e) {
                    showError("Database Error", "Failed to archive announcement: " + e.getMessage());
                }
            }
        });
    }
    
    @FXML
    private void handleMarkAsRead() {
        Announcement selectedAnnouncement = announcementsTable.getSelectionModel().getSelectedItem();
        if (selectedAnnouncement == null) {
            showWarning("No Selection", "Please select an announcement to mark as read.");
            return;
        }
        
        if (selectedAnnouncement.isRead()) {
            showInfo("Already Read", "This announcement is already marked as read.");
            return;
        }
        
        try {
            User currentUser = authService.getCurrentUser();
            if (currentUser == null) {
                showError("Authentication Error", "User session expired. Please login again.");
                return;
            }
            
            boolean marked = announcementService.markAsRead(currentUser, selectedAnnouncement.getId());
            if (marked) {
                selectedAnnouncement.setRead(true);
                announcementsTable.refresh();
                updateUnreadCount();
                showInfo("Success", "Announcement marked as read.");
            } else {
                showError("Error", "Failed to mark announcement as read.");
            }
        } catch (SQLException e) {
            showError("Database Error", "Failed to mark as read: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleRefresh() {
        loadAnnouncements();
        statusLabel.setText("Refreshed");
    }
    
    @FXML
    private void handleBack(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/dashboard.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Dashboard");
            stage.show();
        } catch (IOException e) {
            showError("Navigation Error", "Could not return to dashboard: " + e.getMessage());
        }
    }
    
    private void disableAllControls() {
        if (announcementsTable != null) announcementsTable.setDisable(true);
        if (markAsReadButton != null) markAsReadButton.setDisable(true);
    }
    
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

