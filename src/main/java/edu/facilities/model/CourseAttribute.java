package edu.facilities.model;

import java.time.LocalDateTime;

/**
 * Model representing a flexible attribute for a course (EAV pattern)
 */
public class CourseAttribute {
    private String id;
    private String courseId;
    private String attributeName;
    private String attributeValue;
    private CourseAttributeType attributeType;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;
    
    public CourseAttribute() {
    }
    
    public CourseAttribute(String id, String courseId, String attributeName, 
                          String attributeValue, CourseAttributeType attributeType,
                          LocalDateTime createdDate, LocalDateTime updatedDate) {
        this.id = id;
        this.courseId = courseId;
        this.attributeName = attributeName;
        this.attributeValue = attributeValue;
        this.attributeType = attributeType;
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
    
    public String getCourseId() {
        return courseId;
    }
    
    public void setCourseId(String courseId) {
        this.courseId = courseId;
    }
    
    public String getAttributeName() {
        return attributeName;
    }
    
    public void setAttributeName(String attributeName) {
        this.attributeName = attributeName;
    }
    
    public String getAttributeValue() {
        return attributeValue;
    }
    
    public void setAttributeValue(String attributeValue) {
        this.attributeValue = attributeValue;
    }
    
    public CourseAttributeType getAttributeType() {
        return attributeType;
    }
    
    public void setAttributeType(CourseAttributeType attributeType) {
        this.attributeType = attributeType;
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
}

