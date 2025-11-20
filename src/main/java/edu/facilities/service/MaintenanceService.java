package edu.facilities.service;

import edu.facilities.data.Database;
import edu.facilities.model.MaintenanceTicket;
import edu.facilities.model.Room;
import edu.facilities.model.TicketStatus;
import edu.facilities.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;

public class MaintenanceService {

    public MaintenanceTicket createTicket(Room room,
                                          User reporter,
                                          String description) {

        if (room == null || reporter == null || description == null || description.isBlank()) {
            throw new IllegalArgumentException("Room, reporter, and description are required");
        }

        String sql = "INSERT INTO MaintenanceTickets " +
                "(RoomID, ReporterUserID, Description, Status) " +
                "VALUES (?, ?, ?, 'NEW')";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {


            int roomId = getRoomIdByCode(conn, room.getId());
            if (roomId == -1) {
                throw new IllegalArgumentException("Room with code '" + room.getId() + "' not found");
            }


            int reporterId;
            try {
                reporterId = Integer.parseInt(reporter.getId());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid user ID: " + reporter.getId());
            }

            stmt.setInt(1, roomId);
            stmt.setInt(2, reporterId);
            stmt.setString(3, description);

            stmt.executeUpdate();

            int ticketId = -1;
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    ticketId = keys.getInt(1);
                }
            }

            String id = ticketId > 0 ? String.valueOf(ticketId) : null;
            LocalDateTime now = LocalDateTime.now();

            return new MaintenanceTicket(
                    id,
                    room,
                    reporter,
                    null,
                    description,
                    TicketStatus.NEW,
                    now,
                    null
            );

        } catch (SQLException e) {
            throw new RuntimeException("Error inserting maintenance ticket", e);
        }
    }


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
}
