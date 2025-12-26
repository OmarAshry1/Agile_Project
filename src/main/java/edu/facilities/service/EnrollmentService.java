package edu.facilities.service;

import edu.facilities.model.Course;
import edu.facilities.model.Enrollment;
import edu.facilities.model.EnrollmentStatus;
import edu.facilities.model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for managing student enrollments
 * US 2.3 - Enroll in Courses
 * US 2.4 - View My Enrolled Courses
 */
public class EnrollmentService {
    
    private static final int MAX_CREDITS = 18; // Maximum credits a student can enroll in
    
    /**
     * Enroll a student in a course
     * US 2.3 - Validates prerequisites, seat availability, and 18 credit limit
     */
    public Enrollment enrollStudent(User student, Course course) throws SQLException, IllegalArgumentException {
        // Validation 1: Check if course has available seats
        if (!course.hasAvailableSeats()) {
            throw new IllegalArgumentException("Course " + course.getCode() + " is full. No available seats.");
        }
        
        // Validation 2: Check prerequisites
        if (!hasCompletedPrerequisites(student, course)) {
            List<Course> prerequisites = course.getPrerequisites();
            StringBuilder prereqList = new StringBuilder();
            for (Course prereq : prerequisites) {
                if (prereqList.length() > 0) prereqList.append(", ");
                prereqList.append(prereq.getCode());
            }
            throw new IllegalArgumentException(
                "Prerequisites not met. Required: " + prereqList.toString() + 
                ". Please complete these courses before enrolling.");
        }
        
        // Validation 3: Check if student is already enrolled
        if (isEnrolled(student, course)) {
            throw new IllegalArgumentException("You are already enrolled in " + course.getCode());
        }
        
        // Validation 4: Check 18 credit limit
        int currentCredits = getTotalEnrolledCredits(student);
        if (currentCredits + course.getCredits() > MAX_CREDITS) {
            throw new IllegalArgumentException(
                "Enrollment would exceed maximum credit limit of " + MAX_CREDITS + " credits. " +
                "Current credits: " + currentCredits + ", Course credits: " + course.getCredits() + ".");
        }
        
        // All validations passed - enroll the student
        Connection conn = DatabaseConnection.getConnection();
        boolean originalAutoCommit = conn.getAutoCommit();
        
        try {
            conn.setAutoCommit(false);
            
            // Insert enrollment
            String enrollmentSql = "INSERT INTO Enrollments (StudentUserID, CourseID, EnrollmentDate, Status) " +
                                 "VALUES (?, ?, GETDATE(), 'ENROLLED')";
            
            try (PreparedStatement pstmt = conn.prepareStatement(enrollmentSql, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setInt(1, Integer.parseInt(student.getId()));
                pstmt.setInt(2, Integer.parseInt(course.getId()));
                
                int rowsAffected = pstmt.executeUpdate();
                
                if (rowsAffected > 0) {
                    // Update course seat count
                    String updateSeatsSql = "UPDATE Courses SET CurrentSeats = CurrentSeats + 1 WHERE CourseID = ?";
                    try (PreparedStatement updatePstmt = conn.prepareStatement(updateSeatsSql)) {
                        updatePstmt.setInt(1, Integer.parseInt(course.getId()));
                        updatePstmt.executeUpdate();
                    }
                    
                    conn.commit();
                    
                    // Get the created enrollment
                    try (ResultSet keys = pstmt.getGeneratedKeys()) {
                        if (keys.next()) {
                            int enrollmentId = keys.getInt(1);
                            return getEnrollmentById(String.valueOf(enrollmentId));
                        }
                    }
                }
            }
            
            conn.rollback();
            return null;
            
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            try {
                conn.setAutoCommit(originalAutoCommit);
            } catch (SQLException e) {
                System.err.println("Error restoring auto-commit: " + e.getMessage());
            }
        }
    }
    
    /**
     * Drop a student from a course
     */
    public boolean dropEnrollment(String enrollmentId) throws SQLException {
        // Get enrollment first to get course ID
        Enrollment enrollment = getEnrollmentById(enrollmentId);
        if (enrollment == null) {
            return false;
        }
        
        Connection conn = DatabaseConnection.getConnection();
        boolean originalAutoCommit = conn.getAutoCommit();
        
        try {
            conn.setAutoCommit(false);
            
            // Update enrollment status to DROPPED
            String sql = "UPDATE Enrollments SET Status = 'DROPPED' WHERE EnrollmentID = ?";
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, Integer.parseInt(enrollmentId));
                
                int rowsAffected = pstmt.executeUpdate();
                
                if (rowsAffected > 0) {
                    // Decrease course seat count
                    String updateSeatsSql = "UPDATE Courses SET CurrentSeats = CurrentSeats - 1 WHERE CourseID = ?";
                    try (PreparedStatement updatePstmt = conn.prepareStatement(updateSeatsSql)) {
                        updatePstmt.setInt(1, Integer.parseInt(enrollment.getCourse().getId()));
                        updatePstmt.executeUpdate();
                    }
                    
                    conn.commit();
                    return true;
                }
            }
            
            conn.rollback();
            return false;
            
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            try {
                conn.setAutoCommit(originalAutoCommit);
            } catch (SQLException e) {
                System.err.println("Error restoring auto-commit: " + e.getMessage());
            }
        }
    }
    
    /**
     * Get all enrollments for a student
     * US 2.4 - View My Enrolled Courses
     */
    public List<Enrollment> getStudentEnrollments(User student, boolean enrolledOnly) throws SQLException {
        String sql = "SELECT e.EnrollmentID, e.StudentUserID, e.CourseID, e.EnrollmentDate, e.Status, e.Grade, " +
                    "c.CourseID, c.Code, c.Name, c.Description, c.Credits, c.Department, c.Semester, c.Type, " +
                    "c.MaxSeats, c.CurrentSeats, c.IsActive, c.CreatedDate, c.UpdatedDate " +
                    "FROM Enrollments e " +
                    "INNER JOIN Courses c ON e.CourseID = c.CourseID " +
                    "WHERE e.StudentUserID = ? " +
                    (enrolledOnly ? "AND e.Status = 'ENROLLED' " : "") +
                    "ORDER BY e.EnrollmentDate DESC";
        
        List<Enrollment> enrollments = new ArrayList<>();
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, Integer.parseInt(student.getId()));
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Enrollment enrollment = mapResultSetToEnrollment(rs, student, conn);
                    enrollments.add(enrollment);
                }
            }
        }
        
        return enrollments;
    }
    
    /**
     * Get enrollment by ID
     */
    public Enrollment getEnrollmentById(String enrollmentId) throws SQLException {
        String sql = "SELECT e.EnrollmentID, e.StudentUserID, e.CourseID, e.EnrollmentDate, st.StatusCode as Status, e.Grade, " +
                    "c.CourseID, c.Code, c.Name, c.Description, c.Credits, d.Name as Department, sem.Code as Semester, ct.TypeCode as Type, " +
                    "c.MaxSeats, c.CurrentSeats, c.IsActive, c.CreatedDate, c.UpdatedDate, " +
                    "u.UserID, u.Username, u.Email, ut.TypeCode as UserType " +
                    "FROM Enrollments e " +
                    "INNER JOIN Courses c ON e.CourseID = c.CourseID " +
                    "LEFT JOIN Departments d ON c.DepartmentID = d.DepartmentID " +
                    "LEFT JOIN Semesters sem ON c.SemesterID = sem.SemesterID " +
                    "LEFT JOIN CourseTypes ct ON c.CourseTypeID = ct.CourseTypeID " +
                    "LEFT JOIN StatusTypes st ON e.StatusTypeID = st.StatusTypeID AND st.EntityType = 'ENROLLMENT' " +
                    "INNER JOIN Users u ON e.StudentUserID = u.UserID " +
                    "INNER JOIN UserRoles ur ON u.UserID = ur.UserID AND ur.IsPrimary = true " +
                    "INNER JOIN UserTypes ut ON ur.UserTypeID = ut.UserTypeID " +
                    "WHERE e.EnrollmentID = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, Integer.parseInt(enrollmentId));
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    User student = mapResultSetToUser(rs, 19); // Starting from column 19
                    return mapResultSetToEnrollment(rs, student, conn);
                }
            }
        }
        
        return null;
    }
    
    /**
     * Get total enrolled credits for a student
     */
    public int getTotalEnrolledCredits(User student) throws SQLException {
        String sql = "SELECT SUM(c.Credits) AS TotalCredits " +
                    "FROM Enrollments e " +
                    "INNER JOIN Courses c ON e.CourseID = c.CourseID " +
                    "WHERE e.StudentUserID = ? AND e.Status = 'ENROLLED'";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, Integer.parseInt(student.getId()));
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    int total = rs.getInt("TotalCredits");
                    return rs.wasNull() ? 0 : total;
                }
            }
        }
        
        return 0;
    }
    
    /**
     * Check if student has completed all prerequisites for a course
     */
    private boolean hasCompletedPrerequisites(User student, Course course) throws SQLException {
        List<Course> prerequisites = course.getPrerequisites();
        
        if (prerequisites.isEmpty()) {
            return true; // No prerequisites
        }
        
        // Get all completed courses for the student (prerequisites must be COMPLETED, not just ENROLLED)
        String sql = "SELECT DISTINCT c.CourseID " +
                    "FROM Enrollments e " +
                    "INNER JOIN Courses c ON e.CourseID = c.CourseID " +
                    "WHERE e.StudentUserID = ? AND e.Status = 'COMPLETED'";
        
        List<Integer> completedCourseIds = new ArrayList<>();
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, Integer.parseInt(student.getId()));
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    completedCourseIds.add(rs.getInt("CourseID"));
                }
            }
        }
        
        // Check if all prerequisites are in completed courses
        for (Course prereq : prerequisites) {
            int prereqId = Integer.parseInt(prereq.getId());
            if (!completedCourseIds.contains(prereqId)) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Check if student is already enrolled in a course
     */
    private boolean isEnrolled(User student, Course course) throws SQLException {
        String sql = "SELECT COUNT(*) AS Count FROM Enrollments " +
                    "WHERE StudentUserID = ? AND CourseID = ? AND Status = 'ENROLLED'";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, Integer.parseInt(student.getId()));
            pstmt.setInt(2, Integer.parseInt(course.getId()));
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("Count") > 0;
                }
            }
        }
        
        return false;
    }
    
    // Helper methods
    
    private Enrollment mapResultSetToEnrollment(ResultSet rs, User student, Connection conn) throws SQLException {
        Enrollment enrollment = new Enrollment();
        enrollment.setId(String.valueOf(rs.getInt("EnrollmentID")));
        enrollment.setStudent(student);
        
        // Map course
        Course course = mapResultSetToCourse(rs, 7); // Starting from column 7
        // Load course relations (professors, prerequisites) for full course data
        loadCourseRelations(course, conn);
        enrollment.setCourse(course);
        
        Timestamp enrollmentTs = rs.getTimestamp("EnrollmentDate");
        if (enrollmentTs != null) {
            enrollment.setEnrollmentDate(enrollmentTs.toLocalDateTime());
        }
        
        enrollment.setStatus(EnrollmentStatus.valueOf(rs.getString("Status")));
        enrollment.setGrade(rs.getString("Grade"));
        
        return enrollment;
    }
    
    private Course mapResultSetToCourse(ResultSet rs, int startColumn) throws SQLException {
        Course course = new Course();
        course.setId(String.valueOf(rs.getInt(startColumn)));
        course.setCode(rs.getString(startColumn + 1));
        course.setName(rs.getString(startColumn + 2));
        course.setDescription(rs.getString(startColumn + 3));
        course.setCredits(rs.getInt(startColumn + 4));
        course.setDepartment(rs.getString(startColumn + 5));
        course.setSemester(rs.getString(startColumn + 6));
        course.setType(edu.facilities.model.CourseType.valueOf(rs.getString(startColumn + 7)));
        course.setMaxSeats(rs.getInt(startColumn + 8));
        course.setCurrentSeats(rs.getInt(startColumn + 9));
        course.setActive(rs.getBoolean(startColumn + 10));
        
        Timestamp createdTs = rs.getTimestamp(startColumn + 11);
        if (createdTs != null) {
            course.setCreatedDate(createdTs.toLocalDateTime());
        }
        
        Timestamp updatedTs = rs.getTimestamp(startColumn + 12);
        if (updatedTs != null) {
            course.setUpdatedDate(updatedTs.toLocalDateTime());
        }
        
        return course;
    }
    
    /**
     * Load course relations (professors, prerequisites, attributes)
     * Used to populate full course data for enrollment views
     */
    private void loadCourseRelations(Course course, Connection conn) throws SQLException {
        // Load professors
        String professorsSql = "SELECT u.UserID, u.Username, u.Email, ut.TypeCode as UserType " +
                               "FROM CourseProfessors cp " +
                               "INNER JOIN Users u ON cp.ProfessorUserID = u.UserID " +
                               "INNER JOIN UserRoles ur ON u.UserID = ur.UserID AND ur.IsPrimary = true " +
                               "INNER JOIN UserTypes ut ON ur.UserTypeID = ut.UserTypeID " +
                               "WHERE cp.CourseID = ?";
        
        try (PreparedStatement pstmt = conn.prepareStatement(professorsSql)) {
            pstmt.setInt(1, Integer.parseInt(course.getId()));
            
            try (ResultSet rs = pstmt.executeQuery()) {
                List<User> professors = new ArrayList<>();
                while (rs.next()) {
                    // Create Professor object
                    User professor = new edu.facilities.model.Professor(
                        String.valueOf(rs.getInt("UserID")),
                        rs.getString("Username"),
                        null // Password not needed
                    );
                    professors.add(professor);
                }
                course.setProfessors(professors);
            }
        }
        
        // Load prerequisites
        String prerequisitesSql = "SELECT c.CourseID, c.Code, c.Name, c.Description, c.Credits, " +
                                  "c.Department, c.Semester, c.Type, c.MaxSeats, c.CurrentSeats, " +
                                  "c.IsActive, c.CreatedDate, c.UpdatedDate " +
                                  "FROM Prerequisites p " +
                                  "INNER JOIN Courses c ON p.PrerequisiteCourseID = c.CourseID " +
                                  "WHERE p.CourseID = ?";
        
        try (PreparedStatement pstmt = conn.prepareStatement(prerequisitesSql)) {
            pstmt.setInt(1, Integer.parseInt(course.getId()));
            
            try (ResultSet rs = pstmt.executeQuery()) {
                List<Course> prerequisites = new ArrayList<>();
                while (rs.next()) {
                    Course prereq = mapResultSetToCourse(rs, 1); // Map from first column
                    prerequisites.add(prereq);
                }
                course.setPrerequisites(prerequisites);
            }
        }
    }
    
    private User mapResultSetToUser(ResultSet rs, int startColumn) throws SQLException {
        int userId = rs.getInt(startColumn);
        String username = rs.getString(startColumn + 1);
        // email is at startColumn + 2 but not used in User constructor
        rs.getString(startColumn + 2); // Skip email column
        String userType = rs.getString(startColumn + 3);
        
        // Create appropriate user type (simplified - you may need to load full data)
        // Note: User constructors take (id, username, password) - password is not available from this query
        switch (userType) {
            case "STUDENT":
                return new edu.facilities.model.Student(
                    String.valueOf(userId), username, null
                );
            case "PROFESSOR":
                return new edu.facilities.model.Professor(
                    String.valueOf(userId), username, null
                );
            case "STAFF":
                return new edu.facilities.model.Staff(
                    String.valueOf(userId), username, null
                );
            case "ADMIN":
                return new edu.facilities.model.Admin(
                    String.valueOf(userId), username, null
                );
            default:
                return new edu.facilities.model.Student(
                    String.valueOf(userId), username, null
                );
        }
    }
}

