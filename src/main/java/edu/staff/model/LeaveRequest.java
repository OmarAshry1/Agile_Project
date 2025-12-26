package edu.staff.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Model class representing a Leave Request.
 * US 3.11 - Submit Leave Request
 * US 3.12 - Approve or Reject Leave
 * US 3.13 - View Leave History
 */
public class LeaveRequest {
    private int leaveRequestID;
    private int staffUserID;
    private String staffUsername; // For display purposes
    private String leaveType;
    private LocalDate startDate;
    private LocalDate endDate;
    private int numberOfDays;
    private String reason;
    private String status;
    private java.sql.Timestamp submittedDate;
    private Integer reviewedByUserID;
    private java.sql.Timestamp reviewedDate;
    private String rejectionReason;

    public LeaveRequest() {
        this.status = "PENDING";
    }

    public LeaveRequest(int leaveRequestID, int staffUserID, String leaveType,
                       LocalDate startDate, LocalDate endDate, String reason, String status,
                       java.sql.Timestamp submittedDate, Integer reviewedByUserID,
                       java.sql.Timestamp reviewedDate, String rejectionReason) {
        this.leaveRequestID = leaveRequestID;
        this.staffUserID = staffUserID;
        this.leaveType = leaveType;
        this.startDate = startDate;
        this.endDate = endDate;
        this.numberOfDays = calculateNumberOfDays(startDate, endDate);
        this.reason = reason;
        this.status = status != null ? status : "PENDING";
        this.submittedDate = submittedDate;
        this.reviewedByUserID = reviewedByUserID;
        this.reviewedDate = reviewedDate;
        this.rejectionReason = rejectionReason;
    }

    private int calculateNumberOfDays(LocalDate start, LocalDate end) {
        if (start == null || end == null) return 0;
        return (int) ChronoUnit.DAYS.between(start, end) + 1; // Inclusive
    }

    // Getters and Setters
    public int getLeaveRequestID() {
        return leaveRequestID;
    }

    public void setLeaveRequestID(int leaveRequestID) {
        this.leaveRequestID = leaveRequestID;
    }

    public int getStaffUserID() {
        return staffUserID;
    }

    public void setStaffUserID(int staffUserID) {
        this.staffUserID = staffUserID;
    }

    public String getLeaveType() {
        return leaveType;
    }

    public void setLeaveType(String leaveType) {
        this.leaveType = leaveType;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
        if (endDate != null) {
            this.numberOfDays = calculateNumberOfDays(startDate, endDate);
        }
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
        if (startDate != null) {
            this.numberOfDays = calculateNumberOfDays(startDate, endDate);
        }
    }

    public int getNumberOfDays() {
        return numberOfDays;
    }

    public void setNumberOfDays(int numberOfDays) {
        this.numberOfDays = numberOfDays;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public java.sql.Timestamp getSubmittedDate() {
        return submittedDate;
    }

    public void setSubmittedDate(java.sql.Timestamp submittedDate) {
        this.submittedDate = submittedDate;
    }

    public Integer getReviewedByUserID() {
        return reviewedByUserID;
    }

    public void setReviewedByUserID(Integer reviewedByUserID) {
        this.reviewedByUserID = reviewedByUserID;
    }

    public java.sql.Timestamp getReviewedDate() {
        return reviewedDate;
    }

    public void setReviewedDate(java.sql.Timestamp reviewedDate) {
        this.reviewedDate = reviewedDate;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public String getStaffUsername() {
        return staffUsername;
    }

    public void setStaffUsername(String staffUsername) {
        this.staffUsername = staffUsername;
    }

    @Override
    public String toString() {
        return String.format("LeaveRequest[ID=%d, Type=%s, Status=%s, Days=%d, %s to %s]",
                leaveRequestID, leaveType, status, numberOfDays, startDate, endDate);
    }
}

