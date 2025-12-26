package edu.community.service;

import edu.community.model.Message;
import edu.community.model.MessageThread;
import edu.facilities.service.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for parent-teacher messaging.
 * US 4.1 - Send Message to Teacher
 * US 4.2 - Reply to Parent Message
 * US 4.3 - View Parent-Teacher History
 */
public class ParentTeacherMessageService {

    /**
     * Create or get a message thread between parent and teacher about a student
     */
    public int createOrGetThread(int parentUserID, int teacherUserID, int studentUserID, String subject) throws SQLException {
        // Check if thread already exists
        String checkSql = "SELECT ThreadID FROM MessageThreads " +
                         "WHERE ParentUserID = ? AND TeacherUserID = ? AND StudentUserID = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(checkSql)) {
            
            pstmt.setInt(1, parentUserID);
            pstmt.setInt(2, teacherUserID);
            pstmt.setInt(3, studentUserID);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("ThreadID");
                }
            }
        }

        // Create new thread
        String insertSql = "INSERT INTO MessageThreads (ParentUserID, TeacherUserID, StudentUserID, Subject, CreatedDate, LastMessageDate) " +
                          "VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP) RETURNING ThreadID";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
            
            pstmt.setInt(1, parentUserID);
            pstmt.setInt(2, teacherUserID);
            pstmt.setInt(3, studentUserID);
            pstmt.setString(4, subject);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("ThreadID");
                }
            }
        }
        
        throw new SQLException("Failed to create message thread");
    }

    /**
     * Send a message from parent to teacher (US 4.1)
     */
    public boolean sendParentMessage(int parentUserID, int teacherUserID, int studentUserID, 
                                    String subject, String messageBody) throws SQLException {
        // Verify parent-student relationship for security
        if (!verifyParentStudentRelationship(parentUserID, studentUserID)) {
            throw new SQLException("Parent is not authorized to message about this student");
        }
        
        int threadID = createOrGetThread(parentUserID, teacherUserID, studentUserID, subject);
        
        String sql = "INSERT INTO Messages (SenderUserID, ReceiverUserID, Subject, MessageBody, " +
                     "SentDate, IsRead, ThreadID, MessageType) " +
                     "VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, FALSE, ?, 'PARENT_TEACHER')";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, parentUserID);
            pstmt.setInt(2, teacherUserID);
            pstmt.setString(3, subject);
            pstmt.setString(4, messageBody);
            pstmt.setInt(5, threadID);
            
            boolean success = pstmt.executeUpdate() > 0;
            
            if (success) {
                // Update thread's last message date
                updateThreadLastMessageDate(threadID);
            }
            
            return success;
        }
    }

    /**
     * Reply to a parent message (US 4.2)
     */
    public boolean replyToParentMessage(int teacherUserID, int messageID, String replyContent) throws SQLException {
        // Get the original message
        Message originalMessage = getMessageById(messageID);
        if (originalMessage == null) {
            throw new SQLException("Original message not found");
        }

        // Create reply message
        String replySubject = originalMessage.getSubject().startsWith("Re: ") 
                            ? originalMessage.getSubject() 
                            : "Re: " + originalMessage.getSubject();
        
        String sql = "INSERT INTO Messages (SenderUserID, ReceiverUserID, Subject, MessageBody, " +
                     "SentDate, IsRead, ParentMessageID, ThreadID, MessageType) " +
                     "VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, FALSE, ?, ?, 'PARENT_TEACHER')";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, teacherUserID);
            pstmt.setInt(2, originalMessage.getSenderUserID());
            pstmt.setString(3, replySubject);
            pstmt.setString(4, replyContent);
            pstmt.setInt(5, messageID);
            pstmt.setInt(6, originalMessage.getThreadID() != null ? originalMessage.getThreadID() : 0);
            
            boolean success = pstmt.executeUpdate() > 0;
            
            if (success && originalMessage.getThreadID() != null) {
                updateThreadLastMessageDate(originalMessage.getThreadID());
            }
            
            return success;
        }
    }

    /**
     * Get message thread history (US 4.3)
     */
    public List<Message> getThreadHistory(int threadID, int userID) throws SQLException {
        // Verify user has access to this thread
        if (!hasAccessToThread(threadID, userID)) {
            throw new SQLException("Access denied to this thread");
        }

        String sql = "SELECT m.*, " +
                    "s.USERNAME as SenderName, r.USERNAME as ReceiverName " +
                    "FROM Messages m " +
                    "LEFT JOIN Users s ON m.SenderUserID = s.UserID " +
                    "LEFT JOIN Users r ON m.ReceiverUserID = r.UserID " +
                    "WHERE m.ThreadID = ? " +
                    "ORDER BY m.SentDate ASC";
        
        List<Message> messages = new ArrayList<>();
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, threadID);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    messages.add(mapResultSetToMessage(rs));
                }
            }
        }
        
        return messages;
    }

    /**
     * Get all threads for a parent
     */
    public List<MessageThread> getParentThreads(int parentUserID) throws SQLException {
        String sql = "SELECT mt.*, " +
                    "p.USERNAME as ParentName, t.USERNAME as TeacherName, s.USERNAME as StudentName, " +
                    "COUNT(m.MessageID) as MessageCount " +
                    "FROM MessageThreads mt " +
                    "LEFT JOIN Users p ON mt.ParentUserID = p.UserID " +
                    "LEFT JOIN Users t ON mt.TeacherUserID = t.UserID " +
                    "LEFT JOIN Users s ON mt.StudentUserID = s.UserID " +
                    "LEFT JOIN Messages m ON mt.ThreadID = m.ThreadID " +
                    "WHERE mt.ParentUserID = ? " +
                    "GROUP BY mt.ThreadID, p.USERNAME, t.USERNAME, s.USERNAME " +
                    "ORDER BY mt.LastMessageDate DESC";
        
        List<MessageThread> threads = new ArrayList<>();
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, parentUserID);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    threads.add(mapResultSetToThread(rs));
                }
            }
        }
        
        return threads;
    }

    /**
     * Get all threads for a teacher
     */
    public List<MessageThread> getTeacherThreads(int teacherUserID) throws SQLException {
        String sql = "SELECT mt.*, " +
                    "p.USERNAME as ParentName, t.USERNAME as TeacherName, s.USERNAME as StudentName, " +
                    "COUNT(m.MessageID) as MessageCount " +
                    "FROM MessageThreads mt " +
                    "LEFT JOIN Users p ON mt.ParentUserID = p.UserID " +
                    "LEFT JOIN Users t ON mt.TeacherUserID = t.UserID " +
                    "LEFT JOIN Users s ON mt.StudentUserID = s.UserID " +
                    "LEFT JOIN Messages m ON mt.ThreadID = m.ThreadID " +
                    "WHERE mt.TeacherUserID = ? " +
                    "GROUP BY mt.ThreadID, p.USERNAME, t.USERNAME, s.USERNAME " +
                    "ORDER BY mt.LastMessageDate DESC";
        
        List<MessageThread> threads = new ArrayList<>();
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, teacherUserID);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    threads.add(mapResultSetToThread(rs));
                }
            }
        }
        
        return threads;
    }

    /**
     * Get students linked to a parent (for parent to select their child)
     */
    public List<edu.facilities.model.User> getParentStudents(int parentUserID) throws SQLException {
        String sql = "SELECT DISTINCT u.UserID, u.USERNAME, u.Email, ut.TypeCode as UserType, " +
                    "s.StudentNumber, s.Major, d.Name as Department " +
                    "FROM Users u " +
                    "INNER JOIN UserRoles ur ON u.UserID = ur.UserID AND ur.IsPrimary = true " +
                    "INNER JOIN UserTypes ut ON ur.UserTypeID = ut.UserTypeID " +
                    "INNER JOIN Students s ON u.UserID = s.UserID " +
                    "LEFT JOIN Departments d ON s.DepartmentID = d.DepartmentID " +
                    "INNER JOIN StudentParentRelationship spr ON u.UserID = spr.StudentUserID " +
                    "WHERE spr.ParentUserID = ? " +
                    "ORDER BY u.USERNAME";
        
        List<edu.facilities.model.User> students = new ArrayList<>();
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, parentUserID);
            
            try (ResultSet rs = pstmt.executeQuery()) {
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
                    students.add(user);
                }
            }
        }
        
        return students;
    }

    /**
     * Link a student to a parent (parent self-registration)
     * @param parentUserID The parent's user ID
     * @param studentUsernameOrID The student's username or student ID
     * @return true if successfully linked, false if student not found or already linked
     * @throws SQLException if database error occurs
     */
    public boolean linkStudentToParent(int parentUserID, String studentUsernameOrID) throws SQLException {
        // First, find the student by username or student ID
        String findStudentSql = "SELECT u.UserID, s.StudentNumber " +
                               "FROM Users u " +
                               "INNER JOIN UserRoles ur ON u.UserID = ur.UserID AND ur.IsPrimary = true " +
                               "INNER JOIN UserTypes ut ON ur.UserTypeID = ut.UserTypeID " +
                               "LEFT JOIN Students s ON u.UserID = s.UserID " +
                               "WHERE (u.USERNAME = ? OR s.StudentNumber = ?) " +
                               "AND ut.TypeCode = 'STUDENT'";
        
        Integer studentUserID = null;
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(findStudentSql)) {
            
            pstmt.setString(1, studentUsernameOrID);
            pstmt.setString(2, studentUsernameOrID);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    studentUserID = rs.getInt("UserID");
                } else {
                    return false; // Student not found
                }
            }
        }
        
        // Check if relationship already exists
        if (verifyParentStudentRelationship(parentUserID, studentUserID)) {
            return false; // Already linked
        }
        
        // Create the relationship
        String insertSql = "INSERT INTO StudentParentRelationship (StudentUserID, ParentUserID, RelationshipType, IsPrimary, CreatedDate) " +
                          "VALUES (?, ?, 'PARENT', FALSE, CURRENT_TIMESTAMP)";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
            
            pstmt.setInt(1, studentUserID);
            pstmt.setInt(2, parentUserID);
            
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * Verify that a parent is linked to a specific student
     */
    public boolean verifyParentStudentRelationship(int parentUserID, int studentUserID) throws SQLException {
        String sql = "SELECT COUNT(*) as count FROM StudentParentRelationship " +
                    "WHERE ParentUserID = ? AND StudentUserID = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, parentUserID);
            pstmt.setInt(2, studentUserID);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("count") > 0;
                }
            }
        }
        
        return false;
    }

    /**
     * Get teachers for a student (for parent to select)
     */
    public List<edu.facilities.model.User> getStudentTeachers(int studentUserID) throws SQLException {
        String sql = "SELECT DISTINCT u.UserID, u.USERNAME, u.Email, ut.TypeCode as UserType " +
                    "FROM Users u " +
                    "INNER JOIN UserRoles ur ON u.UserID = ur.UserID AND ur.IsPrimary = true " +
                    "INNER JOIN UserTypes ut ON ur.UserTypeID = ut.UserTypeID " +
                    "INNER JOIN CourseProfessors cp ON u.UserID = cp.ProfessorUserID " +
                    "INNER JOIN Enrollments e ON cp.CourseID = e.CourseID " +
                    "LEFT JOIN StatusTypes st ON e.StatusTypeID = st.StatusTypeID AND st.EntityType = 'ENROLLMENT' " +
                    "WHERE e.StudentUserID = ? AND st.StatusCode = 'ENROLLED' " +
                    "ORDER BY u.USERNAME";
        
        List<edu.facilities.model.User> teachers = new ArrayList<>();
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, studentUserID);
            
            try (ResultSet rs = pstmt.executeQuery()) {
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
                    teachers.add(user);
                }
            }
        }
        
        return teachers;
    }

    private void updateThreadLastMessageDate(int threadID) throws SQLException {
        String sql = "UPDATE MessageThreads SET LastMessageDate = CURRENT_TIMESTAMP WHERE ThreadID = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, threadID);
            pstmt.executeUpdate();
        }
    }

    private boolean hasAccessToThread(int threadID, int userID) throws SQLException {
        String sql = "SELECT COUNT(*) FROM MessageThreads " +
                    "WHERE ThreadID = ? AND (ParentUserID = ? OR TeacherUserID = ?)";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, threadID);
            pstmt.setInt(2, userID);
            pstmt.setInt(3, userID);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        
        return false;
    }

    public Message getMessageById(int messageID) throws SQLException {
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

    private MessageThread mapResultSetToThread(ResultSet rs) throws SQLException {
        MessageThread thread = new MessageThread();
        thread.setThreadID(rs.getInt("ThreadID"));
        thread.setParentUserID(rs.getInt("ParentUserID"));
        thread.setTeacherUserID(rs.getInt("TeacherUserID"));
        thread.setStudentUserID(rs.getInt("StudentUserID"));
        thread.setParentName(rs.getString("ParentName"));
        thread.setTeacherName(rs.getString("TeacherName"));
        thread.setStudentName(rs.getString("StudentName"));
        thread.setSubject(rs.getString("Subject"));
        
        Timestamp createdDate = rs.getTimestamp("CreatedDate");
        if (createdDate != null) {
            thread.setCreatedDate(createdDate.toLocalDateTime());
        }
        
        Timestamp lastMessageDate = rs.getTimestamp("LastMessageDate");
        if (lastMessageDate != null) {
            thread.setLastMessageDate(lastMessageDate.toLocalDateTime());
        }
        
        thread.setMessageCount(rs.getInt("MessageCount"));
        
        return thread;
    }
}

