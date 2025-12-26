package edu.community.service;

import edu.community.model.Event;
import edu.community.model.EventReminder;
import edu.facilities.service.DatabaseConnection;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for events and calendar.
 * US 4.11 - Create Event
 * US 4.12 - View Events Calendar
 * US 4.13 - Event Reminder Notification
 */
public class EventService {

    /**
     * Create a new event (US 4.11)
     */
    public int createEvent(Event event) throws SQLException {
        String sql = "INSERT INTO Events (CreatedByUserID, Title, Description, EventDate, StartTime, " +
                     "EndTime, Location, EventType, IsPublic, IsRecurring, RecurrencePattern, " +
                     "RecurrenceEndDate, CreatedDate, LastModifiedDate) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP) " +
                     "RETURNING EventID";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, event.getCreatedByUserID());
            pstmt.setString(2, event.getTitle());
            pstmt.setString(3, event.getDescription());
            pstmt.setDate(4, Date.valueOf(event.getEventDate()));
            pstmt.setTime(5, Time.valueOf(event.getStartTime()));
            
            if (event.getEndTime() != null) {
                pstmt.setTime(6, Time.valueOf(event.getEndTime()));
            } else {
                pstmt.setNull(6, Types.TIME);
            }
            
            pstmt.setString(7, event.getLocation());
            pstmt.setString(8, event.getEventType());
            pstmt.setBoolean(9, event.isPublic());
            pstmt.setBoolean(10, event.isRecurring());
            pstmt.setString(11, event.getRecurrencePattern());
            
            if (event.getRecurrenceEndDate() != null) {
                pstmt.setDate(12, Date.valueOf(event.getRecurrenceEndDate()));
            } else {
                pstmt.setNull(12, Types.DATE);
            }
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("EventID");
                }
            }
        }
        
        throw new SQLException("Failed to create event");
    }

    /**
     * Get events for a date range (US 4.12)
     */
    public List<Event> getEvents(LocalDate startDate, LocalDate endDate, boolean publicOnly) throws SQLException {
        String sql = "SELECT e.*, u.USERNAME as CreatedByName " +
                    "FROM Events e " +
                    "LEFT JOIN Users u ON e.CreatedByUserID = u.UserID " +
                    "WHERE e.EventDate >= ? AND e.EventDate <= ?";
        
        if (publicOnly) {
            sql += " AND e.IsPublic = TRUE";
        }
        
        sql += " ORDER BY e.EventDate ASC, e.StartTime ASC";
        
        List<Event> events = new ArrayList<>();
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setDate(1, Date.valueOf(startDate));
            pstmt.setDate(2, Date.valueOf(endDate));
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    events.add(mapResultSetToEvent(rs));
                }
            }
        }
        
        return events;
    }

    /**
     * Get events for a specific month
     */
    public List<Event> getEventsForMonth(int year, int month, boolean publicOnly) throws SQLException {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
        return getEvents(startDate, endDate, publicOnly);
    }

    /**
     * Get a single event by ID
     */
    public Event getEventById(int eventID) throws SQLException {
        String sql = "SELECT e.*, u.USERNAME as CreatedByName " +
                    "FROM Events e " +
                    "LEFT JOIN Users u ON e.CreatedByUserID = u.UserID " +
                    "WHERE e.EventID = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, eventID);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToEvent(rs);
                }
            }
        }
        
        return null;
    }

    /**
     * Create an event reminder (US 4.13)
     */
    public int createReminder(EventReminder reminder) throws SQLException {
        String sql = "INSERT INTO EventReminders (EventID, UserID, ReminderTime, IsSent, ReminderType, CreatedDate) " +
                     "VALUES (?, ?, ?, FALSE, ?, CURRENT_TIMESTAMP) RETURNING ReminderID";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, reminder.getEventID());
            pstmt.setInt(2, reminder.getUserID());
            pstmt.setTimestamp(3, Timestamp.valueOf(reminder.getReminderTime()));
            pstmt.setString(4, reminder.getReminderType());
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("ReminderID");
                }
            }
        }
        
        throw new SQLException("Failed to create reminder");
    }

    /**
     * Get reminders for a user
     */
    public List<EventReminder> getUserReminders(int userID) throws SQLException {
        String sql = "SELECT * FROM EventReminders " +
                    "WHERE UserID = ? AND IsSent = FALSE " +
                    "ORDER BY ReminderTime ASC";
        
        List<EventReminder> reminders = new ArrayList<>();
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userID);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    reminders.add(mapResultSetToReminder(rs));
                }
            }
        }
        
        return reminders;
    }

    /**
     * Delete a reminder
     */
    public void deleteReminder(int reminderID) throws SQLException {
        String sql = "DELETE FROM EventReminders WHERE ReminderID = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, reminderID);
            pstmt.executeUpdate();
        }
    }

    /**
     * Get event types
     */
    public List<String> getEventTypes() {
        List<String> types = new ArrayList<>();
        types.add("GENERAL");
        types.add("ACADEMIC");
        types.add("SOCIAL");
        types.add("SPORTS");
        types.add("ADMINISTRATIVE");
        types.add("HOLIDAY");
        return types;
    }

    private Event mapResultSetToEvent(ResultSet rs) throws SQLException {
        Event event = new Event();
        event.setEventID(rs.getInt("EventID"));
        event.setCreatedByUserID(rs.getInt("CreatedByUserID"));
        event.setCreatedByName(rs.getString("CreatedByName"));
        event.setTitle(rs.getString("Title"));
        event.setDescription(rs.getString("Description"));
        
        Date eventDate = rs.getDate("EventDate");
        if (eventDate != null) {
            event.setEventDate(eventDate.toLocalDate());
        }
        
        Time startTime = rs.getTime("StartTime");
        if (startTime != null) {
            event.setStartTime(startTime.toLocalTime());
        }
        
        Time endTime = rs.getTime("EndTime");
        if (endTime != null) {
            event.setEndTime(endTime.toLocalTime());
        }
        
        event.setLocation(rs.getString("Location"));
        event.setEventType(rs.getString("EventType"));
        event.setPublic(rs.getBoolean("IsPublic"));
        event.setRecurring(rs.getBoolean("IsRecurring"));
        event.setRecurrencePattern(rs.getString("RecurrencePattern"));
        
        Date recurrenceEndDate = rs.getDate("RecurrenceEndDate");
        if (recurrenceEndDate != null) {
            event.setRecurrenceEndDate(recurrenceEndDate.toLocalDate());
        }
        
        Timestamp createdDate = rs.getTimestamp("CreatedDate");
        if (createdDate != null) {
            event.setCreatedDate(createdDate.toLocalDateTime());
        }
        
        Timestamp lastModifiedDate = rs.getTimestamp("LastModifiedDate");
        if (lastModifiedDate != null) {
            event.setLastModifiedDate(lastModifiedDate.toLocalDateTime());
        }
        
        return event;
    }

    private EventReminder mapResultSetToReminder(ResultSet rs) throws SQLException {
        EventReminder reminder = new EventReminder();
        reminder.setReminderID(rs.getInt("ReminderID"));
        reminder.setEventID(rs.getInt("EventID"));
        reminder.setUserID(rs.getInt("UserID"));
        
        Timestamp reminderTime = rs.getTimestamp("ReminderTime");
        if (reminderTime != null) {
            reminder.setReminderTime(reminderTime.toLocalDateTime());
        }
        
        reminder.setSent(rs.getBoolean("IsSent"));
        
        Timestamp sentDate = rs.getTimestamp("SentDate");
        if (sentDate != null) {
            reminder.setSentDate(sentDate.toLocalDateTime());
        }
        
        reminder.setReminderType(rs.getString("ReminderType"));
        
        Timestamp createdDate = rs.getTimestamp("CreatedDate");
        if (createdDate != null) {
            reminder.setCreatedDate(createdDate.toLocalDateTime());
        }
        
        return reminder;
    }
}

