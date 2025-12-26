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
import java.util.List;

/**
 * Controller for Editing Announcements
 * US 3.2 - Edit Announcement (Admin/Staff)
 */
public class EditAnnouncementController {

    @FXML private TextField titleField;
    @FXML private TextArea contentArea;
    @FXML private ComboBox<String> targetRoleComboBox;
    @FXML private ComboBox<String> priorityComboBox;
    @FXML private ComboBox<String> statusComboBox;
    @FXML private DatePicker publishDatePicker;
    @FXML private DatePicker expiryDatePicker;
    
    @FXML private TableView<CreateAnnouncementController.AttachmentRow> attachmentsTable;
    @FXML private TableColumn<CreateAnnouncementController.AttachmentRow, String> fileNameColumn;
    @FXML private TableColumn<CreateAnnouncementController.AttachmentRow, String> filePathColumn;
    @FXML private TextField attachmentNameField;
    @FXML private TextField attachmentPathField;
    
    @FXML private TableView<CreateAnnouncementController.LinkRow> linksTable;
    @FXML private TableColumn<CreateAnnouncementController.LinkRow, String> linkTextColumn;
    @FXML private TableColumn<CreateAnnouncementController.LinkRow, String> linkUrlColumn;
    @FXML private TextField linkTextField;
    @FXML private TextField linkUrlField;
    
    @FXML private Button addAttachmentButton;
    @FXML private Button removeAttachmentButton;
    @FXML private Button addLinkButton;
    @FXML private Button removeLinkButton;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;
    @FXML private Button viewHistoryButton;
    @FXML private Label statusLabel;
    
    private ObservableList<CreateAnnouncementController.AttachmentRow> attachmentsList = FXCollections.observableArrayList();
    private ObservableList<CreateAnnouncementController.LinkRow> linksList = FXCollections.observableArrayList();
    private AnnouncementService announcementService = new AnnouncementService();
    private AuthService authService = AuthService.getInstance();
    private String announcementId;
    private Announcement currentAnnouncement;
    
    @FXML
    public void initialize() {
        if (!checkAccess()) {
            disableAllControls();
            return;
        }
        
        setupComboBoxes();
        setupTables();
    }
    
    public void loadAnnouncement(String id) {
        this.announcementId = id;
        try {
            currentAnnouncement = announcementService.getAnnouncementById(id);
            if (currentAnnouncement == null) {
                showError("Error", "Announcement not found.");
                handleCancel(null);
                return;
            }
            
            // Populate fields
            titleField.setText(currentAnnouncement.getTitle());
            contentArea.setText(currentAnnouncement.getContent());
            
            String targetRole = currentAnnouncement.getTargetRole();
            targetRoleComboBox.setValue(targetRole != null ? targetRole : "ALL");
            
            priorityComboBox.setValue(currentAnnouncement.getPriority());
            statusComboBox.setValue(currentAnnouncement.getStatus());
            
            if (currentAnnouncement.getPublishDate() != null) {
                publishDatePicker.setValue(currentAnnouncement.getPublishDate().toLocalDate());
            }
            
            if (currentAnnouncement.getExpiryDate() != null) {
                expiryDatePicker.setValue(currentAnnouncement.getExpiryDate().toLocalDate());
            }
            
            // Load attachments
            attachmentsList.clear();
            for (Announcement.AnnouncementAttachment attachment : currentAnnouncement.getAttachments()) {
                attachmentsList.add(new CreateAnnouncementController.AttachmentRow(
                    attachment.getFileName(), attachment.getFilePath()));
            }
            
            // Load links
            linksList.clear();
            for (Announcement.AnnouncementLink link : currentAnnouncement.getLinks()) {
                linksList.add(new CreateAnnouncementController.LinkRow(
                    link.getLinkText(), link.getLinkUrl()));
            }
            
        } catch (SQLException e) {
            showError("Database Error", "Failed to load announcement: " + e.getMessage());
        }
    }
    
    private boolean checkAccess() {
        if (!authService.isLoggedIn()) {
            showError("Access Denied", "Please login to edit announcements.");
            return false;
        }
        
        String userType = authService.getCurrentUserType();
        if (!"ADMIN".equals(userType) && !"STAFF".equals(userType)) {
            showError("Access Denied", "Only administrators and staff can edit announcements.");
            return false;
        }
        return true;
    }
    
    private void setupComboBoxes() {
        targetRoleComboBox.getItems().addAll("ALL", "STUDENT", "PROFESSOR", "STAFF", "ADMIN");
        priorityComboBox.getItems().addAll("LOW", "NORMAL", "HIGH", "URGENT");
        statusComboBox.getItems().addAll("DRAFT", "PUBLISHED");
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
        
        attachmentsList.add(new CreateAnnouncementController.AttachmentRow(fileName, filePath));
        attachmentNameField.clear();
        attachmentPathField.clear();
    }
    
    @FXML
    private void handleRemoveAttachment() {
        CreateAnnouncementController.AttachmentRow selected = attachmentsTable.getSelectionModel().getSelectedItem();
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
        
        linksList.add(new CreateAnnouncementController.LinkRow(linkText, linkUrl));
        linkTextField.clear();
        linkUrlField.clear();
    }
    
    @FXML
    private void handleRemoveLink() {
        CreateAnnouncementController.LinkRow selected = linksTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            linksList.remove(selected);
        } else {
            showWarning("No Selection", "Please select a link to remove.");
        }
    }
    
    @FXML
    private void handleSave() {
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
        String status = statusComboBox.getValue();
        
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
            
            boolean updated = announcementService.updateAnnouncement(
                announcementId, title, content, targetRole, priority, status, 
                currentUser, publishDate, expiryDate);
            
            if (updated) {
                // Update attachments and links (simplified - in production, track which are new/removed)
                // For now, we'll just add new ones. Full implementation would track deletions.
                for (CreateAnnouncementController.AttachmentRow attachment : attachmentsList) {
                    // Check if it's a new attachment (not in original list)
                    boolean isNew = true;
                    for (Announcement.AnnouncementAttachment orig : currentAnnouncement.getAttachments()) {
                        if (orig.getFileName().equals(attachment.getFileName()) && 
                            orig.getFilePath().equals(attachment.getFilePath())) {
                            isNew = false;
                            break;
                        }
                    }
                    if (isNew) {
                        announcementService.addAttachment(
                            announcementId, attachment.getFileName(), 
                            attachment.getFilePath(), null, null);
                    }
                }
                
                for (CreateAnnouncementController.LinkRow link : linksList) {
                    boolean isNew = true;
                    for (Announcement.AnnouncementLink orig : currentAnnouncement.getLinks()) {
                        if (orig.getLinkText().equals(link.getLinkText()) && 
                            orig.getLinkUrl().equals(link.getLinkUrl())) {
                            isNew = false;
                            break;
                        }
                    }
                    if (isNew) {
                        announcementService.addLink(
                            announcementId, link.getLinkText(), link.getLinkUrl());
                    }
                }
                
                showInfo("Success", "Announcement updated successfully!");
                handleCancel(null);
            } else {
                showError("Error", "Failed to update announcement.");
            }
        } catch (SQLException e) {
            showError("Database Error", "Failed to update announcement: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleViewHistory() {
        try {
            List<java.util.Map<String, Object>> history = announcementService.getEditHistory(announcementId);
            
            StringBuilder historyText = new StringBuilder("Edit History:\n\n");
            if (history.isEmpty()) {
                historyText.append("No edit history available.");
            } else {
                for (java.util.Map<String, Object> entry : history) {
                    historyText.append("Edited by: ").append(entry.get("editedByUsername")).append("\n");
                    historyText.append("Date: ").append(entry.get("editDate")).append("\n");
                    historyText.append("Previous Title: ").append(entry.get("previousTitle")).append("\n");
                    historyText.append("Previous Content: ").append(entry.get("previousContent")).append("\n\n");
                }
            }
            
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Edit History");
            alert.setHeaderText(null);
            alert.setContentText(historyText.toString());
            alert.showAndWait();
        } catch (SQLException e) {
            showError("Database Error", "Failed to load edit history: " + e.getMessage());
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
    
    private void disableAllControls() {
        if (titleField != null) titleField.setDisable(true);
        if (contentArea != null) contentArea.setDisable(true);
        if (saveButton != null) saveButton.setDisable(true);
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

