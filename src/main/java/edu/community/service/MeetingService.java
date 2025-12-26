package edu.community.service;

import edu.community.model.Meeting;
import edu.facilities.service.DatabaseConnection;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for managing meetings between students and staff/professors.
 */
public class MeetingService {

    /**
     * Request a meeting (student requests, staff/professor approves)
     */
    public int requestMeeting(Meeting meeting) throws SQLException {
        // Check for time conflicts
        if (hasTimeConflict(meeting.getStaffUserID(), meeting.getMeetingDate(), 
                           meeting.getStartTime(), meeting.getEndTime(), 0)) {
            throw new IllegalArgumentException("Staff member has a conflicting meeting at this time.");
        }

        // Get PENDING status type ID
        int pendingStatusID = getStatusTypeID("MEETING", "PENDING");

        String sql = "INSERT INTO Meetings (StudentUserID, StaffUserID, Subject, Description, " +
                     "MeetingDate, StartTime, EndTime, Location, StatusTypeID, RequestedDate, CreatedDate) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP) " +
                     "RETURNING MeetingID";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, meeting.getStudentUserID());
            pstmt.setInt(2, meeting.getStaffUserID());
            pstmt.setString(3, meeting.getSubject());
            pstmt.setString(4, meeting.getDescription() != null ? meeting.getDescription() : "");
            pstmt.setDate(5, Date.valueOf(meeting.getMeetingDate()));
            pstmt.setTime(6, Time.valueOf(meeting.getStartTime()));
            pstmt.setTime(7, Time.valueOf(meeting.getEndTime()));
            pstmt.setString(8, meeting.getLocation() != null ? meeting.getLocation() : "");
            pstmt.setInt(9, pendingStatusID);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("MeetingID");
                }
            }
        }

        throw new SQLException("Failed to create meeting request");
    }

    /**
     * Approve or reject a meeting request
     */
    public boolean respondToMeeting(int meetingID, String status, String responseNotes, int responderUserID) throws SQLException {
        if (!"APPROVED".equals(status) && !"REJECTED".equals(status)) {
            throw new IllegalArgumentException("Status must be APPROVED or REJECTED");
        }

        // Get the meeting to check staff user ID
        Meeting meeting = getMeetingById(meetingID);
        if (meeting == null) {
            throw new IllegalArgumentException("Meeting not found");
        }

        if (meeting.getStaffUserID() != responderUserID) {
            throw new IllegalArgumentException("Only the assigned staff member can respond to this meeting");
        }

        // If approving, check for time conflicts
        if ("APPROVED".equals(status)) {
            if (hasTimeConflict(meeting.getStaffUserID(), meeting.getMeetingDate(),
                               meeting.getStartTime(), meeting.getEndTime(), meetingID)) {
                throw new IllegalArgumentException("Staff member has a conflicting meeting at this time.");
            }
        }

        int statusTypeID = getStatusTypeID("MEETING", status);

        String sql = "UPDATE Meetings SET StatusTypeID = ?, ResponseDate = CURRENT_TIMESTAMP, " +
                     "ResponseNotes = ? WHERE MeetingID = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, statusTypeID);
            pstmt.setString(2, responseNotes != null ? responseNotes : "");
            pstmt.setInt(3, meetingID);

            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * Cancel a meeting
     */
    public boolean cancelMeeting(int meetingID, int userID) throws SQLException {
        Meeting meeting = getMeetingById(meetingID);
        if (meeting == null) {
            throw new IllegalArgumentException("Meeting not found");
        }

        // Only student or staff can cancel
        if (meeting.getStudentUserID() != userID && meeting.getStaffUserID() != userID) {
            throw new IllegalArgumentException("You do not have permission to cancel this meeting");
        }

        int cancelledStatusID = getStatusTypeID("MEETING", "CANCELLED");

        String sql = "UPDATE Meetings SET StatusTypeID = ? WHERE MeetingID = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, cancelledStatusID);
            pstmt.setInt(2, meetingID);

            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * Get meetings for a student
     */
    public List<Meeting> getMeetingsByStudent(int studentUserID) throws SQLException {
        String sql = "SELECT m.*, " +
                     "stu.USERNAME as StudentName, " +
                     "staff.USERNAME as StaffName, " +
                     "st.StatusCode as Status " +
                     "FROM Meetings m " +
                     "INNER JOIN Users stu ON m.StudentUserID = stu.UserID " +
                     "INNER JOIN Users staff ON m.StaffUserID = staff.UserID " +
                     "INNER JOIN StatusTypes st ON m.StatusTypeID = st.StatusTypeID AND st.EntityType = 'MEETING' " +
                     "WHERE m.StudentUserID = ? " +
                     "ORDER BY m.MeetingDate DESC, m.StartTime DESC";

        return getMeetings(sql, studentUserID);
    }

    /**
     * Get meetings for a staff member/professor
     */
    public List<Meeting> getMeetingsByStaff(int staffUserID) throws SQLException {
        String sql = "SELECT m.*, " +
                     "stu.USERNAME as StudentName, " +
                     "staff.USERNAME as StaffName, " +
                     "st.StatusCode as Status " +
                     "FROM Meetings m " +
                     "INNER JOIN Users stu ON m.StudentUserID = stu.UserID " +
                     "INNER JOIN Users staff ON m.StaffUserID = staff.UserID " +
                     "INNER JOIN StatusTypes st ON m.StatusTypeID = st.StatusTypeID AND st.EntityType = 'MEETING' " +
                     "WHERE m.StaffUserID = ? " +
                     "ORDER BY m.MeetingDate DESC, m.StartTime DESC";

        return getMeetings(sql, staffUserID);
    }

    /**
     * Get pending meetings for a staff member
     */
    public List<Meeting> getPendingMeetingsByStaff(int staffUserID) throws SQLException {
        int pendingStatusID = getStatusTypeID("MEETING", "PENDING");

        String sql = "SELECT m.*, " +
                     "stu.USERNAME as StudentName, " +
                     "staff.USERNAME as StaffName, " +
                     "st.StatusCode as Status " +
                     "FROM Meetings m " +
                     "INNER JOIN Users stu ON m.StudentUserID = stu.UserID " +
                     "INNER JOIN Users staff ON m.StaffUserID = staff.UserID " +
                     "INNER JOIN StatusTypes st ON m.StatusTypeID = st.StatusTypeID AND st.EntityType = 'MEETING' " +
                     "WHERE m.StaffUserID = ? AND m.StatusTypeID = ? " +
                     "ORDER BY m.RequestedDate ASC";

        List<Meeting> meetings = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, staffUserID);
            pstmt.setInt(2, pendingStatusID);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    meetings.add(mapResultSetToMeeting(rs));
                }
            }
        }

        return meetings;
    }

    /**
     * Get meeting by ID
     */
    public Meeting getMeetingById(int meetingID) throws SQLException {
        String sql = "SELECT m.*, " +
                     "stu.USERNAME as StudentName, " +
                     "staff.USERNAME as StaffName, " +
                     "st.StatusCode as Status " +
                     "FROM Meetings m " +
                     "INNER JOIN Users stu ON m.StudentUserID = stu.UserID " +
                     "INNER JOIN Users staff ON m.StaffUserID = staff.UserID " +
                     "INNER JOIN StatusTypes st ON m.StatusTypeID = st.StatusTypeID AND st.EntityType = 'MEETING' " +
                     "WHERE m.MeetingID = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, meetingID);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToMeeting(rs);
                }
            }
        }

        return null;
    }

    /**
     * Get available staff/professors for a student to request a meeting
     */
    public List<edu.facilities.model.User> getAvailableStaff() throws SQLException {
        List<edu.facilities.model.User> staffList = new ArrayList<>();

        String sql = "SELECT DISTINCT u.UserID, u.USERNAME, u.Email " +
                     "FROM Users u " +
                     "INNER JOIN UserRoles ur ON u.UserID = ur.UserID " +
                     "INNER JOIN UserTypes ut ON ur.UserTypeID = ut.UserTypeID " +
                     "WHERE ut.TypeCode IN ('PROFESSOR', 'STAFF') AND u.IsActive = TRUE " +
                     "ORDER BY u.USERNAME";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                int userId = rs.getInt("UserID");
                String username = rs.getString("USERNAME");
                String email = rs.getString("Email");

                // Create a simple User object (we'll use Staff as base)
                edu.facilities.model.User user = new edu.facilities.model.Staff(
                    String.valueOf(userId), username, email);
                staffList.add(user);
            }
        }

        return staffList;
    }

    /**
     * Check if there's a time conflict for a staff member
     */
    private boolean hasTimeConflict(int staffUserID, LocalDate meetingDate, 
                                   LocalTime startTime, LocalTime endTime, int excludeMeetingID) throws SQLException {
        String sql = "SELECT COUNT(*) as ConflictCount " +
                     "FROM Meetings m " +
                     "INNER JOIN StatusTypes st ON m.StatusTypeID = st.StatusTypeID AND st.EntityType = 'MEETING' " +
                     "WHERE m.StaffUserID = ? " +
                     "AND m.MeetingDate = ? " +
                     "AND st.StatusCode IN ('PENDING', 'APPROVED') " +
                     "AND ((m.StartTime <= ? AND m.EndTime > ?) OR " +
                     "     (m.StartTime < ? AND m.EndTime >= ?) OR " +
                     "     (m.StartTime >= ? AND m.EndTime <= ?))";

        if (excludeMeetingID > 0) {
            sql += " AND m.MeetingID != ?";
        }

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            int paramIndex = 1;
            pstmt.setInt(paramIndex++, staffUserID);
            pstmt.setDate(paramIndex++, Date.valueOf(meetingDate));
            pstmt.setTime(paramIndex++, Time.valueOf(startTime));
            pstmt.setTime(paramIndex++, Time.valueOf(startTime));
            pstmt.setTime(paramIndex++, Time.valueOf(endTime));
            pstmt.setTime(paramIndex++, Time.valueOf(endTime));
            pstmt.setTime(paramIndex++, Time.valueOf(startTime));
            pstmt.setTime(paramIndex++, Time.valueOf(endTime));

            if (excludeMeetingID > 0) {
                pstmt.setInt(paramIndex++, excludeMeetingID);
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("ConflictCount") > 0;
                }
            }
        }

        return false;
    }

    /**
     * Helper method to get status type ID
     */
    private int getStatusTypeID(String entityType, String statusCode) throws SQLException {
        String sql = "SELECT StatusTypeID FROM StatusTypes WHERE EntityType = ? AND StatusCode = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, entityType);
            pstmt.setString(2, statusCode);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("StatusTypeID");
                }
            }
        }

        throw new SQLException("Status type not found: " + entityType + "/" + statusCode);
    }

    /**
     * Helper method to get meetings with a given SQL query
     */
    private List<Meeting> getMeetings(String sql, int userID) throws SQLException {
        List<Meeting> meetings = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userID);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    meetings.add(mapResultSetToMeeting(rs));
                }
            }
        }

        return meetings;
    }

    /**
     * Map ResultSet to Meeting object
     */
    private Meeting mapResultSetToMeeting(ResultSet rs) throws SQLException {
        Meeting meeting = new Meeting();
        meeting.setMeetingID(rs.getInt("MeetingID"));
        meeting.setStudentUserID(rs.getInt("StudentUserID"));
        meeting.setStaffUserID(rs.getInt("StaffUserID"));
        meeting.setSubject(rs.getString("Subject"));
        meeting.setDescription(rs.getString("Description"));
        meeting.setMeetingDate(rs.getDate("MeetingDate").toLocalDate());
        meeting.setStartTime(rs.getTime("StartTime").toLocalTime());
        meeting.setEndTime(rs.getTime("EndTime").toLocalTime());
        meeting.setLocation(rs.getString("Location"));

        // Get status from join
        if (rs.getString("Status") != null) {
            meeting.setStatus(rs.getString("Status"));
        }

        // Get names from join
        meeting.setStudentName(rs.getString("StudentName"));
        meeting.setStaffName(rs.getString("StaffName"));

        Timestamp requestedDateTs = rs.getTimestamp("RequestedDate");
        if (requestedDateTs != null) {
            meeting.setRequestedDate(requestedDateTs.toLocalDateTime());
        }

        Timestamp responseDateTs = rs.getTimestamp("ResponseDate");
        if (responseDateTs != null) {
            meeting.setResponseDate(responseDateTs.toLocalDateTime());
        }

        meeting.setResponseNotes(rs.getString("ResponseNotes"));

        Timestamp createdDateTs = rs.getTimestamp("CreatedDate");
        if (createdDateTs != null) {
            meeting.setCreatedDate(createdDateTs.toLocalDateTime());
        }

        return meeting;
    }
}

