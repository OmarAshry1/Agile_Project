package edu.facilities.service;

import java.sql.*;
import java.time.LocalTime;

/** Business validation for the Sprint 1 course-to-room assignment story. */
public class CourseRoomAssignmentService {
    public void assign(long courseId, long roomId, String day, String start, String end) throws SQLException {
        if (!AuthService.getInstance().hasRole("ADMIN")) {
            throw new SecurityException("Only administrators can assign rooms to courses.");
        }
        String normalizedDay = blankToNull(day);
        String normalizedStart = blankToNull(start);
        String normalizedEnd = blankToNull(end);
        if ((normalizedStart == null) != (normalizedEnd == null)) {
            throw new IllegalArgumentException("Provide both a start time and an end time, or neither.");
        }
        if (normalizedStart != null && normalizedDay == null) {
            throw new IllegalArgumentException("A day is required when a time slot is supplied.");
        }

        Time startTime = null;
        Time endTime = null;
        if (normalizedStart != null) {
            try {
                LocalTime startValue = LocalTime.parse(normalizedStart);
                LocalTime endValue = LocalTime.parse(normalizedEnd);
                if (!startValue.isBefore(endValue)) {
                    throw new IllegalArgumentException("Start time must be before end time.");
                }
                startTime = Time.valueOf(startValue);
                endTime = Time.valueOf(endValue);
            } catch (java.time.format.DateTimeParseException e) {
                throw new IllegalArgumentException("Times must use HH:MM or HH:MM:SS format.");
            }
        }

        try (Connection connection = DatabaseConnection.getConnection()) {
            int courseCapacity;
            try (PreparedStatement statement = connection.prepareStatement(
                    "select capacity from courses where id=? and active=true")) {
                statement.setLong(1, courseId);
                try (ResultSet result = statement.executeQuery()) {
                    if (!result.next()) throw new IllegalArgumentException("Course does not exist or is inactive.");
                    courseCapacity = result.getInt("capacity");
                }
            }

            int roomCapacity;
            try (PreparedStatement statement = connection.prepareStatement(
                    "select capacity from rooms where id=?")) {
                statement.setLong(1, roomId);
                try (ResultSet result = statement.executeQuery()) {
                    if (!result.next()) throw new IllegalArgumentException("Room does not exist.");
                    roomCapacity = result.getInt("capacity");
                }
            }
            if (roomCapacity < courseCapacity) {
                throw new IllegalArgumentException("Room capacity (" + roomCapacity
                        + ") is below course capacity (" + courseCapacity + ").");
            }

            String duplicateSql = "select 1 from course_room_assignments "
                    + "where course_id=? and room_id=? and day_of_week is not distinct from ? "
                    + "and start_time is not distinct from ? and end_time is not distinct from ? limit 1";
            try (PreparedStatement statement = connection.prepareStatement(duplicateSql)) {
                statement.setLong(1, courseId); statement.setLong(2, roomId);
                statement.setString(3, normalizedDay); statement.setTime(4, startTime); statement.setTime(5, endTime);
                try (ResultSet result = statement.executeQuery()) {
                    if (result.next()) throw new IllegalArgumentException("This course-room assignment already exists.");
                }
            }

            if (startTime != null) {
                String conflictSql = "select 1 from course_room_assignments where room_id=? and day_of_week=? "
                        + "and start_time is not null and end_time is not null "
                        + "and start_time < ? and end_time > ? limit 1";
                try (PreparedStatement statement = connection.prepareStatement(conflictSql)) {
                    statement.setLong(1, roomId); statement.setString(2, normalizedDay);
                    statement.setTime(3, endTime); statement.setTime(4, startTime);
                    try (ResultSet result = statement.executeQuery()) {
                        if (result.next()) throw new IllegalArgumentException("The room is already assigned during this time slot.");
                    }
                }
            }

            try (PreparedStatement statement = connection.prepareStatement(
                    "insert into course_room_assignments(course_id,room_id,day_of_week,start_time,end_time) values(?,?,?,?,?)")) {
                statement.setLong(1, courseId); statement.setLong(2, roomId); statement.setString(3, normalizedDay);
                statement.setTime(4, startTime); statement.setTime(5, endTime); statement.executeUpdate();
            }
        }
    }

    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
