package edu.facilities.model;

import java.time.LocalDateTime;

public class MaintenanceTicket {

    private final String id;
    private final edu.facilities.model.Room room;
    private final edu.facilities.model.User reporter;
    private Staff assignedStaff;

    private String description;
    private TicketStatus status;
    private final LocalDateTime createdAt;
    private LocalDateTime resolvedAt;


    public MaintenanceTicket(String id,
                             edu.facilities.model.Room room,
                             edu.facilities.model.User reporter,
                             Staff assignedStaff,
                             String description,
                             TicketStatus status,
                             LocalDateTime createdAt,
                             LocalDateTime resolvedAt) {
        this.id = id;
        this.room = room;
        this.reporter = reporter;
        this.assignedStaff = assignedStaff;
        this.description = description;
        this.status = status;
        this.createdAt = createdAt;
        this.resolvedAt = resolvedAt;
    }


    public MaintenanceTicket(edu.facilities.model.Room room,
                             edu.facilities.model.User reporter,
                             String description) {
        this.id = null;
        this.room = room;
        this.reporter = reporter;
        this.assignedStaff = null;
        this.description = description;
        this.status = TicketStatus.NEW;
        this.createdAt = LocalDateTime.now();
        this.resolvedAt = null;
    }

    public String getId() {
        return id;
    }

    public Room getRoom() {
        return room;
    }

    public User getReporter() {
        return reporter;
    }

    public Staff getAssignedStaff() {
        return assignedStaff;
    }

    public void setAssignedStaff(Staff assignedStaff) {
        this.assignedStaff = assignedStaff;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public void setStatus(TicketStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }
}
