package edu.staff.model;

import java.time.LocalDateTime;

/**
 * Model class representing a Performance Evaluation.
 * US 3.7, 3.8
 */
public class PerformanceEvaluation {
    private int evaluationID;
    private int staffUserID;
    private String evaluationPeriod;
    private double score;
    private Integer evaluatedByUserID;
    private LocalDateTime evaluationDate;
    private String notes;
    private LocalDateTime createdDate;
    
    // Additional fields for display
    private String staffName;
    private String evaluatedByName;

    public PerformanceEvaluation() {
    }

    public PerformanceEvaluation(int evaluationID, int staffUserID, String evaluationPeriod, 
                                 double score, Integer evaluatedByUserID, LocalDateTime evaluationDate, 
                                 String notes, LocalDateTime createdDate) {
        this.evaluationID = evaluationID;
        this.staffUserID = staffUserID;
        this.evaluationPeriod = evaluationPeriod;
        this.score = score;
        this.evaluatedByUserID = evaluatedByUserID;
        this.evaluationDate = evaluationDate;
        this.notes = notes;
        this.createdDate = createdDate;
    }

    // Getters and Setters
    public int getEvaluationID() {
        return evaluationID;
    }

    public void setEvaluationID(int evaluationID) {
        this.evaluationID = evaluationID;
    }

    public int getStaffUserID() {
        return staffUserID;
    }

    public void setStaffUserID(int staffUserID) {
        this.staffUserID = staffUserID;
    }

    public String getEvaluationPeriod() {
        return evaluationPeriod;
    }

    public void setEvaluationPeriod(String evaluationPeriod) {
        this.evaluationPeriod = evaluationPeriod;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public Integer getEvaluatedByUserID() {
        return evaluatedByUserID;
    }

    public void setEvaluatedByUserID(Integer evaluatedByUserID) {
        this.evaluatedByUserID = evaluatedByUserID;
    }

    public LocalDateTime getEvaluationDate() {
        return evaluationDate;
    }

    public void setEvaluationDate(LocalDateTime evaluationDate) {
        this.evaluationDate = evaluationDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public String getStaffName() {
        return staffName;
    }

    public void setStaffName(String staffName) {
        this.staffName = staffName;
    }

    public String getEvaluatedByName() {
        return evaluatedByName;
    }

    public void setEvaluatedByName(String evaluatedByName) {
        this.evaluatedByName = evaluatedByName;
    }

    @Override
    public String toString() {
        return String.format("%s - Score: %.2f", evaluationPeriod, score);
    }
}

