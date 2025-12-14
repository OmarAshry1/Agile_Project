package edu.curriculum.model;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Assignment {
    private final String id;
    private Course course;
    private String title;
    private String instructions;
    private LocalDateTime dueDate;
    private int totalPoints;
    private SubmissionType submissionType;
    private LocalDateTime createdDate;
    private Map<String, String> attributes;

    public Assignment(String id, Course course, String title, String instructions,
                     LocalDateTime dueDate, int totalPoints, SubmissionType submissionType,
                     LocalDateTime createdDate, Map<String, String> attributes) {
        this.id = id;
        this.course = course;
        this.title = title;
        this.instructions = instructions;
        this.dueDate = dueDate;
        this.totalPoints = totalPoints;
        this.submissionType = submissionType;
        this.createdDate = createdDate;
        this.attributes = attributes != null ? new HashMap<>(attributes) : new HashMap<>();
    }

    // --- Getters ---
    public String getId() { return id; }
    public Course getCourse() { return course; }
    public String getTitle() { return title; }
    public String getInstructions() { return instructions; }
    public LocalDateTime getDueDate() { return dueDate; }
    public int getTotalPoints() { return totalPoints; }
    public SubmissionType getSubmissionType() { return submissionType; }
    public LocalDateTime getCreatedDate() { return createdDate; }
    public Map<String, String> getAttributes() { return new HashMap<>(attributes); }

    // --- Setters ---
    public void setCourse(Course course) { this.course = course; }
    public void setTitle(String title) { this.title = title; }
    public void setInstructions(String instructions) { this.instructions = instructions; }
    public void setDueDate(LocalDateTime dueDate) { this.dueDate = dueDate; }
    public void setTotalPoints(int totalPoints) { this.totalPoints = totalPoints; }
    public void setSubmissionType(SubmissionType submissionType) { this.submissionType = submissionType; }
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
                ", due=" + dueDate +
                ", points=" + totalPoints +
                ", type=" + submissionType + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Assignment)) return false;
        Assignment that = (Assignment) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

