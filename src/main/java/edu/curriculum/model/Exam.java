package edu.curriculum.model;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Model representing an Exam
 * US 2.12 - Create Exam
 */
public class Exam {
    private final String id;
    private Course course;
    private String title;
    private LocalDateTime examDate;
    private int durationMinutes;
    private String location;
    private int totalPoints;
    private String instructions;
    private LocalDateTime createdDate;
    private Map<String, String> attributes;

    public Exam(String id, Course course, String title, LocalDateTime examDate,
                int durationMinutes, String location, int totalPoints,
                String instructions, LocalDateTime createdDate, Map<String, String> attributes) {
        this.id = id;
        this.course = course;
        this.title = title;
        this.examDate = examDate;
        this.durationMinutes = durationMinutes;
        this.location = location;
        this.totalPoints = totalPoints;
        this.instructions = instructions;
        this.createdDate = createdDate;
        this.attributes = attributes != null ? new HashMap<>(attributes) : new HashMap<>();
    }

    // --- Getters ---
    public String getId() { return id; }
    public Course getCourse() { return course; }
    public String getTitle() { return title; }
    public LocalDateTime getExamDate() { return examDate; }
    public int getDurationMinutes() { return durationMinutes; }
    public String getLocation() { return location; }
    public int getTotalPoints() { return totalPoints; }
    public String getInstructions() { return instructions; }
    public LocalDateTime getCreatedDate() { return createdDate; }
    public Map<String, String> getAttributes() { return new HashMap<>(attributes); }

    // --- Setters ---
    public void setCourse(Course course) { this.course = course; }
    public void setTitle(String title) { this.title = title; }
    public void setExamDate(LocalDateTime examDate) { this.examDate = examDate; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }
    public void setLocation(String location) { this.location = location; }
    public void setTotalPoints(int totalPoints) { this.totalPoints = totalPoints; }
    public void setInstructions(String instructions) { this.instructions = instructions; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }
    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes != null ? new HashMap<>(attributes) : new HashMap<>();
    }

    // --- Attribute Management Methods ---
    public String getAttribute(String attributeName) {
        return attributes.get(attributeName);
    }

    public void setAttribute(String attributeName, String attributeValue) {
        if (attributeValue != null) {
            attributes.put(attributeName, attributeValue);
        } else {
            attributes.remove(attributeName);
        }
    }

    @Override
    public String toString() {
        return title + " [" + id + "] (" + course.getName() +
                ", date=" + examDate +
                ", duration=" + durationMinutes + " min" +
                ", points=" + totalPoints + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Exam)) return false;
        Exam exam = (Exam) o;
        return Objects.equals(id, exam.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

