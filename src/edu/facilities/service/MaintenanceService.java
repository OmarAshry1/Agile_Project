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

            int roomId = Integer.parseInt(room.getId());
            int reporterId = Integer.parseInt(reporter.getId());

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
                    null,                 // no assigned staff yet
                    description,
                    TicketStatus.NEW,
                    now,
                    null                  // not resolved yet
            );

        } catch (SQLException e) {
            throw new RuntimeException("Error inserting maintenance ticket", e);
        }
    }
}
