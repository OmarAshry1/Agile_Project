package edu.staff.model;

import java.time.LocalDate;

/**
 * Model class representing a Staff Profile.
 */
public class StaffProfile {
    private int staffID;
    private Integer userID; // Can be null if not linked to a system user yet
    private String name;
    private String role;
    private String department;
    private String email;
    private String officeHours;
    private String officeLocation;
    private String phone;
    private LocalDate hireDate;
    private String bio;
    private boolean isActive;

    public StaffProfile() {
    }

    public StaffProfile(int staffID, Integer userID, String name, String role, String department, String email,
            String officeHours, String officeLocation, String phone, LocalDate hireDate, String bio, boolean isActive) {
        this.staffID = staffID;
        this.userID = userID;
        this.name = name;
        this.role = role;
        this.department = department;
        this.email = email;
        this.officeHours = officeHours;
        this.officeLocation = officeLocation;
        this.phone = phone;
        this.hireDate = hireDate;
        this.bio = bio;
        this.isActive = isActive;
    }

    // Getters and Setters

    public int getStaffID() {
        return staffID;
    }

    public void setStaffID(int staffID) {
        this.staffID = staffID;
    }

    public Integer getUserID() {
        return userID;
    }

    public void setUserID(Integer userID) {
        this.userID = userID;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getOfficeHours() {
        return officeHours;
    }

    public void setOfficeHours(String officeHours) {
        this.officeHours = officeHours;
    }

    public String getOfficeLocation() {
        return officeLocation;
    }

    public void setOfficeLocation(String officeLocation) {
        this.officeLocation = officeLocation;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public LocalDate getHireDate() {
        return hireDate;
    }

    public void setHireDate(LocalDate hireDate) {
        this.hireDate = hireDate;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    @Override
    public String toString() {
        return name + " (" + role + ")";
    }
}
