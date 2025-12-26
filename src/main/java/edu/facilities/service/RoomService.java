package edu.facilities.service;

import edu.facilities.model.Room;
import edu.facilities.model.RoomStatus;
import edu.facilities.model.RoomType;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for managing room data using SQL Server database
 */
public class RoomService {

    /**
     * Get all rooms from the database
     * @return List of all rooms
     * @throws SQLException if database error occurs
     */
    public List<Room> getAllRooms() throws SQLException {
        List<Room> rooms = new ArrayList<>();
        String sql = "SELECT r.RoomID, r.Code, r.Name, rt.TypeCode as Type, " +
                     "r.Capacity, r.Location, st.StatusCode as Status " +
                     "FROM Rooms r " +
                     "LEFT JOIN RoomTypes rt ON r.RoomTypeID = rt.RoomTypeID " +
                     "LEFT JOIN StatusTypes st ON r.StatusTypeID = st.StatusTypeID AND st.EntityType = 'ROOM'";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                Room room = mapResultSetToRoom(rs);
                rooms.add(room);
            }
        }
        
        return rooms;
    }

    /**
     * Get all available rooms from the database
     * @return List of available rooms (Status = 'AVAILABLE')
     * @throws SQLException if database error occurs
     */
    public List<Room> getAvailableRooms() throws SQLException {
        List<Room> rooms = new ArrayList<>();
        String sql = "SELECT r.RoomID, r.Code, r.Name, rt.TypeCode as Type, " +
                     "r.Capacity, r.Location, st.StatusCode as Status " +
                     "FROM Rooms r " +
                     "LEFT JOIN RoomTypes rt ON r.RoomTypeID = rt.RoomTypeID " +
                     "LEFT JOIN StatusTypes st ON r.StatusTypeID = st.StatusTypeID AND st.EntityType = 'ROOM' " +
                     "WHERE UPPER(st.StatusCode) = 'AVAILABLE' " +
                     "ORDER BY r.Code";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                Room room = mapResultSetToRoom(rs);
                rooms.add(room);
            }
        }
        
        return rooms;
    }

    /**
     * Create a new room in the database
     * @param roomCode The room code/identifier
     * @param roomName The room name
     * @param type The room type
     * @param capacity The room capacity
     * @param location The room location (format: "Building|Floor|Equipment")
     * @param status The room status
     * @throws SQLException if database error occurs
     */
    public void createRoom(String roomCode, String roomName, RoomType type, 
                          int capacity, String location, RoomStatus status) throws SQLException {
        Connection conn = DatabaseConnection.getConnection();
        // Get RoomTypeID
        int roomTypeId = getRoomTypeId(conn, typeToString(type));
        // Get StatusTypeID for ROOM entity
        int statusTypeId = getStatusTypeId(conn, "ROOM", statusToString(status));
        
        String sql = "INSERT INTO Rooms (Code, Name, RoomTypeID, Capacity, Location, StatusTypeID) VALUES (?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, roomCode);
            pstmt.setString(2, roomName);
            pstmt.setInt(3, roomTypeId);
            pstmt.setInt(4, capacity);
            pstmt.setString(5, location);
            pstmt.setInt(6, statusTypeId);
            
            pstmt.executeUpdate();
        }
    }

    /**
     * Delete a room from the database by room code
     * First deletes related records (MaintenanceTickets, Bookings) to avoid foreign key constraint violations
     * @param roomCode The room code to delete
     * @return true if room was deleted, false otherwise
     * @throws SQLException if database error occurs
     */
    public boolean deleteRoom(String roomCode) throws SQLException {
        Connection conn = DatabaseConnection.getConnection();
        boolean originalAutoCommit = conn.getAutoCommit();
        
        try {
            // Disable auto-commit to use transaction
            conn.setAutoCommit(false);
            
            // First, get the RoomID for this room code (needed for foreign key deletions)
            int roomId = getRoomIdByCode(conn, roomCode);
            if (roomId == -1) {
                // Room doesn't exist
                conn.rollback();
                System.out.println("Room not found for deletion: " + roomCode);
                return false;
            }
            
            // Delete related maintenance tickets first (to avoid foreign key constraint)
            try {
                String deleteTicketsSql = "DELETE FROM MaintenanceTickets WHERE RoomID = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(deleteTicketsSql)) {
                    pstmt.setInt(1, roomId);
                    int ticketsDeleted = pstmt.executeUpdate();
                    if (ticketsDeleted > 0) {
                        System.out.println("Deleted " + ticketsDeleted + " maintenance ticket(s) for room: " + roomCode);
                    }
                }
            } catch (SQLException e) {
                // Table might not exist or no tickets - continue anyway
                System.out.println("Note: Could not delete maintenance tickets (may not exist): " + e.getMessage());
            }
            
            // Delete related bookings if they exist (to avoid foreign key constraint)
            try {
                String deleteBookingsSql = "DELETE FROM Bookings WHERE RoomID = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(deleteBookingsSql)) {
                    pstmt.setInt(1, roomId);
                    int bookingsDeleted = pstmt.executeUpdate();
                    if (bookingsDeleted > 0) {
                        System.out.println("Deleted " + bookingsDeleted + " booking(s) for room: " + roomCode);
                    }
                }
            } catch (SQLException e) {
                // Table might not exist or no bookings - continue anyway
                System.out.println("Note: Could not delete bookings (may not exist): " + e.getMessage());
            }
            
            // Now delete the room itself
            String deleteRoomSql = "DELETE FROM Rooms WHERE Code = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(deleteRoomSql)) {
                pstmt.setString(1, roomCode);
                int rowsAffected = pstmt.executeUpdate();
                
                if (rowsAffected > 0) {
                    // Commit the transaction
                    conn.commit();
                    System.out.println("Successfully deleted room: " + roomCode);
                    return true;
                } else {
                    // No rows affected - room not found (shouldn't happen since we checked above)
                    conn.rollback();
                    System.out.println("Warning: Room not found during deletion: " + roomCode);
                    return false;
                }
            }
        } catch (SQLException e) {
            // Rollback on any error
            try {
                conn.rollback();
            } catch (SQLException rollbackEx) {
                System.err.println("Error during rollback: " + rollbackEx.getMessage());
            }
            System.err.println("Error deleting room '" + roomCode + "': " + e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            // Restore original auto-commit setting
            try {
                conn.setAutoCommit(originalAutoCommit);
            } catch (SQLException e) {
                System.err.println("Error restoring auto-commit: " + e.getMessage());
            }
        }
    }
    
    /**
     * Get room by room code
     * @param roomCode The room code to search for
     * @return Room object if found, null otherwise
     * @throws SQLException if database error occurs
     */
    public Room getRoomById(String roomCode) throws SQLException {
        String sql = "SELECT r.RoomID, r.Code, r.Name, rt.TypeCode as Type, " +
                     "r.Capacity, r.Location, st.StatusCode as Status " +
                     "FROM Rooms r " +
                     "LEFT JOIN RoomTypes rt ON r.RoomTypeID = rt.RoomTypeID " +
                     "LEFT JOIN StatusTypes st ON r.StatusTypeID = st.StatusTypeID AND st.EntityType = 'ROOM' " +
                     "WHERE r.Code = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, roomCode);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToRoom(rs);
                }
            }
        }
        return null;
    }

    /**
     * Get RoomID by room code (helper method for foreign key operations)
     * @param conn The database connection
     * @param roomCode The room code
     * @return RoomID if found, -1 otherwise
     * @throws SQLException if database error occurs
     */
    private int getRoomIdByCode(Connection conn, String roomCode) throws SQLException {
        String sql = "SELECT RoomID FROM Rooms WHERE Code = ?";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
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
     * Update room type for a specific room
     * Uses room code to identify the room (important: uses WHERE clause to update only the specific room)
     * @param roomCode The room code to identify the room
     * @param type The new room type
     * @throws SQLException if database error occurs
     */
    public void updateRoomType(String roomCode, RoomType type) throws SQLException {
        Connection conn = DatabaseConnection.getConnection();
        int roomTypeId = getRoomTypeId(conn, typeToString(type));
        String sql = "UPDATE Rooms SET RoomTypeID = ? WHERE Code = ?";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, roomTypeId);
            pstmt.setString(2, roomCode);
            
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("Room with code '" + roomCode + "' not found");
            }
        }
    }

    /**
     * Update room capacity for a specific room
     * Uses room code to identify the room (important: uses WHERE clause to update only the specific room)
     * @param roomCode The room code to identify the room
     * @param capacity The new capacity
     * @throws SQLException if database error occurs
     */
    public void updateRoomCapacity(String roomCode, int capacity) throws SQLException {
        String sql = "UPDATE Rooms SET Capacity = ? WHERE Code = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, capacity);
            pstmt.setString(2, roomCode);
            
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("Room with code '" + roomCode + "' not found");
            }
        }
    }

    /**
     * Update room status for a specific room
     * Uses room code to identify the room (important: uses WHERE clause to update only the specific room)
     * @param roomCode The room code to identify the room
     * @param status The new status
     * @throws SQLException if database error occurs
     */
    public void updateRoomStatus(String roomCode, RoomStatus status) throws SQLException {
        Connection conn = DatabaseConnection.getConnection();
        int statusTypeId = getStatusTypeId(conn, "ROOM", statusToString(status));
        String sql = "UPDATE Rooms SET StatusTypeID = ? WHERE Code = ?";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, statusTypeId);
            pstmt.setString(2, roomCode);
            
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("Room with code '" + roomCode + "' not found");
            }
        }
    }

    /**
     * Update room code/identifier for a specific room
     * Uses original room code to identify the room, then updates to new code
     * @param originalRoomCode The original room code to identify the room
     * @param newRoomCode The new room code
     * @throws SQLException if database error occurs
     */
    public void updateRoomCode(String originalRoomCode, String newRoomCode) throws SQLException {
        String sql = "UPDATE Rooms SET Code = ?, Name = ? WHERE Code = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, newRoomCode);
            pstmt.setString(2, newRoomCode); // Update name to match code
            pstmt.setString(3, originalRoomCode); // Use original code in WHERE clause
            
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("Room with code '" + originalRoomCode + "' not found");
            }
        }
    }

    /**
     * Update room location for a specific room
     * Uses room code to identify the room (important: uses WHERE clause to update only the specific room)
     * @param roomCode The room code to identify the room
     * @param location The new location (format: "Building|Floor|Equipment")
     * @throws SQLException if database error occurs
     */
    public void updateRoomLocation(String roomCode, String location) throws SQLException {
        String sql = "UPDATE Rooms SET Location = ? WHERE Code = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, location);
            pstmt.setString(2, roomCode);
            
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("Room with code '" + roomCode + "' not found");
            }
        }
    }

    /**
     * Map ResultSet row to Room object
     */
    private Room mapResultSetToRoom(ResultSet rs) throws SQLException {
        String roomId = rs.getString("Code");
        String name = rs.getString("Name");
        RoomType type = stringToRoomType(rs.getString("Type"));
        int capacity = rs.getInt("Capacity");
        String location = rs.getString("Location");
        RoomStatus status = stringToRoomStatus(rs.getString("Status"));
        
        return new Room(roomId, name, type, capacity, location, status);
    }

    /**
     * Update all room attributes in a single transaction
     * @param originalRoomCode The original room code to identify the room
     * @param newRoomCode The new room code (can be same as original)
     * @param roomName The room name
     * @param type The room type
     * @param capacity The room capacity
     * @param location The room location
     * @param status The room status
     * @throws SQLException if database error occurs
     */
    public void updateRoom(String originalRoomCode, String newRoomCode, String roomName,
                          RoomType type, int capacity, String location, RoomStatus status) throws SQLException {
        Connection conn = DatabaseConnection.getConnection();
        int roomTypeId = getRoomTypeId(conn, typeToString(type));
        int statusTypeId = getStatusTypeId(conn, "ROOM", statusToString(status));
        
        String sql = "UPDATE Rooms SET Code = ?, Name = ?, RoomTypeID = ?, Capacity = ?, Location = ?, StatusTypeID = ? WHERE Code = ?";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newRoomCode);
            pstmt.setString(2, roomName);
            pstmt.setInt(3, roomTypeId);
            pstmt.setInt(4, capacity);
            pstmt.setString(5, location);
            pstmt.setInt(6, statusTypeId);
            pstmt.setString(7, originalRoomCode);
            
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("Room with code '" + originalRoomCode + "' not found");
            }
        }
    }

    /**
     * Convert RoomType enum to database string
     * Note: Database only accepts 'CLASSROOM' or 'LAB' per CHECK constraint
     */
    private String typeToString(RoomType type) {
        if (type == null) return "CLASSROOM";
        switch (type) {
            case CLASSROOM: return "CLASSROOM";
            case LAB: return "LAB";
            case OFFICE: 
            case CONFERENCE: 
            default: return "CLASSROOM"; // Map other types to CLASSROOM to satisfy CHECK constraint
        }
    }

    /**
     * Convert database string to RoomType enum
     */
    private RoomType stringToRoomType(String typeStr) {
        if (typeStr == null) return RoomType.CLASSROOM;
        switch (typeStr.toUpperCase()) {
            case "CLASSROOM": return RoomType.CLASSROOM;
            case "LAB": case "LABORATORY": return RoomType.LAB;
            case "OFFICE": return RoomType.OFFICE;
            case "CONFERENCE": return RoomType.CONFERENCE;
            default: return RoomType.CLASSROOM;
        }
    }

    /**
     * Convert RoomStatus enum to database string
     */
    private String statusToString(RoomStatus status) {
        if (status == null) return "AVAILABLE";
        switch (status) {
            case AVAILABLE: return "AVAILABLE";
            case OCCUPIED: return "OCCUPIED";
            case MAINTENANCE: return "MAINTENANCE";
            default: return "AVAILABLE";
        }
    }

    /**
     * Convert database string to RoomStatus enum
     */
    private RoomStatus stringToRoomStatus(String statusStr) {
        if (statusStr == null) return RoomStatus.AVAILABLE;
        switch (statusStr.toUpperCase()) {
            case "AVAILABLE": return RoomStatus.AVAILABLE;
            case "OCCUPIED": case "BOOKED": return RoomStatus.OCCUPIED;
            case "MAINTENANCE": return RoomStatus.MAINTENANCE;
            default: return RoomStatus.AVAILABLE;
        }
    }
    
    /**
     * Get RoomTypeID from RoomTypes table by TypeCode
     */
    private int getRoomTypeId(Connection conn, String typeCode) throws SQLException {
        String sql = "SELECT RoomTypeID FROM RoomTypes WHERE TypeCode = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, typeCode.toUpperCase());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("RoomTypeID");
                }
            }
        }
        throw new SQLException("RoomType with code '" + typeCode + "' not found");
    }
    
    /**
     * Get StatusTypeID from StatusTypes table by EntityType and StatusCode
     */
    private int getStatusTypeId(Connection conn, String entityType, String statusCode) throws SQLException {
        String sql = "SELECT StatusTypeID FROM StatusTypes WHERE EntityType = ? AND StatusCode = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, entityType);
            pstmt.setString(2, statusCode.toUpperCase());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("StatusTypeID");
                }
            }
        }
        throw new SQLException("StatusType with EntityType '" + entityType + "' and StatusCode '" + statusCode + "' not found");
    }
}

