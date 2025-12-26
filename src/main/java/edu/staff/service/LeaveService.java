package edu.staff.service;

import edu.staff.model.LeaveRequest;
import edu.facilities.service.DatabaseConnection;

import java.sql.*;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Service class for managing Leave Requests.
 * US 3.11 - Submit Leave Request
 * US 3.12 - Approve or Reject Leave
 * US 3.13 - View Leave History
 */
public class LeaveService {

    /**
     * Submit a new leave request (Staff only)
     * @param leaveRequest Leave request to submit
     * @throws SQLException Database error
     */
    public void submitLeaveRequest(LeaveRequest leaveRequest) throws SQLException {
        if (leaveRequest.getStartDate() == null || leaveRequest.getEndDate() == null) {
            throw new IllegalArgumentException("Start date and end date are required");
        }

        if (leaveRequest.getEndDate().isBefore(leaveRequest.getStartDate())) {
            throw new IllegalArgumentException("End date must be after or equal to start date");
        }

        // Calculate number of days
        int days = (int) ChronoUnit.DAYS.between(leaveRequest.getStartDate(), leaveRequest.getEndDate()) + 1;
        leaveRequest.setNumberOfDays(days);

        String sql = "INSERT INTO LeaveRequests (StaffUserID, LeaveType, StartDate, EndDate, " +
                     "NumberOfDays, Reason, Status, SubmittedDate) " +
                     "VALUES (?, ?, ?, ?, ?, ?, 'PENDING', CURRENT_TIMESTAMP)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, leaveRequest.getStaffUserID());
            stmt.setString(2, leaveRequest.getLeaveType());
            stmt.setDate(3, Date.valueOf(leaveRequest.getStartDate()));
            stmt.setDate(4, Date.valueOf(leaveRequest.getEndDate()));
            stmt.setInt(5, leaveRequest.getNumberOfDays());
            stmt.setString(6, leaveRequest.getReason());

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        leaveRequest.setLeaveRequestID(rs.getInt(1));
                    }
                }
            }
        }
    }

    /**
     * Approve a leave request (HR Admin only)
     * @param leaveRequestID Leave request ID
     * @param reviewedByUserID HR Admin user ID
     * @throws SQLException Database error
     */
    public void approveLeaveRequest(int leaveRequestID, int reviewedByUserID) throws SQLException {
        String sql = "UPDATE LeaveRequests SET Status = 'APPROVED', " +
                     "ReviewedByUserID = ?, ReviewedDate = CURRENT_TIMESTAMP, RejectionReason = NULL " +
                     "WHERE LeaveRequestID = ? AND Status = 'PENDING'";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, reviewedByUserID);
            stmt.setInt(2, leaveRequestID);

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new IllegalArgumentException("Leave request not found or already processed");
            }
        }
    }

    /**
     * Reject a leave request (HR Admin only)
     * @param leaveRequestID Leave request ID
     * @param reviewedByUserID HR Admin user ID
     * @param rejectionReason Reason for rejection (required)
     * @throws SQLException Database error
     */
    public void rejectLeaveRequest(int leaveRequestID, int reviewedByUserID, String rejectionReason) throws SQLException {
        if (rejectionReason == null || rejectionReason.trim().isEmpty()) {
            throw new IllegalArgumentException("Rejection reason is required");
        }

        String sql = "UPDATE LeaveRequests SET Status = 'REJECTED', " +
                     "ReviewedByUserID = ?, ReviewedDate = CURRENT_TIMESTAMP, RejectionReason = ? " +
                     "WHERE LeaveRequestID = ? AND Status = 'PENDING'";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, reviewedByUserID);
            stmt.setString(2, rejectionReason);
            stmt.setInt(3, leaveRequestID);

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new IllegalArgumentException("Leave request not found or already processed");
            }
        }
    }

    /**
     * Get leave requests for a specific staff member
     * @param staffUserID Staff user ID
     * @return List of leave requests
     * @throws SQLException Database error
     */
    public List<LeaveRequest> getLeaveRequestsByStaff(int staffUserID) throws SQLException {
        List<LeaveRequest> requests = new ArrayList<>();
        String sql = "SELECT * FROM LeaveRequests WHERE StaffUserID = ? ORDER BY SubmittedDate DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, staffUserID);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    requests.add(mapResultSetToLeaveRequest(rs));
                }
            }
        }

        return requests;
    }

    /**
     * Get all pending leave requests (for HR Admin)
     * @return List of pending leave requests
     * @throws SQLException Database error
     */
    public List<LeaveRequest> getPendingLeaveRequests() throws SQLException {
        List<LeaveRequest> requests = new ArrayList<>();
        String sql = "SELECT lr.*, u.USERNAME as StaffUsername " +
                     "FROM LeaveRequests lr " +
                     "LEFT JOIN Users u ON lr.StaffUserID = u.UserID " +
                     "WHERE lr.Status = 'PENDING' " +
                     "ORDER BY lr.SubmittedDate ASC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                requests.add(mapResultSetToLeaveRequest(rs));
            }
        }

        return requests;
    }

    /**
     * Get all leave requests (for HR Admin)
     * @param statusFilter Optional status filter (null for all)
     * @return List of leave requests
     * @throws SQLException Database error
     */
    public List<LeaveRequest> getAllLeaveRequests(String statusFilter) throws SQLException {
        List<LeaveRequest> requests = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
            "SELECT lr.*, u.USERNAME as StaffUsername " +
            "FROM LeaveRequests lr " +
            "LEFT JOIN Users u ON lr.StaffUserID = u.UserID " +
            "WHERE 1=1"
        );

        if (statusFilter != null && !statusFilter.isEmpty()) {
            sql.append(" AND lr.Status = ?");
        }
        sql.append(" ORDER BY lr.SubmittedDate DESC");

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            if (statusFilter != null && !statusFilter.isEmpty()) {
                stmt.setString(1, statusFilter);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    requests.add(mapResultSetToLeaveRequest(rs));
                }
            }
        }

        return requests;
    }

    /**
     * Cancel a leave request (Staff only, if still pending)
     * @param leaveRequestID Leave request ID
     * @param staffUserID Staff user ID (for verification)
     * @throws SQLException Database error
     */
    public void cancelLeaveRequest(int leaveRequestID, int staffUserID) throws SQLException {
        String sql = "UPDATE LeaveRequests SET Status = 'CANCELLED' " +
                     "WHERE LeaveRequestID = ? AND StaffUserID = ? AND Status = 'PENDING'";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, leaveRequestID);
            stmt.setInt(2, staffUserID);

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new IllegalArgumentException("Leave request not found, not yours, or already processed");
            }
        }
    }

    /**
     * Map ResultSet to LeaveRequest object
     */
    private LeaveRequest mapResultSetToLeaveRequest(ResultSet rs) throws SQLException {
        LeaveRequest request = new LeaveRequest();
        request.setLeaveRequestID(rs.getInt("LeaveRequestID"));
        request.setStaffUserID(rs.getInt("StaffUserID"));
        
        // Try to get StaffUsername if available (from JOIN)
        try {
            String staffUsername = rs.getString("StaffUsername");
            if (staffUsername != null) {
                request.setStaffUsername(staffUsername);
            }
        } catch (SQLException e) {
            // Column doesn't exist, ignore
        }
        
        request.setLeaveType(rs.getString("LeaveType"));
        
        Date startDate = rs.getDate("StartDate");
        if (startDate != null) {
            request.setStartDate(startDate.toLocalDate());
        }
        
        Date endDate = rs.getDate("EndDate");
        if (endDate != null) {
            request.setEndDate(endDate.toLocalDate());
        }
        
        request.setNumberOfDays(rs.getInt("NumberOfDays"));
        request.setReason(rs.getString("Reason"));
        request.setStatus(rs.getString("Status"));
        request.setSubmittedDate(rs.getTimestamp("SubmittedDate"));
        
        int reviewedBy = rs.getInt("ReviewedByUserID");
        if (!rs.wasNull()) {
            request.setReviewedByUserID(reviewedBy);
        }
        
        request.setReviewedDate(rs.getTimestamp("ReviewedDate"));
        request.setRejectionReason(rs.getString("RejectionReason"));

        return request;
    }
}

