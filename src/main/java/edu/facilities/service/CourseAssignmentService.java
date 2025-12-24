package edu.facilities.service;

import edu.facilities.model.Course;
import edu.facilities.model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for assigning courses to professors
 * US 3.3 - Assign Courses to Professors
 */
public class CourseAssignmentService {
    
    private CourseService courseService;
    
    public CourseAssignmentService() {
        this.courseService = new CourseService();
    }
    
    /**
     * Assign a course to a professor
     * @param courseId Course ID to assign
     * @param professorUserId Professor User ID
     * @param checkConflicts Whether to check for schedule conflicts
     * @return true if assignment successful, false otherwise
     * @throws SQLException Database error
     * @throws IllegalArgumentException If conflict detected
     */
    public boolean assignCourseToProfessor(String courseId, String professorUserId, boolean checkConflicts) 
            throws SQLException, IllegalArgumentException {
        
        // Get course details
        Course course = courseService.getCourseById(courseId);
        if (course == null) {
            throw new IllegalArgumentException("Course not found");
        }
        
        // Verify professor exists and is a professor
        if (!isProfessor(professorUserId)) {
            throw new IllegalArgumentException("User is not a professor");
        }
        
        // Check for conflicts if requested
        if (checkConflicts) {
            List<String> conflicts = checkScheduleConflicts(professorUserId, course);
            if (!conflicts.isEmpty()) {
                StringBuilder conflictMsg = new StringBuilder("Schedule conflicts detected:\n");
                for (String conflict : conflicts) {
                    conflictMsg.append("- ").append(conflict).append("\n");
                }
                throw new IllegalArgumentException(conflictMsg.toString());
            }
        }
        
        // Assign the course
        return courseService.addProfessorToCourse(courseId, professorUserId);
    }
    
    /**
     * Assign multiple courses to a professor
     * @param courseIds List of course IDs
     * @param professorUserId Professor User ID
     * @param checkConflicts Whether to check for schedule conflicts
     * @return List of successfully assigned course IDs
     * @throws SQLException Database error
     */
    public List<String> assignMultipleCoursesToProfessor(List<String> courseIds, String professorUserId, boolean checkConflicts) 
            throws SQLException {
        
        List<String> assignedCourses = new ArrayList<>();
        List<String> failedCourses = new ArrayList<>();
        
        // First, check all courses for conflicts if requested
        if (checkConflicts) {
            for (String courseId : courseIds) {
                try {
                    Course course = courseService.getCourseById(courseId);
                    if (course != null) {
                        List<String> conflicts = checkScheduleConflicts(professorUserId, course);
                        if (!conflicts.isEmpty()) {
                            failedCourses.add(course.getCode() + " - " + String.join(", ", conflicts));
                            continue;
                        }
                    }
                } catch (Exception e) {
                    failedCourses.add("Course " + courseId + " - " + e.getMessage());
                }
            }
        }
        
        // Assign courses that passed conflict check
        for (String courseId : courseIds) {
            try {
                boolean assigned = assignCourseToProfessor(courseId, professorUserId, false); // Already checked conflicts
                if (assigned) {
                    assignedCourses.add(courseId);
                } else {
                    failedCourses.add("Course " + courseId + " - Already assigned or assignment failed");
                }
            } catch (Exception e) {
                failedCourses.add("Course " + courseId + " - " + e.getMessage());
            }
        }
        
        // If some failed, throw exception with details
        if (!failedCourses.isEmpty() && assignedCourses.isEmpty()) {
            throw new IllegalArgumentException("Failed to assign courses:\n" + String.join("\n", failedCourses));
        }
        
        return assignedCourses;
    }
    
    /**
     * Check for schedule conflicts for a professor with a course
     * Simple conflict detection: same professor, same semester
     * Can be extended later for time slot conflicts
     */
    private List<String> checkScheduleConflicts(String professorUserId, Course newCourse) throws SQLException {
        List<String> conflicts = new ArrayList<>();
        
        // Get all courses currently assigned to this professor in the same semester
        String sql = "SELECT c.CourseID, c.Code, c.Name, c.Semester " +
                    "FROM CourseProfessors cp " +
                    "INNER JOIN Courses c ON cp.CourseID = c.CourseID " +
                    "WHERE cp.ProfessorUserID = ? " +
                    "AND c.Semester = ? " +
                    "AND c.IsActive = 1 " +
                    "AND c.CourseID != ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, Integer.parseInt(professorUserId));
            pstmt.setString(2, newCourse.getSemester());
            pstmt.setInt(3, Integer.parseInt(newCourse.getId()));
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String conflictCourse = rs.getString("Code") + " - " + rs.getString("Name");
                    conflicts.add("Already assigned to " + conflictCourse + " in " + rs.getString("Semester"));
                }
            }
        }
        
        return conflicts;
    }
    
    /**
     * Get all courses assigned to a professor
     */
    public List<Course> getProfessorCourses(String professorUserId) throws SQLException {
        String sql = "SELECT c.CourseID, c.Code, c.Name, c.Description, c.Credits, " +
                    "c.Department, c.Semester, c.Type, c.MaxSeats, c.CurrentSeats, " +
                    "c.IsActive, c.CreatedDate, c.UpdatedDate " +
                    "FROM CourseProfessors cp " +
                    "INNER JOIN Courses c ON cp.CourseID = c.CourseID " +
                    "WHERE cp.ProfessorUserID = ? " +
                    "AND c.IsActive = 1 " +
                    "ORDER BY c.Semester, c.Code";
        
        List<Course> courses = new ArrayList<>();
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, Integer.parseInt(professorUserId));
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String courseId = String.valueOf(rs.getInt("CourseID"));
                    Course course = courseService.getCourseById(courseId);
                    if (course != null) {
                        courses.add(course);
                    }
                }
            }
        }
        
        return courses;
    }
    
    /**
     * Remove a course assignment from a professor
     */
    public boolean removeCourseAssignment(String courseId, String professorUserId) throws SQLException {
        String sql = "DELETE FROM CourseProfessors WHERE CourseID = ? AND ProfessorUserID = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, Integer.parseInt(courseId));
            pstmt.setInt(2, Integer.parseInt(professorUserId));
            
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        }
    }
    
    /**
     * Get all professors (for selection in UI)
     */
    public List<User> getAllProfessors() throws SQLException {
        String sql = "SELECT u.UserID, u.Username, u.Email, u.UserType, p.Department " +
                    "FROM Users u " +
                    "INNER JOIN Professors p ON u.UserID = p.UserID " +
                    "WHERE u.UserType = 'PROFESSOR' " +
                    "ORDER BY u.Username";
        
        List<User> professors = new ArrayList<>();
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    User professor = new edu.facilities.model.Professor(
                        String.valueOf(rs.getInt("UserID")),
                        rs.getString("Username"),
                        null // Password not needed
                    );
                    professors.add(professor);
                }
            }
        }
        
        return professors;
    }
    
    /**
     * Check if a user is a professor
     */
    private boolean isProfessor(String userId) throws SQLException {
        String sql = "SELECT COUNT(*) AS ProfCount " +
                    "FROM Users u " +
                    "INNER JOIN Professors p ON u.UserID = p.UserID " +
                    "WHERE u.UserID = ? AND u.UserType = 'PROFESSOR'";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, Integer.parseInt(userId));
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("ProfCount") > 0;
                }
            }
        }
        
        return false;
    }
}

