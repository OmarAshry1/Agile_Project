package edu.facilities.model;

import java.time.LocalDateTime;

/**
 * Represents a room booking in the system
 */
public class Booking {
    private final String id;
    private final Room room;
    private final User user;
    private final LocalDateTime bookingDate;
    private final LocalDateTime endDate;
    private String purpose;
    private BookingStatus status;
    private final LocalDateTime createdAt;

    public Booking(String id, Room room, User user, LocalDateTime bookingDate, 
                   LocalDateTime endDate, String purpose, BookingStatus status, 
                   LocalDateTime createdAt) {
        this.id = id;
        this.room = room;
        this.user = user;
        this.bookingDate = bookingDate;
        this.endDate = endDate;
        this.purpose = purpose;
        this.status = status;
        this.createdAt = createdAt;
    }

    // Getters
    public String getId() {
        return id;
    }

    public Room getRoom() {
        return room;
    }

    public User getUser() {
        return user;
    }

    public LocalDateTime getBookingDate() {
        return bookingDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public void setStatus(BookingStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public String toString() {
        return "Booking{" +
                "id='" + id + '\'' +
                ", room=" + room.getId() +
                ", user=" + user.getUsername() +
                ", bookingDate=" + bookingDate +
                ", endDate=" + endDate +
                ", purpose='" + purpose + '\'' +
                ", status=" + status +
                '}';
    }
}

