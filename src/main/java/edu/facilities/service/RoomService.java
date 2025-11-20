package edu.facilities.service;

import edu.facilities.model.Room;
import edu.facilities.model.RoomType;
import edu.facilities.model.RoomStatus;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for managing rooms using SQL Server database
 */
public class RoomService {

    /**
     * Create a new room in the database
     * Note: RoomID is auto-increment, Code is the identifier
     */
    public void createRoom(String code, String name, RoomType type, int capacity,
                           String location, RoomStatus status) throws SQLException {
        String sql = "INSERT INTO Rooms (Code, Name, Type, Capacity, Location, Status) VALUES (?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, code);
            pstmt.setString(2, name);
            pstmt.setString(3, type.toString());
            pstmt.setInt(4, capacity);
            pstmt.setString(5, location);
            pstmt.setString(6, status.toString());
            
            pstmt.executeUpdate();
        }
    }

    /**
     * Get a room by Code from the database
     * Note: Uses Code field as identifier (e.g., 'R101', 'LAB1')
     */
    public Room getRoomById(String roomCode) throws SQLException {
        String sql = "SELECT Code, Name, Type, Capacity, Location, Status FROM Rooms WHERE Code = ?";
        
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
     * Get all rooms from the database
     */
    public List<Room> getAllRooms() throws SQLException {
        List<Room> rooms = new ArrayList<>();
        String sql = "SELECT Code, Name, Type, Capacity, Location, Status FROM Rooms ORDER BY Code";
        
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                rooms.add(mapResultSetToRoom(rs));
            }
        }
        return rooms;
    }

    /**
     * Delete a room from the database by Code
     */
    public boolean deleteRoom(String roomCode) throws SQLException {
        String sql = "DELETE FROM Rooms WHERE Code = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, roomCode);
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        }
    }

    /**
     * Update room type in the database
     */
    public boolean updateRoomType(String roomCode, RoomType newType) throws SQLException {
        String sql = "UPDATE Rooms SET Type = ? WHERE Code = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, newType.toString());
            pstmt.setString(2, roomCode);
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        }
    }

    /**
     * Update room capacity in the database
     */
    public boolean updateRoomCapacity(String roomCode, int newCapacity) throws SQLException {
        String sql = "UPDATE Rooms SET Capacity = ? WHERE Code = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, newCapacity);
            pstmt.setString(2, roomCode);
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        }
    }

    /**
     * Update room status in the database
     */
    public boolean updateRoomStatus(String roomCode, RoomStatus newStatus) throws SQLException {
        String sql = "UPDATE Rooms SET Status = ? WHERE Code = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, newStatus.toString());
            pstmt.setString(2, roomCode);
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        }
    }

    /**
     * Update room name in the database
     */
    public boolean updateRoomName(String roomCode, String newName) throws SQLException {
        String sql = "UPDATE Rooms SET Name = ? WHERE Code = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, newName);
            pstmt.setString(2, roomCode);
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        }
    }

    /**
     * Update room location in the database
     */
    public boolean updateRoomLocation(String roomCode, String newLocation) throws SQLException {
        String sql = "UPDATE Rooms SET Location = ? WHERE Code = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, newLocation);
            pstmt.setString(2, roomCode);
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        }
    }

    /**
     * Get all available rooms from the database
     */
    public List<Room> getAvailableRooms() throws SQLException {
        List<Room> availableRooms = new ArrayList<>();
        String sql = "SELECT Code, Name, Type, Capacity, Location, Status FROM Rooms WHERE Status = 'AVAILABLE' ORDER BY Code";
        
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                availableRooms.add(mapResultSetToRoom(rs));
            }
        }
        return availableRooms;
    }

    /**
     * Map ResultSet row to Room object
     * Note: Code field maps to Room.id, RoomID is auto-increment and not used
     */
    private Room mapResultSetToRoom(ResultSet rs) throws SQLException {
        String code = rs.getString("Code");
        String name = rs.getString("Name");
        RoomType type = RoomType.valueOf(rs.getString("Type"));
        int capacity = rs.getInt("Capacity");
        String location = rs.getString("Location");
        RoomStatus status = RoomStatus.valueOf(rs.getString("Status"));
        
        return new Room(code, name, type, capacity, location, status);
    }
}

