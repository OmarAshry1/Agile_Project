package edu.curriculum.service;

import edu.curriculum.model.Assignment;
import edu.curriculum.model.AssignmentSubmission;
import edu.curriculum.model.SubmissionStatus;
import edu.facilities.model.Student;
import edu.facilities.service.DatabaseConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for managing assignment submission data using SQL Server database
 */
public class SubmissionService {

    /**
     * Submit an assignment
     * @param assignmentId The assignment ID
     * @param studentId The student user ID
     * @param submissionText Submission text
     * @param fileName File name (optional)
     * @return Created AssignmentSubmission object or null if failed
     * @throws SQLException if database error occurs
     */
    public AssignmentSubmission submitAssignment(String assignmentId, String studentId,
                                                  String submissionText, String fileName) throws SQLException {
        if (assignmentId == null || assignmentId.isBlank() || studentId == null || studentId.isBlank()) {
            return null;
        }

        int assignmentIdInt;
        int studentIdInt;
        try {
            assignmentIdInt = Integer.parseInt(assignmentId);
            studentIdInt = Integer.parseInt(studentId);
        } catch (NumberFormatException e) {
            return null;
        }

        // Check if already submitted
        String checkSql = "SELECT SubmissionID FROM AssignmentSubmissions WHERE AssignmentID = ? AND StudentUserID = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
            
            checkStmt.setInt(1, assignmentIdInt);
            checkStmt.setInt(2, studentIdInt);
            
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next()) {
                    System.out.println("Student " + studentId + " has already submitted assignment " + assignmentId);
                    // Update existing submission
                    return updateSubmission(assignmentId, studentId, submissionText, fileName);
                }
            }
        }

        // Create new submission
        String sql = "INSERT INTO AssignmentSubmissions (AssignmentID, StudentUserID, SubmissionText, FileName, SubmittedDate, Status) " +
                     "VALUES (?, ?, ?, ?, GETDATE(), 'SUBMITTED')";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setInt(1, assignmentIdInt);
            pstmt.setInt(2, studentIdInt);
            pstmt.setString(3, submissionText);
            pstmt.setString(4, fileName);
            
            int rowsAffected = pstmt.executeUpdate();
            
            if (rowsAffected > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        int submissionId = generatedKeys.getInt(1);
                        System.out.println("Submission created with ID: " + submissionId);
                        return getSubmission(assignmentId, studentId);
                    }
                }
            }
        }
        
        return null;
    }

    /**
     * Update existing submission
     */
    private AssignmentSubmission updateSubmission(String assignmentId, String studentId,
                                                  String submissionText, String fileName) throws SQLException {
        int assignmentIdInt = Integer.parseInt(assignmentId);
        int studentIdInt = Integer.parseInt(studentId);

        String sql = "UPDATE AssignmentSubmissions SET SubmissionText = ?, FileName = ?, SubmittedDate = GETDATE() " +
                     "WHERE AssignmentID = ? AND StudentUserID = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, submissionText);
            pstmt.setString(2, fileName);
            pstmt.setInt(3, assignmentIdInt);
            pstmt.setInt(4, studentIdInt);
            
            int rowsAffected = pstmt.executeUpdate();
            
            if (rowsAffected > 0) {
                System.out.println("Submission updated for assignment " + assignmentId + " by student " + studentId);
                return getSubmission(assignmentId, studentId);
            }
        }
        
        return null;
    }

    /**
     * Get submission for a specific assignment and student
     * @param assignmentId The assignment ID
     * @param studentId The student user ID
     * @return AssignmentSubmission object if found, null otherwise
     * @throws SQLException if database error occurs
     */
    public AssignmentSubmission getSubmission(String assignmentId, String studentId) throws SQLException {
        if (assignmentId == null || assignmentId.isBlank() || studentId == null || studentId.isBlank()) {
            return null;
        }

        int assignmentIdInt;
        int studentIdInt;
        try {
            assignmentIdInt = Integer.parseInt(assignmentId);
            studentIdInt = Integer.parseInt(studentId);
        } catch (NumberFormatException e) {
            return null;
        }

        String sql = "SELECT SubmissionID, AssignmentID, StudentUserID, SubmissionText, FileName, " +
                     "SubmittedDate, Score, Feedback, Status, GradedDate " +
                     "FROM AssignmentSubmissions WHERE AssignmentID = ? AND StudentUserID = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, assignmentIdInt);
            pstmt.setInt(2, studentIdInt);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToSubmission(rs, conn);
                }
            }
        }
        return null;
    }

    /**
     * Get all submissions for an assignment
     * @param assignmentId The assignment ID
     * @return List of submissions
     * @throws SQLException if database error occurs
     */
    public List<AssignmentSubmission> getSubmissionsByAssignment(String assignmentId) throws SQLException {
        if (assignmentId == null || assignmentId.isBlank()) {
            return new ArrayList<>();
        }

        int assignmentIdInt;
        try {
            assignmentIdInt = Integer.parseInt(assignmentId);
        } catch (NumberFormatException e) {
            return new ArrayList<>();
        }

        List<AssignmentSubmission> submissions = new ArrayList<>();
        String sql = "SELECT SubmissionID, AssignmentID, StudentUserID, SubmissionText, FileName, " +
                     "SubmittedDate, Score, Feedback, Status, GradedDate " +
                     "FROM AssignmentSubmissions WHERE AssignmentID = ? ORDER BY SubmittedDate";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, assignmentIdInt);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    AssignmentSubmission submission = mapResultSetToSubmission(rs, conn);
                    if (submission != null) {
                        submissions.add(submission);
                    }
                }
            }
        }
        
        System.out.println("Retrieved " + submissions.size() + " submissions for assignment " + assignmentId);
        return submissions;
    }

    /**
     * Grade a submission
     * @param submissionId The submission ID
     * @param score The score
     * @param feedback The feedback
     * @return true if grading successful, false otherwise
     * @throws SQLException if database error occurs
     */
    public boolean gradeSubmission(String submissionId, int score, String feedback) throws SQLException {
        if (submissionId == null || submissionId.isBlank()) {
            return false;
        }

        int submissionIdInt;
        try {
            submissionIdInt = Integer.parseInt(submissionId);
        } catch (NumberFormatException e) {
            return false;
        }

        String sql = "UPDATE AssignmentSubmissions SET Score = ?, Feedback = ?, Status = 'GRADED', GradedDate = GETDATE() " +
                     "WHERE SubmissionID = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, score);
            pstmt.setString(2, feedback);
            pstmt.setInt(3, submissionIdInt);
            
            int rowsAffected = pstmt.executeUpdate();
            
            if (rowsAffected > 0) {
                System.out.println("Submission " + submissionId + " graded with score " + score);
                return true;
            }
        }
        
        return false;
    }

    /**
     * Get all submissions for a student
     * @param studentId The student user ID
     * @return List of submissions
     * @throws SQLException if database error occurs
     */
    public List<AssignmentSubmission> getStudentSubmissions(String studentId) throws SQLException {
        if (studentId == null || studentId.isBlank()) {
            return new ArrayList<>();
        }

        int studentIdInt;
        try {
            studentIdInt = Integer.parseInt(studentId);
        } catch (NumberFormatException e) {
            return new ArrayList<>();
        }

        List<AssignmentSubmission> submissions = new ArrayList<>();
        String sql = "SELECT SubmissionID, AssignmentID, StudentUserID, SubmissionText, FileName, " +
                     "SubmittedDate, Score, Feedback, Status, GradedDate " +
                     "FROM AssignmentSubmissions WHERE StudentUserID = ? ORDER BY SubmittedDate DESC";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, studentIdInt);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    AssignmentSubmission submission = mapResultSetToSubmission(rs, conn);
                    if (submission != null) {
                        submissions.add(submission);
                    }
                }
            }
        }
        
        System.out.println("Retrieved " + submissions.size() + " submissions for student " + studentId);
        return submissions;
    }

    /**
     * Map ResultSet row to AssignmentSubmission object
     */
    private AssignmentSubmission mapResultSetToSubmission(ResultSet rs, Connection conn) throws SQLException {
        String submissionId = String.valueOf(rs.getInt("SubmissionID"));
        int assignmentIdInt = rs.getInt("AssignmentID");
        int studentIdInt = rs.getInt("StudentUserID");
        String submissionText = rs.getString("SubmissionText");
        String fileName = rs.getString("FileName");
        Timestamp submittedDateTs = rs.getTimestamp("SubmittedDate");
        Integer score = rs.getObject("Score", Integer.class);
        String feedback = rs.getString("Feedback");
        String statusStr = rs.getString("Status");
        Timestamp gradedDateTs = rs.getTimestamp("GradedDate");
        
        Assignment assignment = getAssignmentById(conn, assignmentIdInt);
        if (assignment == null) {
            return null;
        }
        
        Student student = getStudentById(conn, studentIdInt);
        if (student == null) {
            return null;
        }
        
        LocalDateTime submittedDate = null;
        if (submittedDateTs != null) {
            submittedDate = submittedDateTs.toLocalDateTime();
        }
        
        LocalDateTime gradedDate = null;
        if (gradedDateTs != null) {
            gradedDate = gradedDateTs.toLocalDateTime();
        }
        
        SubmissionStatus status = stringToSubmissionStatus(statusStr);
        
        return new AssignmentSubmission(submissionId, assignment, student, submissionText, fileName,
                                       submittedDate, score, feedback, status, gradedDate);
    }

    /**
     * Get assignment by ID (helper method)
     */
    private Assignment getAssignmentById(Connection conn, int assignmentId) throws SQLException {
        edu.curriculum.service.AssignmentService assignmentService = new edu.curriculum.service.AssignmentService();
        return assignmentService.getAssignmentById(String.valueOf(assignmentId));
    }

    /**
     * Get student by ID (helper method)
     */
    private Student getStudentById(Connection conn, int userId) throws SQLException {
        String sql = "SELECT UserID, USERNAME, Email, UserType FROM Users WHERE UserID = ?";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String id = String.valueOf(rs.getInt("UserID"));
                    String username = rs.getString("USERNAME");
                    String userType = rs.getString("UserType");
                    
                    if ("STUDENT".equals(userType)) {
                        return new Student(id, username, null);
                    }
                }
            }
        }
        return null;
    }

    /**
     * Convert database string to SubmissionStatus enum
     */
    private SubmissionStatus stringToSubmissionStatus(String statusStr) {
        if (statusStr == null) return SubmissionStatus.SUBMITTED;
        switch (statusStr.toUpperCase()) {
            case "SUBMITTED": return SubmissionStatus.SUBMITTED;
            case "GRADED": return SubmissionStatus.GRADED;
            default: return SubmissionStatus.SUBMITTED;
        }
    }
}

