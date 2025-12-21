package edu.curriculum.service;

import edu.curriculum.model.*;
import edu.facilities.service.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for calculating final grades using weight distributions
 * US: As a professor/system, I want to calculate final grades using weight distributions
 */
public class GradeCalculationService {

    private AssignmentService assignmentService = new AssignmentService();
    private SubmissionService submissionService = new SubmissionService();
    private QuizService quizService = new QuizService();
    private ExamService examService = new ExamService();

    /**
     * Save or update grade weights for a course
     */
    public boolean saveGradeWeights(CourseGradeWeights weights) throws SQLException {
        if (weights == null || !weights.isValid()) {
            throw new IllegalArgumentException("Grade weights must sum to 100%");
        }

        int courseIdInt;
        try {
            courseIdInt = Integer.parseInt(weights.getCourseId());
        } catch (NumberFormatException e) {
            return false;
        }

        // Check if weights already exist
        CourseGradeWeights existing = getGradeWeights(weights.getCourseId());
        
        if (existing != null) {
            // Update existing weights
            String sql = "UPDATE CourseGradeWeights SET AssignmentsWeight = ?, QuizzesWeight = ?, ExamsWeight = ? " +
                        "WHERE CourseID = ?";
            
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                
                pstmt.setDouble(1, weights.getAssignmentsWeight());
                pstmt.setDouble(2, weights.getQuizzesWeight());
                pstmt.setDouble(3, weights.getExamsWeight());
                pstmt.setInt(4, courseIdInt);
                
                return pstmt.executeUpdate() > 0;
            }
        } else {
            // Insert new weights
            String sql = "INSERT INTO CourseGradeWeights (CourseID, AssignmentsWeight, QuizzesWeight, ExamsWeight) " +
                        "VALUES (?, ?, ?, ?)";
            
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                
                pstmt.setInt(1, courseIdInt);
                pstmt.setDouble(2, weights.getAssignmentsWeight());
                pstmt.setDouble(3, weights.getQuizzesWeight());
                pstmt.setDouble(4, weights.getExamsWeight());
                
                return pstmt.executeUpdate() > 0;
            }
        }
    }

    /**
     * Get grade weights for a course
     */
    public CourseGradeWeights getGradeWeights(String courseId) throws SQLException {
        if (courseId == null || courseId.isBlank()) {
            return null;
        }

        int courseIdInt;
        try {
            courseIdInt = Integer.parseInt(courseId);
        } catch (NumberFormatException e) {
            return null;
        }

        String sql = "SELECT CourseID, AssignmentsWeight, QuizzesWeight, ExamsWeight " +
                    "FROM CourseGradeWeights WHERE CourseID = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, courseIdInt);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new CourseGradeWeights(
                            String.valueOf(rs.getInt("CourseID")),
                            rs.getDouble("AssignmentsWeight"),
                            rs.getDouble("QuizzesWeight"),
                            rs.getDouble("ExamsWeight")
                    );
                }
            }
        }
        
        return null;
    }

    /**
     * Calculate final grade percentage for a student in a course
     * @param courseId The course ID
     * @param studentId The student user ID
     * @return Final grade percentage (0-100), or null if insufficient data
     */
    public Double calculateFinalGrade(String courseId, String studentId) throws SQLException {
        CourseGradeWeights weights = getGradeWeights(courseId);
        if (weights == null) {
            return null; // No weights configured
        }

        double assignmentsAverage = calculateAssignmentsAverage(courseId, studentId);
        double quizzesAverage = calculateQuizzesAverage(courseId, studentId);
        double examsAverage = calculateExamsAverage(courseId, studentId);

        // Check if we have at least one category with grades
        boolean hasAssignments = !Double.isNaN(assignmentsAverage);
        boolean hasQuizzes = !Double.isNaN(quizzesAverage);
        boolean hasExams = !Double.isNaN(examsAverage);

        if (!hasAssignments && !hasQuizzes && !hasExams) {
            return null; // No grades available
        }

        // Calculate weighted average
        double weightedTotal = 0.0;
        double totalWeight = 0.0;

        if (hasAssignments) {
            weightedTotal += assignmentsAverage * weights.getAssignmentsWeight();
            totalWeight += weights.getAssignmentsWeight();
        }
        if (hasQuizzes) {
            weightedTotal += quizzesAverage * weights.getQuizzesWeight();
            totalWeight += weights.getQuizzesWeight();
        }
        if (hasExams) {
            weightedTotal += examsAverage * weights.getExamsWeight();
            totalWeight += weights.getExamsWeight();
        }

        if (totalWeight == 0) {
            return null;
        }

        // Normalize if not all categories have grades
        return weightedTotal / totalWeight;
    }

    /**
     * Calculate average percentage for assignments
     */
    private double calculateAssignmentsAverage(String courseId, String studentId) throws SQLException {
        List<Assignment> assignments = assignmentService.getAssignmentsByCourse(courseId);
        if (assignments.isEmpty()) {
            return Double.NaN;
        }

        double totalPointsEarned = 0.0;
        double totalPointsPossible = 0.0;
        int gradedCount = 0;

        for (Assignment assignment : assignments) {
            AssignmentSubmission submission = submissionService.getSubmission(assignment.getId(), studentId);
            if (submission != null && submission.getScore() != null) {
                totalPointsEarned += submission.getScore();
                totalPointsPossible += assignment.getTotalPoints();
                gradedCount++;
            }
        }

        if (gradedCount == 0 || totalPointsPossible == 0) {
            return Double.NaN;
        }

        return (totalPointsEarned / totalPointsPossible) * 100.0;
    }

    /**
     * Calculate average percentage for quizzes (using best attempt)
     */
    private double calculateQuizzesAverage(String courseId, String studentId) throws SQLException {
        List<Quiz> quizzes = quizService.getQuizzesByCourse(courseId);
        if (quizzes.isEmpty()) {
            return Double.NaN;
        }

        double totalPointsEarned = 0.0;
        double totalPointsPossible = 0.0;
        int gradedCount = 0;

        for (Quiz quiz : quizzes) {
            List<QuizAttempt> attempts = quizService.getQuizAttempts(quiz.getId(), studentId);
            if (!attempts.isEmpty()) {
                // Find best attempt
                Integer bestScore = null;
                for (QuizAttempt attempt : attempts) {
                    if (attempt.getScore() != null && attempt.getStatus() == QuizAttemptStatus.COMPLETED) {
                        if (bestScore == null || attempt.getScore() > bestScore) {
                            bestScore = attempt.getScore();
                        }
                    }
                }
                
                if (bestScore != null) {
                    totalPointsEarned += bestScore;
                    totalPointsPossible += quiz.getTotalPoints();
                    gradedCount++;
                }
            }
        }

        if (gradedCount == 0 || totalPointsPossible == 0) {
            return Double.NaN;
        }

        return (totalPointsEarned / totalPointsPossible) * 100.0;
    }

    /**
     * Calculate average percentage for exams
     */
    private double calculateExamsAverage(String courseId, String studentId) throws SQLException {
        List<Exam> exams = examService.getExamsByCourse(courseId);
        if (exams.isEmpty()) {
            return Double.NaN;
        }

        double totalPointsEarned = 0.0;
        double totalPointsPossible = 0.0;
        int gradedCount = 0;

        for (Exam exam : exams) {
            ExamGrade examGrade = examService.getExamGrade(exam.getId(), studentId);
            if (examGrade != null && examGrade.getPointsEarned() != null) {
                totalPointsEarned += examGrade.getPointsEarned();
                totalPointsPossible += exam.getTotalPoints();
                gradedCount++;
            }
        }

        if (gradedCount == 0 || totalPointsPossible == 0) {
            return Double.NaN;
        }

        return (totalPointsEarned / totalPointsPossible) * 100.0;
    }

    /**
     * Convert percentage to letter grade
     * Standard grading scale: A (90-100), B (80-89), C (70-79), D (60-69), F (<60)
     * With +/- variations
     */
    public String percentageToLetterGrade(double percentage) {
        if (percentage >= 97) return "A+";
        if (percentage >= 93) return "A";
        if (percentage >= 89) return "A-";
        if (percentage >= 84) return "B+";
        if (percentage >= 80) return "B";
        if (percentage >= 76) return "B-";
        if (percentage >= 73) return "C+";
        if (percentage >= 70) return "C";
        if (percentage >= 67) return "C-";
        if (percentage >= 64) return "D+";
        if (percentage >= 60) return "D";
        return "F";
    }

    /**
     * Get all students enrolled in a course with their calculated final grades
     */
    public List<StudentFinalGrade> getStudentFinalGrades(String courseId) throws SQLException {
        List<StudentFinalGrade> grades = new ArrayList<>();
        
        // Get all enrollments for this course
        String sql = "SELECT e.EnrollmentID, e.StudentUserID, e.Grade, " +
                    "u.Username " +
                    "FROM Enrollments e " +
                    "INNER JOIN Users u ON e.StudentUserID = u.UserID " +
                    "WHERE e.CourseID = ? AND e.Status = 'ENROLLED'";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            int courseIdInt = Integer.parseInt(courseId);
            pstmt.setInt(1, courseIdInt);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String studentId = String.valueOf(rs.getInt("StudentUserID"));
                    String studentName = rs.getString("Username");
                    String currentGrade = rs.getString("Grade");
                    
                    Double calculatedPercentage = calculateFinalGrade(courseId, studentId);
                    String calculatedGrade = null;
                    if (calculatedPercentage != null) {
                        calculatedGrade = percentageToLetterGrade(calculatedPercentage);
                    }
                    
                    grades.add(new StudentFinalGrade(
                            String.valueOf(rs.getInt("EnrollmentID")),
                            studentId,
                            studentName,
                            courseId,
                            calculatedPercentage,
                            calculatedGrade,
                            currentGrade
                    ));
                }
            }
        }
        
        return grades;
    }

    /**
     * Update final grade for a student enrollment
     */
    public boolean updateFinalGrade(String enrollmentId, String letterGrade) throws SQLException {
        if (enrollmentId == null || enrollmentId.isBlank() || letterGrade == null || letterGrade.isBlank()) {
            return false;
        }

        int enrollmentIdInt;
        try {
            enrollmentIdInt = Integer.parseInt(enrollmentId);
        } catch (NumberFormatException e) {
            return false;
        }

        String sql = "UPDATE Enrollments SET Grade = ? WHERE EnrollmentID = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, letterGrade);
            pstmt.setInt(2, enrollmentIdInt);
            
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * Inner class to represent a student's final grade information
     */
    public static class StudentFinalGrade {
        private String enrollmentId;
        private String studentId;
        private String studentName;
        private String courseId;
        private Double calculatedPercentage;
        private String calculatedGrade;
        private String currentGrade; // Grade stored in enrollment (may be overridden)

        public StudentFinalGrade(String enrollmentId, String studentId, String studentName, String courseId,
                                Double calculatedPercentage, String calculatedGrade, String currentGrade) {
            this.enrollmentId = enrollmentId;
            this.studentId = studentId;
            this.studentName = studentName;
            this.courseId = courseId;
            this.calculatedPercentage = calculatedPercentage;
            this.calculatedGrade = calculatedGrade;
            this.currentGrade = currentGrade;
        }

        // Getters
        public String getEnrollmentId() { return enrollmentId; }
        public String getStudentId() { return studentId; }
        public String getStudentName() { return studentName; }
        public String getCourseId() { return courseId; }
        public Double getCalculatedPercentage() { return calculatedPercentage; }
        public String getCalculatedGrade() { return calculatedGrade; }
        public String getCurrentGrade() { return currentGrade; }
        
        // Check if grade was overridden
        public boolean isOverridden() {
            return currentGrade != null && !currentGrade.equals(calculatedGrade);
        }
    }
}

