package edu.community.model;

import java.time.LocalDateTime;

/**
 * Model class representing an event reminder.
 * US 4.13 - Event Reminder Notification
 */
public class EventReminder {
    private int reminderID;
    private int eventID;
    private int userID;
    private LocalDateTime reminderTime;
    private boolean isSent;
    private LocalDateTime sentDate;
    private String reminderType; // EMAIL, IN_APP, BOTH
    private LocalDateTime createdDate;

    public EventReminder() {
        this.reminderType = "IN_APP";
        this.isSent = false;
    }

    public EventReminder(int reminderID, int eventID, int userID, LocalDateTime reminderTime,
                        String reminderType) {
        this.reminderID = reminderID;
        this.eventID = eventID;
        this.userID = userID;
        this.reminderTime = reminderTime;
        this.reminderType = reminderType != null ? reminderType : "IN_APP";
        this.isSent = false;
    }

    // Getters and Setters
    public int getReminderID() {
        return reminderID;
    }

    public void setReminderID(int reminderID) {
        this.reminderID = reminderID;
    }

    public int getEventID() {
        return eventID;
    }

    public void setEventID(int eventID) {
        this.eventID = eventID;
    }

    public int getUserID() {
        return userID;
    }

    public void setUserID(int userID) {
        this.userID = userID;
    }

    public LocalDateTime getReminderTime() {
        return reminderTime;
    }

    public void setReminderTime(LocalDateTime reminderTime) {
        this.reminderTime = reminderTime;
    }

    public boolean isSent() {
        return isSent;
    }

    public void setSent(boolean sent) {
        isSent = sent;
    }

    public LocalDateTime getSentDate() {
        return sentDate;
    }

    public void setSentDate(LocalDateTime sentDate) {
        this.sentDate = sentDate;
    }

    public String getReminderType() {
        return reminderType;
    }

    public void setReminderType(String reminderType) {
        this.reminderType = reminderType;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }
}

