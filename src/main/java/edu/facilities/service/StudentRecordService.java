package edu.facilities.service;

import edu.facilities.model.Student;
import edu.facilities.model.StudentStatus;
import edu.facilities.model.YearLevel;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for managing student records
 * US 2.1 - Log Student Records
 * Admin-only access for creating, editing, and deleting student records
 */
public class StudentRecordService {

    /**
     * Get all student records
     * @return List of all students with their records
     * @throws SQLException if database error occurs
     */
    public List<StudentRecord> getAllStudentRecords() throws SQLException {
        List<StudentRecord> records = new ArrayList<>();
        String sql = "SELECT u.UserID, u.USERNAME, u.Email, ut.TypeCode as UserType, " +
                    "s.StudentNumber, s.Major, d.Name as Department, " +
                    "s.EnrollmentDate, s.GPA, st.StatusCode as Status, s.AdmissionDate, yl.LevelName as YearLevel, s.Notes " +
                    "FROM Users u " +
                    "INNER JOIN UserRoles ur ON u.UserID = ur.UserID AND ur.IsPrimary = true " +
                    "INNER JOIN UserTypes ut ON ur.UserTypeID = ut.UserTypeID " +
                    "INNER JOIN Students s ON u.UserID = s.UserID " +
                    "LEFT JOIN Departments d ON s.DepartmentID = d.DepartmentID " +
                    "LEFT JOIN StatusTypes st ON s.StatusTypeID = st.StatusTypeID AND st.EntityType = 'STUDENT' " +
                    "LEFT JOIN YearLevels yl ON s.YearLevelID = yl.YearLevelID " +
                    "ORDER BY u.USERNAME";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                StudentRecord record = mapResultSetToStudentRecord(rs);
                records.add(record);
            }
        }
        
        return records;
    }

    /**
     * Get student record by user ID
     * @param userId The user ID
     * @return StudentRecord or null if not found
     * @throws SQLException if database error occurs
     */
    public StudentRecord getStudentRecordById(String userId) throws SQLException {
        if (userId == null || userId.isBlank()) {
            return null;
        }

        int userIdInt;
        try {
            userIdInt = Integer.parseInt(userId);
        } catch (NumberFormatException e) {
            return null;
        }

        String sql = "SELECT u.UserID, u.USERNAME, u.Email, u.UserType, " +
                    "s.StudentNumber, s.Major, s.Department, " +
                    "s.EnrollmentDate, s.GPA, s.Status, s.AdmissionDate, s.YearLevel, s.Notes " +
                    "FROM Users u " +
                    "INNER JOIN Students s ON u.UserID = s.UserID " +
                    "WHERE u.UserID = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userIdInt);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToStudentRecord(rs);
                }
            }
        }
        return null;
    }

    /**
     * Create a new student record
     * This creates both a User and a Student record
     * @param username Username (must be unique)
     * @param password Password (will be hashed)
     * @param email Email
     * @param studentNumber Student number
     * @param major Major
     * @param department Department
     * @param enrollmentDate Enrollment date
     * @param gpa GPA
     * @param status Student status
     * @param admissionDate Admission date
     * @param yearLevel Year level
     * @param notes Notes
     * @return Created StudentRecord or null if failed
     * @throws SQLException if database error occurs
     */
    public StudentRecord createStudentRecord(String username, String password, String email,
                                            String studentNumber, String major, String department,
                                            LocalDate enrollmentDate, Double gpa, StudentStatus status,
                                            LocalDate admissionDate, YearLevel yearLevel, String notes) throws SQLException {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new IllegalArgumentException("Username and password are required");
        }

        // Check if username already exists
        String checkSql = "SELECT COUNT(*) as count FROM Users WHERE USERNAME = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
            
            checkStmt.setString(1, username);
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next() && rs.getInt("count") > 0) {
                    throw new IllegalArgumentException("Username already exists: " + username);
                }
            }
        }

        // Hash password
        String hashedPassword = hashPassword(password);

        Connection conn = DatabaseConnection.getConnection();
        boolean originalAutoCommit = conn.getAutoCommit();
        
        try {
            conn.setAutoCommit(false);

            // Insert into Users table
            String userSql = "INSERT INTO Users (USERNAME, Password, Email, UserType) VALUES (?, ?, ?, 'STUDENT')";
            int userId = -1;
            
            try (PreparedStatement userStmt = conn.prepareStatement(userSql, Statement.RETURN_GENERATED_KEYS)) {
                userStmt.setString(1, username);
                userStmt.setString(2, hashedPassword);
                userStmt.setString(3, email != null && !email.isBlank() ? email : null);
                
                int rowsAffected = userStmt.executeUpdate();
                if (rowsAffected > 0) {
                    try (ResultSet keys = userStmt.getGeneratedKeys()) {
                        if (keys.next()) {
                            userId = keys.getInt(1);
                        }
                    }
                }
            }

            if (userId == -1) {
                conn.rollback();
                throw new SQLException("Failed to create user record");
            }

            // Insert into Students table
            String studentSql = "INSERT INTO Students (UserID, StudentNumber, Major, Department, " +
                               "EnrollmentDate, GPA, Status, AdmissionDate, YearLevel, Notes) " +
                               "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            
            try (PreparedStatement studentStmt = conn.prepareStatement(studentSql)) {
                studentStmt.setInt(1, userId);
                studentStmt.setString(2, studentNumber);
                studentStmt.setString(3, major);
                studentStmt.setString(4, department);
                studentStmt.setDate(5, enrollmentDate != null ? Date.valueOf(enrollmentDate) : null);
                studentStmt.setObject(6, gpa, Types.DECIMAL);
                studentStmt.setString(7, statusToString(status));
                studentStmt.setDate(8, admissionDate != null ? Date.valueOf(admissionDate) : null);
                studentStmt.setString(9, yearLevel != null ? yearLevelToString(yearLevel) : null);
                studentStmt.setString(10, notes);

                studentStmt.executeUpdate();
            }

            conn.commit();
            
            // Return the created record
            return getStudentRecordById(String.valueOf(userId));
            
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(originalAutoCommit);
        }
    }

    /**
     * Update an existing student record
     * @param userId The user ID
     * @param email Email (optional)
     * @param studentNumber Student number (optional)
     * @param major Major (optional)
     * @param department Department (optional)
     * @param enrollmentDate Enrollment date (optional)
     * @param gpa GPA (optional)
     * @param status Student status (optional)
     * @param admissionDate Admission date (optional)
     * @param yearLevel Year level (optional)
     * @param notes Notes (optional)
     * @return Updated StudentRecord or null if failed
     * @throws SQLException if database error occurs
     */
    public StudentRecord updateStudentRecord(String userId, String email, String studentNumber,
                                            String major, String department, LocalDate enrollmentDate,
                                            Double gpa, StudentStatus status, LocalDate admissionDate,
                                            YearLevel yearLevel, String notes) throws SQLException {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("User ID is required");
        }

        int userIdInt;
        try {
            userIdInt = Integer.parseInt(userId);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid user ID format");
        }

        Connection conn = DatabaseConnection.getConnection();
        boolean originalAutoCommit = conn.getAutoCommit();
        
        try {
            conn.setAutoCommit(false);

            // Update Users table if email is provided
            if (email != null) {
                String userSql = "UPDATE Users SET Email = ? WHERE UserID = ?";
                try (PreparedStatement userStmt = conn.prepareStatement(userSql)) {
                    userStmt.setString(1, email.isBlank() ? null : email);
                    userStmt.setInt(2, userIdInt);
                    userStmt.executeUpdate();
                }
            }

            // Update Students table
            String studentSql = "UPDATE Students SET " +
                               "StudentNumber = COALESCE(?, StudentNumber), " +
                               "Major = COALESCE(?, Major), " +
                               "Department = COALESCE(?, Department), " +
                               "EnrollmentDate = COALESCE(?, EnrollmentDate), " +
                               "GPA = COALESCE(?, GPA), " +
                               "Status = COALESCE(?, Status), " +
                               "AdmissionDate = COALESCE(?, AdmissionDate), " +
                               "YearLevel = COALESCE(?, YearLevel), " +
                               "Notes = COALESCE(?, Notes) " +
                               "WHERE UserID = ?";
            
            try (PreparedStatement studentStmt = conn.prepareStatement(studentSql)) {
                int paramIndex = 1;
                studentStmt.setString(paramIndex++, studentNumber);
                studentStmt.setString(paramIndex++, major);
                studentStmt.setString(paramIndex++, department);
                studentStmt.setDate(paramIndex++, enrollmentDate != null ? Date.valueOf(enrollmentDate) : null);
                studentStmt.setObject(paramIndex++, gpa, Types.DECIMAL);
                studentStmt.setString(paramIndex++, status != null ? statusToString(status) : null);
                studentStmt.setDate(paramIndex++, admissionDate != null ? Date.valueOf(admissionDate) : null);
                studentStmt.setString(paramIndex++, yearLevel != null ? yearLevelToString(yearLevel) : null);
                studentStmt.setString(paramIndex++, notes);
                studentStmt.setInt(paramIndex++, userIdInt);

                int rowsAffected = studentStmt.executeUpdate();
                if (rowsAffected == 0) {
                    conn.rollback();
                    throw new SQLException("Student record not found for user ID: " + userId);
                }
            }

            conn.commit();
            
            // Return the updated record
            return getStudentRecordById(userId);
            
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(originalAutoCommit);
        }
    }

    /**
     * Delete a student record
     * This deletes both the Student record and the User record
     * @param userId The user ID
     * @return true if deletion successful
     * @throws SQLException if database error occurs
     */
    public boolean deleteStudentRecord(String userId) throws SQLException {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("User ID is required");
        }

        int userIdInt;
        try {
            userIdInt = Integer.parseInt(userId);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid user ID format");
        }

        Connection conn = DatabaseConnection.getConnection();
        boolean originalAutoCommit = conn.getAutoCommit();
        
        try {
            conn.setAutoCommit(false);

            // Delete from Students table first (foreign key constraint)
            String studentSql = "DELETE FROM Students WHERE UserID = ?";
            try (PreparedStatement studentStmt = conn.prepareStatement(studentSql)) {
                studentStmt.setInt(1, userIdInt);
                studentStmt.executeUpdate();
            }

            // Delete from Users table
            String userSql = "DELETE FROM Users WHERE UserID = ?";
            try (PreparedStatement userStmt = conn.prepareStatement(userSql)) {
                userStmt.setInt(1, userIdInt);
                int rowsAffected = userStmt.executeUpdate();
                
                if (rowsAffected > 0) {
                    conn.commit();
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

    // Helper methods

    private StudentRecord mapResultSetToStudentRecord(ResultSet rs) throws SQLException {
        String userId = String.valueOf(rs.getInt("UserID"));
        String username = rs.getString("USERNAME");
        String email = rs.getString("Email");
        String studentNumber = rs.getString("StudentNumber");
        String major = rs.getString("Major");
        String department = rs.getString("Department");
        Date enrollmentDate = rs.getDate("EnrollmentDate");
        Double gpa = rs.getObject("GPA", Double.class);
        String statusStr = rs.getString("Status");
        Date admissionDate = rs.getDate("AdmissionDate");
        String yearLevelStr = rs.getString("YearLevel");
        String notes = rs.getString("Notes");

        // Convert status string to enum
        StudentStatus status = StudentStatus.ACTIVE;
        if (statusStr != null) {
            try {
                status = StudentStatus.valueOf(statusStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                status = StudentStatus.ACTIVE;
            }
        }

        // Convert year level string to enum
        YearLevel yearLevel = null;
        if (yearLevelStr != null) {
            try {
                yearLevel = YearLevel.valueOf(yearLevelStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                // Leave as null
            }
        }

        Student student = new Student(userId, username, null);
        
        return new StudentRecord(
            student,
            email,
            studentNumber,
            major,
            department,
            enrollmentDate != null ? enrollmentDate.toLocalDate() : null,
            gpa,
            status,
            admissionDate != null ? admissionDate.toLocalDate() : null,
            yearLevel,
            notes
        );
    }

    private String statusToString(StudentStatus status) {
        if (status == null) return "ACTIVE";
        switch (status) {
            case ACTIVE: return "ACTIVE";
            case INACTIVE: return "INACTIVE";
            case GRADUATED: return "GRADUATED";
            case SUSPENDED: return "SUSPENDED";
            case WITHDRAWN: return "WITHDRAWN";
            default: return "ACTIVE";
        }
    }

    private String yearLevelToString(YearLevel yearLevel) {
        if (yearLevel == null) return null;
        switch (yearLevel) {
            case FRESHMAN: return "FRESHMAN";
            case SOPHOMORE: return "SOPHOMORE";
            case JUNIOR: return "JUNIOR";
            case SENIOR: return "SENIOR";
            case GRADUATE: return "GRADUATE";
            default: return null;
        }
    }

    private String hashPassword(String password) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = md.digest(password.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            System.err.println("Warning: Password hashing failed, using plain text");
            return password;
        }
    }

    /**
     * Data class for student record with extended information
     */
    public static class StudentRecord {
        private final Student student;
        private String email;
        private String studentNumber;
        private String major;
        private String department;
        private LocalDate enrollmentDate;
        private Double gpa;
        private StudentStatus status;
        private LocalDate admissionDate;
        private YearLevel yearLevel;
        private String notes;

        public StudentRecord(Student student, String email, String studentNumber, String major,
                           String department, LocalDate enrollmentDate, Double gpa, StudentStatus status,
                           LocalDate admissionDate, YearLevel yearLevel, String notes) {
            this.student = student;
            this.email = email;
            this.studentNumber = studentNumber;
            this.major = major;
            this.department = department;
            this.enrollmentDate = enrollmentDate;
            this.gpa = gpa;
            this.status = status;
            this.admissionDate = admissionDate;
            this.yearLevel = yearLevel;
            this.notes = notes;
        }

        // Getters
        public Student getStudent() { return student; }
        public String getEmail() { return email; }
        public String getStudentNumber() { return studentNumber; }
        public String getMajor() { return major; }
        public String getDepartment() { return department; }
        public LocalDate getEnrollmentDate() { return enrollmentDate; }
        public Double getGpa() { return gpa; }
        public StudentStatus getStatus() { return status; }
        public LocalDate getAdmissionDate() { return admissionDate; }
        public YearLevel getYearLevel() { return yearLevel; }
        public String getNotes() { return notes; }

        // Setters
        public void setEmail(String email) { this.email = email; }
        public void setStudentNumber(String studentNumber) { this.studentNumber = studentNumber; }
        public void setMajor(String major) { this.major = major; }
        public void setDepartment(String department) { this.department = department; }
        public void setEnrollmentDate(LocalDate enrollmentDate) { this.enrollmentDate = enrollmentDate; }
        public void setGpa(Double gpa) { this.gpa = gpa; }
        public void setStatus(StudentStatus status) { this.status = status; }
        public void setAdmissionDate(LocalDate admissionDate) { this.admissionDate = admissionDate; }
        public void setYearLevel(YearLevel yearLevel) { this.yearLevel = yearLevel; }
        public void setNotes(String notes) { this.notes = notes; }
    }
}


