package edu.staff.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Model class representing a Research Activity.
 * US 3.9, 3.10
 */
public class ResearchActivity {
    private int researchID;
    private int staffUserID;
    private String title;
    private String type;
    private LocalDate publicationDate;
    private String description;
    private String journalName;
    private String conferenceName;
    private String publisher;
    private String doi;
    private String url;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;
    
    // Additional fields for display
    private String staffName;
    private String department;

    public ResearchActivity() {
    }

    public ResearchActivity(int researchID, int staffUserID, String title, String type, 
                           LocalDate publicationDate, String description, String journalName, 
                           String conferenceName, String publisher, String doi, String url,
                           LocalDateTime createdDate, LocalDateTime updatedDate) {
        this.researchID = researchID;
        this.staffUserID = staffUserID;
        this.title = title;
        this.type = type;
        this.publicationDate = publicationDate;
        this.description = description;
        this.journalName = journalName;
        this.conferenceName = conferenceName;
        this.publisher = publisher;
        this.doi = doi;
        this.url = url;
        this.createdDate = createdDate;
        this.updatedDate = updatedDate;
    }

    // Getters and Setters
    public int getResearchID() {
        return researchID;
    }

    public void setResearchID(int researchID) {
        this.researchID = researchID;
    }

    public int getStaffUserID() {
        return staffUserID;
    }

    public void setStaffUserID(int staffUserID) {
        this.staffUserID = staffUserID;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public LocalDate getPublicationDate() {
        return publicationDate;
    }

    public void setPublicationDate(LocalDate publicationDate) {
        this.publicationDate = publicationDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getJournalName() {
        return journalName;
    }

    public void setJournalName(String journalName) {
        this.journalName = journalName;
    }

    public String getConferenceName() {
        return conferenceName;
    }

    public void setConferenceName(String conferenceName) {
        this.conferenceName = conferenceName;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public String getDoi() {
        return doi;
    }

    public void setDoi(String doi) {
        this.doi = doi;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
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

    public String getStaffName() {
        return staffName;
    }

    public void setStaffName(String staffName) {
        this.staffName = staffName;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    @Override
    public String toString() {
        return title;
    }
}

