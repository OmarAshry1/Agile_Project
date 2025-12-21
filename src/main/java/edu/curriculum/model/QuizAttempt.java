package edu.curriculum.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Model representing a Quiz Attempt by a student
 * US 2.11 - Take Quiz
 */
public class QuizAttempt {
    private final String id;
    private Quiz quiz;
    private String studentUserId;
    private int attemptNumber;
    private LocalDateTime startedDate;
    private LocalDateTime completedDate;
    private Integer score;
    private QuizAttemptStatus status;

    public QuizAttempt(String id, Quiz quiz, String studentUserId, int attemptNumber,
                       LocalDateTime startedDate, LocalDateTime completedDate,
                       Integer score, QuizAttemptStatus status) {
        this.id = id;
        this.quiz = quiz;
        this.studentUserId = studentUserId;
        this.attemptNumber = attemptNumber;
        this.startedDate = startedDate;
        this.completedDate = completedDate;
        this.score = score;
        this.status = status;
    }

    // --- Getters ---
    public String getId() { return id; }
    public Quiz getQuiz() { return quiz; }
    public String getStudentUserId() { return studentUserId; }
    public int getAttemptNumber() { return attemptNumber; }
    public LocalDateTime getStartedDate() { return startedDate; }
    public LocalDateTime getCompletedDate() { return completedDate; }
    public Integer getScore() { return score; }
    public QuizAttemptStatus getStatus() { return status; }

    // --- Setters ---
    public void setQuiz(Quiz quiz) { this.quiz = quiz; }
    public void setStudentUserId(String studentUserId) { this.studentUserId = studentUserId; }
    public void setAttemptNumber(int attemptNumber) { this.attemptNumber = attemptNumber; }
    public void setStartedDate(LocalDateTime startedDate) { this.startedDate = startedDate; }
    public void setCompletedDate(LocalDateTime completedDate) { this.completedDate = completedDate; }
    public void setScore(Integer score) { this.score = score; }
    public void setStatus(QuizAttemptStatus status) { this.status = status; }

    @Override
    public String toString() {
        return "QuizAttempt [id=" + id + ", quiz=" + quiz.getTitle() +
                ", attempt=" + attemptNumber + ", status=" + status + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof QuizAttempt)) return false;
        QuizAttempt that = (QuizAttempt) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

