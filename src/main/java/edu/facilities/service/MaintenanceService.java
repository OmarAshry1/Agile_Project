package edu.facilities.service;

import edu.facilities.model.MaintenanceTicket;
import edu.facilities.model.Room;
import edu.facilities.model.TicketStatus;
import edu.facilities.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MaintenanceService {

    public MaintenanceTicket createTicket(Room room,
                                          User reporter,
                                          String description) {

        if (room == null || reporter == null || description == null || description.isBlank()) {
            throw new IllegalArgumentException("Room, reporter, and description are required");
        }
        
        // REQUIREMENT: Admins cannot create maintenance tickets
        // Check if reporter is an admin by checking user type
        // Assuming User class has a method to get user type, or we need to check via database
        // For now, we'll check the user type from the reporter object if available
        // This is a defense-in-depth check in addition to the UI check
        String userType = getUserType(reporter);
        if ("ADMIN".equals(userType)) {
            throw new IllegalArgumentException("Administrators cannot create maintenance tickets. Only students, staff, and professors can create tickets.");
        }

        String sql = "INSERT INTO MaintenanceTickets " +
                "(RoomID, ReporterUserID, Description, Status) " +
                "VALUES (?, ?, ?, 'NEW')";

        try (Connection conn = DatabaseConnection.getConnection();
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
    
    /**
     * Get user type from database based on user ID
     * @param user The user object
     * @return User type as String (ADMIN, STUDENT, PROFESSOR, STAFF) or null if not found
     */
    private String getUserType(User user) {
        if (user == null || user.getId() == null) {
            return null;
        }
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            int userId;
            try {
                userId = Integer.parseInt(user.getId());
            } catch (NumberFormatException e) {
                // Try to find user by username if ID is not numeric
                try {
                    // Check if getUsername() method exists and is accessible
                    String username = user.getUsername();
                    if (username != null && !username.isBlank()) {
                        return getUserTypeByUsername(conn, username);
                    }
                } catch (Exception ex) {
                    // If getUsername() doesn't exist or throws exception, return null
                    System.err.println("Could not get username from user object: " + ex.getMessage());
                }
                return null;
            }
            
            String sql = "SELECT UserType FROM Users WHERE UserID = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, userId);
                
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getString("UserType");
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error querying user type: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
    
    /**
     * Get user type from database based on username
     * @param conn Database connection
     * @param username Username to look up
     * @return User type as String or null if not found
     */
    private String getUserTypeByUsername(Connection conn, String username) throws SQLException {
        if (username == null || username.isBlank()) {
            return null;
        }
        
        String sql = "SELECT UserType FROM Users WHERE Username = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("UserType");
                }
            }
        }
        return null;
    }
    
    /**
     * Get all maintenance tickets (for admins)
     * Note: Requires MaintenanceTickets table to have AssignedToUserID column (nullable)
     * If column doesn't exist, run: ALTER TABLE MaintenanceTickets ADD AssignedToUserID INT NULL;
     * @return List of all maintenance tickets
     */
    public List<MaintenanceTicket> getAllTickets() throws SQLException {
        List<MaintenanceTicket> tickets = new ArrayList<>();
        // Check if AssignedToUserID column exists, if not use a simpler query
        String sql = "SELECT t.TicketID, t.RoomID, t.ReporterUserID, " +
                     "ISNULL(t.AssignedToUserID, NULL) as AssignedToUserID, " +
                     "t.Description, t.Status, t.CreatedDate, t.ResolvedDate, " +
                     "r.Code as RoomCode, r.Name as RoomName " +
                     "FROM MaintenanceTickets t " +
                     "LEFT JOIN Rooms r ON t.RoomID = r.RoomID " +
                     "ORDER BY t.CreatedDate DESC";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                MaintenanceTicket ticket = mapResultSetToTicket(rs, conn);
                if (ticket != null) {
                    tickets.add(ticket);
                }
            }
        }
        return tickets;
    }
    
    /**
     * Get tickets assigned to a specific staff member
     * Note: Requires MaintenanceTickets table to have AssignedToUserID column
     * @param staffUserId The user ID of the staff member
     * @return List of tickets assigned to the staff member
     */
    public List<MaintenanceTicket> getTicketsByAssignee(String staffUserId) throws SQLException {
        List<MaintenanceTicket> tickets = new ArrayList<>();
        int assigneeId;
        try {
            assigneeId = Integer.parseInt(staffUserId);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid staff user ID: " + staffUserId);
        }
        
        String sql = "SELECT t.TicketID, t.RoomID, t.ReporterUserID, " +
                     "ISNULL(t.AssignedToUserID, NULL) as AssignedToUserID, " +
                     "t.Description, t.Status, t.CreatedDate, t.ResolvedDate, " +
                     "r.Code as RoomCode, r.Name as RoomName " +
                     "FROM MaintenanceTickets t " +
                     "LEFT JOIN Rooms r ON t.RoomID = r.RoomID " +
                     "WHERE t.AssignedToUserID = ? " +
                     "ORDER BY t.CreatedDate DESC";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, assigneeId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    MaintenanceTicket ticket = mapResultSetToTicket(rs, conn);
                    if (ticket != null) {
                        tickets.add(ticket);
                    }
                }
            }
        }
        return tickets;
    }
    
    /**
     * Assign a ticket to a staff member
     * Note: Requires MaintenanceTickets table to have AssignedToUserID column
     * If column doesn't exist, run: ALTER TABLE MaintenanceTickets ADD AssignedToUserID INT NULL;
     * @param ticketId The ticket ID
     * @param staffUserId The user ID of the staff member to assign to
     * @return true if assignment was successful
     */
    public boolean assignTicketToStaff(String ticketId, String staffUserId) throws SQLException {
        if (ticketId == null || ticketId.isBlank()) {
            throw new IllegalArgumentException("Ticket ID is required");
        }
        if (staffUserId == null || staffUserId.isBlank()) {
            throw new IllegalArgumentException("Staff user ID is required");
        }
        
        int ticketIdInt;
        int staffIdInt;
        try {
            ticketIdInt = Integer.parseInt(ticketId);
            staffIdInt = Integer.parseInt(staffUserId);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid ID format");
        }
        
        // Verify the user is staff
        String userType = getUserTypeById(staffIdInt);
        if (!"STAFF".equals(userType)) {
            throw new IllegalArgumentException("User must be a staff member to be assigned tickets");
        }
        
        String sql = "UPDATE MaintenanceTickets SET AssignedToUserID = ? WHERE TicketID = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, staffIdInt);
            stmt.setInt(2, ticketIdInt);
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        }
    }
    
    /**
     * Get all staff users
     * @return List of staff users
     */
    public List<User> getStaffUsers() throws SQLException {
        List<User> staffUsers = new ArrayList<>();
        String sql = "SELECT UserID, Username, Email FROM Users WHERE UserType = 'STAFF' ORDER BY Username";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                // Create User object - adjust constructor based on your User model
                String userId = String.valueOf(rs.getInt("UserID"));
                String username = rs.getString("Username");
                String email = rs.getString("Email");
                
                // Assuming User has a constructor that takes these parameters
                // You may need to adjust this based on your User model
                User user = new User(userId, username, email);
                staffUsers.add(user);
            }
        }
        return staffUsers;
    }
    
    /**
     * Map a ResultSet row to a MaintenanceTicket object
     */
    private MaintenanceTicket mapResultSetToTicket(ResultSet rs, Connection conn) throws SQLException {
        try {
            int ticketId = rs.getInt("TicketID");
            int roomId = rs.getInt("RoomID");
            int reporterId = rs.getInt("ReporterUserID");
            Integer assignedToId = rs.getObject("AssignedToUserID", Integer.class);
            String description = rs.getString("Description");
            String statusStr = rs.getString("Status");
            Timestamp createdDate = rs.getTimestamp("CreatedDate");
            Timestamp resolvedDate = rs.getTimestamp("ResolvedDate");
            String roomCode = rs.getString("RoomCode");
            
            // Get Room object
            Room room = getRoomById(conn, roomId, roomCode);
            if (room == null) {
                System.err.println("Room not found for ticket " + ticketId);
                return null;
            }
            
            // Get Reporter User object
            User reporter = getUserById(conn, reporterId);
            if (reporter == null) {
                System.err.println("Reporter not found for ticket " + ticketId);
                return null;
            }
            
            // Get Assignee User object if assigned
            User assignee = null;
            if (assignedToId != null) {
                assignee = getUserById(conn, assignedToId);
            }
            
            // Convert status string to enum
            TicketStatus status = TicketStatus.NEW;
            if (statusStr != null) {
                try {
                    status = TicketStatus.valueOf(statusStr.toUpperCase());
                } catch (IllegalArgumentException e) {
                    status = TicketStatus.NEW;
                }
            }
            
            // Convert timestamps
            LocalDateTime created = createdDate != null ? createdDate.toLocalDateTime() : LocalDateTime.now();
            LocalDateTime resolved = resolvedDate != null ? resolvedDate.toLocalDateTime() : null;
            
            return new MaintenanceTicket(
                    String.valueOf(ticketId),
                    room,
                    reporter,
                    assignee,
                    description,
                    status,
                    created,
                    resolved
            );
        } catch (Exception e) {
            System.err.println("Error mapping ticket from ResultSet: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Get room by ID and code
     */
    private Room getRoomById(Connection conn, int roomId, String roomCode) throws SQLException {
        RoomService roomService = new RoomService();
        if (roomCode != null && !roomCode.isBlank()) {
            return roomService.getRoomById(roomCode);
        }
        
        // Fallback: query by RoomID
        String sql = "SELECT Code FROM Rooms WHERE RoomID = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, roomId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String code = rs.getString("Code");
                    return roomService.getRoomById(code);
                }
            }
        }
        return null;
    }
    
    /**
     * Get user by ID
     */
    private User getUserById(Connection conn, int userId) throws SQLException {
        String sql = "SELECT UserID, Username, Email FROM Users WHERE UserID = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String id = String.valueOf(rs.getInt("UserID"));
                    String username = rs.getString("Username");
                    String email = rs.getString("Email");
                    return new User(id, username, email);
                }
            }
        }
        return null;
    }
    
    /**
     * Get user type by user ID
     */
    private String getUserTypeById(int userId) throws SQLException {
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
        return null;
    }
}
