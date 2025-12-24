package edu.community.service;

import edu.community.model.Message;
import edu.facilities.model.User;
import edu.facilities.service.DatabaseConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MessageService {

    public boolean sendMessage(Message message) {
        String sql = "INSERT INTO Messages (SenderUserID, ReceiverUserID, Subject, MessageBody, SentDate, IsRead, ParentMessageID) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, message.getSenderUserID());
            pstmt.setInt(2, message.getReceiverUserID());
            pstmt.setString(3, message.getSubject());
            pstmt.setString(4, message.getMessageBody());
            pstmt.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
            pstmt.setBoolean(6, false);
            if (message.getParentMessageID() != null) {
                pstmt.setInt(7, message.getParentMessageID());
            } else {
                pstmt.setNull(7, Types.INTEGER);
            }
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<Message> getInboxMessages(int userID) {
        List<Message> messages = new ArrayList<>();
        String sql = "SELECT m.*, u.username as SenderName FROM Messages m JOIN Users u ON m.SenderUserID = u.id WHERE m.ReceiverUserID = ? ORDER BY m.SentDate DESC";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userID);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                messages.add(mapResultSetToMessage(rs, true));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return messages;
    }

    public List<Message> getSentMessages(int userID) {
        List<Message> messages = new ArrayList<>();
        String sql = "SELECT m.*, u.username as ReceiverName FROM Messages m JOIN Users u ON m.ReceiverUserID = u.id WHERE m.SenderUserID = ? ORDER BY m.SentDate DESC";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userID);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                messages.add(mapResultSetToMessage(rs, false));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return messages;
    }

    public void markAsRead(int messageID) {
        String sql = "UPDATE Messages SET IsRead = 1 WHERE MessageID = ?";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, messageID);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public int getUnreadCount(int userID) throws SQLException {
        String sql = "SELECT COUNT(*) FROM Messages WHERE ReceiverUserID = ? AND IsRead = 0";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userID);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    public Message getMessageById(int messageID) {
        String sql = "SELECT m.*, s.username as SenderName, r.username as ReceiverName FROM Messages m " +
                "JOIN Users s ON m.SenderUserID = s.id " +
                "JOIN Users r ON m.ReceiverUserID = r.id WHERE m.MessageID = ?";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, messageID);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                Message m = mapResultSetToMessage(rs, true);
                m.setReceiverName(rs.getString("ReceiverName"));
                return m;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<User> getAllStaffMembers() {
        List<User> staff = new ArrayList<>();
        String sql = "SELECT id, username, user_type FROM Users WHERE user_type IN ('STAFF', 'PROFESSOR', 'ADMIN')";
        try (Connection conn = DatabaseConnection.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                final String id = rs.getString("id");
                final String username = rs.getString("username");
                final String userType = rs.getString("user_type");

                User user = new User(id, username, null) {
                    @Override
                    public String getUserType() {
                        return userType;
                    }
                };
                staff.add(user);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return staff;
    }

    public boolean replyToMessage(Message reply) {
        if (reply.getSubject() != null && !reply.getSubject().startsWith("Re: ")) {
            reply.setSubject("Re: " + reply.getSubject());
        }
        return sendMessage(reply);
    }

    private Message mapResultSetToMessage(ResultSet rs, boolean isInbox) throws SQLException {
        Message m = new Message();
        m.setMessageID(rs.getInt("MessageID"));
        m.setSenderUserID(rs.getInt("SenderUserID"));
        m.setReceiverUserID(rs.getInt("ReceiverUserID"));
        m.setSubject(rs.getString("Subject"));
        m.setMessageBody(rs.getString("MessageBody"));
        m.setSentDate(rs.getTimestamp("SentDate").toLocalDateTime());
        m.setRead(rs.getBoolean("IsRead"));
        int parentID = rs.getInt("ParentMessageID");
        if (!rs.wasNull()) {
            m.setParentMessageID(parentID);
        }

        if (isInbox) {
            m.setSenderName(rs.getString("SenderName"));
        } else {
            m.setReceiverName(rs.getString("ReceiverName"));
        }

        return m;
    }
}
