package edu.facilities.service;

import edu.facilities.model.Course;
import edu.facilities.model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for assigning staff to courses
 * US 3.3.1 - Assign Staff to Course
 * US 3.3.2 - View Assigned Courses
 */
public class StaffCourseAssignmentService {
    
    private CourseService courseService;
    
    public StaffCourseAssignmentService() {
        this.courseService = new CourseService();
    }
    
    /**
     * Assign a staff member to a course
     * US 3.3.1 - Only eligible staff can be assigned, no double-booking
     * @param courseId Course ID to assign
     * @param staffUserId Staff User ID
     * @param role Optional role (TA, LAB_ASSISTANT, TUTOR, COORDINATOR, etc.)
     * @param createdByUserId Admin who is making the assignment
     * @param checkConflicts Whether to check for schedule conflicts
     * @return true if assignment successful, false otherwise
     * @throws SQLException Database error
     * @throws IllegalArgumentException If conflict detected or staff not eligible
     */
    public boolean assignStaffToCourse(String courseId, String staffUserId, String role,
                                       String createdByUserId, boolean checkConflicts) 
            throws SQLException, IllegalArgumentException {
        
        // Get course details
        Course course = courseService.getCourseById(courseId);
        if (course == null) {
            throw new IllegalArgumentException("Course not found");
        }
        
        // Verify staff exists and is a staff member
        if (!isStaff(staffUserId)) {
            throw new IllegalArgumentException("User is not a staff member");
        }
        
        // Check for conflicts if requested (double-booking prevention)
        if (checkConflicts) {
            List<String> conflicts = checkScheduleConflicts(staffUserId, course);
            if (!conflicts.isEmpty()) {
                StringBuilder conflictMsg = new StringBuilder("Schedule conflicts detected:\n");
                for (String conflict : conflicts) {
                    conflictMsg.append("- ").append(conflict).append("\n");
                }
                throw new IllegalArgumentException(conflictMsg.toString());
            }
        }
        
        // Check if already assigned
        if (isStaffAssignedToCourse(courseId, staffUserId)) {
            throw new IllegalArgumentException("Staff member is already assigned to this course");
        }
        
        // Assign the staff member
        String sql = "INSERT INTO CourseStaff (CourseID, StaffUserID, Role, CreatedByUserID, CreatedDate) " +
                    "VALUES (?, ?, ?, ?, GETDATE())";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, Integer.parseInt(courseId));
            pstmt.setInt(2, Integer.parseInt(staffUserId));
            if (role != null && !role.isEmpty()) {
                pstmt.setString(3, role);
            } else {
                pstmt.setNull(3, Types.VARCHAR);
            }
            if (createdByUserId != null) {
                pstmt.setInt(4, Integer.parseInt(createdByUserId));
            } else {
                pstmt.setNull(4, Types.INTEGER);
            }
            
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            if (e.getErrorCode() == 2627 || e.getMessage().contains("UNIQUE")) {
                // Duplicate entry - already assigned
                throw new IllegalArgumentException("Staff member is already assigned to this course");
            }
            throw e;
        }
    }
    
    /**
     * Assign multiple staff members to courses
     */
    public List<String> assignMultipleStaffToCourses(List<String> courseIds, String staffUserId, 
                                                     String role, String createdByUserId, boolean checkConflicts) 
            throws SQLException {
        
        List<String> assignedCourses = new ArrayList<>();
        List<String> failedCourses = new ArrayList<>();
        
        // First, check all courses for conflicts if requested
        if (checkConflicts) {
            for (String courseId : courseIds) {
                try {
                    Course course = courseService.getCourseById(courseId);
                    if (course != null) {
                        List<String> conflicts = checkScheduleConflicts(staffUserId, course);
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
                boolean assigned = assignStaffToCourse(courseId, staffUserId, role, createdByUserId, false);
                if (assigned) {
                    assignedCourses.add(courseId);
                } else {
                    failedCourses.add("Course " + courseId + " - Assignment failed");
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
     * Check for schedule conflicts for a staff member with a course
     * Simple conflict detection: same staff, same semester
     * Can be extended later for time slot conflicts
     */
    private List<String> checkScheduleConflicts(String staffUserId, Course newCourse) throws SQLException {
        List<String> conflicts = new ArrayList<>();
        
        // Get all courses currently assigned to this staff member in the same semester
        String sql = "SELECT c.CourseID, c.Code, c.Name, c.Semester " +
                    "FROM CourseStaff cs " +
                    "INNER JOIN Courses c ON cs.CourseID = c.CourseID " +
                    "WHERE cs.StaffUserID = ? " +
                    "AND c.Semester = ? " +
                    "AND c.IsActive = 1 " +
                    "AND c.CourseID != ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, Integer.parseInt(staffUserId));
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
     * Get all courses assigned to a staff member
     * US 3.3.2 - View Assigned Courses
     */
    public List<CourseAssignment> getStaffAssignedCourses(String staffUserId) throws SQLException {
        String sql = "SELECT cs.CourseStaffID, cs.CourseID, cs.Role, cs.CreatedDate, " +
                    "c.Code, c.Name, c.Description, c.Credits, c.Department, c.Semester, " +
                    "c.Type, c.MaxSeats, c.CurrentSeats, c.IsActive, c.CreatedDate as CourseCreatedDate, " +
                    "c.UpdatedDate " +
                    "FROM CourseStaff cs " +
                    "INNER JOIN Courses c ON cs.CourseID = c.CourseID " +
                    "WHERE cs.StaffUserID = ? " +
                    "AND c.IsActive = 1 " +
                    "ORDER BY c.Semester, c.Code";
        
        List<CourseAssignment> assignments = new ArrayList<>();
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, Integer.parseInt(staffUserId));
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    CourseAssignment assignment = new CourseAssignment();
                    assignment.setAssignmentId(String.valueOf(rs.getInt("CourseStaffID")));
                    assignment.setCourseId(String.valueOf(rs.getInt("CourseID")));
                    assignment.setRole(rs.getString("Role"));
                    
                    Timestamp createdDate = rs.getTimestamp("CreatedDate");
                    if (createdDate != null) {
                        assignment.setAssignedDate(createdDate.toLocalDateTime());
                    }
                    
                    // Load course details
                    String courseId = String.valueOf(rs.getInt("CourseID"));
                    Course course = courseService.getCourseById(courseId);
                    assignment.setCourse(course);
                    
                    assignments.add(assignment);
                }
            }
        }
        
        return assignments;
    }
    
    /**
     * Get all staff members (for selection in UI)
     */
    public List<User> getAllStaff() throws SQLException {
        String sql = "SELECT u.UserID, u.Username, u.Email, u.UserType, s.Department " +
                    "FROM Users u " +
                    "INNER JOIN Staff s ON u.UserID = s.UserID " +
                    "WHERE u.UserType = 'STAFF' " +
                    "ORDER BY u.Username";
        
        List<User> staffList = new ArrayList<>();
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    User staff = new edu.facilities.model.Staff(
                        String.valueOf(rs.getInt("UserID")),
                        rs.getString("Username"),
                        null // Password not needed
                    );
                    staffList.add(staff);
                }
            }
        }
        
        return staffList;
    }
    
    /**
     * Remove a staff assignment from a course
     */
    public boolean removeStaffAssignment(String courseId, String staffUserId) throws SQLException {
        String sql = "DELETE FROM CourseStaff WHERE CourseID = ? AND StaffUserID = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, Integer.parseInt(courseId));
            pstmt.setInt(2, Integer.parseInt(staffUserId));
            
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        }
    }
    
    /**
     * Check if staff is assigned to a course
     */
    public boolean isStaffAssignedToCourse(String courseId, String staffUserId) throws SQLException {
        String sql = "SELECT COUNT(*) AS AssignmentCount " +
                    "FROM CourseStaff " +
                    "WHERE CourseID = ? AND StaffUserID = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, Integer.parseInt(courseId));
            pstmt.setInt(2, Integer.parseInt(staffUserId));
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("AssignmentCount") > 0;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Check if a user is a staff member
     */
    private boolean isStaff(String userId) throws SQLException {
        String sql = "SELECT COUNT(*) AS StaffCount " +
                    "FROM Users u " +
                    "INNER JOIN Staff s ON u.UserID = s.UserID " +
                    "WHERE u.UserID = ? AND u.UserType = 'STAFF'";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, Integer.parseInt(userId));
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("StaffCount") > 0;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Inner class to represent a staff-course assignment
     */
    public static class CourseAssignment {
        private String assignmentId;
        private String courseId;
        private Course course;
        private String role;
        private java.time.LocalDateTime assignedDate;
        
        public CourseAssignment() {}
        
        // Getters and Setters
        public String getAssignmentId() { return assignmentId; }
        public void setAssignmentId(String assignmentId) { this.assignmentId = assignmentId; }
        
        public String getCourseId() { return courseId; }
        public void setCourseId(String courseId) { this.courseId = courseId; }
        
        public Course getCourse() { return course; }
        public void setCourse(Course course) { this.course = course; }
        
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        
        public java.time.LocalDateTime getAssignedDate() { return assignedDate; }
        public void setAssignedDate(java.time.LocalDateTime assignedDate) { this.assignedDate = assignedDate; }
    }
}



