package edu.facilities.service;

import edu.facilities.model.AdmissionApplication;
import edu.facilities.model.ApplicationStatus;
import edu.facilities.model.User;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for managing admission applications
 * US 2.5 - Admission Application Management
 */
public class AdmissionService {

    /**
     * Get all admission applications
     * @return List of all applications
     * @throws SQLException if database error occurs
     */
    public List<AdmissionApplication> getAllApplications() throws SQLException {
        List<AdmissionApplication> applications = new ArrayList<>();
        String sql = "SELECT aa.ApplicationID, aa.FirstName, aa.LastName, aa.Email, aa.PhoneNumber, " +
                    "aa.DateOfBirth, aa.Address, aa.City, aa.State, aa.ZipCode, aa.Country, aa.Program, " +
                    "aa.PreviousEducation, aa.Documents, st.StatusCode as Status, aa.SubmittedDate, aa.ReviewedDate, " +
                    "aa.ReviewedByUserID, aa.Notes " +
                    "FROM AdmissionApplications aa " +
                    "INNER JOIN StatusTypes st ON aa.StatusTypeID = st.StatusTypeID " +
                    "WHERE st.EntityType = 'ADMISSION' " +
                    "ORDER BY aa.SubmittedDate DESC";

        Connection conn = DatabaseConnection.getConnection();
        if (conn == null || conn.isClosed()) {
            throw new SQLException("Database connection is closed or invalid");
        }

        List<ApplicationData> applicationDataList = new ArrayList<>();
        try (PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                ApplicationData data = new ApplicationData();
                data.applicationId = rs.getInt("ApplicationID");
                data.firstName = rs.getString("FirstName");
                data.lastName = rs.getString("LastName");
                data.email = rs.getString("Email");
                data.phoneNumber = rs.getString("PhoneNumber");
                Date dob = rs.getDate("DateOfBirth");
                data.dateOfBirth = dob != null ? dob.toLocalDate() : null;
                data.address = rs.getString("Address");
                data.city = rs.getString("City");
                data.state = rs.getString("State");
                data.zipCode = rs.getString("ZipCode");
                data.country = rs.getString("Country");
                data.program = rs.getString("Program");
                data.previousEducation = rs.getString("PreviousEducation");
                data.documents = rs.getString("Documents");
                data.statusStr = rs.getString("Status");
                Timestamp submitted = rs.getTimestamp("SubmittedDate");
                data.submittedDate = submitted != null ? submitted.toLocalDateTime() : LocalDateTime.now();
                Timestamp reviewed = rs.getTimestamp("ReviewedDate");
                data.reviewedDate = reviewed != null ? reviewed.toLocalDateTime() : null;
                data.reviewedByUserId = rs.getObject("ReviewedByUserID", Integer.class);
                data.notes = rs.getString("Notes");
                applicationDataList.add(data);
            }
        }

        // Convert to AdmissionApplication objects
        for (ApplicationData data : applicationDataList) {
            try {
                AdmissionApplication app = createApplicationFromData(data, conn);
                if (app != null) {
                    applications.add(app);
                }
            } catch (Exception e) {
                System.err.println("Error creating application from data for ID " + data.applicationId + ": " + e.getMessage());
                e.printStackTrace();
            }
        }

        return applications;
    }

    /**
     * Get application by ID
     * @param applicationId The application ID
     * @return AdmissionApplication or null if not found
     * @throws SQLException if database error occurs
     */
    public AdmissionApplication getApplicationById(String applicationId) throws SQLException {
        if (applicationId == null || applicationId.isBlank()) {
            return null;
        }

        int appId;
        try {
            appId = Integer.parseInt(applicationId);
        } catch (NumberFormatException e) {
            return null;
        }

        String sql = "SELECT ApplicationID, FirstName, LastName, Email, PhoneNumber, " +
                    "DateOfBirth, Address, City, State, ZipCode, Country, Program, " +
                    "PreviousEducation, Documents, Status, SubmittedDate, ReviewedDate, " +
                    "ReviewedByUserID, Notes " +
                    "FROM AdmissionApplications " +
                    "WHERE ApplicationID = ?";

        Connection conn = DatabaseConnection.getConnection();
        ApplicationData data = null;

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, appId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    data = new ApplicationData();
                    data.applicationId = rs.getInt("ApplicationID");
                    data.firstName = rs.getString("FirstName");
                    data.lastName = rs.getString("LastName");
                    data.email = rs.getString("Email");
                    data.phoneNumber = rs.getString("PhoneNumber");
                    Date dob = rs.getDate("DateOfBirth");
                    data.dateOfBirth = dob != null ? dob.toLocalDate() : null;
                    data.address = rs.getString("Address");
                    data.city = rs.getString("City");
                    data.state = rs.getString("State");
                    data.zipCode = rs.getString("ZipCode");
                    data.country = rs.getString("Country");
                    data.program = rs.getString("Program");
                    data.previousEducation = rs.getString("PreviousEducation");
                    data.documents = rs.getString("Documents");
                    data.statusStr = rs.getString("Status");
                    Timestamp submitted = rs.getTimestamp("SubmittedDate");
                    data.submittedDate = submitted != null ? submitted.toLocalDateTime() : LocalDateTime.now();
                    Timestamp reviewed = rs.getTimestamp("ReviewedDate");
                    data.reviewedDate = reviewed != null ? reviewed.toLocalDateTime() : null;
                    data.reviewedByUserId = rs.getObject("ReviewedByUserID", Integer.class);
                    data.notes = rs.getString("Notes");
                }
            }
        }

        if (data != null) {
            return createApplicationFromData(data, conn);
        }
        return null;
    }

    /**
     * Update application status
     * @param applicationId The application ID
     * @param newStatus The new status
     * @param reviewedBy The admin user reviewing
     * @param notes Optional notes
     * @return true if update successful
     * @throws SQLException if database error occurs
     */
    public boolean updateApplicationStatus(String applicationId, ApplicationStatus newStatus,
                                         User reviewedBy, String notes) throws SQLException {
        if (applicationId == null || applicationId.isBlank() || newStatus == null) {
            throw new IllegalArgumentException("Application ID and status are required");
        }

        int appId;
        int reviewerId = -1;
        try {
            appId = Integer.parseInt(applicationId);
            if (reviewedBy != null && reviewedBy.getId() != null) {
                reviewerId = Integer.parseInt(reviewedBy.getId());
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid application ID or reviewer ID format");
        }

        // Get StatusTypeID for the new status
        int statusTypeId = getStatusTypeId(statusToString(newStatus), "ADMISSION");
        
        String sql = "UPDATE AdmissionApplications SET StatusTypeID = ?, ReviewedDate = CURRENT_TIMESTAMP, " +
                    "ReviewedByUserID = ?, Notes = ? WHERE ApplicationID = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, statusTypeId);
            if (reviewerId > 0) {
                pstmt.setInt(2, reviewerId);
            } else {
                pstmt.setNull(2, Types.INTEGER);
            }
            pstmt.setString(3, notes != null ? notes : "");
            pstmt.setInt(4, appId);

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        }
    }

    /**
     * Create a new admission application (for testing or manual entry)
     * @param firstName First name
     * @param lastName Last name
     * @param email Email
     * @param program Program applying for
     * @return Created AdmissionApplication or null if failed
     * @throws SQLException if database error occurs
     */
    public AdmissionApplication createApplication(String firstName, String lastName, String email,
                                                  String program) throws SQLException {
        if (firstName == null || firstName.isBlank() || 
            lastName == null || lastName.isBlank() || 
            email == null || email.isBlank()) {
            throw new IllegalArgumentException("First name, last name, and email are required");
        }

        String sql = "INSERT INTO AdmissionApplications " +
                    "(FirstName, LastName, Email, Program, Status) " +
                    "VALUES (?, ?, ?, ?, 'SUBMITTED')";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setString(1, firstName);
            pstmt.setString(2, lastName);
            pstmt.setString(3, email);
            pstmt.setString(4, program != null ? program : "");

            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                try (ResultSet keys = pstmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        int appId = keys.getInt(1);
                        return getApplicationById(String.valueOf(appId));
                    }
                }
            }
        }
        return null;
    }

    // Helper methods

    private AdmissionApplication createApplicationFromData(ApplicationData data, Connection conn) throws SQLException {
        // Get reviewed by user if available
        User reviewedBy = null;
        if (data.reviewedByUserId != null) {
            reviewedBy = getUserById(conn, data.reviewedByUserId);
        }

        // Convert status string to enum
        ApplicationStatus status = ApplicationStatus.SUBMITTED;
        if (data.statusStr != null) {
            try {
                status = ApplicationStatus.valueOf(data.statusStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                status = ApplicationStatus.SUBMITTED;
            }
        }

        return new AdmissionApplication(
            String.valueOf(data.applicationId),
            data.firstName,
            data.lastName,
            data.email,
            data.phoneNumber,
            data.dateOfBirth,
            data.address,
            data.city,
            data.state,
            data.zipCode,
            data.country,
            data.program,
            data.previousEducation,
            data.documents,
            status,
            data.submittedDate,
            data.reviewedDate,
            reviewedBy,
            data.notes
        );
    }

    private User getUserById(Connection conn, int userId) throws SQLException {
        String sql = "SELECT u.UserID, u.Username, u.Email, ut.TypeCode as UserType " +
                     "FROM Users u " +
                     "INNER JOIN UserRoles ur ON u.UserID = ur.UserID AND ur.IsPrimary = true " +
                     "INNER JOIN UserTypes ut ON ur.UserTypeID = ut.UserTypeID " +
                     "WHERE u.UserID = ?";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String id = String.valueOf(rs.getInt("UserID"));
                    String username = rs.getString("Username");
                    String userType = rs.getString("UserType");
                    return createUser(id, username, userType);
                }
            }
        }
        return null;
    }

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

    private String statusToString(ApplicationStatus status) {
        if (status == null) return "SUBMITTED";
        switch (status) {
            case SUBMITTED: return "SUBMITTED";
            case UNDER_REVIEW: return "UNDER_REVIEW";
            case ACCEPTED: return "ACCEPTED";
            case REJECTED: return "REJECTED";
            default: return "SUBMITTED";
        }
    }

    // Helper class for application data
    private static class ApplicationData {
        int applicationId;
        String firstName;
        String lastName;
        String email;
        String phoneNumber;
        LocalDate dateOfBirth;
        String address;
        String city;
        String state;
        String zipCode;
        String country;
        String program;
        String previousEducation;
        String documents;
        String statusStr;
        LocalDateTime submittedDate;
        LocalDateTime reviewedDate;
        Integer reviewedByUserId;
        String notes;
    }
    
    /**
     * Get StatusTypeID from StatusTypes table
     */
    private int getStatusTypeId(String statusCode, String entityType) throws SQLException {
        String sql = "SELECT StatusTypeID FROM StatusTypes WHERE StatusCode = ? AND EntityType = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, statusCode);
            pstmt.setString(2, entityType);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("StatusTypeID");
                }
            }
        }
        throw new SQLException("Status type not found: " + statusCode + " for entity: " + entityType);
    }
}


