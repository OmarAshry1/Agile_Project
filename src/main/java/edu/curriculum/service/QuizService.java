package edu.curriculum.service;

import edu.curriculum.model.Course;
import edu.curriculum.model.Quiz;
import edu.curriculum.model.QuizAttempt;
import edu.curriculum.model.QuizAttemptStatus;
import edu.facilities.service.DatabaseConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for managing quiz data using SQL Server database
 * US 2.10 - Create Quiz, US 2.11 - Take Quiz
 */
public class QuizService {

    /**
     * Get quizzes by course ID
     */
    public List<Quiz> getQuizzesByCourse(String courseId) throws SQLException {
        if (courseId == null || courseId.isBlank()) {
            return new ArrayList<>();
        }

        int courseIdInt;
        try {
            courseIdInt = Integer.parseInt(courseId);
        } catch (NumberFormatException e) {
            return new ArrayList<>();
        }

        List<Quiz> quizzes = new ArrayList<>();
        String sql = "SELECT QuizID, CourseID, Title, Instructions, TotalPoints, DueDate, CreatedDate " +
                     "FROM Quizzes WHERE CourseID = ? ORDER BY DueDate";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, courseIdInt);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Quiz quiz = mapResultSetToQuiz(rs, conn);
                    if (quiz != null) {
                        quizzes.add(quiz);
                    }
                }
            }
        }
        
        return quizzes;
    }

    /**
     * Get quiz by ID
     */
    public Quiz getQuizById(String quizId) throws SQLException {
        if (quizId == null || quizId.isBlank()) {
            return null;
        }

        int quizIdInt;
        try {
            quizIdInt = Integer.parseInt(quizId);
        } catch (NumberFormatException e) {
            return null;
        }

        String sql = "SELECT QuizID, CourseID, Title, Instructions, TotalPoints, DueDate, CreatedDate " +
                     "FROM Quizzes WHERE QuizID = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, quizIdInt);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToQuiz(rs, conn);
                }
            }
        }
        return null;
    }

    /**
     * Create a new quiz
     */
    public Quiz createQuiz(String courseId, String title, String instructions,
                          int totalPoints, LocalDateTime dueDate,
                          Map<String, String> attributes) throws SQLException {
        if (courseId == null || courseId.isBlank() || title == null || title.isBlank() || dueDate == null) {
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
            
            // Insert quiz
            String sql = "INSERT INTO Quizzes (CourseID, Title, Instructions, TotalPoints, DueDate, CreatedDate) " +
                         "VALUES (?, ?, ?, ?, ?, GETDATE())";
            
            int quizId;
            try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setInt(1, courseIdInt);
                pstmt.setString(2, title);
                pstmt.setString(3, instructions);
                pstmt.setInt(4, totalPoints);
                pstmt.setTimestamp(5, Timestamp.valueOf(dueDate));
                
                int rowsAffected = pstmt.executeUpdate();
                
                if (rowsAffected > 0) {
                    try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            quizId = generatedKeys.getInt(1);
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
                String attrSql = "INSERT INTO QuizAttributes (QuizID, AttributeName, AttributeValue) VALUES (?, ?, ?)";
                try (PreparedStatement attrStmt = conn.prepareStatement(attrSql)) {
                    for (Map.Entry<String, String> entry : attributes.entrySet()) {
                        attrStmt.setInt(1, quizId);
                        attrStmt.setString(2, entry.getKey());
                        attrStmt.setString(3, entry.getValue());
                        attrStmt.addBatch();
                    }
                    attrStmt.executeBatch();
                }
            }
            
            conn.commit();
            return getQuizById(String.valueOf(quizId));
            
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(originalAutoCommit);
        }
    }

    /**
     * Get quizzes for a student (all quizzes from enrolled courses)
     */
    public List<Quiz> getStudentQuizzes(String studentId) throws SQLException {
        if (studentId == null || studentId.isBlank()) {
            return new ArrayList<>();
        }

        int studentIdInt;
        try {
            studentIdInt = Integer.parseInt(studentId);
        } catch (NumberFormatException e) {
            return new ArrayList<>();
        }

        List<Quiz> quizzes = new ArrayList<>();
        String sql = "SELECT DISTINCT q.QuizID, q.CourseID, q.Title, q.Instructions, q.TotalPoints, q.DueDate, q.CreatedDate " +
                     "FROM Quizzes q " +
                     "INNER JOIN Courses c ON q.CourseID = c.CourseID " +
                     "INNER JOIN Enrollments e ON c.CourseID = e.CourseID " +
                     "WHERE e.StudentUserID = ? ORDER BY q.DueDate";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, studentIdInt);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Quiz quiz = mapResultSetToQuiz(rs, conn);
                    if (quiz != null) {
                        quizzes.add(quiz);
                    }
                }
            }
        }
        
        return quizzes;
    }

    /**
     * Start a quiz attempt
     */
    public QuizAttempt startQuizAttempt(String quizId, String studentId) throws SQLException {
        if (quizId == null || quizId.isBlank() || studentId == null || studentId.isBlank()) {
            return null;
        }

        int quizIdInt, studentIdInt;
        try {
            quizIdInt = Integer.parseInt(quizId);
            studentIdInt = Integer.parseInt(studentId);
        } catch (NumberFormatException e) {
            return null;
        }

        // Get next attempt number
        int attemptNumber = getNextAttemptNumber(quizIdInt, studentIdInt);

        String sql = "INSERT INTO QuizAttempts (QuizID, StudentUserID, AttemptNumber, StartedDate, Status) " +
                     "VALUES (?, ?, ?, GETDATE(), 'IN_PROGRESS')";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setInt(1, quizIdInt);
            pstmt.setInt(2, studentIdInt);
            pstmt.setInt(3, attemptNumber);
            
            int rowsAffected = pstmt.executeUpdate();
            
            if (rowsAffected > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        int attemptId = generatedKeys.getInt(1);
                        return getQuizAttemptById(String.valueOf(attemptId));
                    }
                }
            }
        }
        return null;
    }

    /**
     * Complete a quiz attempt
     */
    public boolean completeQuizAttempt(String attemptId, Integer score) throws SQLException {
        if (attemptId == null || attemptId.isBlank()) {
            return false;
        }

        int attemptIdInt;
        try {
            attemptIdInt = Integer.parseInt(attemptId);
        } catch (NumberFormatException e) {
            return false;
        }

        String sql = "UPDATE QuizAttempts SET CompletedDate = GETDATE(), Score = ?, Status = 'COMPLETED' " +
                     "WHERE AttemptID = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            if (score != null) {
                pstmt.setInt(1, score);
            } else {
                pstmt.setNull(1, Types.INTEGER);
            }
            pstmt.setInt(2, attemptIdInt);
            
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * Get quiz attempt by ID
     */
    public QuizAttempt getQuizAttemptById(String attemptId) throws SQLException {
        if (attemptId == null || attemptId.isBlank()) {
            return null;
        }

        int attemptIdInt;
        try {
            attemptIdInt = Integer.parseInt(attemptId);
        } catch (NumberFormatException e) {
            return null;
        }

        String sql = "SELECT AttemptID, QuizID, StudentUserID, AttemptNumber, StartedDate, CompletedDate, Score, Status " +
                     "FROM QuizAttempts WHERE AttemptID = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, attemptIdInt);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToQuizAttempt(rs, conn);
                }
            }
        }
        return null;
    }

    /**
     * Get quiz attempts for a student and quiz
     */
    public List<QuizAttempt> getQuizAttempts(String quizId, String studentId) throws SQLException {
        if (quizId == null || quizId.isBlank() || studentId == null || studentId.isBlank()) {
            return new ArrayList<>();
        }

        int quizIdInt, studentIdInt;
        try {
            quizIdInt = Integer.parseInt(quizId);
            studentIdInt = Integer.parseInt(studentId);
        } catch (NumberFormatException e) {
            return new ArrayList<>();
        }

        List<QuizAttempt> attempts = new ArrayList<>();
        String sql = "SELECT AttemptID, QuizID, StudentUserID, AttemptNumber, StartedDate, CompletedDate, Score, Status " +
                     "FROM QuizAttempts WHERE QuizID = ? AND StudentUserID = ? ORDER BY AttemptNumber";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, quizIdInt);
            pstmt.setInt(2, studentIdInt);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    QuizAttempt attempt = mapResultSetToQuizAttempt(rs, conn);
                    if (attempt != null) {
                        attempts.add(attempt);
                    }
                }
            }
        }
        
        return attempts;
    }

    /**
     * Get next attempt number for a student and quiz
     */
    private int getNextAttemptNumber(int quizId, int studentId) throws SQLException {
        String sql = "SELECT MAX(AttemptNumber) as MaxAttempt FROM QuizAttempts WHERE QuizID = ? AND StudentUserID = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, quizId);
            pstmt.setInt(2, studentId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    int maxAttempt = rs.getInt("MaxAttempt");
                    return rs.wasNull() ? 1 : maxAttempt + 1;
                }
            }
        }
        return 1;
    }

    /**
     * Map ResultSet row to Quiz object
     */
    private Quiz mapResultSetToQuiz(ResultSet rs, Connection conn) throws SQLException {
        String quizId = String.valueOf(rs.getInt("QuizID"));
        int courseIdInt = rs.getInt("CourseID");
        String title = rs.getString("Title");
        String instructions = rs.getString("Instructions");
        int totalPoints = rs.getInt("TotalPoints");
        Timestamp dueDateTs = rs.getTimestamp("DueDate");
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
        
        // Load EAV attributes
        Map<String, String> attributes = loadAttributes(conn, Integer.parseInt(quizId));
        
        return new Quiz(quizId, course, title, instructions, totalPoints, dueDate, createdDate, attributes);
    }

    /**
     * Map ResultSet row to QuizAttempt object
     */
    private QuizAttempt mapResultSetToQuizAttempt(ResultSet rs, Connection conn) throws SQLException {
        String attemptId = String.valueOf(rs.getInt("AttemptID"));
        int quizIdInt = rs.getInt("QuizID");
        int studentIdInt = rs.getInt("StudentUserID");
        int attemptNumber = rs.getInt("AttemptNumber");
        Timestamp startedDateTs = rs.getTimestamp("StartedDate");
        Timestamp completedDateTs = rs.getTimestamp("CompletedDate");
        Integer score = rs.getObject("Score") != null ? rs.getInt("Score") : null;
        String statusStr = rs.getString("Status");
        
        Quiz quiz = getQuizById(String.valueOf(quizIdInt));
        if (quiz == null) {
            return null;
        }
        
        LocalDateTime startedDate = null;
        if (startedDateTs != null) {
            startedDate = startedDateTs.toLocalDateTime();
        }
        
        LocalDateTime completedDate = null;
        if (completedDateTs != null) {
            completedDate = completedDateTs.toLocalDateTime();
        }
        
        QuizAttemptStatus status = stringToStatus(statusStr);
        
        return new QuizAttempt(attemptId, quiz, String.valueOf(studentIdInt), attemptNumber,
                              startedDate, completedDate, score, status);
    }

    /**
     * Load EAV attributes for a quiz
     */
    private Map<String, String> loadAttributes(Connection conn, int quizId) throws SQLException {
        Map<String, String> attributes = new HashMap<>();
        String sql = "SELECT AttributeName, AttributeValue FROM QuizAttributes WHERE QuizID = ?";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, quizId);
            
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
        edu.curriculum.service.CourseService courseService = new edu.curriculum.service.CourseService();
        return courseService.getCourseById(String.valueOf(courseId));
    }

    /**
     * Convert database string to QuizAttemptStatus enum
     */
    private QuizAttemptStatus stringToStatus(String statusStr) {
        if (statusStr == null) return QuizAttemptStatus.IN_PROGRESS;
        switch (statusStr.toUpperCase()) {
            case "IN_PROGRESS": return QuizAttemptStatus.IN_PROGRESS;
            case "COMPLETED": return QuizAttemptStatus.COMPLETED;
            case "TIMED_OUT": return QuizAttemptStatus.TIMED_OUT;
            default: return QuizAttemptStatus.IN_PROGRESS;
        }
    }
}

