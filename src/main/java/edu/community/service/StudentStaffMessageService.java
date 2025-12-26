package edu.community.service;

import edu.community.model.Message;
import edu.facilities.service.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for student-staff messaging.
 * US 4.4 - Send Message to Staff
 * US 4.5 - Receive Student Messages
 */
public class StudentStaffMessageService {

    /**
     * Send a message from student to staff (US 4.4)
     */
    public boolean sendStudentMessage(int studentUserID, int staffUserID, String subject, String messageBody) throws SQLException {
        String sql = "INSERT INTO Messages (SenderUserID, ReceiverUserID, Subject, MessageBody, " +
                     "SentDate, IsRead, MessageType) " +
                     "VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, FALSE, 'STUDENT_STAFF')";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, studentUserID);
            pstmt.setInt(2, staffUserID);
            pstmt.setString(3, subject);
            pstmt.setString(4, messageBody);
            
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * Get all staff members for student to select (US 4.4)
     */
    public List<edu.facilities.model.User> getAllStaff() throws SQLException {
        String sql = "SELECT u.UserID, u.USERNAME, u.Email, ut.TypeCode as UserType " +
                    "FROM Users u " +
                    "INNER JOIN UserRoles ur ON u.UserID = ur.UserID AND ur.IsPrimary = true " +
                    "INNER JOIN UserTypes ut ON ur.UserTypeID = ut.UserTypeID " +
                    "WHERE ut.TypeCode IN ('STAFF', 'PROFESSOR', 'ADMIN') " +
                    "ORDER BY u.USERNAME";
        
        List<edu.facilities.model.User> staff = new ArrayList<>();
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                String userId = String.valueOf(rs.getInt("UserID"));
                String username = rs.getString("USERNAME");
                String email = rs.getString("Email");
                String userType = rs.getString("UserType");
                
                edu.facilities.model.User user = new edu.facilities.model.User(userId, username, email) {
                    @Override
                    public String getUserType() {
                        return userType;
                    }
                };
                staff.add(user);
            }
        }
        
        return staff;
    }

    /**
     * Get messages received by staff from students (US 4.5)
     */
    public List<Message> getStaffInbox(int staffUserID) throws SQLException {
        String sql = "SELECT m.*, " +
                    "s.USERNAME as SenderName, r.USERNAME as ReceiverName " +
                    "FROM Messages m " +
                    "LEFT JOIN Users s ON m.SenderUserID = s.UserID " +
                    "LEFT JOIN Users r ON m.ReceiverUserID = r.UserID " +
                    "WHERE m.ReceiverUserID = ? AND m.MessageType = 'STUDENT_STAFF' " +
                    "ORDER BY m.SentDate DESC";
        
        List<Message> messages = new ArrayList<>();
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, staffUserID);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    messages.add(mapResultSetToMessage(rs));
                }
            }
        }
        
        return messages;
    }

    /**
     * Reply to a student message
     */
    public boolean replyToStudentMessage(int staffUserID, int messageID, String replyContent) throws SQLException {
        // Get the original message
        Message originalMessage = getMessageById(messageID);
        if (originalMessage == null) {
            throw new SQLException("Original message not found");
        }

        String replySubject = originalMessage.getSubject().startsWith("Re: ") 
                            ? originalMessage.getSubject() 
                            : "Re: " + originalMessage.getSubject();
        
        String sql = "INSERT INTO Messages (SenderUserID, ReceiverUserID, Subject, MessageBody, " +
                     "SentDate, IsRead, ParentMessageID, MessageType) " +
                     "VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, FALSE, ?, 'STUDENT_STAFF')";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, staffUserID);
            pstmt.setInt(2, originalMessage.getSenderUserID());
            pstmt.setString(3, replySubject);
            pstmt.setString(4, replyContent);
            pstmt.setInt(5, messageID);
            
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * Mark message as read
     */
    public void markAsRead(int messageID) throws SQLException {
        String sql = "UPDATE Messages SET IsRead = TRUE WHERE MessageID = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, messageID);
            pstmt.executeUpdate();
        }
    }

    /**
     * Get unread message count for staff
     */
    public int getUnreadCount(int staffUserID) throws SQLException {
        String sql = "SELECT COUNT(*) FROM Messages " +
                    "WHERE ReceiverUserID = ? AND IsRead = FALSE AND MessageType = 'STUDENT_STAFF'";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, staffUserID);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        
        return 0;
    }

    private Message getMessageById(int messageID) throws SQLException {
        String sql = "SELECT m.*, " +
                    "s.USERNAME as SenderName, r.USERNAME as ReceiverName " +
                    "FROM Messages m " +
                    "LEFT JOIN Users s ON m.SenderUserID = s.UserID " +
                    "LEFT JOIN Users r ON m.ReceiverUserID = r.UserID " +
                    "WHERE m.MessageID = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, messageID);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToMessage(rs);
                }
            }
        }
        
        return null;
    }

    private Message mapResultSetToMessage(ResultSet rs) throws SQLException {
        Message m = new Message();
        m.setMessageID(rs.getInt("MessageID"));
        m.setSenderUserID(rs.getInt("SenderUserID"));
        m.setReceiverUserID(rs.getInt("ReceiverUserID"));
        m.setSubject(rs.getString("Subject"));
        m.setMessageBody(rs.getString("MessageBody"));
        
        Timestamp sentDate = rs.getTimestamp("SentDate");
        if (sentDate != null) {
            m.setSentDate(sentDate.toLocalDateTime());
        }
        
        m.setRead(rs.getBoolean("IsRead"));
        
        int parentID = rs.getInt("ParentMessageID");
        if (!rs.wasNull()) {
            m.setParentMessageID(parentID);
        }
        
        int threadID = rs.getInt("ThreadID");
        if (!rs.wasNull()) {
            m.setThreadID(threadID);
        }
        
        String messageType = rs.getString("MessageType");
        if (messageType != null) {
            m.setMessageType(messageType);
        }
        
        m.setSenderName(rs.getString("SenderName"));
        m.setReceiverName(rs.getString("ReceiverName"));
        
        return m;
    }
}

