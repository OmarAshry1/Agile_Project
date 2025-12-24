package edu.facilities.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Model representing an Announcement
 * US 3.1-3.4, US 4.4 - Announcements Management
 */
public class Announcement {
    private String id;
    private String title;
    private String content;
    private String targetRole;  // NULL = all users, or 'STUDENT', 'PROFESSOR', 'STAFF', 'ADMIN'
    private String createdByUserId;
    private LocalDateTime createdDate;
    private LocalDateTime publishDate;
    private LocalDateTime expiryDate;
    private LocalDateTime lastModifiedDate;
    private String lastModifiedByUserId;
    private boolean isActive;
    private boolean isArchived;
    private String status;  // DRAFT, PUBLISHED
    private String priority;  // LOW, NORMAL, HIGH, URGENT
    private boolean isRead;  // For current user
    private List<AnnouncementAttachment> attachments;
    private List<AnnouncementLink> links;
    
    public Announcement() {
        this.attachments = new ArrayList<>();
        this.links = new ArrayList<>();
    }
    
    public Announcement(String id, String title, String content, String targetRole,
                       String createdByUserId, LocalDateTime createdDate, LocalDateTime expiryDate,
                       boolean isActive, String priority) {
        this();
        this.id = id;
        this.title = title;
        this.content = content;
        this.targetRole = targetRole;
        this.createdByUserId = createdByUserId;
        this.createdDate = createdDate;
        this.expiryDate = expiryDate;
        this.isActive = isActive;
        this.priority = priority;
        this.isRead = false;
        this.status = "PUBLISHED";
        this.isArchived = false;
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public String getTargetRole() {
        return targetRole;
    }
    
    public void setTargetRole(String targetRole) {
        this.targetRole = targetRole;
    }
    
    public String getCreatedByUserId() {
        return createdByUserId;
    }
    
    public void setCreatedByUserId(String createdByUserId) {
        this.createdByUserId = createdByUserId;
    }
    
    public LocalDateTime getCreatedDate() {
        return createdDate;
    }
    
    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }
    
    public LocalDateTime getExpiryDate() {
        return expiryDate;
    }
    
    public void setExpiryDate(LocalDateTime expiryDate) {
        this.expiryDate = expiryDate;
    }
    
    public boolean isActive() {
        return isActive;
    }
    
    public void setActive(boolean active) {
        isActive = active;
    }
    
    public String getPriority() {
        return priority;
    }
    
    public void setPriority(String priority) {
        this.priority = priority;
    }
    
    public boolean isRead() {
        return isRead;
    }
    
    public void setRead(boolean read) {
        isRead = read;
    }
    
    public LocalDateTime getPublishDate() {
        return publishDate;
    }
    
    public void setPublishDate(LocalDateTime publishDate) {
        this.publishDate = publishDate;
    }
    
    public LocalDateTime getLastModifiedDate() {
        return lastModifiedDate;
    }
    
    public void setLastModifiedDate(LocalDateTime lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }
    
    public String getLastModifiedByUserId() {
        return lastModifiedByUserId;
    }
    
    public void setLastModifiedByUserId(String lastModifiedByUserId) {
        this.lastModifiedByUserId = lastModifiedByUserId;
    }
    
    public boolean isArchived() {
        return isArchived;
    }
    
    public void setArchived(boolean archived) {
        isArchived = archived;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public List<AnnouncementAttachment> getAttachments() {
        return attachments;
    }
    
    public void setAttachments(List<AnnouncementAttachment> attachments) {
        this.attachments = attachments;
    }
    
    public List<AnnouncementLink> getLinks() {
        return links;
    }
    
    public void setLinks(List<AnnouncementLink> links) {
        this.links = links;
    }
    
    public boolean isExpired() {
        return expiryDate != null && expiryDate.isBefore(LocalDateTime.now());
    }
    
    public boolean isPublished() {
        return "PUBLISHED".equals(status) && !isArchived && isActive;
    }
    
    public boolean isDraft() {
        return "DRAFT".equals(status);
    }
    
    @Override
    public String toString() {
        return title;
    }
    
    /**
     * Inner class for announcement attachments
     */
    public static class AnnouncementAttachment {
        private String id;
        private String fileName;
        private String filePath;
        private Long fileSize;
        private String mimeType;
        private LocalDateTime uploadedDate;
        
        public AnnouncementAttachment() {}
        
        public AnnouncementAttachment(String id, String fileName, String filePath, Long fileSize, String mimeType, LocalDateTime uploadedDate) {
            this.id = id;
            this.fileName = fileName;
            this.filePath = filePath;
            this.fileSize = fileSize;
            this.mimeType = mimeType;
            this.uploadedDate = uploadedDate;
        }
        
        // Getters and Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        public String getFilePath() { return filePath; }
        public void setFilePath(String filePath) { this.filePath = filePath; }
        public Long getFileSize() { return fileSize; }
        public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
        public String getMimeType() { return mimeType; }
        public void setMimeType(String mimeType) { this.mimeType = mimeType; }
        public LocalDateTime getUploadedDate() { return uploadedDate; }
        public void setUploadedDate(LocalDateTime uploadedDate) { this.uploadedDate = uploadedDate; }
    }
    
    /**
     * Inner class for announcement links
     */
    public static class AnnouncementLink {
        private String id;
        private String linkText;
        private String linkUrl;
        private LocalDateTime createdDate;
        
        public AnnouncementLink() {}
        
        public AnnouncementLink(String id, String linkText, String linkUrl, LocalDateTime createdDate) {
            this.id = id;
            this.linkText = linkText;
            this.linkUrl = linkUrl;
            this.createdDate = createdDate;
        }
        
        // Getters and Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getLinkText() { return linkText; }
        public void setLinkText(String linkText) { this.linkText = linkText; }
        public String getLinkUrl() { return linkUrl; }
        public void setLinkUrl(String linkUrl) { this.linkUrl = linkUrl; }
        public LocalDateTime getCreatedDate() { return createdDate; }
        public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }
    }
}

