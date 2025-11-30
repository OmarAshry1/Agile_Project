package edu.facilities.service;

import edu.facilities.model.Booking;
import edu.facilities.model.BookingStatus;
import edu.facilities.model.Room;
import edu.facilities.model.RoomStatus;
import edu.facilities.model.User;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for managing room bookings using SQL Server database
 */
public class BookingService {
    
    /**
     * Check if a room is available for booking at a specific time slot
     * @param roomId The room ID (RoomID from database)
     * @param startTime The requested start time
     * @param endTime The requested end time
     * @return true if room is available, false if there's a conflict
     * @throws SQLException if database error occurs
     */
    public boolean isRoomAvailable(int roomId, LocalDateTime startTime, LocalDateTime endTime) throws SQLException {
        // Check if room exists and is in AVAILABLE status
        String roomCheckSql = "SELECT Status FROM Rooms WHERE RoomID = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(roomCheckSql)) {
            
            pstmt.setInt(1, roomId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (!rs.next()) {
                    return false; // Room doesn't exist
                }
                String status = rs.getString("Status");
                if (!"AVAILABLE".equalsIgnoreCase(status)) {
                    return false; // Room is not available (OCCUPIED or MAINTENANCE)
                }
            }
        }
        
        // Check for overlapping bookings
        // A booking conflicts if:
        // - New start time is between existing booking start and end
        // - New end time is between existing booking start and end
        // - New booking completely encompasses an existing booking
        String conflictSql = "SELECT COUNT(*) AS ConflictCount FROM Bookings " +
                            "WHERE RoomID = ? AND Status = 'CONFIRMED' " +
                            "AND ((BookingDate <= ? AND EndDate > ?) OR " +
                            "     (BookingDate < ? AND EndDate >= ?) OR " +
                            "     (BookingDate >= ? AND EndDate <= ?))";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(conflictSql)) {
            
            pstmt.setInt(1, roomId);
            pstmt.setTimestamp(2, Timestamp.valueOf(startTime));
            pstmt.setTimestamp(3, Timestamp.valueOf(startTime));
            pstmt.setTimestamp(4, Timestamp.valueOf(endTime));
            pstmt.setTimestamp(5, Timestamp.valueOf(endTime));
            pstmt.setTimestamp(6, Timestamp.valueOf(startTime));
            pstmt.setTimestamp(7, Timestamp.valueOf(endTime));
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("ConflictCount") == 0;
                }
            }
        }
        
        return true;
    }
    
    /**
     * Check if a room is available for booking at a specific time slot (using room code)
     * @param roomCode The room code
     * @param startTime The requested start time
     * @param endTime The requested end time
     * @return true if room is available, false if there's a conflict
     * @throws SQLException if database error occurs
     */
    public boolean isRoomAvailable(String roomCode, LocalDateTime startTime, LocalDateTime endTime) throws SQLException {
        // First get RoomID from room code
        int roomId = getRoomIdByCode(roomCode);
        if (roomId == -1) {
            return false; // Room not found
        }
        
        return isRoomAvailable(roomId, startTime, endTime);
    }
    
    /**
     * Create a new booking
     * @param room The room to book
     * @param user The user making the booking
     * @param startTime The booking start time
     * @param endTime The booking end time
     * @param purpose Optional purpose/description
     * @return Booking object if successful, null otherwise
     * @throws SQLException if database error occurs
     * @throws IllegalArgumentException if room is not available or invalid parameters
     */
    public Booking createBooking(Room room, User user, LocalDateTime startTime, 
                                 LocalDateTime endTime, String purpose) throws SQLException {
        // Validate parameters
        if (room == null || user == null || startTime == null || endTime == null) {
            throw new IllegalArgumentException("Room, user, start time, and end time are required");
        }
        
        if (endTime.isBefore(startTime) || endTime.isEqual(startTime)) {
            throw new IllegalArgumentException("End time must be after start time");
        }
        
        if (startTime.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Cannot book rooms in the past");
        }
        
        // Check user type - only PROFESSOR and STAFF can book
        String userType = getUserType(user);
        if (!"PROFESSOR".equals(userType) && !"STAFF".equals(userType)) {
            throw new IllegalArgumentException("Only professors and staff can book rooms. Your role: " + userType);
        }
        
        // Get RoomID from room code
        int roomId = getRoomIdByCode(room.getId());
        if (roomId == -1) {
            throw new IllegalArgumentException("Room with code '" + room.getId() + "' not found");
        }
        
        // Check availability
        if (!isRoomAvailable(roomId, startTime, endTime)) {
            throw new IllegalArgumentException("Room is not available for the requested time slot. " +
                                               "Please choose a different time or room.");
        }
        
        // Get UserID
        int userId;
        try {
            userId = Integer.parseInt(user.getId());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid user ID: " + user.getId());
        }
        
        // Insert booking
        String sql = "INSERT INTO Bookings (RoomID, UserID, BookingDate, EndDate, Purpose, Status) " +
                    "VALUES (?, ?, ?, ?, ?, 'CONFIRMED')";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setInt(1, roomId);
            pstmt.setInt(2, userId);
            pstmt.setTimestamp(3, Timestamp.valueOf(startTime));
            pstmt.setTimestamp(4, Timestamp.valueOf(endTime));
            pstmt.setString(5, purpose != null ? purpose : "");
            
            int rowsAffected = pstmt.executeUpdate();
            
            if (rowsAffected > 0) {
                // Get generated booking ID
                try (ResultSet keys = pstmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        int bookingId = keys.getInt(1);
                        String bookingIdStr = String.valueOf(bookingId);
                        
                        // Create and return Booking object
                        Booking booking = new Booking(
                            bookingIdStr,
                            room,
                            user,
                            startTime,
                            endTime,
                            purpose,
                            BookingStatus.CONFIRMED,
                            LocalDateTime.now()
                        );
                        
                        System.out.println("Booking created successfully: ID=" + bookingIdStr + 
                                         ", Room=" + room.getId() + ", User=" + user.getUsername());
                        return booking;
                    }
                }
            }
        }
        
        return null;
    }
    
    /**
     * Get all bookings for a specific user
     * @param user The user
     * @return List of bookings
     * @throws SQLException if database error occurs
     */
    public List<Booking> getBookingsByUser(User user) throws SQLException {
        List<Booking> bookings = new ArrayList<>();
        
        if (user == null || user.getId() == null) {
            return bookings;
        }
        
        int userId;
        try {
            userId = Integer.parseInt(user.getId());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid user ID: " + user.getId());
        }
        
        String sql = "SELECT b.BookingID, b.RoomID, b.UserID, b.BookingDate, b.EndDate, " +
                    "b.Purpose, b.Status, b.CreatedDate, " +
                    "r.Code as RoomCode, r.Name as RoomName, r.Type as RoomType, " +
                    "r.Capacity, r.Location, r.Status as RoomStatus " +
                    "FROM Bookings b " +
                    "INNER JOIN Rooms r ON b.RoomID = r.RoomID " +
                    "WHERE b.UserID = ? AND b.Status = 'CONFIRMED' " +
                    "ORDER BY b.BookingDate DESC";
        
        Connection conn = DatabaseConnection.getConnection();
        if (conn == null || conn.isClosed()) {
            throw new SQLException("Database connection is closed or invalid");
        }
        
        List<BookingData> bookingDataList = new ArrayList<>();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    BookingData data = new BookingData();
                    data.bookingId = rs.getInt("BookingID");
                    data.roomId = rs.getInt("RoomID");
                    data.userId = rs.getInt("UserID");
                    data.bookingDate = rs.getTimestamp("BookingDate");
                    data.endDate = rs.getTimestamp("EndDate");
                    data.purpose = rs.getString("Purpose");
                    data.status = rs.getString("Status");
                    data.createdDate = rs.getTimestamp("CreatedDate");
                    data.roomCode = rs.getString("RoomCode");
                    data.roomName = rs.getString("RoomName");
                    data.roomType = rs.getString("RoomType");
                    data.capacity = rs.getInt("Capacity");
                    data.location = rs.getString("Location");
                    data.roomStatus = rs.getString("RoomStatus");
                    bookingDataList.add(data);
                }
            }
        }
        
        // Convert to Booking objects
        for (BookingData data : bookingDataList) {
            try {
                Booking booking = createBookingFromData(data, conn);
                if (booking != null) {
                    bookings.add(booking);
                }
            } catch (Exception e) {
                System.err.println("Error creating booking from data for booking ID " + data.bookingId + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        return bookings;
    }
    
    /**
     * Cancel a booking
     * @param bookingId The booking ID
     * @return true if cancelled successfully
     * @throws SQLException if database error occurs
     */
    public boolean cancelBooking(String bookingId) throws SQLException {
        if (bookingId == null || bookingId.isBlank()) {
            throw new IllegalArgumentException("Booking ID is required");
        }
        
        int bookingIdInt;
        try {
            bookingIdInt = Integer.parseInt(bookingId);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid booking ID format");
        }
        
        String sql = "UPDATE Bookings SET Status = 'CANCELLED' WHERE BookingID = ? AND Status = 'CONFIRMED'";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, bookingIdInt);
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        }
    }
    
    /**
     * Update an existing booking
     * @param bookingId The booking ID to update
     * @param newRoom The new room (can be same as current)
     * @param newStartTime The new start time
     * @param newEndTime The new end time
     * @param newPurpose The new purpose (optional)
     * @return Updated Booking object if successful, null otherwise
     * @throws SQLException if database error occurs
     * @throws IllegalArgumentException if validation fails
     */
    public Booking updateBooking(String bookingId, Room newRoom, LocalDateTime newStartTime, 
                                LocalDateTime newEndTime, String newPurpose) throws SQLException {
        if (bookingId == null || bookingId.isBlank()) {
            throw new IllegalArgumentException("Booking ID is required");
        }
        
        if (newRoom == null || newStartTime == null || newEndTime == null) {
            throw new IllegalArgumentException("Room, start time, and end time are required");
        }
        
        if (newEndTime.isBefore(newStartTime) || newEndTime.isEqual(newStartTime)) {
            throw new IllegalArgumentException("End time must be after start time");
        }
        
        if (newStartTime.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Cannot book rooms in the past");
        }
        
        int bookingIdInt;
        try {
            bookingIdInt = Integer.parseInt(bookingId);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid booking ID format");
        }
        
        // Get the current booking to verify it exists and get user info
        Booking currentBooking = getBookingById(bookingIdInt);
        if (currentBooking == null) {
            throw new IllegalArgumentException("Booking not found");
        }
        
        if (currentBooking.getStatus() != BookingStatus.CONFIRMED) {
            throw new IllegalArgumentException("Can only update confirmed bookings");
        }
        
        // Get RoomID from room code
        int newRoomId = getRoomIdByCode(newRoom.getId());
        if (newRoomId == -1) {
            throw new IllegalArgumentException("Room with code '" + newRoom.getId() + "' not found");
        }
        
        // Check if the new time slot is available
        // Exclude the current booking from availability check
        if (!isRoomAvailableExcludingBooking(newRoomId, newStartTime, newEndTime, bookingIdInt)) {
            throw new IllegalArgumentException("Room is not available for the requested time slot. " +
                                             "Please choose a different time or room.");
        }
        
        // Update the booking
        String sql = "UPDATE Bookings SET RoomID = ?, BookingDate = ?, EndDate = ?, Purpose = ? " +
                    "WHERE BookingID = ? AND Status = 'CONFIRMED'";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, newRoomId);
            pstmt.setTimestamp(2, Timestamp.valueOf(newStartTime));
            pstmt.setTimestamp(3, Timestamp.valueOf(newEndTime));
            pstmt.setString(4, newPurpose != null ? newPurpose : "");
            pstmt.setInt(5, bookingIdInt);
            
            int rowsAffected = pstmt.executeUpdate();
            
            if (rowsAffected > 0) {
                // Get updated room info
                Room updatedRoom = getRoomById(conn, newRoomId, newRoom.getId());
                if (updatedRoom == null) {
                    updatedRoom = newRoom; // Fallback to provided room
                }
                
                // Return updated booking
                return new Booking(
                    bookingId,
                    updatedRoom,
                    currentBooking.getUser(),
                    newStartTime,
                    newEndTime,
                    newPurpose,
                    BookingStatus.CONFIRMED,
                    currentBooking.getCreatedAt()
                );
            }
        }
        
        return null;
    }
    
    /**
     * Get a booking by ID
     * @param bookingId The booking ID
     * @return Booking object or null if not found
     * @throws SQLException if database error occurs
     */
    private Booking getBookingById(int bookingId) throws SQLException {
        String sql = "SELECT b.BookingID, b.RoomID, b.UserID, b.BookingDate, b.EndDate, " +
                    "b.Purpose, b.Status, b.CreatedDate, " +
                    "r.Code as RoomCode, r.Name as RoomName, r.Type as RoomType, " +
                    "r.Capacity, r.Location, r.Status as RoomStatus " +
                    "FROM Bookings b " +
                    "INNER JOIN Rooms r ON b.RoomID = r.RoomID " +
                    "WHERE b.BookingID = ?";
        
        Connection conn = DatabaseConnection.getConnection();
        BookingData data = null;
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, bookingId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    data = new BookingData();
                    data.bookingId = rs.getInt("BookingID");
                    data.roomId = rs.getInt("RoomID");
                    data.userId = rs.getInt("UserID");
                    data.bookingDate = rs.getTimestamp("BookingDate");
                    data.endDate = rs.getTimestamp("EndDate");
                    data.purpose = rs.getString("Purpose");
                    data.status = rs.getString("Status");
                    data.createdDate = rs.getTimestamp("CreatedDate");
                    data.roomCode = rs.getString("RoomCode");
                    data.roomName = rs.getString("RoomName");
                    data.roomType = rs.getString("RoomType");
                    data.capacity = rs.getInt("Capacity");
                    data.location = rs.getString("Location");
                    data.roomStatus = rs.getString("RoomStatus");
                }
            }
        }
        
        if (data != null) {
            return createBookingFromData(data, conn);
        }
        return null;
    }
    
    /**
     * Check if a room is available excluding a specific booking (for update operations)
     * @param roomId The room ID
     * @param startTime The requested start time
     * @param endTime The requested end time
     * @param excludeBookingId The booking ID to exclude from conflict check
     * @return true if room is available, false if there's a conflict
     * @throws SQLException if database error occurs
     */
    private boolean isRoomAvailableExcludingBooking(int roomId, LocalDateTime startTime, 
                                                    LocalDateTime endTime, int excludeBookingId) throws SQLException {
        // Check if room exists and is in AVAILABLE status
        String roomCheckSql = "SELECT Status FROM Rooms WHERE RoomID = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(roomCheckSql)) {
            
            pstmt.setInt(1, roomId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (!rs.next()) {
                    return false; // Room doesn't exist
                }
                String status = rs.getString("Status");
                if (!"AVAILABLE".equalsIgnoreCase(status)) {
                    return false; // Room is not available
                }
            }
        }
        
        // Check for overlapping bookings, excluding the current booking
        String conflictSql = "SELECT COUNT(*) AS ConflictCount FROM Bookings " +
                            "WHERE RoomID = ? AND Status = 'CONFIRMED' AND BookingID != ? " +
                            "AND ((BookingDate <= ? AND EndDate > ?) OR " +
                            "     (BookingDate < ? AND EndDate >= ?) OR " +
                            "     (BookingDate >= ? AND EndDate <= ?))";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(conflictSql)) {
            
            pstmt.setInt(1, roomId);
            pstmt.setInt(2, excludeBookingId);
            pstmt.setTimestamp(3, Timestamp.valueOf(startTime));
            pstmt.setTimestamp(4, Timestamp.valueOf(startTime));
            pstmt.setTimestamp(5, Timestamp.valueOf(endTime));
            pstmt.setTimestamp(6, Timestamp.valueOf(endTime));
            pstmt.setTimestamp(7, Timestamp.valueOf(startTime));
            pstmt.setTimestamp(8, Timestamp.valueOf(endTime));
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("ConflictCount") == 0;
                }
            }
        }
        
        return true;
    }
    
    /**
     * Get all bookings for a specific room
     * @param roomCode The room code
     * @return List of bookings
     * @throws SQLException if database error occurs
     */
    public List<Booking> getBookingsByRoom(String roomCode) throws SQLException {
        List<Booking> bookings = new ArrayList<>();
        
        int roomId = getRoomIdByCode(roomCode);
        if (roomId == -1) {
            return bookings;
        }
        
        String sql = "SELECT b.BookingID, b.RoomID, b.UserID, b.BookingDate, b.EndDate, " +
                    "b.Purpose, b.Status, b.CreatedDate, " +
                    "r.Code as RoomCode, r.Name as RoomName, r.Type as RoomType, " +
                    "r.Capacity, r.Location, r.Status as RoomStatus " +
                    "FROM Bookings b " +
                    "INNER JOIN Rooms r ON b.RoomID = r.RoomID " +
                    "WHERE b.RoomID = ? AND b.Status = 'CONFIRMED' " +
                    "ORDER BY b.BookingDate ASC";
        
        Connection conn = DatabaseConnection.getConnection();
        List<BookingData> bookingDataList = new ArrayList<>();
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, roomId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    BookingData data = new BookingData();
                    data.bookingId = rs.getInt("BookingID");
                    data.roomId = rs.getInt("RoomID");
                    data.userId = rs.getInt("UserID");
                    data.bookingDate = rs.getTimestamp("BookingDate");
                    data.endDate = rs.getTimestamp("EndDate");
                    data.purpose = rs.getString("Purpose");
                    data.status = rs.getString("Status");
                    data.createdDate = rs.getTimestamp("CreatedDate");
                    data.roomCode = rs.getString("RoomCode");
                    data.roomName = rs.getString("RoomName");
                    data.roomType = rs.getString("RoomType");
                    data.capacity = rs.getInt("Capacity");
                    data.location = rs.getString("Location");
                    data.roomStatus = rs.getString("RoomStatus");
                    bookingDataList.add(data);
                }
            }
        }
        
        // Convert to Booking objects
        for (BookingData data : bookingDataList) {
            try {
                Booking booking = createBookingFromData(data, conn);
                if (booking != null) {
                    bookings.add(booking);
                }
            } catch (Exception e) {
                System.err.println("Error creating booking from data: " + e.getMessage());
            }
        }
        
        return bookings;
    }
    
    // Helper methods
    
    private int getRoomIdByCode(String roomCode) throws SQLException {
        String sql = "SELECT RoomID FROM Rooms WHERE Code = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, roomCode);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("RoomID");
                }
            }
        }
        return -1;
    }
    
    /**
     * Get room by ID and code using the provided connection
     */
    private Room getRoomById(Connection conn, int roomId, String roomCode) throws SQLException {
        if (conn == null || conn.isClosed()) {
            throw new SQLException("Connection is closed or invalid");
        }
        
        String sql = "SELECT RoomID, Code, Name, Type, Capacity, Location, Status FROM Rooms WHERE RoomID = ?";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, roomId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    BookingData data = new BookingData();
                    data.roomId = rs.getInt("RoomID");
                    data.roomCode = rs.getString("Code");
                    data.roomName = rs.getString("Name");
                    data.roomType = rs.getString("Type");
                    data.capacity = rs.getInt("Capacity");
                    data.location = rs.getString("Location");
                    data.roomStatus = rs.getString("Status");
                    return createRoomFromData(data);
                }
            }
        }
        return null;
    }
    
    private String getUserType(User user) throws SQLException {
        if (user == null) {
            return null;
        }
        
        // Try to get from user object first
        try {
            String userType = user.getUserType();
            if (userType != null && !userType.isBlank()) {
                return userType;
            }
        } catch (Exception e) {
            // Fall through to database query
        }
        
        // Query database
        if (user.getId() == null) {
            return null;
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
    
    private Booking createBookingFromData(BookingData data, Connection conn) throws SQLException {
        // Create Room object
        Room room = createRoomFromData(data);
        
        // Create User object
        User user = getUserById(conn, data.userId);
        if (user == null) {
            System.err.println("User not found for booking " + data.bookingId);
            return null;
        }
        
        // Convert status
        BookingStatus status = BookingStatus.CONFIRMED;
        if ("CANCELLED".equalsIgnoreCase(data.status)) {
            status = BookingStatus.CANCELLED;
        }
        
        // Convert timestamps
        LocalDateTime bookingDate = data.bookingDate != null ? data.bookingDate.toLocalDateTime() : LocalDateTime.now();
        LocalDateTime endDate = data.endDate != null ? data.endDate.toLocalDateTime() : bookingDate.plusHours(1);
        LocalDateTime createdAt = data.createdDate != null ? data.createdDate.toLocalDateTime() : LocalDateTime.now();
        
        return new Booking(
            String.valueOf(data.bookingId),
            room,
            user,
            bookingDate,
            endDate,
            data.purpose,
            status,
            createdAt
        );
    }
    
    private Room createRoomFromData(BookingData data) {
        // Import RoomType and RoomStatus enums
        edu.facilities.model.RoomType roomType = edu.facilities.model.RoomType.CLASSROOM;
        if (data.roomType != null) {
            switch (data.roomType.toUpperCase()) {
                case "LAB": case "LABORATORY":
                    roomType = edu.facilities.model.RoomType.LAB;
                    break;
                case "OFFICE":
                    roomType = edu.facilities.model.RoomType.OFFICE;
                    break;
                case "CONFERENCE":
                    roomType = edu.facilities.model.RoomType.CONFERENCE;
                    break;
            }
        }
        
        RoomStatus roomStatus = RoomStatus.AVAILABLE;
        if (data.roomStatus != null) {
            switch (data.roomStatus.toUpperCase()) {
                case "OCCUPIED":
                    roomStatus = RoomStatus.OCCUPIED;
                    break;
                case "MAINTENANCE":
                    roomStatus = RoomStatus.MAINTENANCE;
                    break;
            }
        }
        
        return new Room(
            data.roomCode,
            data.roomName != null ? data.roomName : data.roomCode,
            roomType,
            data.capacity,
            data.location != null ? data.location : "",
            roomStatus
        );
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
    
    // Helper class for booking data
    private static class BookingData {
        int bookingId;
        int roomId;
        int userId;
        Timestamp bookingDate;
        Timestamp endDate;
        String purpose;
        String status;
        Timestamp createdDate;
        String roomCode;
        String roomName;
        String roomType;
        int capacity;
        String location;
        String roomStatus;
    }
    
    // Legacy methods for backward compatibility
    
    /**
     * Check if a room has any future bookings (legacy method)
     * @param roomId The room code
     * @return true if room has future bookings
     */
    public boolean hasFutureBookings(String roomId) throws SQLException {
        int roomIdInt = getRoomIdByCode(roomId);
        if (roomIdInt == -1) {
            return false;
        }
        
        String sql = "SELECT COUNT(*) AS BookingCount FROM Bookings WHERE RoomID = ? AND BookingDate >= GETDATE() AND Status = 'CONFIRMED'";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, roomIdInt);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("BookingCount") > 0;
                }
            }
        }
        return false;
    }
    
    /**
     * Get count of future bookings for a room (legacy method)
     * @param roomId The room code
     * @return Number of future bookings
     */
    public int getFutureBookingCount(String roomId) throws SQLException {
        int roomIdInt = getRoomIdByCode(roomId);
        if (roomIdInt == -1) {
            return 0;
        }
        
        String sql = "SELECT COUNT(*) AS BookingCount FROM Bookings WHERE RoomID = ? AND BookingDate >= GETDATE() AND Status = 'CONFIRMED'";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, roomIdInt);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("BookingCount");
                }
            }
        }
        return 0;
    }
    
    /**
     * Get list of future booking dates for a room (legacy method)
     * @param roomId The room code
     * @return List of future booking dates
     */
    public List<LocalDateTime> getFutureBookingDates(String roomId) throws SQLException {
        List<LocalDateTime> bookingDates = new ArrayList<>();
        int roomIdInt = getRoomIdByCode(roomId);
        if (roomIdInt == -1) {
            return bookingDates;
        }
        
        String sql = "SELECT BookingDate FROM Bookings WHERE RoomID = ? AND BookingDate >= GETDATE() AND Status = 'CONFIRMED' ORDER BY BookingDate";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, roomIdInt);
            
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
     * Delete a booking (legacy method - now calls cancelBooking)
     * @param bookingId The booking ID to delete
     * @return true if booking deleted successfully
     */
    public boolean deleteBooking(String bookingId) throws SQLException {
        return cancelBooking(bookingId);
    }
}
