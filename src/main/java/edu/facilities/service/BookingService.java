package edu.facilities.service;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for managing room bookings using SQL Server database
 * NOTE: The Bookings table does not exist in Sprint(1)_Query.sql schema.
 * This service is prepared for when the Bookings table is added in a future sprint.
 * When adding the table, ensure RoomID is INT (matching Rooms.RoomID) and UserID is INT (matching Users.UserID).
 */
public class BookingService {
    
    /**
     * Check if a room has any future bookings
     * Uses SQL Server GETDATE() function
     * @param roomId The room ID to check
     * @return true if room has future bookings
     */
    public boolean hasFutureBookings(String roomId) throws SQLException {
        String sql = "SELECT COUNT(*) AS BookingCount FROM Bookings WHERE RoomID = ? AND BookingDate >= GETDATE()";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, roomId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("BookingCount") > 0;
                }
            }
        }
        return false;
    }
    
    /**
     * Get count of future bookings for a room
     * Uses SQL Server GETDATE() function
     * @param roomId The room ID to check
     * @return Number of future bookings
     */
    public int getFutureBookingCount(String roomId) throws SQLException {
        String sql = "SELECT COUNT(*) AS BookingCount FROM Bookings WHERE RoomID = ? AND BookingDate >= GETDATE()";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, roomId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("BookingCount");
                }
            }
        }
        return 0;
    }
    
    /**
     * Get list of future booking dates for a room
     * Uses SQL Server GETDATE() function
     * @param roomId The room ID to check
     * @return List of future booking dates
     */
    public List<LocalDateTime> getFutureBookingDates(String roomId) throws SQLException {
        List<LocalDateTime> bookingDates = new ArrayList<>();
        String sql = "SELECT BookingDate FROM Bookings WHERE RoomID = ? AND BookingDate >= GETDATE() ORDER BY BookingDate";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, roomId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Timestamp timestamp = rs.getTimestamp("BookingDate");
                    if (timestamp != null) {
                        bookingDates.add(timestamp.toLocalDateTime());
                    }
                }
            }
        }
        return bookingDates;
    }
    
    /**
     * Create a new booking
     * @param bookingId The booking ID
     * @param roomId The room ID
     * @param userId The user ID making the booking
     * @param bookingDate The booking date/time
     * @return true if booking created successfully
     */
    public boolean createBooking(String bookingId, String roomId, String userId, LocalDateTime bookingDate) throws SQLException {
        String sql = "INSERT INTO Bookings (BookingID, RoomID, UserID, BookingDate) VALUES (?, ?, ?, ?)";
        
        try (Connection conn =DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, bookingId);
            pstmt.setString(2, roomId);
            pstmt.setString(3, userId);
            pstmt.setTimestamp(4, Timestamp.valueOf(bookingDate));
            
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        }
    }
    
    /**
     * Delete a booking
     * @param bookingId The booking ID to delete
     * @return true if booking deleted successfully
     */
    public boolean deleteBooking(String bookingId) throws SQLException {
        String sql = "DELETE FROM Bookings WHERE BookingID = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, bookingId);
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        }
    }
}

