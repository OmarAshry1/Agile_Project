package edu.curriculum.model;

import edu.facilities.model.Student;
import java.time.LocalDateTime;
import java.util.Objects;

public class AssignmentSubmission {
    private final String id;
    private Assignment assignment;
    private Student student;
    private String submissionText;
    private String fileName;
    private LocalDateTime submittedDate;
    private Integer score;
    private String feedback;
    private SubmissionStatus status;
    private LocalDateTime gradedDate;

    public AssignmentSubmission(String id, Assignment assignment, Student student,
                               String submissionText, String fileName,
                               LocalDateTime submittedDate, Integer score, String feedback,
                               SubmissionStatus status, LocalDateTime gradedDate) {
        this.id = id;
        this.assignment = assignment;
        this.student = student;
        this.submissionText = submissionText;
        this.fileName = fileName;
        this.submittedDate = submittedDate;
        this.score = score;
        this.feedback = feedback;
        this.status = status;
        this.gradedDate = gradedDate;
    }

    // --- Getters ---
    public String getId() { return id; }
    public Assignment getAssignment() { return assignment; }
    public Student getStudent() { return student; }
    public String getSubmissionText() { return submissionText; }
    public String getFileName() { return fileName; }
    public LocalDateTime getSubmittedDate() { return submittedDate; }
    public Integer getScore() { return score; }
    public String getFeedback() { return feedback; }
    public SubmissionStatus getStatus() { return status; }
    public LocalDateTime getGradedDate() { return gradedDate; }

    // --- Setters ---
    public void setAssignment(Assignment assignment) { this.assignment = assignment; }
    public void setStudent(Student student) { this.student = student; }
    public void setSubmissionText(String submissionText) { this.submissionText = submissionText; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public void setSubmittedDate(LocalDateTime submittedDate) { this.submittedDate = submittedDate; }
    public void setScore(Integer score) { this.score = score; }
    public void setFeedback(String feedback) { this.feedback = feedback; }
    public void setStatus(SubmissionStatus status) { this.status = status; }
    public void setGradedDate(LocalDateTime gradedDate) { this.gradedDate = gradedDate; }

    @Override
    public String toString() {
        return "Submission [" + id + "] (" + assignment.getTitle() +
                ", student=" + student.getUsername() +
                ", score=" + (score != null ? score : "N/A") +
                ", status=" + status + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AssignmentSubmission)) return false;
        AssignmentSubmission that = (AssignmentSubmission) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

