package edu.community.model;

import java.time.LocalDateTime;

/**
 * Model class representing a Parent user.
 * Used for parent-teacher communication features.
 */
public class Parent {
    private int parentID;
    private int userID;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String email;
    private String address;
    private LocalDateTime createdDate;

    public Parent() {
    }

    public Parent(int parentID, int userID, String firstName, String lastName, 
                  String phoneNumber, String email, String address, LocalDateTime createdDate) {
        this.parentID = parentID;
        this.userID = userID;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.address = address;
        this.createdDate = createdDate;
    }

    // Getters and Setters
    public int getParentID() {
        return parentID;
    }

    public void setParentID(int parentID) {
        this.parentID = parentID;
    }

    public int getUserID() {
        return userID;
    }

    public void setUserID(int userID) {
        this.userID = userID;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }
}

