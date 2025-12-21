package edu.curriculum.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Model representing an Exam Grade
 * US 2.13 - Record Exam Grades
 */
public class ExamGrade {
    private final String id;
    private Exam exam;
    private String studentUserId;
    private Integer pointsEarned;
    private String comments;
    private LocalDateTime gradedDate;

    public ExamGrade(String id, Exam exam, String studentUserId,
                     Integer pointsEarned, String comments, LocalDateTime gradedDate) {
        this.id = id;
        this.exam = exam;
        this.studentUserId = studentUserId;
        this.pointsEarned = pointsEarned;
        this.comments = comments;
        this.gradedDate = gradedDate;
    }

    // --- Getters ---
    public String getId() { return id; }
    public Exam getExam() { return exam; }
    public String getStudentUserId() { return studentUserId; }
    public Integer getPointsEarned() { return pointsEarned; }
    public String getComments() { return comments; }
    public LocalDateTime getGradedDate() { return gradedDate; }

    // --- Setters ---
    public void setExam(Exam exam) { this.exam = exam; }
    public void setStudentUserId(String studentUserId) { this.studentUserId = studentUserId; }
    public void setPointsEarned(Integer pointsEarned) { this.pointsEarned = pointsEarned; }
    public void setComments(String comments) { this.comments = comments; }
    public void setGradedDate(LocalDateTime gradedDate) { this.gradedDate = gradedDate; }

    /**
     * Calculate percentage score
     */
    public double getPercentage() {
        if (pointsEarned == null || exam == null || exam.getTotalPoints() == 0) {
            return 0.0;
        }
        return (pointsEarned.doubleValue() / exam.getTotalPoints()) * 100.0;
    }

    @Override
    public String toString() {
        return "ExamGrade [id=" + id + ", exam=" + exam.getTitle() +
                ", points=" + pointsEarned + "/" + exam.getTotalPoints() + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ExamGrade)) return false;
        ExamGrade examGrade = (ExamGrade) o;
        return Objects.equals(id, examGrade.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

