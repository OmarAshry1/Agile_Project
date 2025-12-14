package edu.facilities.model;

import java.time.LocalDateTime;

/**
 * Model for transcript request
 * US 2.2, 2.3, 2.4 - Transcript Management
 */
public class TranscriptRequest {
    private final String id;
    private final Student student;
    private final User requestedBy;
    private User processedBy;
    private final LocalDateTime requestDate;
    private LocalDateTime processedDate;
    private LocalDateTime completedDate;
    private LocalDateTime pickupDate;
    private TranscriptStatus status;
    private String purpose;
    private String notes;
    private String pdfPath;

    public TranscriptRequest(String id, Student student, User requestedBy, User processedBy,
                            LocalDateTime requestDate, LocalDateTime processedDate,
                            LocalDateTime completedDate, LocalDateTime pickupDate,
                            TranscriptStatus status, String purpose, String notes, String pdfPath) {
        this.id = id;
        this.student = student;
        this.requestedBy = requestedBy;
        this.processedBy = processedBy;
        this.requestDate = requestDate;
        this.processedDate = processedDate;
        this.completedDate = completedDate;
        this.pickupDate = pickupDate;
        this.status = status;
        this.purpose = purpose;
        this.notes = notes;
        this.pdfPath = pdfPath;
    }

    // Getters
    public String getId() {
        return id;
    }

    public Student getStudent() {
        return student;
    }

    public User getRequestedBy() {
        return requestedBy;
    }

    public User getProcessedBy() {
        return processedBy;
    }

    public LocalDateTime getRequestDate() {
        return requestDate;
    }

    public LocalDateTime getProcessedDate() {
        return processedDate;
    }

    public LocalDateTime getCompletedDate() {
        return completedDate;
    }

    public LocalDateTime getPickupDate() {
        return pickupDate;
    }

    public TranscriptStatus getStatus() {
        return status;
    }

    public String getPurpose() {
        return purpose;
    }

    public String getNotes() {
        return notes;
    }

    public String getPdfPath() {
        return pdfPath;
    }

    // Setters
    public void setProcessedBy(User processedBy) {
        this.processedBy = processedBy;
    }

    public void setProcessedDate(LocalDateTime processedDate) {
        this.processedDate = processedDate;
    }

    public void setCompletedDate(LocalDateTime completedDate) {
        this.completedDate = completedDate;
    }

    public void setPickupDate(LocalDateTime pickupDate) {
        this.pickupDate = pickupDate;
    }

    public void setStatus(TranscriptStatus status) {
        this.status = status;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public void setPdfPath(String pdfPath) {
        this.pdfPath = pdfPath;
    }

    @Override
    public String toString() {
        return "TranscriptRequest{" +
                "id='" + id + '\'' +
                ", student=" + (student != null ? student.getUsername() : "null") +
                ", status=" + status +
                ", requestDate=" + requestDate +
                '}';
    }
}


