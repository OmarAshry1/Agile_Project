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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Controller for Creating Announcements
 * US 3.1 - Create Announcement (Admin/Staff)
 */
public class CreateAnnouncementController {

    @FXML private TextField titleField;
    @FXML private TextArea contentArea;
    @FXML private ComboBox<String> targetRoleComboBox;
    @FXML private ComboBox<String> priorityComboBox;
    @FXML private ComboBox<String> statusComboBox;
    @FXML private DatePicker publishDatePicker;
    @FXML private DatePicker expiryDatePicker;
    
    @FXML private TableView<AttachmentRow> attachmentsTable;
    @FXML private TableColumn<AttachmentRow, String> fileNameColumn;
    @FXML private TableColumn<AttachmentRow, String> filePathColumn;
    @FXML private TextField attachmentNameField;
    @FXML private TextField attachmentPathField;
    
    @FXML private TableView<LinkRow> linksTable;
    @FXML private TableColumn<LinkRow, String> linkTextColumn;
    @FXML private TableColumn<LinkRow, String> linkUrlColumn;
    @FXML private TextField linkTextField;
    @FXML private TextField linkUrlField;
    
    @FXML private Button addAttachmentButton;
    @FXML private Button backButton;
    @FXML private Button removeAttachmentButton;
    @FXML private Button addLinkButton;
    @FXML private Button removeLinkButton;
    @FXML private Button saveDraftButton;
    @FXML private Button publishButton;
    @FXML private Button cancelButton;
    @FXML private Label statusLabel;
    
    private ObservableList<AttachmentRow> attachmentsList = FXCollections.observableArrayList();
    private ObservableList<LinkRow> linksList = FXCollections.observableArrayList();
    private AnnouncementService announcementService = new AnnouncementService();
    private AuthService authService = AuthService.getInstance();
    
    // Helper classes for table rows
    public static class AttachmentRow {
        private String fileName;
        private String filePath;
        
        public AttachmentRow(String fileName, String filePath) {
            this.fileName = fileName;
            this.filePath = filePath;
        }
        
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        public String getFilePath() { return filePath; }
        public void setFilePath(String filePath) { this.filePath = filePath; }
    }
    
    public static class LinkRow {
        private String linkText;
        private String linkUrl;
        
        public LinkRow(String linkText, String linkUrl) {
            this.linkText = linkText;
            this.linkUrl = linkUrl;
        }
        
        public String getLinkText() { return linkText; }
        public void setLinkText(String linkText) { this.linkText = linkText; }
        public String getLinkUrl() { return linkUrl; }
        public void setLinkUrl(String linkUrl) { this.linkUrl = linkUrl; }
    }
    
    @FXML
    public void initialize() {
        if (!checkAccess()) {
            disableAllControls();
            return;
        }
        
        setupComboBoxes();
        setupTables();
    }
    
    private boolean checkAccess() {
        if (!authService.isLoggedIn()) {
            showError("Access Denied", "Please login to create announcements.");
            return false;
        }
        
        String userType = authService.getCurrentUserType();
        if (!"ADMIN".equals(userType) && !"STAFF".equals(userType)) {
            showError("Access Denied", "Only administrators and staff can create announcements.");
            return false;
        }
        return true;
    }
    
    private void setupComboBoxes() {
        targetRoleComboBox.getItems().addAll("ALL", "STUDENT", "PROFESSOR", "STAFF", "ADMIN");
        targetRoleComboBox.setValue("ALL");
        
        priorityComboBox.getItems().addAll("LOW", "NORMAL", "HIGH", "URGENT");
        priorityComboBox.setValue("NORMAL");
        
        statusComboBox.getItems().addAll("DRAFT", "PUBLISHED");
        statusComboBox.setValue("DRAFT");
        
        // Set default publish date to today
        publishDatePicker.setValue(LocalDate.now());
    }
    
    private void setupTables() {
        fileNameColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getFileName())
        );
        filePathColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getFilePath())
        );
        attachmentsTable.setItems(attachmentsList);
        
        linkTextColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getLinkText())
        );
        linkUrlColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getLinkUrl())
        );
        linksTable.setItems(linksList);
    }
    
    @FXML
    private void handleAddAttachment() {
        String fileName = attachmentNameField.getText().trim();
        String filePath = attachmentPathField.getText().trim();
        
        if (fileName.isEmpty() || filePath.isEmpty()) {
            showWarning("Invalid Input", "Please enter both file name and file path.");
            return;
        }
        
        attachmentsList.add(new AttachmentRow(fileName, filePath));
        attachmentNameField.clear();
        attachmentPathField.clear();
    }
    
    @FXML
    private void handleRemoveAttachment() {
        AttachmentRow selected = attachmentsTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            attachmentsList.remove(selected);
        } else {
            showWarning("No Selection", "Please select an attachment to remove.");
        }
    }
    
    @FXML
    private void handleAddLink() {
        String linkText = linkTextField.getText().trim();
        String linkUrl = linkUrlField.getText().trim();
        
        if (linkText.isEmpty() || linkUrl.isEmpty()) {
            showWarning("Invalid Input", "Please enter both link text and URL.");
            return;
        }
        
        if (!linkUrl.startsWith("http://") && !linkUrl.startsWith("https://")) {
            linkUrl = "https://" + linkUrl;
        }
        
        linksList.add(new LinkRow(linkText, linkUrl));
        linkTextField.clear();
        linkUrlField.clear();
    }
    
    @FXML
    private void handleRemoveLink() {
        LinkRow selected = linksTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            linksList.remove(selected);
        } else {
            showWarning("No Selection", "Please select a link to remove.");
        }
    }
    
    @FXML
    private void handleSaveDraft() {
        saveAnnouncement("DRAFT");
    }
    
    @FXML
    private void handlePublish() {
        saveAnnouncement("PUBLISHED");
    }
    
    private void saveAnnouncement(String status) {
        // Validate required fields
        String title = titleField.getText().trim();
        String content = contentArea.getText().trim();
        
        if (title.isEmpty()) {
            showError("Validation Error", "Title is required.");
            return;
        }
        
        if (content.isEmpty()) {
            showError("Validation Error", "Content is required.");
            return;
        }
        
        String targetRole = targetRoleComboBox.getValue();
        if ("ALL".equals(targetRole)) {
            targetRole = null;
        }
        
        String priority = priorityComboBox.getValue();
        LocalDate publishDateValue = publishDatePicker.getValue();
        LocalDateTime publishDate = null;
        if (publishDateValue != null) {
            publishDate = LocalDateTime.of(publishDateValue, LocalTime.now());
        }
        
        LocalDate expiryDateValue = expiryDatePicker.getValue();
        LocalDateTime expiryDate = null;
        if (expiryDateValue != null) {
            expiryDate = LocalDateTime.of(expiryDateValue, LocalTime.of(23, 59));
        }
        
        try {
            User currentUser = authService.getCurrentUser();
            if (currentUser == null) {
                showError("Authentication Error", "User session expired. Please login again.");
                return;
            }
            
            Announcement announcement = announcementService.createAnnouncement(
                title, content, targetRole, currentUser, priority, status, publishDate, expiryDate);
            
            if (announcement != null) {
                // Add attachments
                for (AttachmentRow attachment : attachmentsList) {
                    announcementService.addAttachment(
                        announcement.getId(), attachment.getFileName(), 
                        attachment.getFilePath(), null, null);
                }
                
                // Add links
                for (LinkRow link : linksList) {
                    announcementService.addLink(
                        announcement.getId(), link.getLinkText(), link.getLinkUrl());
                }
                
                showInfo("Success", "Announcement " + (status.equals("PUBLISHED") ? "published" : "saved as draft") + " successfully!");
                
                // Navigate back to announcements view
                handleCancel(null);
            } else {
                showError("Error", "Failed to create announcement.");
            }
        } catch (SQLException e) {
            showError("Database Error", "Failed to create announcement: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleCancel(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/view_announcements.fxml"));
            Stage stage;
            if (event != null && event.getSource() instanceof Node) {
                stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            } else {
                stage = (Stage) cancelButton.getScene().getWindow();
            }
            stage.setScene(new Scene(root));
            stage.setTitle("Announcements");
            stage.setMaximized(true);
            stage.show();
        } catch (IOException e) {
            showError("Navigation Error", "Could not return to announcements: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleBack(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/dashboard.fxml"));
            Stage stage;
            if (event != null && event.getSource() instanceof Node) {
                stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            } else {
                stage = (Stage) backButton.getScene().getWindow();
            }
            stage.setScene(new Scene(root));
            stage.setTitle("Dashboard");
            stage.setMaximized(true);
            stage.show();
        } catch (IOException e) {
            showError("Navigation Error", "Could not return to dashboard: " + e.getMessage());
        }
    }
    
    private void disableAllControls() {
        if (titleField != null) titleField.setDisable(true);
        if (contentArea != null) contentArea.setDisable(true);
        if (saveDraftButton != null) saveDraftButton.setDisable(true);
        if (publishButton != null) publishButton.setDisable(true);
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

