package edu.facilities.model;

import java.time.LocalDateTime;

/**
 * Model representing a Course in the catalog
 */
public class Course {
    private String id;
    private String code;
    private String name;
    private String description;
    private int credits;
    private String department;
    private String semester;
    private CourseType type;
    private int maxSeats;
    private int currentSeats;
    private boolean isActive;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;
    
    // Related entities
    
    public Course() {
    }
    
    public Course(String id, String code, String name, String description, int credits,
                  String department, String semester, CourseType type, int maxSeats, int currentSeats,
                  boolean isActive, LocalDateTime createdDate, LocalDateTime updatedDate) {
        this();
        this.id = id;
        this.code = code;
        this.name = name;
        this.description = description;
        this.credits = credits;
        this.department = department;
        this.semester = semester;
        this.type = type;
        this.maxSeats = maxSeats;
        this.currentSeats = currentSeats;
        this.isActive = isActive;
        this.createdDate = createdDate;
        this.updatedDate = updatedDate;
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getCode() {
        return code;
    }
    
    public void setCode(String code) {
        this.code = code;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public int getCredits() {
        return credits;
    }
    
    public void setCredits(int credits) {
        this.credits = credits;
    }
    
    public String getDepartment() {
        return department;
    }
    
    public void setDepartment(String department) {
        this.department = department;
    }
    
    public String getSemester() {
        return semester;
    }
    
    public void setSemester(String semester) {
        this.semester = semester;
    }
    
    public CourseType getType() {
        return type;
    }
    
    public void setType(CourseType type) {
        this.type = type;
    }
    
    public int getMaxSeats() {
        return maxSeats;
    }
    
    public void setMaxSeats(int maxSeats) {
        this.maxSeats = maxSeats;
    }
    
    public int getCurrentSeats() {
        return currentSeats;
    }
    
    public void setCurrentSeats(int currentSeats) {
        this.currentSeats = currentSeats;
    }
    
    public boolean isActive() {
        return isActive;
    }
    
    public void setActive(boolean active) {
        isActive = active;
    }
    
    public LocalDateTime getCreatedDate() {
        return createdDate;
    }
    
    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }
    
    public LocalDateTime getUpdatedDate() {
        return updatedDate;
    }
    
    public void setUpdatedDate(LocalDateTime updatedDate) {
        this.updatedDate = updatedDate;
    }
    
    
    public boolean hasAvailableSeats() {
        return currentSeats < maxSeats;
    }
    
    public int getAvailableSeats() {
        return maxSeats - currentSeats;
    }
    
    @Override
    public String toString() {
        return code + " - " + name;
    }
}
