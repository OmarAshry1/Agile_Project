package edu.curriculum.service;

import edu.curriculum.model.Assignment;
import edu.curriculum.model.Course;
import edu.curriculum.model.SubmissionType;
import edu.facilities.service.DatabaseConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for managing assignment data using SQL Server database
 */
public class AssignmentService {

    /**
     * Get assignments by course ID
     * @param courseId The course ID
     * @return List of assignments for the course
     * @throws SQLException if database error occurs
     */
    public List<Assignment> getAssignmentsByCourse(String courseId) throws SQLException {
        if (courseId == null || courseId.isBlank()) {
            return new ArrayList<>();
        }

        int courseIdInt;
        try {
            courseIdInt = Integer.parseInt(courseId);
        } catch (NumberFormatException e) {
            return new ArrayList<>();
        }

        List<Assignment> assignments = new ArrayList<>();
        String sql = "SELECT AssignmentID, CourseID, Title, Instructions, DueDate, TotalPoints, SubmissionType, CreatedDate " +
                     "FROM Assignments WHERE CourseID = ? ORDER BY DueDate";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, courseIdInt);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Assignment assignment = mapResultSetToAssignment(rs, conn);
                    if (assignment != null) {
                        assignments.add(assignment);
                    }
                }
            }
        }
        
        System.out.println("Retrieved " + assignments.size() + " assignments for course " + courseId);
        return assignments;
    }

    /**
     * Get assignment by ID
     * @param assignmentId The assignment ID
     * @return Assignment object if found, null otherwise
     * @throws SQLException if database error occurs
     */
    public Assignment getAssignmentById(String assignmentId) throws SQLException {
        if (assignmentId == null || assignmentId.isBlank()) {
            return null;
        }

        int assignmentIdInt;
        try {
            assignmentIdInt = Integer.parseInt(assignmentId);
        } catch (NumberFormatException e) {
            return null;
        }

        String sql = "SELECT AssignmentID, CourseID, Title, Instructions, DueDate, TotalPoints, SubmissionType, CreatedDate " +
                     "FROM Assignments WHERE AssignmentID = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, assignmentIdInt);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToAssignment(rs, conn);
                }
            }
        }
        return null;
    }

    /**
     * Create a new assignment
     * @param courseId The course ID
     * @param title Assignment title
     * @param instructions Assignment instructions
     * @param dueDate Due date
     * @param totalPoints Total points
     * @param submissionType Submission type
     * @param attributes EAV attributes map
     * @return Created Assignment object or null if failed
     * @throws SQLException if database error occurs
     */
    public Assignment createAssignment(String courseId, String title, String instructions,
                                       LocalDateTime dueDate, int totalPoints,
                                       SubmissionType submissionType, Map<String, String> attributes) throws SQLException {
        if (courseId == null || courseId.isBlank() || title == null || title.isBlank() ||
            instructions == null || instructions.isBlank() || dueDate == null) {
            return null;
        }

        int courseIdInt;
        try {
            courseIdInt = Integer.parseInt(courseId);
        } catch (NumberFormatException e) {
            return null;
        }

        Connection conn = DatabaseConnection.getConnection();
        boolean originalAutoCommit = conn.getAutoCommit();
        
        try {
            conn.setAutoCommit(false);
            
            // Insert assignment
            String sql = "INSERT INTO Assignments (CourseID, Title, Instructions, DueDate, TotalPoints, SubmissionType, CreatedDate) " +
                         "VALUES (?, ?, ?, ?, ?, ?, GETDATE())";
            
            int assignmentId;
            try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setInt(1, courseIdInt);
                pstmt.setString(2, title);
                pstmt.setString(3, instructions);
                pstmt.setTimestamp(4, Timestamp.valueOf(dueDate));
                pstmt.setInt(5, totalPoints);
                pstmt.setString(6, typeToString(submissionType));
                
                int rowsAffected = pstmt.executeUpdate();
                
                if (rowsAffected > 0) {
                    try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            assignmentId = generatedKeys.getInt(1);
                        } else {
                            conn.rollback();
                            return null;
                        }
                    }
                } else {
                    conn.rollback();
                    return null;
                }
            }
            
            // Insert EAV attributes
            if (attributes != null && !attributes.isEmpty()) {
                String attrSql = "INSERT INTO AssignmentAttributes (AssignmentID, AttributeName, AttributeValue) VALUES (?, ?, ?)";
                try (PreparedStatement attrStmt = conn.prepareStatement(attrSql)) {
                    for (Map.Entry<String, String> entry : attributes.entrySet()) {
                        attrStmt.setInt(1, assignmentId);
                        attrStmt.setString(2, entry.getKey());
                        attrStmt.setString(3, entry.getValue());
                        attrStmt.addBatch();
                    }
                    attrStmt.executeBatch();
                }
            }
            
            conn.commit();
            System.out.println("Assignment created with ID: " + assignmentId);
            return getAssignmentById(String.valueOf(assignmentId));
            
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(originalAutoCommit);
        }
    }

    /**
     * Delete an assignment
     * @param assignmentId The assignment ID
     * @return true if deleted, false otherwise
     * @throws SQLException if database error occurs
     */
    public boolean deleteAssignment(String assignmentId) throws SQLException {
        if (assignmentId == null || assignmentId.isBlank()) {
            return false;
        }

        int assignmentIdInt;
        try {
            assignmentIdInt = Integer.parseInt(assignmentId);
        } catch (NumberFormatException e) {
            return false;
        }

        Connection conn = DatabaseConnection.getConnection();
        boolean originalAutoCommit = conn.getAutoCommit();
        
        try {
            conn.setAutoCommit(false);
            
            // Delete attributes first
            String deleteAttrSql = "DELETE FROM AssignmentAttributes WHERE AssignmentID = ?";
            try (PreparedStatement attrStmt = conn.prepareStatement(deleteAttrSql)) {
                attrStmt.setInt(1, assignmentIdInt);
                attrStmt.executeUpdate();
            }
            
            // Delete submissions
            String deleteSubSql = "DELETE FROM AssignmentSubmissions WHERE AssignmentID = ?";
            try (PreparedStatement subStmt = conn.prepareStatement(deleteSubSql)) {
                subStmt.setInt(1, assignmentIdInt);
                subStmt.executeUpdate();
            }
            
            // Delete assignment
            String sql = "DELETE FROM Assignments WHERE AssignmentID = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, assignmentIdInt);
                int rowsAffected = pstmt.executeUpdate();
                
                if (rowsAffected > 0) {
                    conn.commit();
                    System.out.println("Assignment " + assignmentId + " deleted successfully");
                    return true;
                } else {
                    conn.rollback();
                    return false;
                }
            }
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(originalAutoCommit);
        }
    }

    /**
     * Get assignments for a student (all assignments from enrolled courses)
     * @param studentId The student user ID
     * @return List of assignments
     * @throws SQLException if database error occurs
     */
    public List<Assignment> getStudentAssignments(String studentId) throws SQLException {
        if (studentId == null || studentId.isBlank()) {
            return new ArrayList<>();
        }

        int studentIdInt;
        try {
            studentIdInt = Integer.parseInt(studentId);
        } catch (NumberFormatException e) {
            return new ArrayList<>();
        }

        List<Assignment> assignments = new ArrayList<>();
        String sql = "SELECT DISTINCT a.AssignmentID, a.CourseID, a.Title, a.Instructions, a.DueDate, a.TotalPoints, a.SubmissionType, a.CreatedDate " +
                     "FROM Assignments a " +
                     "INNER JOIN Courses c ON a.CourseID = c.CourseID " +
                     "INNER JOIN Enrollments e ON c.CourseID = e.CourseID " +
                     "WHERE e.StudentUserID = ? ORDER BY a.DueDate";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, studentIdInt);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Assignment assignment = mapResultSetToAssignment(rs, conn);
                    if (assignment != null) {
                        assignments.add(assignment);
                    }
                }
            }
        }
        
        System.out.println("Retrieved " + assignments.size() + " assignments for student " + studentId);
        return assignments;
    }

    /**
     * Map ResultSet row to Assignment object
     */
    private Assignment mapResultSetToAssignment(ResultSet rs, Connection conn) throws SQLException {
        String assignmentId = String.valueOf(rs.getInt("AssignmentID"));
        int courseIdInt = rs.getInt("CourseID");
        String title = rs.getString("Title");
        String instructions = rs.getString("Instructions");
        Timestamp dueDateTs = rs.getTimestamp("DueDate");
        int totalPoints = rs.getInt("TotalPoints");
        String submissionTypeStr = rs.getString("SubmissionType");
        Timestamp createdDateTs = rs.getTimestamp("CreatedDate");
        
        Course course = getCourseById(conn, courseIdInt);
        if (course == null) {
            return null;
        }
        
        LocalDateTime dueDate = null;
        if (dueDateTs != null) {
            dueDate = dueDateTs.toLocalDateTime();
        }
        
        LocalDateTime createdDate = null;
        if (createdDateTs != null) {
            createdDate = createdDateTs.toLocalDateTime();
        }
        
        SubmissionType submissionType = stringToSubmissionType(submissionTypeStr);
        
        // Load EAV attributes
        Map<String, String> attributes = loadAttributes(conn, Integer.parseInt(assignmentId));
        
        return new Assignment(assignmentId, course, title, instructions, dueDate, totalPoints, submissionType, createdDate, attributes);
    }

    /**
     * Load EAV attributes for an assignment
     */
    private Map<String, String> loadAttributes(Connection conn, int assignmentId) throws SQLException {
        Map<String, String> attributes = new HashMap<>();
        String sql = "SELECT AttributeName, AttributeValue FROM AssignmentAttributes WHERE AssignmentID = ?";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, assignmentId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String name = rs.getString("AttributeName");
                    String value = rs.getString("AttributeValue");
                    attributes.put(name, value);
                }
            }
        }
        
        return attributes;
    }

    /**
     * Get course by ID (helper method)
     */
    private Course getCourseById(Connection conn, int courseId) throws SQLException {
        String sql = "SELECT CourseID, Code, Name, Description, Credits, Department, Semester, Type, ProfessorUserID, CreatedDate " +
                     "FROM Courses WHERE CourseID = ?";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, courseId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    // Use CourseService mapping logic
                    edu.curriculum.service.CourseService courseService = new edu.curriculum.service.CourseService();
                    return courseService.getCourseById(String.valueOf(courseId));
                }
            }
        }
        return null;
    }

    /**
     * Convert SubmissionType enum to database string
     */
    private String typeToString(SubmissionType type) {
        if (type == null) return "TEXT";
        switch (type) {
            case FILE: return "FILE";
            case TEXT: return "TEXT";
            case BOTH: return "BOTH";
            default: return "TEXT";
        }
    }

    /**
     * Convert database string to SubmissionType enum
     */
    private SubmissionType stringToSubmissionType(String typeStr) {
        if (typeStr == null) return SubmissionType.TEXT;
        switch (typeStr.toUpperCase()) {
            case "FILE": return SubmissionType.FILE;
            case "TEXT": return SubmissionType.TEXT;
            case "BOTH": return SubmissionType.BOTH;
            default: return SubmissionType.TEXT;
        }
    }
}

