package edu.facilities.service;

import edu.facilities.model.Course;
import edu.facilities.model.CourseAttribute;
import edu.facilities.model.CourseAttributeType;
import edu.facilities.model.CourseType;
import edu.facilities.model.User;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for managing courses in the catalog
 * US 2.1 - Manage Course Catalog (Admin)
 */
public class CourseService {
    
    /**
     * Create a new course
     */
    public Course createCourse(String code, String name, String description, int credits,
                              String department, String semester, CourseType type, int maxSeats) throws SQLException {
        String sql = "INSERT INTO Courses (Code, Name, Description, Credits, Department, Semester, Type, MaxSeats, CurrentSeats, IsActive, CreatedDate, UpdatedDate) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 0, 1, GETDATE(), GETDATE())";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setString(1, code);
            pstmt.setString(2, name);
            pstmt.setString(3, description);
            pstmt.setInt(4, credits);
            pstmt.setString(5, department);
            pstmt.setString(6, semester);
            pstmt.setString(7, type.toString());
            pstmt.setInt(8, maxSeats);
            
            int rowsAffected = pstmt.executeUpdate();
            
            if (rowsAffected > 0) {
                try (ResultSet keys = pstmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        int courseId = keys.getInt(1);
                        return getCourseById(String.valueOf(courseId));
                    }
                }
            }
        }
        
        return null;
    }
    
    /**
     * Update an existing course
     */
    public boolean updateCourse(String courseId, String code, String name, String description, int credits,
                               String department, String semester, CourseType type, int maxSeats) throws SQLException {
        String sql = "UPDATE Courses SET Code = ?, Name = ?, Description = ?, Credits = ?, " +
                    "Department = ?, Semester = ?, Type = ?, MaxSeats = ?, UpdatedDate = GETDATE() " +
                    "WHERE CourseID = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, code);
            pstmt.setString(2, name);
            pstmt.setString(3, description);
            pstmt.setInt(4, credits);
            pstmt.setString(5, department);
            pstmt.setString(6, semester);
            pstmt.setString(7, type.toString());
            pstmt.setInt(8, maxSeats);
            pstmt.setInt(9, Integer.parseInt(courseId));
            
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        }
    }
    
    /**
     * Delete a course (soft delete - sets IsActive = 0)
     * US 2.1 - Cannot delete if students are enrolled
     */
    public boolean deleteCourse(String courseId) throws SQLException {
        // Check if students are enrolled
        String checkEnrollmentsSql = "SELECT COUNT(*) AS EnrollmentCount FROM Enrollments " +
                                    "WHERE CourseID = ? AND Status = 'ENROLLED'";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(checkEnrollmentsSql)) {
            
            pstmt.setInt(1, Integer.parseInt(courseId));
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    int enrollmentCount = rs.getInt("EnrollmentCount");
                    if (enrollmentCount > 0) {
                        throw new IllegalArgumentException(
                            "Cannot delete course: " + enrollmentCount + " student(s) are currently enrolled. " +
                            "Please drop all enrollments first.");
                    }
                }
            }
        }
        
        // Soft delete by setting IsActive = 0
        String sql = "UPDATE Courses SET IsActive = 0, UpdatedDate = GETDATE() WHERE CourseID = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, Integer.parseInt(courseId));
            
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        }
    }
    
    /**
     * Get course by ID
     */
    public Course getCourseById(String courseId) throws SQLException {
        String sql = "SELECT CourseID, Code, Name, Description, Credits, Department, Semester, Type, " +
                    "MaxSeats, CurrentSeats, IsActive, CreatedDate, UpdatedDate " +
                    "FROM Courses WHERE CourseID = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, Integer.parseInt(courseId));
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Course course = mapResultSetToCourse(rs);
                    // Load related data
                    loadCourseRelations(course, conn);
                    return course;
                }
            }
        }
        
        return null;
    }
    
    /**
     * Get course by code
     */
    public Course getCourseByCode(String code) throws SQLException {
        String sql = "SELECT CourseID, Code, Name, Description, Credits, Department, Semester, Type, " +
                    "MaxSeats, CurrentSeats, IsActive, CreatedDate, UpdatedDate " +
                    "FROM Courses WHERE Code = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, code);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Course course = mapResultSetToCourse(rs);
                    loadCourseRelations(course, conn);
                    return course;
                }
            }
        }
        
        return null;
    }
    
    /**
     * Get all courses (optionally filter by active status)
     */
    public List<Course> getAllCourses(boolean activeOnly) throws SQLException {
        String sql = "SELECT CourseID, Code, Name, Description, Credits, Department, Semester, Type, " +
                    "MaxSeats, CurrentSeats, IsActive, CreatedDate, UpdatedDate " +
                    "FROM Courses " +
                    (activeOnly ? "WHERE IsActive = 1 " : "") +
                    "ORDER BY Code";
        
        List<Course> courses = new ArrayList<>();
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                Course course = mapResultSetToCourse(rs);
                loadCourseRelations(course, conn);
                courses.add(course);
            }
        }
        
        return courses;
    }
    
    /**
     * Search courses by keyword (searches code, name, description, department)
     */
    public List<Course> searchCourses(String keyword, boolean activeOnly) throws SQLException {
        String sql = "SELECT CourseID, Code, Name, Description, Credits, Department, Semester, Type, " +
                    "MaxSeats, CurrentSeats, IsActive, CreatedDate, UpdatedDate " +
                    "FROM Courses " +
                    "WHERE (Code LIKE ? OR Name LIKE ? OR Description LIKE ? OR Department LIKE ?) " +
                    (activeOnly ? "AND IsActive = 1 " : "") +
                    "ORDER BY Code";
        
        List<Course> courses = new ArrayList<>();
        String searchPattern = "%" + keyword + "%";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, searchPattern);
            pstmt.setString(2, searchPattern);
            pstmt.setString(3, searchPattern);
            pstmt.setString(4, searchPattern);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Course course = mapResultSetToCourse(rs);
                    loadCourseRelations(course, conn);
                    courses.add(course);
                }
            }
        }
        
        return courses;
    }
    
    /**
     * Filter courses by department, semester, type
     */
    public List<Course> filterCourses(String department, String semester, CourseType type, boolean activeOnly) throws SQLException {
        StringBuilder sql = new StringBuilder(
            "SELECT CourseID, Code, Name, Description, Credits, Department, Semester, Type, " +
            "MaxSeats, CurrentSeats, IsActive, CreatedDate, UpdatedDate " +
            "FROM Courses WHERE 1=1 "
        );
        
        List<String> params = new ArrayList<>();
        
        if (activeOnly) {
            sql.append("AND IsActive = 1 ");
        }
        if (department != null && !department.isEmpty()) {
            sql.append("AND Department = ? ");
            params.add(department);
        }
        if (semester != null && !semester.isEmpty()) {
            sql.append("AND Semester = ? ");
            params.add(semester);
        }
        if (type != null) {
            sql.append("AND Type = ? ");
            params.add(type.toString());
        }
        
        sql.append("ORDER BY Code");
        
        List<Course> courses = new ArrayList<>();
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            
            for (int i = 0; i < params.size(); i++) {
                pstmt.setString(i + 1, params.get(i));
            }
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Course course = mapResultSetToCourse(rs);
                    loadCourseRelations(course, conn);
                    courses.add(course);
                }
            }
        }
        
        return courses;
    }
    
    /**
     * Add a professor to a course
     */
    public boolean addProfessorToCourse(String courseId, String professorUserId) throws SQLException {
        String sql = "INSERT INTO CourseProfessors (CourseID, ProfessorUserID) VALUES (?, ?)";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, Integer.parseInt(courseId));
            pstmt.setInt(2, Integer.parseInt(professorUserId));
            
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            if (e.getErrorCode() == 2627 || e.getMessage().contains("UNIQUE")) {
                // Duplicate entry - professor already assigned
                return false;
            }
            throw e;
        }
    }
    
    /**
     * Remove a professor from a course
     */
    public boolean removeProfessorFromCourse(String courseId, String professorUserId) throws SQLException {
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
     * Add a prerequisite to a course
     */
    public boolean addPrerequisite(String courseId, String prerequisiteCourseId) throws SQLException {
        String sql = "INSERT INTO Prerequisites (CourseID, PrerequisiteCourseID) VALUES (?, ?)";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, Integer.parseInt(courseId));
            pstmt.setInt(2, Integer.parseInt(prerequisiteCourseId));
            
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            if (e.getErrorCode() == 2627 || e.getMessage().contains("UNIQUE")) {
                // Duplicate entry - prerequisite already exists
                return false;
            }
            throw e;
        }
    }
    
    /**
     * Remove a prerequisite from a course
     */
    public boolean removePrerequisite(String courseId, String prerequisiteCourseId) throws SQLException {
        String sql = "DELETE FROM Prerequisites WHERE CourseID = ? AND PrerequisiteCourseID = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, Integer.parseInt(courseId));
            pstmt.setInt(2, Integer.parseInt(prerequisiteCourseId));
            
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        }
    }
    
    /**
     * Add or update a course attribute (EAV pattern)
     */
    public boolean setCourseAttribute(String courseId, String attributeName, String attributeValue,
                                     CourseAttributeType attributeType) throws SQLException {
        // Check if attribute exists
        String checkSql = "SELECT AttributeID FROM CourseAttributes WHERE CourseID = ? AND AttributeName = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement checkPstmt = conn.prepareStatement(checkSql)) {
            
            checkPstmt.setInt(1, Integer.parseInt(courseId));
            checkPstmt.setString(2, attributeName);
            
            try (ResultSet rs = checkPstmt.executeQuery()) {
                if (rs.next()) {
                    // Update existing attribute
                    String updateSql = "UPDATE CourseAttributes SET AttributeValue = ?, AttributeType = ?, " +
                                     "UpdatedDate = GETDATE() WHERE CourseID = ? AND AttributeName = ?";
                    
                    try (PreparedStatement updatePstmt = conn.prepareStatement(updateSql)) {
                        updatePstmt.setString(1, attributeValue);
                        updatePstmt.setString(2, attributeType.toString());
                        updatePstmt.setInt(3, Integer.parseInt(courseId));
                        updatePstmt.setString(4, attributeName);
                        
                        return updatePstmt.executeUpdate() > 0;
                    }
                } else {
                    // Insert new attribute
                    String insertSql = "INSERT INTO CourseAttributes (CourseID, AttributeName, AttributeValue, " +
                                     "AttributeType, CreatedDate, UpdatedDate) VALUES (?, ?, ?, ?, GETDATE(), GETDATE())";
                    
                    try (PreparedStatement insertPstmt = conn.prepareStatement(insertSql)) {
                        insertPstmt.setInt(1, Integer.parseInt(courseId));
                        insertPstmt.setString(2, attributeName);
                        insertPstmt.setString(3, attributeValue);
                        insertPstmt.setString(4, attributeType.toString());
                        
                        return insertPstmt.executeUpdate() > 0;
                    }
                }
            }
        }
    }
    
    /**
     * Get course attribute by name
     */
    public CourseAttribute getCourseAttribute(String courseId, String attributeName) throws SQLException {
        String sql = "SELECT AttributeID, CourseID, AttributeName, AttributeValue, AttributeType, " +
                    "CreatedDate, UpdatedDate FROM CourseAttributes " +
                    "WHERE CourseID = ? AND AttributeName = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, Integer.parseInt(courseId));
            pstmt.setString(2, attributeName);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToCourseAttribute(rs);
                }
            }
        }
        
        return null;
    }
    
    /**
     * Delete a course attribute
     */
    public boolean deleteCourseAttribute(String courseId, String attributeName) throws SQLException {
        String sql = "DELETE FROM CourseAttributes WHERE CourseID = ? AND AttributeName = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, Integer.parseInt(courseId));
            pstmt.setString(2, attributeName);
            
            return pstmt.executeUpdate() > 0;
        }
    }
    
    /**
     * Get all departments
     */
    public List<String> getAllDepartments() throws SQLException {
        String sql = "SELECT DISTINCT Department FROM Courses WHERE IsActive = 1 ORDER BY Department";
        List<String> departments = new ArrayList<>();
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                departments.add(rs.getString("Department"));
            }
        }
        
        return departments;
    }
    
    /**
     * Get all semesters
     */
    public List<String> getAllSemesters() throws SQLException {
        String sql = "SELECT DISTINCT Semester FROM Courses WHERE IsActive = 1 ORDER BY Semester DESC";
        List<String> semesters = new ArrayList<>();
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                semesters.add(rs.getString("Semester"));
            }
        }
        
        return semesters;
    }
    
    // Helper methods
    
    private Course mapResultSetToCourse(ResultSet rs) throws SQLException {
        Course course = new Course();
        course.setId(String.valueOf(rs.getInt("CourseID")));
        course.setCode(rs.getString("Code"));
        course.setName(rs.getString("Name"));
        course.setDescription(rs.getString("Description"));
        course.setCredits(rs.getInt("Credits"));
        course.setDepartment(rs.getString("Department"));
        course.setSemester(rs.getString("Semester"));
        course.setType(CourseType.valueOf(rs.getString("Type")));
        course.setMaxSeats(rs.getInt("MaxSeats"));
        course.setCurrentSeats(rs.getInt("CurrentSeats"));
        course.setActive(rs.getBoolean("IsActive"));
        
        Timestamp createdTs = rs.getTimestamp("CreatedDate");
        if (createdTs != null) {
            course.setCreatedDate(createdTs.toLocalDateTime());
        }
        
        Timestamp updatedTs = rs.getTimestamp("UpdatedDate");
        if (updatedTs != null) {
            course.setUpdatedDate(updatedTs.toLocalDateTime());
        }
        
        return course;
    }
    
    private void loadCourseRelations(Course course, Connection conn) throws SQLException {
        // Load professors
        String professorsSql = "SELECT u.UserID, u.Username, u.Email, u.UserType " +
                               "FROM CourseProfessors cp " +
                               "INNER JOIN Users u ON cp.ProfessorUserID = u.UserID " +
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
                    Course prereq = mapResultSetToCourse(rs);
                    prerequisites.add(prereq);
                }
                course.setPrerequisites(prerequisites);
            }
        }
        
        // Load attributes
        String attributesSql = "SELECT AttributeID, CourseID, AttributeName, AttributeValue, " +
                             "AttributeType, CreatedDate, UpdatedDate " +
                             "FROM CourseAttributes WHERE CourseID = ?";
        
        try (PreparedStatement pstmt = conn.prepareStatement(attributesSql)) {
            pstmt.setInt(1, Integer.parseInt(course.getId()));
            
            try (ResultSet rs = pstmt.executeQuery()) {
                List<CourseAttribute> attributes = new ArrayList<>();
                while (rs.next()) {
                    attributes.add(mapResultSetToCourseAttribute(rs));
                }
                course.setAttributes(attributes);
            }
        }
    }
    
    private CourseAttribute mapResultSetToCourseAttribute(ResultSet rs) throws SQLException {
        CourseAttribute attr = new CourseAttribute();
        attr.setId(String.valueOf(rs.getInt("AttributeID")));
        attr.setCourseId(String.valueOf(rs.getInt("CourseID")));
        attr.setAttributeName(rs.getString("AttributeName"));
        attr.setAttributeValue(rs.getString("AttributeValue"));
        attr.setAttributeType(CourseAttributeType.valueOf(rs.getString("AttributeType")));
        
        Timestamp createdTs = rs.getTimestamp("CreatedDate");
        if (createdTs != null) {
            attr.setCreatedDate(createdTs.toLocalDateTime());
        }
        
        Timestamp updatedTs = rs.getTimestamp("UpdatedDate");
        if (updatedTs != null) {
            attr.setUpdatedDate(updatedTs.toLocalDateTime());
        }
        
        return attr;
    }
}

