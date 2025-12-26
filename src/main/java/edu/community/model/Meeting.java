package edu.community.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

/**
 * Model class representing a meeting appointment between a student and staff/professor.
 */
public class Meeting {
    private int meetingID;
    private int studentUserID;
    private String studentName;
    private int staffUserID;
    private String staffName;
    private String subject;
    private String description;
    private LocalDate meetingDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String location;
    private String status; // PENDING, APPROVED, REJECTED, CANCELLED, COMPLETED
    private LocalDateTime requestedDate;
    private LocalDateTime responseDate;
    private String responseNotes;
    private LocalDateTime createdDate;

    public Meeting() {
        this.status = "PENDING";
    }

    public Meeting(int meetingID, int studentUserID, int staffUserID, String subject,
                   String description, LocalDate meetingDate, LocalTime startTime, 
                   LocalTime endTime, String location, String status) {
        this.meetingID = meetingID;
        this.studentUserID = studentUserID;
        this.staffUserID = staffUserID;
        this.subject = subject;
        this.description = description;
        this.meetingDate = meetingDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.location = location;
        this.status = status != null ? status : "PENDING";
    }

    // Getters and Setters
    public int getMeetingID() {
        return meetingID;
    }

    public void setMeetingID(int meetingID) {
        this.meetingID = meetingID;
    }

    public int getStudentUserID() {
        return studentUserID;
    }

    public void setStudentUserID(int studentUserID) {
        this.studentUserID = studentUserID;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public int getStaffUserID() {
        return staffUserID;
    }

    public void setStaffUserID(int staffUserID) {
        this.staffUserID = staffUserID;
    }

    public String getStaffName() {
        return staffName;
    }

    public void setStaffName(String staffName) {
        this.staffName = staffName;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getMeetingDate() {
        return meetingDate;
    }

    public void setMeetingDate(LocalDate meetingDate) {
        this.meetingDate = meetingDate;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getRequestedDate() {
        return requestedDate;
    }

    public void setRequestedDate(LocalDateTime requestedDate) {
        this.requestedDate = requestedDate;
    }

    public LocalDateTime getResponseDate() {
        return responseDate;
    }

    public void setResponseDate(LocalDateTime responseDate) {
        this.responseDate = responseDate;
    }

    public String getResponseNotes() {
        return responseNotes;
    }

    public void setResponseNotes(String responseNotes) {
        this.responseNotes = responseNotes;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    @Override
    public String toString() {
        return "Meeting{" +
                "meetingID=" + meetingID +
                ", studentName='" + studentName + '\'' +
                ", staffName='" + staffName + '\'' +
                ", subject='" + subject + '\'' +
                ", meetingDate=" + meetingDate +
                ", status='" + status + '\'' +
                '}';
    }
}

