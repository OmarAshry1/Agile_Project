package edu.facilities.service;

import edu.facilities.model.Announcement;
import edu.facilities.model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for managing announcements
 * US 3.1-3.4, US 4.4 - Announcements Management
 */
public class AnnouncementService {
    
    /**
     * Get all announcements visible to a user (filtered by role)
     */
    public List<Announcement> getAnnouncementsForUser(User user, boolean includeRead) throws SQLException {
        String userType = user.getUserType();
        
        String sql = "SELECT a.AnnouncementID, a.Title, a.Content, a.TargetRole, a.CreatedByUserID, " +
                    "a.CreatedDate, a.PublishDate, a.ExpiryDate, a.IsActive, a.IsArchived, " +
                    "a.Status, a.Priority, a.LastModifiedDate, a.LastModifiedByUserID " +
                    "FROM Announcements a " +
                    "WHERE a.IsActive = 1 " +
                    "AND a.IsArchived = 0 " +
                    "AND a.Status = 'PUBLISHED' " +
                    "AND (a.TargetRole IS NULL OR a.TargetRole = ?) " +
                    "AND (a.ExpiryDate IS NULL OR a.ExpiryDate >= GETDATE()) " +
                    "ORDER BY " +
                    "CASE a.Priority " +
                    "  WHEN 'URGENT' THEN 1 " +
                    "  WHEN 'HIGH' THEN 2 " +
                    "  WHEN 'NORMAL' THEN 3 " +
                    "  WHEN 'LOW' THEN 4 " +
                    "END, " +
                    "a.PublishDate DESC";
        
        List<Announcement> announcements = new ArrayList<>();
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, userType);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Announcement announcement = mapResultSetToAnnouncement(rs);
                    loadAnnouncementRelations(announcement, conn);
                    
                    // Check if user has read this announcement
                    if (includeRead || !hasUserReadAnnouncement(user, announcement.getId(), conn)) {
                        announcement.setRead(hasUserReadAnnouncement(user, announcement.getId(), conn));
                        announcements.add(announcement);
                    }
                }
            }
        }
        
        return announcements;
    }
    
    /**
     * Get all announcements for admin/staff (including drafts and archived)
     */
    public List<Announcement> getAllAnnouncementsForAdmin(boolean includeArchived) throws SQLException {
        String sql = "SELECT a.AnnouncementID, a.Title, a.Content, a.TargetRole, a.CreatedByUserID, " +
                    "a.CreatedDate, a.PublishDate, a.ExpiryDate, a.IsActive, a.IsArchived, " +
                    "a.Status, a.Priority, a.LastModifiedDate, a.LastModifiedByUserID " +
                    "FROM Announcements a " +
                    "WHERE a.IsActive = 1 " +
                    (includeArchived ? "" : "AND a.IsArchived = 0 ") +
                    "ORDER BY a.CreatedDate DESC";
        
        List<Announcement> announcements = new ArrayList<>();
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Announcement announcement = mapResultSetToAnnouncement(rs);
                    loadAnnouncementRelations(announcement, conn);
                    announcements.add(announcement);
                }
            }
        }
        
        return announcements;
    }
    
    /**
     * Get unread announcements count for a user
     */
    public int getUnreadCount(User user) throws SQLException {
        String userType = user.getUserType();
        
        String sql = "SELECT COUNT(*) AS UnreadCount " +
                    "FROM Announcements a " +
                    "WHERE a.IsActive = 1 " +
                    "AND (a.TargetRole IS NULL OR a.TargetRole = ?) " +
                    "AND (a.ExpiryDate IS NULL OR a.ExpiryDate >= GETDATE()) " +
                    "AND NOT EXISTS ( " +
                    "  SELECT 1 FROM AnnouncementReadStatus ars " +
                    "  WHERE ars.AnnouncementID = a.AnnouncementID " +
                    "  AND ars.UserID = ? " +
                    ")";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, userType);
            pstmt.setInt(2, Integer.parseInt(user.getId()));
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("UnreadCount");
                }
            }
        }
        
        return 0;
    }
    
    /**
     * Mark an announcement as read for a user
     */
    public boolean markAsRead(User user, String announcementId) throws SQLException {
        // Check if already read
        if (hasUserReadAnnouncement(user, announcementId, null)) {
            return true; // Already marked as read
        }
        
        String sql = "INSERT INTO AnnouncementReadStatus (AnnouncementID, UserID, ReadDate) " +
                    "VALUES (?, ?, GETDATE())";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, Integer.parseInt(announcementId));
            pstmt.setInt(2, Integer.parseInt(user.getId()));
            
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            if (e.getErrorCode() == 2627 || e.getMessage().contains("UNIQUE")) {
                // Already read - not an error
                return true;
            }
            throw e;
        }
    }
    
    /**
     * Create a new announcement (US 3.1)
     * @param title Required
     * @param content Required
     * @param targetRole ALL, STUDENT, PROFESSOR, STAFF, ADMIN, or null for all
     * @param createdBy User creating the announcement
     * @param priority LOW, NORMAL, HIGH, URGENT
     * @param status DRAFT or PUBLISHED
     * @param publishDate Date to publish (null for immediate publish if status is PUBLISHED)
     * @param expiryDate Optional expiry date
     * @return Created announcement
     */
    public Announcement createAnnouncement(String title, String content, String targetRole,
                                          User createdBy, String priority, String status,
                                          java.time.LocalDateTime publishDate, java.time.LocalDateTime expiryDate) throws SQLException {
        Connection conn = DatabaseConnection.getConnection();
        boolean originalAutoCommit = conn.getAutoCommit();
        
        try {
            conn.setAutoCommit(false);
            
            String sql = "INSERT INTO Announcements (Title, Content, TargetRole, CreatedByUserID, Priority, " +
                        "Status, PublishDate, ExpiryDate, CreatedDate, IsActive, IsArchived) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, GETDATE(), 1, 0)";
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setString(1, title);
                pstmt.setString(2, content);
                if (targetRole == null || targetRole.isEmpty() || "ALL".equals(targetRole)) {
                    pstmt.setNull(3, Types.VARCHAR);
                } else {
                    pstmt.setString(3, targetRole);
                }
                pstmt.setInt(4, Integer.parseInt(createdBy.getId()));
                pstmt.setString(5, priority != null ? priority : "NORMAL");
                pstmt.setString(6, status != null ? status : "DRAFT");
                
                if (publishDate != null) {
                    pstmt.setTimestamp(7, Timestamp.valueOf(publishDate));
                } else if ("PUBLISHED".equals(status)) {
                    pstmt.setTimestamp(7, Timestamp.valueOf(java.time.LocalDateTime.now()));
                } else {
                    pstmt.setNull(7, Types.TIMESTAMP);
                }
                
                if (expiryDate != null) {
                    pstmt.setTimestamp(8, Timestamp.valueOf(expiryDate));
                } else {
                    pstmt.setNull(8, Types.TIMESTAMP);
                }
                
                int rowsAffected = pstmt.executeUpdate();
                
                if (rowsAffected > 0) {
                    try (ResultSet keys = pstmt.getGeneratedKeys()) {
                        if (keys.next()) {
                            int announcementId = keys.getInt(1);
                            conn.commit();
                            return getAnnouncementById(String.valueOf(announcementId));
                        }
                    }
                }
            }
            
            conn.rollback();
            return null;
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(originalAutoCommit);
            conn.close();
        }
    }
    
    /**
     * Update an announcement (US 3.2)
     */
    public boolean updateAnnouncement(String announcementId, String title, String content, String targetRole,
                                     String priority, String status, User modifiedBy,
                                     java.time.LocalDateTime publishDate, java.time.LocalDateTime expiryDate) throws SQLException {
        Connection conn = DatabaseConnection.getConnection();
        boolean originalAutoCommit = conn.getAutoCommit();
        
        try {
            conn.setAutoCommit(false);
            
            // Get current values for edit history
            Announcement current = getAnnouncementById(announcementId);
            if (current == null) {
                return false;
            }
            
            // Save edit history
            String historySql = "INSERT INTO AnnouncementEditHistory (AnnouncementID, EditedByUserID, EditDate, " +
                              "PreviousTitle, PreviousContent, PreviousTargetRole) " +
                              "VALUES (?, ?, GETDATE(), ?, ?, ?)";
            
            try (PreparedStatement historyPstmt = conn.prepareStatement(historySql)) {
                historyPstmt.setInt(1, Integer.parseInt(announcementId));
                historyPstmt.setInt(2, Integer.parseInt(modifiedBy.getId()));
                historyPstmt.setString(3, current.getTitle());
                historyPstmt.setString(4, current.getContent());
                historyPstmt.setString(5, current.getTargetRole());
                historyPstmt.executeUpdate();
            }
            
            // Update announcement
            String sql = "UPDATE Announcements SET Title = ?, Content = ?, TargetRole = ?, Priority = ?, " +
                        "Status = ?, PublishDate = ?, ExpiryDate = ?, LastModifiedDate = GETDATE(), " +
                        "LastModifiedByUserID = ? WHERE AnnouncementID = ?";
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, title);
                pstmt.setString(2, content);
                if (targetRole == null || targetRole.isEmpty() || "ALL".equals(targetRole)) {
                    pstmt.setNull(3, Types.VARCHAR);
                } else {
                    pstmt.setString(3, targetRole);
                }
                pstmt.setString(4, priority != null ? priority : "NORMAL");
                pstmt.setString(5, status != null ? status : current.getStatus());
                
                if (publishDate != null) {
                    pstmt.setTimestamp(6, Timestamp.valueOf(publishDate));
                } else if ("PUBLISHED".equals(status) && current.getPublishDate() == null) {
                    pstmt.setTimestamp(6, Timestamp.valueOf(java.time.LocalDateTime.now()));
                } else {
                    pstmt.setTimestamp(6, current.getPublishDate() != null ? 
                                      Timestamp.valueOf(current.getPublishDate()) : null);
                }
                
                if (expiryDate != null) {
                    pstmt.setTimestamp(7, Timestamp.valueOf(expiryDate));
                } else {
                    pstmt.setTimestamp(7, current.getExpiryDate() != null ? 
                                      Timestamp.valueOf(current.getExpiryDate()) : null);
                }
                pstmt.setInt(8, Integer.parseInt(modifiedBy.getId()));
                pstmt.setInt(9, Integer.parseInt(announcementId));
                
                int rowsAffected = pstmt.executeUpdate();
                conn.commit();
                return rowsAffected > 0;
            }
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(originalAutoCommit);
            conn.close();
        }
    }
    
    /**
     * Archive an announcement (US 3.3)
     */
    public boolean archiveAnnouncement(String announcementId) throws SQLException {
        String sql = "UPDATE Announcements SET IsArchived = 1 WHERE AnnouncementID = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, Integer.parseInt(announcementId));
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        }
    }
    
    /**
     * Permanently delete an announcement (US 3.3)
     */
    public boolean deleteAnnouncement(String announcementId) throws SQLException {
        String sql = "UPDATE Announcements SET IsActive = 0 WHERE AnnouncementID = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, Integer.parseInt(announcementId));
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        }
    }
    
    /**
     * Add attachment to announcement
     */
    public boolean addAttachment(String announcementId, String fileName, String filePath, Long fileSize, String mimeType) throws SQLException {
        String sql = "INSERT INTO AnnouncementAttachments (AnnouncementID, FileName, FilePath, FileSize, MimeType, UploadedDate) " +
                    "VALUES (?, ?, ?, ?, ?, GETDATE())";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, Integer.parseInt(announcementId));
            pstmt.setString(2, fileName);
            pstmt.setString(3, filePath);
            if (fileSize != null) {
                pstmt.setLong(4, fileSize);
            } else {
                pstmt.setNull(4, Types.BIGINT);
            }
            pstmt.setString(5, mimeType);
            
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        }
    }
    
    /**
     * Add link to announcement
     */
    public boolean addLink(String announcementId, String linkText, String linkUrl) throws SQLException {
        String sql = "INSERT INTO AnnouncementLinks (AnnouncementID, LinkText, LinkURL, CreatedDate) " +
                    "VALUES (?, ?, ?, GETDATE())";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, Integer.parseInt(announcementId));
            pstmt.setString(2, linkText);
            pstmt.setString(3, linkUrl);
            
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        }
    }
    
    /**
     * Remove attachment
     */
    public boolean removeAttachment(String attachmentId) throws SQLException {
        String sql = "DELETE FROM AnnouncementAttachments WHERE AttachmentID = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, Integer.parseInt(attachmentId));
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        }
    }
    
    /**
     * Remove link
     */
    public boolean removeLink(String linkId) throws SQLException {
        String sql = "DELETE FROM AnnouncementLinks WHERE LinkID = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, Integer.parseInt(linkId));
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        }
    }
    
    /**
     * Load attachments and links for an announcement
     */
    private void loadAnnouncementRelations(Announcement announcement, Connection conn) throws SQLException {
        // Load attachments
        String attachmentsSql = "SELECT AttachmentID, FileName, FilePath, FileSize, MimeType, UploadedDate " +
                               "FROM AnnouncementAttachments WHERE AnnouncementID = ?";
        
        try (PreparedStatement pstmt = conn.prepareStatement(attachmentsSql)) {
            pstmt.setInt(1, Integer.parseInt(announcement.getId()));
            
            try (ResultSet rs = pstmt.executeQuery()) {
                List<Announcement.AnnouncementAttachment> attachments = new ArrayList<>();
                while (rs.next()) {
                    Announcement.AnnouncementAttachment attachment = new Announcement.AnnouncementAttachment();
                    attachment.setId(String.valueOf(rs.getInt("AttachmentID")));
                    attachment.setFileName(rs.getString("FileName"));
                    attachment.setFilePath(rs.getString("FilePath"));
                    attachment.setFileSize(rs.getLong("FileSize"));
                    attachment.setMimeType(rs.getString("MimeType"));
                    Timestamp uploadedDate = rs.getTimestamp("UploadedDate");
                    if (uploadedDate != null) {
                        attachment.setUploadedDate(uploadedDate.toLocalDateTime());
                    }
                    attachments.add(attachment);
                }
                announcement.setAttachments(attachments);
            }
        }
        
        // Load links
        String linksSql = "SELECT LinkID, LinkText, LinkURL, CreatedDate " +
                         "FROM AnnouncementLinks WHERE AnnouncementID = ?";
        
        try (PreparedStatement pstmt = conn.prepareStatement(linksSql)) {
            pstmt.setInt(1, Integer.parseInt(announcement.getId()));
            
            try (ResultSet rs = pstmt.executeQuery()) {
                List<Announcement.AnnouncementLink> links = new ArrayList<>();
                while (rs.next()) {
                    Announcement.AnnouncementLink link = new Announcement.AnnouncementLink();
                    link.setId(String.valueOf(rs.getInt("LinkID")));
                    link.setLinkText(rs.getString("LinkText"));
                    link.setLinkUrl(rs.getString("LinkURL"));
                    Timestamp createdDate = rs.getTimestamp("CreatedDate");
                    if (createdDate != null) {
                        link.setCreatedDate(createdDate.toLocalDateTime());
                    }
                    links.add(link);
                }
                announcement.setLinks(links);
            }
        }
    }
    
    /**
     * Get announcement by ID
     */
    public Announcement getAnnouncementById(String announcementId) throws SQLException {
        String sql = "SELECT AnnouncementID, Title, Content, TargetRole, CreatedByUserID, " +
                    "CreatedDate, PublishDate, ExpiryDate, IsActive, IsArchived, " +
                    "Status, Priority, LastModifiedDate, LastModifiedByUserID " +
                    "FROM Announcements WHERE AnnouncementID = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, Integer.parseInt(announcementId));
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Announcement announcement = mapResultSetToAnnouncement(rs);
                    loadAnnouncementRelations(announcement, conn);
                    return announcement;
                }
            }
        }
        
        return null;
    }
    
    /**
     * Get edit history for an announcement
     */
    public List<java.util.Map<String, Object>> getEditHistory(String announcementId) throws SQLException {
        String sql = "SELECT eh.EditHistoryID, eh.EditedByUserID, eh.EditDate, " +
                    "eh.PreviousTitle, eh.PreviousContent, eh.PreviousTargetRole, " +
                    "u.Username " +
                    "FROM AnnouncementEditHistory eh " +
                    "INNER JOIN Users u ON eh.EditedByUserID = u.UserID " +
                    "WHERE eh.AnnouncementID = ? " +
                    "ORDER BY eh.EditDate DESC";
        
        List<java.util.Map<String, Object>> history = new ArrayList<>();
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, Integer.parseInt(announcementId));
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    java.util.Map<String, Object> entry = new java.util.HashMap<>();
                    entry.put("editId", rs.getInt("EditHistoryID"));
                    entry.put("editedByUserId", rs.getInt("EditedByUserID"));
                    entry.put("editedByUsername", rs.getString("Username"));
                    Timestamp editDate = rs.getTimestamp("EditDate");
                    if (editDate != null) {
                        entry.put("editDate", editDate.toLocalDateTime());
                    }
                    entry.put("previousTitle", rs.getString("PreviousTitle"));
                    entry.put("previousContent", rs.getString("PreviousContent"));
                    entry.put("previousTargetRole", rs.getString("PreviousTargetRole"));
                    history.add(entry);
                }
            }
        }
        
        return history;
    }
    
    /**
     * Check if user has read an announcement
     */
    private boolean hasUserReadAnnouncement(User user, String announcementId, Connection conn) throws SQLException {
        boolean shouldClose = (conn == null);
        if (conn == null) {
            conn = DatabaseConnection.getConnection();
        }
        
        try {
            String sql = "SELECT COUNT(*) AS ReadCount " +
                        "FROM AnnouncementReadStatus " +
                        "WHERE AnnouncementID = ? AND UserID = ?";
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, Integer.parseInt(announcementId));
                pstmt.setInt(2, Integer.parseInt(user.getId()));
                
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt("ReadCount") > 0;
                    }
                }
            }
        } finally {
            if (shouldClose && conn != null) {
                conn.close();
            }
        }
        
        return false;
    }
    
    /**
     * Map ResultSet to Announcement object
     */
    private Announcement mapResultSetToAnnouncement(ResultSet rs) throws SQLException {
        Announcement announcement = new Announcement();
        
        announcement.setId(String.valueOf(rs.getInt("AnnouncementID")));
        announcement.setTitle(rs.getString("Title"));
        announcement.setContent(rs.getString("Content"));
        announcement.setTargetRole(rs.getString("TargetRole"));
        announcement.setCreatedByUserId(String.valueOf(rs.getInt("CreatedByUserID")));
        
        Timestamp createdDate = rs.getTimestamp("CreatedDate");
        if (createdDate != null) {
            announcement.setCreatedDate(createdDate.toLocalDateTime());
        }
        
        Timestamp publishDate = rs.getTimestamp("PublishDate");
        if (publishDate != null) {
            announcement.setPublishDate(publishDate.toLocalDateTime());
        }
        
        Timestamp expiryDate = rs.getTimestamp("ExpiryDate");
        if (expiryDate != null) {
            announcement.setExpiryDate(expiryDate.toLocalDateTime());
        }
        
        Timestamp lastModifiedDate = rs.getTimestamp("LastModifiedDate");
        if (lastModifiedDate != null) {
            announcement.setLastModifiedDate(lastModifiedDate.toLocalDateTime());
        }
        
        if (rs.getMetaData().getColumnCount() > 10) {
            try {
                int lastModifiedByUserId = rs.getInt("LastModifiedByUserID");
                if (!rs.wasNull()) {
                    announcement.setLastModifiedByUserId(String.valueOf(lastModifiedByUserId));
                }
            } catch (SQLException e) {
                // Column might not exist in older schema
            }
        }
        
        announcement.setActive(rs.getBoolean("IsActive"));
        
        try {
            announcement.setArchived(rs.getBoolean("IsArchived"));
        } catch (SQLException e) {
            // Column might not exist in older schema
            announcement.setArchived(false);
        }
        
        try {
            announcement.setStatus(rs.getString("Status"));
        } catch (SQLException e) {
            // Column might not exist in older schema
            announcement.setStatus("PUBLISHED");
        }
        
        announcement.setPriority(rs.getString("Priority"));
        
        return announcement;
    }
}

