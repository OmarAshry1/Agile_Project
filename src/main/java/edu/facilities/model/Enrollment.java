package edu.facilities.model;

import java.time.LocalDateTime;

/**
 * Model representing a student enrollment in a course
 */
public class Enrollment {
    private String id;
    private User student;
    private Course course;
    private LocalDateTime enrollmentDate;
    private EnrollmentStatus status;
    private String grade;
    
    public Enrollment() {
    }
    
    public Enrollment(String id, User student, Course course, LocalDateTime enrollmentDate,
                     EnrollmentStatus status, String grade) {
        this.id = id;
        this.student = student;
        this.course = course;
        this.enrollmentDate = enrollmentDate;
        this.status = status;
        this.grade = grade;
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public User getStudent() {
        return student;
    }
    
    public void setStudent(User student) {
        this.student = student;
    }
    
    public Course getCourse() {
        return course;
    }
    
    public void setCourse(Course course) {
        this.course = course;
    }
    
    public LocalDateTime getEnrollmentDate() {
        return enrollmentDate;
    }
    
    public void setEnrollmentDate(LocalDateTime enrollmentDate) {
        this.enrollmentDate = enrollmentDate;
    }
    
    public EnrollmentStatus getStatus() {
        return status;
    }
    
    public void setStatus(EnrollmentStatus status) {
        this.status = status;
    }
    
    public String getGrade() {
        return grade;
    }
    
    public void setGrade(String grade) {
        this.grade = grade;
    }
    
    @Override
    public String toString() {
        return course != null ? course.getCode() + " - " + course.getName() : "Enrollment #" + id;
    }
}

