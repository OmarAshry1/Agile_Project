package edu.facilities.service;

import edu.facilities.model.Student;
import edu.facilities.model.TranscriptRequest;
import edu.facilities.model.TranscriptStatus;
import edu.facilities.model.User;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for managing transcript requests
 * US 2.2, 2.3, 2.4 - Transcript Management
 */
public class TranscriptService {

    /**
     * Create a new transcript request (Student)
     * US 2.2 - Student Request Transcript
     * @param student The student requesting the transcript
     * @param purpose Optional purpose/description
     * @return Created TranscriptRequest or null if failed
     * @throws SQLException if database error occurs
     */
    public TranscriptRequest createTranscriptRequest(Student student, String purpose) throws SQLException {
        System.out.println("=== createTranscriptRequest START ===");
        
        if (student == null) {
            System.err.println("ERROR: Student is null");
            throw new IllegalArgumentException("Student is required");
        }

        System.out.println("Student ID: " + student.getId() + ", Username: " + student.getUsername());

        // Verify user is a student
        String userType = null;
        try {
            userType = getUserType(student);
            System.out.println("User type retrieved: " + userType);
        } catch (SQLException e) {
            System.err.println("ERROR: Failed to get user type: " + e.getMessage());
            e.printStackTrace();
            throw new SQLException("Failed to verify user type: " + e.getMessage(), e);
        }
        
        if (!"STUDENT".equals(userType)) {
            System.err.println("ERROR: User is not a student. UserType: " + userType);
            throw new IllegalArgumentException("Only students can request transcripts. Current type: " + userType);
        }

        int studentId;
        try {
            studentId = Integer.parseInt(student.getId());
            System.out.println("Parsed student ID: " + studentId);
        } catch (NumberFormatException e) {
            System.err.println("ERROR: Invalid student ID format: " + student.getId());
            throw new IllegalArgumentException("Invalid student ID: " + student.getId());
        }

        // Verify student exists in database
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement checkStmt = conn.prepareStatement("SELECT UserID, USERNAME, UserType FROM Users WHERE UserID = ?")) {
            checkStmt.setInt(1, studentId);
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (!rs.next()) {
                    System.err.println("ERROR: Student with UserID " + studentId + " does not exist in Users table");
                    throw new SQLException("Student with ID " + studentId + " not found in database");
                }
                System.out.println("Verified student exists: " + rs.getString("USERNAME") + " (Type: " + rs.getString("UserType") + ")");
            }
        }

        String sql = "INSERT INTO TranscriptRequests " +
                    "(StudentUserID, RequestedByUserID, Status, Purpose, RequestDate) " +
                    "VALUES (?, ?, 'PENDING', ?, GETDATE())";

        System.out.println("SQL: " + sql);
        System.out.println("Parameters: StudentUserID=" + studentId + ", RequestedByUserID=" + studentId + ", Purpose=" + (purpose != null ? purpose : "(null)"));

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setInt(1, studentId);
            pstmt.setInt(2, studentId);
            pstmt.setString(3, purpose != null ? purpose : "");

            System.out.println("Executing INSERT statement...");
            int rowsAffected = pstmt.executeUpdate();
            System.out.println("Rows affected: " + rowsAffected);

            if (rowsAffected > 0) {
                System.out.println("INSERT successful. Retrieving generated keys...");
                try (ResultSet keys = pstmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        // Try to get the key - SQL Server may return it as column 1 or by name
                        int requestId = 0;
                        try {
                            // Try by column index first (most reliable)
                            requestId = keys.getInt(1);
                            System.out.println("Generated RequestID (by index): " + requestId);
                        } catch (SQLException e) {
                            // Try by column name
                            try {
                                requestId = keys.getInt("RequestID");
                                System.out.println("Generated RequestID (by name): " + requestId);
                            } catch (SQLException e2) {
                                // Try SCOPE_IDENTITY() as fallback
                                System.err.println("WARNING: Could not get generated key from ResultSet");
                                System.err.println("Attempting to retrieve using SCOPE_IDENTITY()...");
                                try (PreparedStatement idStmt = conn.prepareStatement("SELECT SCOPE_IDENTITY() AS RequestID")) {
                                    try (ResultSet idRs = idStmt.executeQuery()) {
                                        if (idRs.next()) {
                                            requestId = idRs.getInt("RequestID");
                                            System.out.println("Retrieved RequestID via SCOPE_IDENTITY(): " + requestId);
                                        }
                                    }
                                }
                            }
                        }
                        
                        if (requestId > 0) {
                            System.out.println("Generated RequestID: " + requestId);
                            
                            TranscriptRequest request = getTranscriptRequestById(String.valueOf(requestId));
                            if (request != null) {
                                System.out.println("Successfully created and retrieved transcript request: " + request.getId());
                                System.out.println("=== createTranscriptRequest SUCCESS ===");
                                return request;
                            } else {
                                System.err.println("ERROR: Failed to retrieve created transcript request with ID: " + requestId);
                                System.err.println("Attempting direct query to verify request exists...");
                                // Try direct query to see if request exists
                                try (PreparedStatement verifyStmt = conn.prepareStatement("SELECT RequestID FROM TranscriptRequests WHERE RequestID = ?")) {
                                    verifyStmt.setInt(1, requestId);
                                    try (ResultSet verifyRs = verifyStmt.executeQuery()) {
                                        if (verifyRs.next()) {
                                            System.err.println("Request exists in database but getTranscriptRequestById returned null");
                                            System.err.println("This suggests an issue with getTranscriptRequestById() method");
                                        } else {
                                            System.err.println("Request does not exist in database - INSERT may have failed silently");
                                        }
                                    }
                                }
                                throw new SQLException("Failed to retrieve created transcript request with ID: " + requestId);
                            }
                        } else {
                            System.err.println("ERROR: Could not determine generated RequestID");
                            throw new SQLException("Could not determine generated RequestID from INSERT statement");
                        }
                    } else {
                        System.err.println("ERROR: No generated keys returned from INSERT");
                        System.err.println("ResultSet from getGeneratedKeys() is empty");
                        throw new SQLException("No generated keys returned from INSERT statement");
                    }
                }
            } else {
                System.err.println("ERROR: INSERT returned 0 rows affected");
                throw new SQLException("INSERT statement affected 0 rows. Request was not created.");
            }
        } catch (SQLException e) {
            System.err.println("=== SQLException in createTranscriptRequest ===");
            System.err.println("Error Message: " + e.getMessage());
            System.err.println("SQL State: " + e.getSQLState());
            System.err.println("Error Code: " + e.getErrorCode());
            System.err.println("SQL: " + sql);
            System.err.println("Student ID: " + studentId);
            e.printStackTrace();
            System.err.println("=== END SQLException ===");
            throw e;
        }
    }

    /**
     * Get all transcript requests for a specific student
     * US 2.3 - Student Request Transcript Status
     * @param student The student
     * @return List of transcript requests
     * @throws SQLException if database error occurs
     */
    public List<TranscriptRequest> getTranscriptRequestsByStudent(Student student) throws SQLException {
        List<TranscriptRequest> requests = new ArrayList<>();
        
        if (student == null || student.getId() == null) {
            return requests;
        }

        int studentId;
        try {
            studentId = Integer.parseInt(student.getId());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid student ID: " + student.getId());
        }

        String sql = "SELECT RequestID, StudentUserID, RequestedByUserID, ProcessedByUserID, " +
                    "RequestDate, ProcessedDate, CompletedDate, PickupDate, " +
                    "Status, Purpose, Notes, PDFPath " +
                    "FROM TranscriptRequests " +
                    "WHERE StudentUserID = ? " +
                    "ORDER BY RequestDate DESC";

        Connection conn = DatabaseConnection.getConnection();
        if (conn == null || conn.isClosed()) {
            throw new SQLException("Database connection is closed or invalid");
        }

        List<TranscriptRequestData> requestDataList = new ArrayList<>();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, studentId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    TranscriptRequestData data = new TranscriptRequestData();
                    data.requestId = rs.getInt("RequestID");
                    data.studentId = rs.getInt("StudentUserID");
                    data.requestedById = rs.getInt("RequestedByUserID");
                    data.processedById = rs.getObject("ProcessedByUserID", Integer.class);
                    data.requestDate = rs.getTimestamp("RequestDate");
                    data.processedDate = rs.getTimestamp("ProcessedDate");
                    data.completedDate = rs.getTimestamp("CompletedDate");
                    data.pickupDate = rs.getTimestamp("PickupDate");
                    data.statusStr = rs.getString("Status");
                    data.purpose = rs.getString("Purpose");
                    data.notes = rs.getString("Notes");
                    data.pdfPath = rs.getString("PDFPath");
                    requestDataList.add(data);
                }
            }
        }

        // Convert to TranscriptRequest objects
        for (TranscriptRequestData data : requestDataList) {
            try {
                TranscriptRequest request = createTranscriptRequestFromData(data, conn);
                if (request != null) {
                    requests.add(request);
                }
            } catch (Exception e) {
                System.err.println("Error creating transcript request from data for ID " + data.requestId + ": " + e.getMessage());
                e.printStackTrace();
            }
        }

        return requests;
    }

    /**
     * Get all transcript requests (Admin view)
     * @return List of all transcript requests
     * @throws SQLException if database error occurs
     */
    public List<TranscriptRequest> getAllTranscriptRequests() throws SQLException {
        List<TranscriptRequest> requests = new ArrayList<>();
        String sql = "SELECT RequestID, StudentUserID, RequestedByUserID, ProcessedByUserID, " +
                    "RequestDate, ProcessedDate, CompletedDate, PickupDate, " +
                    "Status, Purpose, Notes, PDFPath " +
                    "FROM TranscriptRequests " +
                    "ORDER BY RequestDate DESC";

        Connection conn = DatabaseConnection.getConnection();
        if (conn == null || conn.isClosed()) {
            throw new SQLException("Database connection is closed or invalid");
        }

        List<TranscriptRequestData> requestDataList = new ArrayList<>();
        try (PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                TranscriptRequestData data = new TranscriptRequestData();
                data.requestId = rs.getInt("RequestID");
                data.studentId = rs.getInt("StudentUserID");
                data.requestedById = rs.getInt("RequestedByUserID");
                data.processedById = rs.getObject("ProcessedByUserID", Integer.class);
                data.requestDate = rs.getTimestamp("RequestDate");
                data.processedDate = rs.getTimestamp("ProcessedDate");
                data.completedDate = rs.getTimestamp("CompletedDate");
                data.pickupDate = rs.getTimestamp("PickupDate");
                data.statusStr = rs.getString("Status");
                data.purpose = rs.getString("Purpose");
                data.notes = rs.getString("Notes");
                data.pdfPath = rs.getString("PDFPath");
                requestDataList.add(data);
            }
        }

        // Convert to TranscriptRequest objects
        for (TranscriptRequestData data : requestDataList) {
            try {
                TranscriptRequest request = createTranscriptRequestFromData(data, conn);
                if (request != null) {
                    requests.add(request);
                }
            } catch (Exception e) {
                System.err.println("Error creating transcript request from data for ID " + data.requestId + ": " + e.getMessage());
                e.printStackTrace();
            }
        }

        return requests;
    }

    /**
     * Get transcript request by ID
     * @param requestId The request ID
     * @return TranscriptRequest or null if not found
     * @throws SQLException if database error occurs
     */
    public TranscriptRequest getTranscriptRequestById(String requestId) throws SQLException {
        if (requestId == null || requestId.isBlank()) {
            return null;
        }

        int reqId;
        try {
            reqId = Integer.parseInt(requestId);
        } catch (NumberFormatException e) {
            return null;
        }

        String sql = "SELECT RequestID, StudentUserID, RequestedByUserID, ProcessedByUserID, " +
                    "RequestDate, ProcessedDate, CompletedDate, PickupDate, " +
                    "Status, Purpose, Notes, PDFPath " +
                    "FROM TranscriptRequests " +
                    "WHERE RequestID = ?";

        Connection conn = DatabaseConnection.getConnection();
        TranscriptRequestData data = null;

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, reqId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    data = new TranscriptRequestData();
                    data.requestId = rs.getInt("RequestID");
                    data.studentId = rs.getInt("StudentUserID");
                    data.requestedById = rs.getInt("RequestedByUserID");
                    data.processedById = rs.getObject("ProcessedByUserID", Integer.class);
                    data.requestDate = rs.getTimestamp("RequestDate");
                    data.processedDate = rs.getTimestamp("ProcessedDate");
                    data.completedDate = rs.getTimestamp("CompletedDate");
                    data.pickupDate = rs.getTimestamp("PickupDate");
                    data.statusStr = rs.getString("Status");
                    data.purpose = rs.getString("Purpose");
                    data.notes = rs.getString("Notes");
                    data.pdfPath = rs.getString("PDFPath");
                }
            }
        }

        if (data != null) {
            return createTranscriptRequestFromData(data, conn);
        }
        return null;
    }

    /**
     * Update transcript request status (Admin)
     * US 2.4 - Generate Student Transcript
     * @param requestId The request ID
     * @param newStatus The new status
     * @param processedBy The admin processing the request
     * @param notes Optional notes
     * @param pdfPath Optional PDF path if transcript is generated
     * @return true if update successful
     * @throws SQLException if database error occurs
     */
    public boolean updateTranscriptRequestStatus(String requestId, TranscriptStatus newStatus,
                                                User processedBy, String notes, String pdfPath) throws SQLException {
        if (requestId == null || requestId.isBlank() || newStatus == null) {
            throw new IllegalArgumentException("Request ID and status are required");
        }

        int reqId;
        int processorId = -1;
        try {
            reqId = Integer.parseInt(requestId);
            if (processedBy != null && processedBy.getId() != null) {
                processorId = Integer.parseInt(processedBy.getId());
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid request ID or processor ID format");
        }

        // Build update SQL based on status
        String sql = "UPDATE TranscriptRequests SET Status = ?";
        
        if (newStatus == TranscriptStatus.IN_PROGRESS) {
            sql += ", ProcessedByUserID = ?, ProcessedDate = GETDATE()";
        } else if (newStatus == TranscriptStatus.READY_FOR_PICKUP || newStatus == TranscriptStatus.COMPLETED) {
            sql += ", ProcessedByUserID = ?, CompletedDate = GETDATE()";
            if (newStatus == TranscriptStatus.COMPLETED) {
                sql += ", PickupDate = GETDATE()";
            }
        }

        if (notes != null) {
            sql += ", Notes = ?";
        }
        if (pdfPath != null) {
            sql += ", PDFPath = ?";
        }

        sql += " WHERE RequestID = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            int paramIndex = 1;
            pstmt.setString(paramIndex++, statusToString(newStatus));
            
            if (newStatus == TranscriptStatus.IN_PROGRESS || 
                newStatus == TranscriptStatus.READY_FOR_PICKUP || 
                newStatus == TranscriptStatus.COMPLETED) {
                if (processorId > 0) {
                    pstmt.setInt(paramIndex++, processorId);
                } else {
                    pstmt.setNull(paramIndex++, Types.INTEGER);
                }
            }
            
            if (notes != null) {
                pstmt.setString(paramIndex++, notes);
            }
            if (pdfPath != null) {
                pstmt.setString(paramIndex++, pdfPath);
            }
            
            pstmt.setInt(paramIndex++, reqId);

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        }
    }

    /**
     * Generate transcript PDF (simplified - returns path)
     * In a real implementation, this would generate an actual PDF file
     * US 2.4 - Generate Student Transcript
     * @param request The transcript request
     * @return Path to generated PDF
     * @throws SQLException if database error occurs
     */
    public String generateTranscriptPDF(TranscriptRequest request) throws SQLException {
        if (request == null) {
            throw new IllegalArgumentException("Transcript request is required");
        }

        // In a real implementation, this would:
        // 1. Collect student data (grades, courses, etc.)
        // 2. Generate PDF using a library like iText or Apache PDFBox
        // 3. Save PDF to file system
        // 4. Return the file path

        // For now, we'll create a simple path
        String pdfPath = "transcripts/transcript_" + request.getId() + "_" + 
                        request.getStudent().getUsername() + ".pdf";
        
        // Update request with PDF path and set status to READY_FOR_PICKUP
        updateTranscriptRequestStatus(request.getId(), TranscriptStatus.READY_FOR_PICKUP,
                                     request.getProcessedBy(), request.getNotes(), pdfPath);
        
        return pdfPath;
    }

    // Helper methods

    private TranscriptRequest createTranscriptRequestFromData(TranscriptRequestData data, Connection conn) throws SQLException {
        // Get Student object
        Student student = getStudentById(conn, data.studentId);
        if (student == null) {
            System.err.println("Student not found for transcript request " + data.requestId);
            return null;
        }

        // Get RequestedBy User object
        User requestedBy = getUserById(conn, data.requestedById);
        if (requestedBy == null) {
            System.err.println("RequestedBy user not found for transcript request " + data.requestId);
            return null;
        }

        // Get ProcessedBy User object if available
        User processedBy = null;
        if (data.processedById != null) {
            processedBy = getUserById(conn, data.processedById);
        }

        // Convert status string to enum
        TranscriptStatus status = TranscriptStatus.PENDING;
        if (data.statusStr != null) {
            try {
                status = TranscriptStatus.valueOf(data.statusStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                status = TranscriptStatus.PENDING;
            }
        }

        // Convert timestamps
        LocalDateTime requestDate = data.requestDate != null ? data.requestDate.toLocalDateTime() : LocalDateTime.now();
        LocalDateTime processedDate = data.processedDate != null ? data.processedDate.toLocalDateTime() : null;
        LocalDateTime completedDate = data.completedDate != null ? data.completedDate.toLocalDateTime() : null;
        LocalDateTime pickupDate = data.pickupDate != null ? data.pickupDate.toLocalDateTime() : null;

        return new TranscriptRequest(
            String.valueOf(data.requestId),
            student,
            requestedBy,
            processedBy,
            requestDate,
            processedDate,
            completedDate,
            pickupDate,
            status,
            data.purpose,
            data.notes,
            data.pdfPath
        );
    }

    private Student getStudentById(Connection conn, int userId) throws SQLException {
        String sql = "SELECT u.UserID, u.USERNAME, u.Email, u.UserType " +
                    "FROM Users u " +
                    "INNER JOIN Students s ON u.UserID = s.UserID " +
                    "WHERE u.UserID = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String id = String.valueOf(rs.getInt("UserID"));
                    String username = rs.getString("USERNAME");
                    return new Student(id, username, null);
                }
            }
        }
        return null;
    }

    private User getUserById(Connection conn, int userId) throws SQLException {
        String sql = "SELECT UserID, Username, Email, UserType FROM Users WHERE UserID = ?";
        
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
            return new Student(id, username, null);
        }
        
        switch (userType.toUpperCase()) {
            case "STUDENT":
                return new Student(id, username, null);
            case "PROFESSOR":
                return new edu.facilities.model.Professor(id, username, null);
            case "STAFF":
                return new edu.facilities.model.Staff(id, username, null);
            case "ADMIN":
                return new edu.facilities.model.Admin(id, username, null);
            default:
                return new Student(id, username, null);
        }
    }

    private String getUserType(User user) throws SQLException {
        if (user == null || user.getId() == null) {
            return null;
        }
        
        try {
            String userType = user.getUserType();
            if (userType != null && !userType.isBlank()) {
                return userType;
            }
        } catch (Exception e) {
            // Fall through to database query
        }
        
        try {
            int userId = Integer.parseInt(user.getId());
            String sql = "SELECT UserType FROM Users WHERE UserID = ?";
            
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                
                pstmt.setInt(1, userId);
                
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getString("UserType");
                    }
                }
            }
        } catch (NumberFormatException e) {
            return null;
        }
        
        return null;
    }

    private String statusToString(TranscriptStatus status) {
        if (status == null) return "PENDING";
        switch (status) {
            case PENDING: return "PENDING";
            case IN_PROGRESS: return "IN_PROGRESS";
            case READY_FOR_PICKUP: return "READY_FOR_PICKUP";
            case COMPLETED: return "COMPLETED";
            case CANCELLED: return "CANCELLED";
            default: return "PENDING";
        }
    }

    // Helper class for transcript request data
    private static class TranscriptRequestData {
        int requestId;
        int studentId;
        int requestedById;
        Integer processedById;
        Timestamp requestDate;
        Timestamp processedDate;
        Timestamp completedDate;
        Timestamp pickupDate;
        String statusStr;
        String purpose;
        String notes;
        String pdfPath;
    }
}


