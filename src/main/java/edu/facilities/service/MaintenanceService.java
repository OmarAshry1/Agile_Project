package edu.facilities.service;

import edu.facilities.model.MaintenanceTicket;
import edu.facilities.model.Room;
import edu.facilities.model.RoomType;
import edu.facilities.model.RoomStatus;
import edu.facilities.model.TicketStatus;
import edu.facilities.model.User;
import edu.facilities.model.Student;
import edu.facilities.model.Staff;
import edu.facilities.model.Professor;
import edu.facilities.model.Admin;

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

            int rowsAffected = stmt.executeUpdate();
            System.out.println("Ticket insert executed, rows affected: " + rowsAffected);

            int ticketId = -1;
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    ticketId = keys.getInt(1);
                    System.out.println("Generated ticket ID: " + ticketId);
                } else {
                    System.err.println("Warning: No generated key returned for ticket");
                }
            }

            String id = ticketId > 0 ? String.valueOf(ticketId) : null;
            LocalDateTime now = LocalDateTime.now();

            MaintenanceTicket ticket = new MaintenanceTicket(
                    id,
                    room,
                    reporter,
                    null,
                    description,
                    TicketStatus.NEW,
                    now,
                    null
            );
            
            System.out.println("Ticket created successfully: ID=" + id + ", Room=" + room.getId() + ", Reporter=" + reporter.getUsername());
            return ticket;

        } catch (SQLException e) {
            System.err.println("SQL Error inserting maintenance ticket: " + e.getMessage());
            e.printStackTrace();
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
     * Get user type from user object or database
     * Get user type from user object or database
     * @param user The user object
     * @return User type as String (ADMIN, STUDENT, PROFESSOR, STAFF) or null if not found
     */
    private String getUserType(User user) {
        if (user == null) {
            return null;
        }
        
        // First try to get user type from the User object itself (using abstract method)
        try {
            String userType = user.getUserType();
            if (userType != null && !userType.isBlank()) {
                return userType;
            }
        } catch (Exception e) {
            // If getUserType() fails, fall back to database query
            System.err.println("Could not get user type from user object, querying database: " + e.getMessage());
        }
        
        // Fallback: query database if getUserType() didn't work
        if (user.getId() == null) {
            return null;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            int userId;
            try {
                userId = Integer.parseInt(user.getId());
            } catch (NumberFormatException e) {
                // Try to find user by username if ID is not numeric
                try {
                    String username = user.getUsername();
                    if (username != null && !username.isBlank()) {
                        return getUserTypeByUsername(conn, username);
                    }
                } catch (Exception ex) {
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
     * Get a single ticket by ID
     * @param ticketId The ticket ID
     * @return MaintenanceTicket object or null if not found
     * @throws SQLException if database error occurs
     */
    public MaintenanceTicket getTicketById(String ticketId) throws SQLException {
        if (ticketId == null || ticketId.isBlank()) {
            return null;
        }
        
        int ticketIdInt;
        try {
            ticketIdInt = Integer.parseInt(ticketId);
        } catch (NumberFormatException e) {
            return null;
        }
        
        String sql = "SELECT t.TicketID, t.RoomID, t.ReporterUserID, " +
                "t.AssignedToUserID, " +
                "t.Description, t.Status, t.CreatedDate, t.ResolvedDate, " +
                "r.Code as RoomCode, r.Name as RoomName " +
                "FROM MaintenanceTickets t " +
                "LEFT JOIN Rooms r ON t.RoomID = r.RoomID " +
                "WHERE t.TicketID = ?";

        Connection conn = DatabaseConnection.getConnection();
        if (conn == null || conn.isClosed()) {
            throw new SQLException("Database connection is closed or invalid");
        }
        
        TicketData data = null;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, ticketIdInt);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    data = new TicketData();
                    data.ticketId = rs.getInt("TicketID");
                    data.roomId = rs.getInt("RoomID");
                    data.reporterId = rs.getInt("ReporterUserID");
                    data.assignedToId = rs.getObject("AssignedToUserID", Integer.class);
                    data.description = rs.getString("Description");
                    data.statusStr = rs.getString("Status");
                    data.createdDate = rs.getTimestamp("CreatedDate");
                    data.resolvedDate = rs.getTimestamp("ResolvedDate");
                    data.roomCode = rs.getString("RoomCode");
                }
            }
        }
        
        if (data != null) {
            return createTicketFromData(data, conn);
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
                "t.AssignedToUserID, " +
                "t.Description, t.Status, t.CreatedDate, t.ResolvedDate, " +
                "r.Code as RoomCode, r.Name as RoomName " +
                "FROM MaintenanceTickets t " +
                "LEFT JOIN Rooms r ON t.RoomID = r.RoomID " +
                "ORDER BY t.CreatedDate DESC";

        // Get connection - ensure it's valid and stays open
        Connection conn = DatabaseConnection.getConnection();
        if (conn == null || conn.isClosed()) {
            throw new SQLException("Database connection is closed or invalid");
        }
        
        // Ensure connection is still valid before processing
        if (conn.isClosed()) {
            conn = DatabaseConnection.getConnection(); // Get a fresh connection
        }
        
        // First, read all ticket data into a list to avoid ResultSet conflicts
        List<TicketData> ticketDataList = new ArrayList<>();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement(sql);
            rs = stmt.executeQuery();
            
            System.out.println("Querying all tickets from database...");
            int count = 0;
            while (rs.next()) {
                TicketData data = new TicketData();
                data.ticketId = rs.getInt("TicketID");
                data.roomId = rs.getInt("RoomID");
                data.reporterId = rs.getInt("ReporterUserID");
                data.assignedToId = rs.getObject("AssignedToUserID", Integer.class);
                data.description = rs.getString("Description");
                data.statusStr = rs.getString("Status");
                data.createdDate = rs.getTimestamp("CreatedDate");
                data.resolvedDate = rs.getTimestamp("ResolvedDate");
                data.roomCode = rs.getString("RoomCode");
                ticketDataList.add(data);
                count++;
            }
            System.out.println("Found " + count + " tickets in database");
        } finally {
            // Close ResultSet and PreparedStatement but keep connection open
            if (rs != null) {
                try { rs.close(); } catch (SQLException e) { /* ignore */ }
            }
            if (stmt != null) {
                try { stmt.close(); } catch (SQLException e) { /* ignore */ }
            }
        }
        
        // Verify connection is still valid before processing tickets
        if (conn.isClosed()) {
            System.err.println("Connection closed after query, getting new connection...");
            conn = DatabaseConnection.getConnection();
        }
        
        // Now process each ticket data to create MaintenanceTicket objects
        System.out.println("Processing " + ticketDataList.size() + " tickets...");
        for (TicketData data : ticketDataList) {
            try {
                // Verify connection before each ticket
                if (conn.isClosed()) {
                    System.err.println("Connection closed, getting new connection for ticket " + data.ticketId);
                    conn = DatabaseConnection.getConnection();
                }
                
                MaintenanceTicket ticket = createTicketFromData(data, conn);
                if (ticket != null) {
                    tickets.add(ticket);
                    System.out.println("Successfully loaded ticket ID: " + data.ticketId);
                } else {
                    System.err.println("Failed to create ticket object for ticket ID " + data.ticketId + " (returned null)");
                }
            } catch (Exception e) {
                System.err.println("Error creating ticket from data for ticket ID " + data.ticketId + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        System.out.println("Total tickets loaded: " + tickets.size() + " out of " + ticketDataList.size() + " found in database");
        return tickets;
    }
    
    /**
     * Helper class to store ticket data from ResultSet
     */
    private static class TicketData {
        int ticketId;
        int roomId;
        int reporterId;
        Integer assignedToId;
        String description;
        String statusStr;
        Timestamp createdDate;
        Timestamp resolvedDate;
        String roomCode;
    }
    
    /**
     * Create MaintenanceTicket from TicketData
     */
    private MaintenanceTicket createTicketFromData(TicketData data, Connection conn) throws SQLException {
        // Get Room object
        Room room = getRoomById(conn, data.roomId, data.roomCode);
        if (room == null) {
            System.err.println("Room not found for ticket " + data.ticketId);
            return null;
        }

        // Get Reporter User object
        User reporter = getUserById(conn, data.reporterId);
        if (reporter == null) {
            System.err.println("Reporter not found for ticket " + data.ticketId);
            return null;
        }
        
        // Get Assignee Staff object if assigned
        Staff assignee = null;
        if (data.assignedToId != null) {
            assignee = getStaffById(conn, data.assignedToId);
            if (assignee == null) {
                System.err.println("Warning: Ticket " + data.ticketId + " assigned to non-staff user ID " + data.assignedToId);
            }
        }

        // Convert status string to enum
        TicketStatus status = TicketStatus.NEW;
        if (data.statusStr != null) {
            try {
                status = TicketStatus.valueOf(data.statusStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                status = TicketStatus.NEW;
            }
        }

        // Convert timestamps
        LocalDateTime created = data.createdDate != null ? data.createdDate.toLocalDateTime() : LocalDateTime.now();
        LocalDateTime resolved = data.resolvedDate != null ? data.resolvedDate.toLocalDateTime() : null;

        return new MaintenanceTicket(
                String.valueOf(data.ticketId),
                room,
                reporter,
                assignee,
                data.description,
                status,
                created,
                resolved
        );
    }

    /**
     * Get tickets created by a specific user (reporter)
     * @param reporterUserId The user ID of the reporter
     * @return List of tickets created by the user
     * @throws SQLException if database error occurs
     */
    public List<MaintenanceTicket> getTicketsByReporter(String reporterUserId) throws SQLException {
        List<MaintenanceTicket> tickets = new ArrayList<>();
        int reporterId;
        try {
            reporterId = Integer.parseInt(reporterUserId);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid reporter user ID: " + reporterUserId);
        }

        String sql = "SELECT t.TicketID, t.RoomID, t.ReporterUserID, " +
                "t.AssignedToUserID, " +
                "t.Description, t.Status, t.CreatedDate, t.ResolvedDate, " +
                "r.Code as RoomCode, r.Name as RoomName " +
                "FROM MaintenanceTickets t " +
                "LEFT JOIN Rooms r ON t.RoomID = r.RoomID " +
                "WHERE t.ReporterUserID = ? " +
                "ORDER BY t.CreatedDate DESC";

        // Get connection - ensure it's valid
        Connection conn = DatabaseConnection.getConnection();
        if (conn == null || conn.isClosed()) {
            throw new SQLException("Database connection is closed or invalid");
        }
        
        // First, read all ticket data into a list to avoid ResultSet conflicts
        List<TicketData> ticketDataList = new ArrayList<>();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, reporterId);

            rs = stmt.executeQuery();
            while (rs.next()) {
                TicketData data = new TicketData();
                data.ticketId = rs.getInt("TicketID");
                data.roomId = rs.getInt("RoomID");
                data.reporterId = rs.getInt("ReporterUserID");
                data.assignedToId = rs.getObject("AssignedToUserID", Integer.class);
                data.description = rs.getString("Description");
                data.statusStr = rs.getString("Status");
                data.createdDate = rs.getTimestamp("CreatedDate");
                data.resolvedDate = rs.getTimestamp("ResolvedDate");
                data.roomCode = rs.getString("RoomCode");
                ticketDataList.add(data);
            }
        } finally {
            if (rs != null) {
                try { rs.close(); } catch (SQLException e) { /* ignore */ }
            }
            if (stmt != null) {
                try { stmt.close(); } catch (SQLException e) { /* ignore */ }
            }
        }
        
        // Verify connection is still valid before processing tickets
        if (conn.isClosed()) {
            System.err.println("Connection closed after query, getting new connection...");
            conn = DatabaseConnection.getConnection();
        }
        
        // Now process each ticket data to create MaintenanceTicket objects
        for (TicketData data : ticketDataList) {
            try {
                // Verify connection before each ticket
                if (conn.isClosed()) {
                    System.err.println("Connection closed, getting new connection for ticket " + data.ticketId);
                    conn = DatabaseConnection.getConnection();
                }
                
                MaintenanceTicket ticket = createTicketFromData(data, conn);
                if (ticket != null) {
                    tickets.add(ticket);
                }
            } catch (Exception e) {
                System.err.println("Error creating ticket from data for ticket ID " + data.ticketId + ": " + e.getMessage());
                e.printStackTrace();
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
                "t.AssignedToUserID, " +
                "t.Description, t.Status, t.CreatedDate, t.ResolvedDate, " +
                "r.Code as RoomCode, r.Name as RoomName " +
                "FROM MaintenanceTickets t " +
                "LEFT JOIN Rooms r ON t.RoomID = r.RoomID " +
                "WHERE t.AssignedToUserID = ? " +
                "ORDER BY t.CreatedDate DESC";

        // Get connection - ensure it's valid
        Connection conn = DatabaseConnection.getConnection();
        if (conn == null || conn.isClosed()) {
            throw new SQLException("Database connection is closed or invalid");
        }
        
        // First, read all ticket data into a list to avoid ResultSet conflicts
        List<TicketData> ticketDataList = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, assigneeId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    TicketData data = new TicketData();
                    data.ticketId = rs.getInt("TicketID");
                    data.roomId = rs.getInt("RoomID");
                    data.reporterId = rs.getInt("ReporterUserID");
                    data.assignedToId = rs.getObject("AssignedToUserID", Integer.class);
                    data.description = rs.getString("Description");
                    data.statusStr = rs.getString("Status");
                    data.createdDate = rs.getTimestamp("CreatedDate");
                    data.resolvedDate = rs.getTimestamp("ResolvedDate");
                    data.roomCode = rs.getString("RoomCode");
                    ticketDataList.add(data);
                }
            }
        }
        
        // Now process each ticket data to create MaintenanceTicket objects
        for (TicketData data : ticketDataList) {
            try {
                MaintenanceTicket ticket = createTicketFromData(data, conn);
                if (ticket != null) {
                    tickets.add(ticket);
                }
            } catch (Exception e) {
                System.err.println("Error creating ticket from data for ticket ID " + data.ticketId + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        return tickets;
    }

    /**
     * Update ticket status
     * @param ticketId The ticket ID
     * @param newStatus The new status to set
     * @return true if update was successful
     * @throws SQLException if database error occurs
     */
    public boolean updateTicketStatus(String ticketId, TicketStatus newStatus) throws SQLException {
        if (ticketId == null || ticketId.isBlank()) {
            throw new IllegalArgumentException("Ticket ID is required");
        }
        if (newStatus == null) {
            throw new IllegalArgumentException("Status is required");
        }

        int ticketIdInt;
        try {
            ticketIdInt = Integer.parseInt(ticketId);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid ticket ID format");
        }

        String statusStr = newStatus.toString();
        String sql = "UPDATE MaintenanceTickets SET Status = ?";
        
        // If status is RESOLVED, also set ResolvedDate
        if (newStatus == TicketStatus.RESOLVED) {
            sql += ", ResolvedDate = GETDATE()";
        } else {
            // If changing from RESOLVED to another status, clear ResolvedDate
            sql += ", ResolvedDate = NULL";
        }
        
        sql += " WHERE TicketID = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, statusStr);
            pstmt.setInt(2, ticketIdInt);

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        }
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
                // Create Staff object since we're querying for STAFF users
                String userId = String.valueOf(rs.getInt("UserID"));
                String username = rs.getString("Username");
                
                // Create Staff object (password not needed here, pass null)
                User user = new Staff(userId, username, null);
                staffUsers.add(user);
            }
        }
        return staffUsers;
    }

    /**
     * Get room by ID and code using the provided connection
     */
    private Room getRoomById(Connection conn, int roomId, String roomCode) throws SQLException {
        // Validate connection first
        if (conn == null || conn.isClosed()) {
            throw new SQLException("Connection is closed or invalid in getRoomById");
        }
        
        try {
            String sql;
            PreparedStatement pstmt;
            
            if (roomCode != null && !roomCode.isBlank()) {
                // Query by room code
                sql = "SELECT RoomID, Code, Name, Type, Capacity, Location, Status FROM Rooms WHERE Code = ?";
                pstmt = conn.prepareStatement(sql);
                pstmt.setString(1, roomCode);
            } else {
                // Query by RoomID
                sql = "SELECT RoomID, Code, Name, Type, Capacity, Location, Status FROM Rooms WHERE RoomID = ?";
                pstmt = conn.prepareStatement(sql);
                pstmt.setInt(1, roomId);
            }
            
            try (pstmt; ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToRoom(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting room by ID: " + e.getMessage());
            throw e;
        }
        return null;
    }
    
    /**
     * Map ResultSet to Room object
     */
    private Room mapResultSetToRoom(ResultSet rs) throws SQLException {
        String code = rs.getString("Code");
        String name = rs.getString("Name");
        String typeStr = rs.getString("Type");
        int capacity = rs.getInt("Capacity");
        String location = rs.getString("Location");
        String statusStr = rs.getString("Status");
        
        // Convert type string to enum
        RoomType type = RoomType.CLASSROOM;
        if (typeStr != null) {
            switch (typeStr.toUpperCase()) {
                case "CLASSROOM": type = RoomType.CLASSROOM; break;
                case "LAB": case "LABORATORY": type = RoomType.LAB; break;
                case "OFFICE": type = RoomType.OFFICE; break;
                case "CONFERENCE": type = RoomType.CONFERENCE; break;
            }
        }
        
        // Convert status string to enum
        RoomStatus status = RoomStatus.AVAILABLE;
        if (statusStr != null) {
            switch (statusStr.toUpperCase()) {
                case "AVAILABLE": status = RoomStatus.AVAILABLE; break;
                case "OCCUPIED": status = RoomStatus.OCCUPIED; break;
                case "MAINTENANCE": status = RoomStatus.MAINTENANCE; break;
            }
        }
        
        return new Room(code, name, type, capacity, location, status);
    }

    /**
     * Get user by ID
     */
    private User getUserById(Connection conn, int userId) throws SQLException {
        // Validate connection first
        if (conn == null || conn.isClosed()) {
            throw new SQLException("Connection is closed or invalid in getUserById");
        }
        
        String sql = "SELECT UserID, Username, Email, UserType FROM Users WHERE UserID = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String id = String.valueOf(rs.getInt("UserID"));
                    String username = rs.getString("Username");
                    String userType = rs.getString("UserType");
                    
                    // Create appropriate concrete User instance based on userType
                    return createUser(id, username, userType);
                }
            }
        }
        return null;
    }
    
    /**
     * Create appropriate User instance based on userType
     * @param id User ID
     * @param username Username
     * @param userType User type (STUDENT, PROFESSOR, STAFF, ADMIN)
     * @return User instance of appropriate type
     */
    private User createUser(String id, String username, String userType) {
        if (userType == null || userType.isBlank()) {
            return new Student(id, username, null); // Default to Student
        }
        
        // Create appropriate concrete User instance based on userType
        switch (userType.toUpperCase()) {
            case "STUDENT":
                return new Student(id, username, null);
            case "PROFESSOR":
                return new Professor(id, username, null);
            case "STAFF":
                return new Staff(id, username, null);
            case "ADMIN":
                return new Admin(id, username, null);
            default:
                // Default to Student if userType is unknown
                return new Student(id, username, null);
        }
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
    
    /**
     * Get Staff user by ID
     * @param conn Database connection
     * @param userId User ID
     * @return Staff object if user is Staff, null otherwise
     */
    private Staff getStaffById(Connection conn, int userId) throws SQLException {
        String sql = "SELECT UserID, Username, Email, UserType FROM Users WHERE UserID = ? AND UserType = 'STAFF'";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String id = String.valueOf(rs.getInt("UserID"));
                    String username = rs.getString("Username");
                    // Create Staff object directly since we know it's Staff
                    return new Staff(id, username, null);
                }
            }
        }
        return null;
    }
}
