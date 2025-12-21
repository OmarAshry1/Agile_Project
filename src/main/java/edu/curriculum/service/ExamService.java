package edu.curriculum.service;

import edu.curriculum.model.Course;
import edu.curriculum.model.Exam;
import edu.curriculum.model.ExamGrade;
import edu.facilities.service.DatabaseConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for managing exam data using SQL Server database
 * US 2.12 - Create Exam, US 2.13 - Record Exam Grades
 */
public class ExamService {

    /**
     * Get exams by course ID
     */
    public List<Exam> getExamsByCourse(String courseId) throws SQLException {
        if (courseId == null || courseId.isBlank()) {
            return new ArrayList<>();
        }

        int courseIdInt;
        try {
            courseIdInt = Integer.parseInt(courseId);
        } catch (NumberFormatException e) {
            return new ArrayList<>();
        }

        List<Exam> exams = new ArrayList<>();
        String sql = "SELECT ExamID, CourseID, Title, ExamDate, DurationMinutes, Location, TotalPoints, Instructions, CreatedDate " +
                     "FROM Exams WHERE CourseID = ? ORDER BY ExamDate";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, courseIdInt);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Exam exam = mapResultSetToExam(rs, conn);
                    if (exam != null) {
                        exams.add(exam);
                    }
                }
            }
        }
        
        return exams;
    }

    /**
     * Get exam by ID
     */
    public Exam getExamById(String examId) throws SQLException {
        if (examId == null || examId.isBlank()) {
            return null;
        }

        int examIdInt;
        try {
            examIdInt = Integer.parseInt(examId);
        } catch (NumberFormatException e) {
            return null;
        }

        String sql = "SELECT ExamID, CourseID, Title, ExamDate, DurationMinutes, Location, TotalPoints, Instructions, CreatedDate " +
                     "FROM Exams WHERE ExamID = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, examIdInt);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToExam(rs, conn);
                }
            }
        }
        return null;
    }

    /**
     * Create a new exam
     */
    public Exam createExam(String courseId, String title, LocalDateTime examDate,
                          int durationMinutes, String location, int totalPoints,
                          String instructions, Map<String, String> attributes) throws SQLException {
        if (courseId == null || courseId.isBlank() || title == null || title.isBlank() || examDate == null) {
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
            
            // Insert exam
            String sql = "INSERT INTO Exams (CourseID, Title, ExamDate, DurationMinutes, Location, TotalPoints, Instructions, CreatedDate) " +
                         "VALUES (?, ?, ?, ?, ?, ?, ?, GETDATE())";
            
            int examId;
            try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setInt(1, courseIdInt);
                pstmt.setString(2, title);
                pstmt.setTimestamp(3, Timestamp.valueOf(examDate));
                pstmt.setInt(4, durationMinutes);
                pstmt.setString(5, location);
                pstmt.setInt(6, totalPoints);
                pstmt.setString(7, instructions);
                
                int rowsAffected = pstmt.executeUpdate();
                
                if (rowsAffected > 0) {
                    try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            examId = generatedKeys.getInt(1);
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
                String attrSql = "INSERT INTO ExamAttributes (ExamID, AttributeName, AttributeValue) VALUES (?, ?, ?)";
                try (PreparedStatement attrStmt = conn.prepareStatement(attrSql)) {
                    for (Map.Entry<String, String> entry : attributes.entrySet()) {
                        attrStmt.setInt(1, examId);
                        attrStmt.setString(2, entry.getKey());
                        attrStmt.setString(3, entry.getValue());
                        attrStmt.addBatch();
                    }
                    attrStmt.executeBatch();
                }
            }
            
            conn.commit();
            return getExamById(String.valueOf(examId));
            
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(originalAutoCommit);
        }
    }

    /**
     * Get exams for a student (all exams from enrolled courses)
     */
    public List<Exam> getStudentExams(String studentId) throws SQLException {
        if (studentId == null || studentId.isBlank()) {
            return new ArrayList<>();
        }

        int studentIdInt;
        try {
            studentIdInt = Integer.parseInt(studentId);
        } catch (NumberFormatException e) {
            return new ArrayList<>();
        }

        List<Exam> exams = new ArrayList<>();
        String sql = "SELECT DISTINCT e.ExamID, e.CourseID, e.Title, e.ExamDate, e.DurationMinutes, e.Location, e.TotalPoints, e.Instructions, e.CreatedDate " +
                     "FROM Exams e " +
                     "INNER JOIN Courses c ON e.CourseID = c.CourseID " +
                     "INNER JOIN Enrollments en ON c.CourseID = en.CourseID " +
                     "WHERE en.StudentUserID = ? ORDER BY e.ExamDate";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, studentIdInt);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Exam exam = mapResultSetToExam(rs, conn);
                    if (exam != null) {
                        exams.add(exam);
                    }
                }
            }
        }
        
        return exams;
    }

    /**
     * Get enrolled students for an exam (for grading)
     */
    public List<String> getEnrolledStudentIds(String examId) throws SQLException {
        if (examId == null || examId.isBlank()) {
            return new ArrayList<>();
        }

        int examIdInt;
        try {
            examIdInt = Integer.parseInt(examId);
        } catch (NumberFormatException e) {
            return new ArrayList<>();
        }

        List<String> studentIds = new ArrayList<>();
        String sql = "SELECT DISTINCT e.StudentUserID " +
                     "FROM Enrollments e " +
                     "INNER JOIN Exams ex ON e.CourseID = ex.CourseID " +
                     "WHERE ex.ExamID = ? AND e.Status = 'ENROLLED'";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, examIdInt);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    studentIds.add(String.valueOf(rs.getInt("StudentUserID")));
                }
            }
        }
        
        return studentIds;
    }

    /**
     * Record or update exam grade
     */
    public ExamGrade recordExamGrade(String examId, String studentId, Integer pointsEarned, String comments) throws SQLException {
        if (examId == null || examId.isBlank() || studentId == null || studentId.isBlank()) {
            return null;
        }

        int examIdInt, studentIdInt;
        try {
            examIdInt = Integer.parseInt(examId);
            studentIdInt = Integer.parseInt(studentId);
        } catch (NumberFormatException e) {
            return null;
        }

        // Check if grade already exists
        ExamGrade existingGrade = getExamGrade(examId, studentId);
        
        if (existingGrade != null) {
            // Update existing grade
            String sql = "UPDATE ExamGrades SET PointsEarned = ?, Comments = ?, GradedDate = GETDATE() " +
                         "WHERE ExamID = ? AND StudentUserID = ?";
            
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                
                if (pointsEarned != null) {
                    pstmt.setInt(1, pointsEarned);
                } else {
                    pstmt.setNull(1, Types.INTEGER);
                }
                pstmt.setString(2, comments);
                pstmt.setInt(3, examIdInt);
                pstmt.setInt(4, studentIdInt);
                
                pstmt.executeUpdate();
            }
        } else {
            // Insert new grade
            String sql = "INSERT INTO ExamGrades (ExamID, StudentUserID, PointsEarned, Comments, GradedDate) " +
                         "VALUES (?, ?, ?, ?, GETDATE())";
            
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                
                pstmt.setInt(1, examIdInt);
                pstmt.setInt(2, studentIdInt);
                if (pointsEarned != null) {
                    pstmt.setInt(3, pointsEarned);
                } else {
                    pstmt.setNull(3, Types.INTEGER);
                }
                pstmt.setString(4, comments);
                
                pstmt.executeUpdate();
            }
        }
        
        return getExamGrade(examId, studentId);
    }

    /**
     * Get exam grade for a student
     */
    public ExamGrade getExamGrade(String examId, String studentId) throws SQLException {
        if (examId == null || examId.isBlank() || studentId == null || studentId.isBlank()) {
            return null;
        }

        int examIdInt, studentIdInt;
        try {
            examIdInt = Integer.parseInt(examId);
            studentIdInt = Integer.parseInt(studentId);
        } catch (NumberFormatException e) {
            return null;
        }

        String sql = "SELECT ExamGradeID, ExamID, StudentUserID, PointsEarned, Comments, GradedDate " +
                     "FROM ExamGrades WHERE ExamID = ? AND StudentUserID = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, examIdInt);
            pstmt.setInt(2, studentIdInt);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToExamGrade(rs, conn);
                }
            }
        }
        return null;
    }

    /**
     * Get all exam grades for an exam
     */
    public List<ExamGrade> getExamGrades(String examId) throws SQLException {
        if (examId == null || examId.isBlank()) {
            return new ArrayList<>();
        }

        int examIdInt;
        try {
            examIdInt = Integer.parseInt(examId);
        } catch (NumberFormatException e) {
            return new ArrayList<>();
        }

        List<ExamGrade> grades = new ArrayList<>();
        String sql = "SELECT ExamGradeID, ExamID, StudentUserID, PointsEarned, Comments, GradedDate " +
                     "FROM ExamGrades WHERE ExamID = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, examIdInt);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    ExamGrade grade = mapResultSetToExamGrade(rs, conn);
                    if (grade != null) {
                        grades.add(grade);
                    }
                }
            }
        }
        
        return grades;
    }

    /**
     * Map ResultSet row to Exam object
     */
    private Exam mapResultSetToExam(ResultSet rs, Connection conn) throws SQLException {
        String examId = String.valueOf(rs.getInt("ExamID"));
        int courseIdInt = rs.getInt("CourseID");
        String title = rs.getString("Title");
        Timestamp examDateTs = rs.getTimestamp("ExamDate");
        int durationMinutes = rs.getInt("DurationMinutes");
        String location = rs.getString("Location");
        int totalPoints = rs.getInt("TotalPoints");
        String instructions = rs.getString("Instructions");
        Timestamp createdDateTs = rs.getTimestamp("CreatedDate");
        
        Course course = getCourseById(conn, courseIdInt);
        if (course == null) {
            return null;
        }
        
        LocalDateTime examDate = null;
        if (examDateTs != null) {
            examDate = examDateTs.toLocalDateTime();
        }
        
        LocalDateTime createdDate = null;
        if (createdDateTs != null) {
            createdDate = createdDateTs.toLocalDateTime();
        }
        
        // Load EAV attributes
        Map<String, String> attributes = loadAttributes(conn, Integer.parseInt(examId));
        
        return new Exam(examId, course, title, examDate, durationMinutes, location,
                       totalPoints, instructions, createdDate, attributes);
    }

    /**
     * Map ResultSet row to ExamGrade object
     */
    private ExamGrade mapResultSetToExamGrade(ResultSet rs, Connection conn) throws SQLException {
        String gradeId = String.valueOf(rs.getInt("ExamGradeID"));
        int examIdInt = rs.getInt("ExamID");
        int studentIdInt = rs.getInt("StudentUserID");
        Integer pointsEarned = rs.getObject("PointsEarned") != null ? rs.getInt("PointsEarned") : null;
        String comments = rs.getString("Comments");
        Timestamp gradedDateTs = rs.getTimestamp("GradedDate");
        
        Exam exam = getExamById(String.valueOf(examIdInt));
        if (exam == null) {
            return null;
        }
        
        LocalDateTime gradedDate = null;
        if (gradedDateTs != null) {
            gradedDate = gradedDateTs.toLocalDateTime();
        }
        
        return new ExamGrade(gradeId, exam, String.valueOf(studentIdInt), pointsEarned, comments, gradedDate);
    }

    /**
     * Load EAV attributes for an exam
     */
    private Map<String, String> loadAttributes(Connection conn, int examId) throws SQLException {
        Map<String, String> attributes = new HashMap<>();
        String sql = "SELECT AttributeName, AttributeValue FROM ExamAttributes WHERE ExamID = ?";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, examId);
            
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
}

