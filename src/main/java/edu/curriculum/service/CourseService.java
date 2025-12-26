package edu.curriculum.service;

import edu.curriculum.model.Course;
import edu.curriculum.model.CourseType;
import edu.facilities.model.User;
import edu.facilities.service.DatabaseConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for managing course data using SQL Server database
 */
public class CourseService {

    /**
     * Get all courses from the database
     * @return List of all courses
     * @throws SQLException if database error occurs
     */
    public List<Course> getAllCourses() throws SQLException {
        List<Course> courses = new ArrayList<>();
        String sql = "SELECT c.CourseID, c.Code, c.Name, c.Description, c.Credits, " +
                     "d.Name as Department, s.Code as Semester, ct.TypeCode as Type, " +
                     "c.ProfessorUserID, c.CreatedDate " +
                     "FROM Courses c " +
                     "INNER JOIN Departments d ON c.DepartmentID = d.DepartmentID " +
                     "INNER JOIN Semesters s ON c.SemesterID = s.SemesterID " +
                     "INNER JOIN CourseTypes ct ON c.CourseTypeID = ct.CourseTypeID";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                Course course = mapResultSetToCourse(rs, conn);
                if (course != null) {
                    courses.add(course);
                }
            }
        }
        
        System.out.println("Retrieved " + courses.size() + " courses from database");
        return courses;
    }

    /**
     * Get course by ID
     * @param courseId The course ID
     * @return Course object if found, null otherwise
     * @throws SQLException if database error occurs
     */
    public Course getCourseById(String courseId) throws SQLException {
        if (courseId == null || courseId.isBlank()) {
            return null;
        }

        int courseIdInt;
        try {
            courseIdInt = Integer.parseInt(courseId);
        } catch (NumberFormatException e) {
            return null;
        }

        String sql = "SELECT c.CourseID, c.Code, c.Name, c.Description, c.Credits, " +
                     "d.Name as Department, s.Code as Semester, ct.TypeCode as Type, " +
                     "c.ProfessorUserID, c.CreatedDate " +
                     "FROM Courses c " +
                     "INNER JOIN Departments d ON c.DepartmentID = d.DepartmentID " +
                     "INNER JOIN Semesters s ON c.SemesterID = s.SemesterID " +
                     "INNER JOIN CourseTypes ct ON c.CourseTypeID = ct.CourseTypeID " +
                     "WHERE c.CourseID = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, courseIdInt);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToCourse(rs, conn);
                }
            }
        }
        return null;
    }

    /**
     * Get courses by professor ID
     * @param professorId The professor user ID
     * @return List of courses taught by the professor
     * @throws SQLException if database error occurs
     */
    public List<Course> getCoursesByProfessor(String professorId) throws SQLException {
        if (professorId == null || professorId.isBlank()) {
            System.err.println("WARNING: getCoursesByProfessor called with null or blank professorId");
            return new ArrayList<>();
        }

        int professorIdInt;
        try {
            professorIdInt = Integer.parseInt(professorId);
        } catch (NumberFormatException e) {
            System.err.println("ERROR: Invalid professorId format: " + professorId);
            return new ArrayList<>();
        }

        System.out.println("Querying courses for professor UserID: " + professorIdInt);
        
        List<Course> courses = new ArrayList<>();
        // Use CourseProfessors junction table (primary method)
        // Also check ProfessorUserID for backward compatibility with existing data
        String sql = "SELECT DISTINCT c.CourseID, c.Code, c.Name, c.Description, c.Credits, " +
                     "d.Name as Department, s.Code as Semester, ct.TypeCode as Type, " +
                     "c.MaxSeats, c.CurrentSeats, c.IsActive, c.CreatedDate, c.UpdatedDate, c.ProfessorUserID " +
                     "FROM Courses c " +
                     "INNER JOIN Departments d ON c.DepartmentID = d.DepartmentID " +
                     "INNER JOIN Semesters s ON c.SemesterID = s.SemesterID " +
                     "INNER JOIN CourseTypes ct ON c.CourseTypeID = ct.CourseTypeID " +
                     "INNER JOIN CourseProfessors cp ON c.CourseID = cp.CourseID " +
                     "WHERE cp.ProfessorUserID = ? AND (c.IsActive = true OR c.IsActive IS NULL) " +
                     "UNION " +
                     "SELECT DISTINCT c.CourseID, c.Code, c.Name, c.Description, c.Credits, " +
                     "d.Name as Department, s.Code as Semester, ct.TypeCode as Type, " +
                     "c.MaxSeats, c.CurrentSeats, c.IsActive, c.CreatedDate, c.UpdatedDate, c.ProfessorUserID " +
                     "FROM Courses c " +
                     "INNER JOIN Departments d ON c.DepartmentID = d.DepartmentID " +
                     "INNER JOIN Semesters s ON c.SemesterID = s.SemesterID " +
                     "INNER JOIN CourseTypes ct ON c.CourseTypeID = ct.CourseTypeID " +
                     "WHERE c.ProfessorUserID = ? AND (c.IsActive = true OR c.IsActive IS NULL) " +
                     "AND NOT EXISTS (SELECT 1 FROM CourseProfessors cp2 WHERE cp2.CourseID = c.CourseID) " +
                     "ORDER BY Code";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, professorIdInt);
            pstmt.setInt(2, professorIdInt);
            
            System.out.println("Executing SQL query for professor: " + professorIdInt);
            System.out.println("SQL: " + sql.replace("?", String.valueOf(professorIdInt)));
            
            try (ResultSet rs = pstmt.executeQuery()) {
                int count = 0;
                while (rs.next()) {
                    count++;
                    Course course = mapResultSetToCourse(rs, conn);
                    if (course != null) {
                        courses.add(course);
                        System.out.println("  - Found course: " + course.getCode() + " - " + course.getName() + " (ID: " + course.getId() + ")");
                    } else {
                        System.err.println("  - WARNING: mapResultSetToCourse returned null for row " + count);
                    }
                }
                
                if (count == 0) {
                    System.out.println("  - No courses found in database for ProfessorUserID = " + professorIdInt);
                    System.out.println("  - Suggestion: Check if courses exist and are assigned to this professor");
                }
            }
        } catch (SQLException e) {
            System.err.println("SQLException in getCoursesByProfessor: " + e.getMessage());
            System.err.println("SQL State: " + e.getSQLState());
            System.err.println("Error Code: " + e.getErrorCode());
            e.printStackTrace();
            throw e;
        }
        
        System.out.println("Retrieved " + courses.size() + " courses for professor " + professorId);
        return courses;
    }

    /**
     * Get enrolled courses for a student
     * @param studentId The student user ID
     * @return List of courses the student is enrolled in
     * @throws SQLException if database error occurs
     */
    public List<Course> getEnrolledCourses(String studentId) throws SQLException {
        if (studentId == null || studentId.isBlank()) {
            return new ArrayList<>();
        }

        int studentIdInt;
        try {
            studentIdInt = Integer.parseInt(studentId);
        } catch (NumberFormatException e) {
            return new ArrayList<>();
        }

        List<Course> courses = new ArrayList<>();
        String sql = "SELECT c.CourseID, c.Code, c.Name, c.Description, c.Credits, " +
                     "d.Name as Department, s.Code as Semester, ct.TypeCode as Type, " +
                     "c.ProfessorUserID, c.CreatedDate " +
                     "FROM Courses c " +
                     "INNER JOIN Departments d ON c.DepartmentID = d.DepartmentID " +
                     "INNER JOIN Semesters s ON c.SemesterID = s.SemesterID " +
                     "INNER JOIN CourseTypes ct ON c.CourseTypeID = ct.CourseTypeID " +
                     "INNER JOIN Enrollments e ON c.CourseID = e.CourseID " +
                     "WHERE e.StudentUserID = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, studentIdInt);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Course course = mapResultSetToCourse(rs, conn);
                    if (course != null) {
                        courses.add(course);
                    }
                }
            }
        }
        
        System.out.println("Retrieved " + courses.size() + " enrolled courses for student " + studentId);
        return courses;
    }

    /**
     * Enroll a student in a course
     * @param studentId The student user ID
     * @param courseId The course ID
     * @return true if enrollment successful, false otherwise
     * @throws SQLException if database error occurs
     */
    public boolean enrollStudent(String studentId, String courseId) throws SQLException {
        if (studentId == null || studentId.isBlank() || courseId == null || courseId.isBlank()) {
            return false;
        }

        int studentIdInt;
        int courseIdInt;
        try {
            studentIdInt = Integer.parseInt(studentId);
            courseIdInt = Integer.parseInt(courseId);
        } catch (NumberFormatException e) {
            return false;
        }

        // Check if already enrolled
        String checkSql = "SELECT EnrollmentID FROM Enrollments WHERE StudentUserID = ? AND CourseID = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
            
            checkStmt.setInt(1, studentIdInt);
            checkStmt.setInt(2, courseIdInt);
            
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next()) {
                    System.out.println("Student " + studentId + " is already enrolled in course " + courseId);
                    return false;
                }
            }
        }

        // Enroll student
        try (Connection conn = DatabaseConnection.getConnection()) {
            // Get StatusTypeID for ENROLLED status
            int enrolledStatusTypeId = getEnrollmentStatusTypeId(conn, "ENROLLED");
            if (enrolledStatusTypeId == 0) {
                throw new SQLException("ENROLLED status type not found");
            }
            
            String sql = "INSERT INTO Enrollments (StudentUserID, CourseID, EnrollmentDate, StatusTypeID) VALUES (?, ?, CURRENT_TIMESTAMP, ?)";
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, studentIdInt);
                pstmt.setInt(2, courseIdInt);
                pstmt.setInt(3, enrolledStatusTypeId);
                
                int rowsAffected = pstmt.executeUpdate();
                
                if (rowsAffected > 0) {
                    System.out.println("Student " + studentId + " enrolled in course " + courseId);
                    return true;
                }
            }
        }
        
        return false;
    }

    /**
     * Create a new course
     * @param code Course code
     * @param name Course name
     * @param description Course description
     * @param credits Number of credits
     * @param department Department
     * @param semester Semester
     * @param type Course type
     * @param professorId Professor user ID
     * @return Created Course object or null if failed
     * @throws SQLException if database error occurs
     */
    public Course createCourse(String code, String name, String description, int credits,
                               String department, String semester, CourseType type,
                               String professorId) throws SQLException {
        if (code == null || code.isBlank() || name == null || name.isBlank()) {
            return null;
        }

        int professorIdInt = 0;
        if (professorId != null && !professorId.isBlank()) {
            try {
                professorIdInt = Integer.parseInt(professorId);
            } catch (NumberFormatException e) {
                professorIdInt = 0;
            }
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            // Get DepartmentID from department name
            int departmentId = getDepartmentIdByName(conn, department);
            if (departmentId == 0) {
                throw new SQLException("Department not found: " + department);
            }
            
            // Get SemesterID from semester code
            int semesterId = getSemesterIdByCode(conn, semester);
            if (semesterId == 0) {
                throw new SQLException("Semester not found: " + semester);
            }
            
            // Get CourseTypeID from type code
            int courseTypeId = getCourseTypeIdByCode(conn, typeToString(type));
            if (courseTypeId == 0) {
                throw new SQLException("Course type not found: " + typeToString(type));
            }
            
            String sql = "INSERT INTO Courses (Code, Name, Description, Credits, DepartmentID, SemesterID, CourseTypeID, ProfessorUserID, CreatedDate) " +
                         "VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)";
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                
                pstmt.setString(1, code);
                pstmt.setString(2, name);
                pstmt.setString(3, description);
                pstmt.setInt(4, credits);
                pstmt.setInt(5, departmentId);
                pstmt.setInt(6, semesterId);
                pstmt.setInt(7, courseTypeId);
                if (professorIdInt > 0) {
                    pstmt.setInt(8, professorIdInt);
                } else {
                    pstmt.setNull(8, Types.INTEGER);
                }
                
                int rowsAffected = pstmt.executeUpdate();
                
                if (rowsAffected > 0) {
                    try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            int courseId = generatedKeys.getInt(1);
                            System.out.println("Course created with ID: " + courseId);
                            return getCourseById(String.valueOf(courseId));
                        }
                    }
                }
            }
        }
        
        return null;
    }

    /**
     * Map ResultSet row to Course object
     */
    private Course mapResultSetToCourse(ResultSet rs, Connection conn) throws SQLException {
        String courseId = String.valueOf(rs.getInt("CourseID"));
        String code = rs.getString("Code");
        String name = rs.getString("Name");
        String description = rs.getString("Description");
        int credits = rs.getInt("Credits");
        String department = rs.getString("Department");
        String semester = rs.getString("Semester");
        String typeStr = rs.getString("Type");
        Integer professorUserId = rs.getObject("ProfessorUserID", Integer.class);
        Timestamp createdDateTs = rs.getTimestamp("CreatedDate");
        
        CourseType type = stringToCourseType(typeStr);
        User professor = null;
        if (professorUserId != null && professorUserId > 0) {
            professor = getUserById(conn, professorUserId);
        }
        
        LocalDateTime createdDate = null;
        if (createdDateTs != null) {
            createdDate = createdDateTs.toLocalDateTime();
        }
        
        return new Course(courseId, code, name, description, credits, department, semester, type, professor, createdDate);
    }

    /**
     * Get DepartmentID by department name
     */
    private int getDepartmentIdByName(Connection conn, String departmentName) throws SQLException {
        if (departmentName == null || departmentName.isBlank()) {
            return 0;
        }
        
        String sql = "SELECT DepartmentID FROM Departments WHERE Name = ? OR Code = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, departmentName);
            pstmt.setString(2, departmentName);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("DepartmentID");
                }
            }
        }
        return 0;
    }

    /**
     * Get SemesterID by semester code
     */
    private int getSemesterIdByCode(Connection conn, String semesterCode) throws SQLException {
        if (semesterCode == null || semesterCode.isBlank()) {
            return 0;
        }
        
        String sql = "SELECT SemesterID FROM Semesters WHERE Code = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, semesterCode);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("SemesterID");
                }
            }
        }
        return 0;
    }

    /**
     * Get CourseTypeID by type code
     */
    private int getCourseTypeIdByCode(Connection conn, String typeCode) throws SQLException {
        if (typeCode == null || typeCode.isBlank()) {
            return 0;
        }
        
        String sql = "SELECT CourseTypeID FROM CourseTypes WHERE TypeCode = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, typeCode);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("CourseTypeID");
                }
            }
        }
        return 0;
    }

    /**
     * Get StatusTypeID for enrollment status
     */
    private int getEnrollmentStatusTypeId(Connection conn, String statusCode) throws SQLException {
        if (statusCode == null || statusCode.isBlank()) {
            return 0;
        }
        
        String sql = "SELECT StatusTypeID FROM StatusTypes WHERE EntityType = 'ENROLLMENT' AND StatusCode = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, statusCode);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("StatusTypeID");
                }
            }
        }
        return 0;
    }

    /**
     * Get user by ID
     */
    private User getUserById(Connection conn, int userId) throws SQLException {
        String sql = "SELECT u.UserID, u.USERNAME, u.Email, ut.TypeCode as UserType " +
                     "FROM Users u " +
                     "INNER JOIN UserRoles ur ON u.UserID = ur.UserID AND ur.IsPrimary = true " +
                     "INNER JOIN UserTypes ut ON ur.UserTypeID = ut.UserTypeID " +
                     "WHERE u.UserID = ?";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String id = String.valueOf(rs.getInt("UserID"));
                    String username = rs.getString("USERNAME");
                    String userType = rs.getString("UserType");
                    
                    return createUser(id, username, userType);
                }
            }
        }
        return null;
    }

    /**
     * Create appropriate User instance based on userType
     */
    private User createUser(String id, String username, String userType) {
        if (userType == null || userType.isBlank()) {
            return new edu.facilities.model.Student(id, username, null);
        }
        
        switch (userType.toUpperCase()) {
            case "STUDENT":
                return new edu.facilities.model.Student(id, username, null);
            case "PROFESSOR":
                return new edu.facilities.model.Professor(id, username, null);
            case "STAFF":
                return new edu.facilities.model.Staff(id, username, null);
            case "ADMIN":
                return new edu.facilities.model.Admin(id, username, null);
            default:
                return new edu.facilities.model.Student(id, username, null);
        }
    }

    /**
     * Convert CourseType enum to database string
     */
    private String typeToString(CourseType type) {
        if (type == null) return "CORE";
        switch (type) {
            case CORE: return "CORE";
            case ELECTIVE: return "ELECTIVE";
            default: return "CORE";
        }
    }

    /**
     * Convert database string to CourseType enum
     */
    private CourseType stringToCourseType(String typeStr) {
        if (typeStr == null) return CourseType.CORE;
        switch (typeStr.toUpperCase()) {
            case "CORE": return CourseType.CORE;
            case "ELECTIVE": return CourseType.ELECTIVE;
            default: return CourseType.CORE;
        }
    }
}

