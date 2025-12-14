package edu.facilities.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Model for admission application
 * US 2.5 - Admission Application Management
 */
public class AdmissionApplication {
    private final String id;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private LocalDate dateOfBirth;
    private String address;
    private String city;
    private String state;
    private String zipCode;
    private String country;
    private String program;
    private String previousEducation;
    private String documents;
    private ApplicationStatus status;
    private final LocalDateTime submittedDate;
    private LocalDateTime reviewedDate;
    private User reviewedBy;
    private String notes;

    public AdmissionApplication(String id, String firstName, String lastName, String email,
                               String phoneNumber, LocalDate dateOfBirth, String address,
                               String city, String state, String zipCode, String country,
                               String program, String previousEducation, String documents,
                               ApplicationStatus status, LocalDateTime submittedDate,
                               LocalDateTime reviewedDate, User reviewedBy, String notes) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.dateOfBirth = dateOfBirth;
        this.address = address;
        this.city = city;
        this.state = state;
        this.zipCode = zipCode;
        this.country = country;
        this.program = program;
        this.previousEducation = previousEducation;
        this.documents = documents;
        this.status = status;
        this.submittedDate = submittedDate;
        this.reviewedDate = reviewedDate;
        this.reviewedBy = reviewedBy;
        this.notes = notes;
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public String getEmail() {
        return email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public String getAddress() {
        return address;
    }

    public String getCity() {
        return city;
    }

    public String getState() {
        return state;
    }

    public String getZipCode() {
        return zipCode;
    }

    public String getCountry() {
        return country;
    }

    public String getProgram() {
        return program;
    }

    public String getPreviousEducation() {
        return previousEducation;
    }

    public String getDocuments() {
        return documents;
    }

    public ApplicationStatus getStatus() {
        return status;
    }

    public LocalDateTime getSubmittedDate() {
        return submittedDate;
    }

    public LocalDateTime getReviewedDate() {
        return reviewedDate;
    }

    public User getReviewedBy() {
        return reviewedBy;
    }

    public String getNotes() {
        return notes;
    }

    // Setters
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public void setState(String state) {
        this.state = state;
    }

    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public void setProgram(String program) {
        this.program = program;
    }

    public void setPreviousEducation(String previousEducation) {
        this.previousEducation = previousEducation;
    }

    public void setDocuments(String documents) {
        this.documents = documents;
    }

    public void setStatus(ApplicationStatus status) {
        this.status = status;
    }

    public void setReviewedDate(LocalDateTime reviewedDate) {
        this.reviewedDate = reviewedDate;
    }

    public void setReviewedBy(User reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    @Override
    public String toString() {
        return "AdmissionApplication{" +
                "id='" + id + '\'' +
                ", name='" + getFullName() + '\'' +
                ", email='" + email + '\'' +
                ", program='" + program + '\'' +
                ", status=" + status +
                ", submittedDate=" + submittedDate +
                '}';
    }
}


