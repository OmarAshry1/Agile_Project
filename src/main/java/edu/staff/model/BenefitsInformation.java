package edu.staff.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Model class representing Benefits Information.
 * US 3.15 - View Benefits Information
 */
public class BenefitsInformation {
    private int benefitID;
    private int staffUserID;
    private String benefitType;
    private String benefitName;
    private BigDecimal coverageAmount;
    private String coverageDetails;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private String provider;
    private String policyNumber;
    private String notes;
    private java.sql.Timestamp createdDate;
    private java.sql.Timestamp updatedDate;
    private Integer updatedByUserID;

    public BenefitsInformation() {
        this.status = "ACTIVE";
    }

    public BenefitsInformation(int benefitID, int staffUserID, String benefitType,
                               String benefitName, BigDecimal coverageAmount, String coverageDetails,
                               LocalDate startDate, LocalDate endDate, String status,
                               String provider, String policyNumber, String notes,
                               java.sql.Timestamp createdDate, java.sql.Timestamp updatedDate,
                               Integer updatedByUserID) {
        this.benefitID = benefitID;
        this.staffUserID = staffUserID;
        this.benefitType = benefitType;
        this.benefitName = benefitName;
        this.coverageAmount = coverageAmount;
        this.coverageDetails = coverageDetails;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status != null ? status : "ACTIVE";
        this.provider = provider;
        this.policyNumber = policyNumber;
        this.notes = notes;
        this.createdDate = createdDate;
        this.updatedDate = updatedDate;
        this.updatedByUserID = updatedByUserID;
    }

    // Getters and Setters
    public int getBenefitID() {
        return benefitID;
    }

    public void setBenefitID(int benefitID) {
        this.benefitID = benefitID;
    }

    public int getStaffUserID() {
        return staffUserID;
    }

    public void setStaffUserID(int staffUserID) {
        this.staffUserID = staffUserID;
    }

    public String getBenefitType() {
        return benefitType;
    }

    public void setBenefitType(String benefitType) {
        this.benefitType = benefitType;
    }

    public String getBenefitName() {
        return benefitName;
    }

    public void setBenefitName(String benefitName) {
        this.benefitName = benefitName;
    }

    public BigDecimal getCoverageAmount() {
        return coverageAmount;
    }

    public void setCoverageAmount(BigDecimal coverageAmount) {
        this.coverageAmount = coverageAmount;
    }

    public String getCoverageDetails() {
        return coverageDetails;
    }

    public void setCoverageDetails(String coverageDetails) {
        this.coverageDetails = coverageDetails;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getPolicyNumber() {
        return policyNumber;
    }

    public void setPolicyNumber(String policyNumber) {
        this.policyNumber = policyNumber;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public java.sql.Timestamp getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(java.sql.Timestamp createdDate) {
        this.createdDate = createdDate;
    }

    public java.sql.Timestamp getUpdatedDate() {
        return updatedDate;
    }

    public void setUpdatedDate(java.sql.Timestamp updatedDate) {
        this.updatedDate = updatedDate;
    }

    public Integer getUpdatedByUserID() {
        return updatedByUserID;
    }

    public void setUpdatedByUserID(Integer updatedByUserID) {
        this.updatedByUserID = updatedByUserID;
    }

    @Override
    public String toString() {
        return String.format("BenefitsInformation[ID=%d, Type=%s, Name=%s, Status=%s]",
                benefitID, benefitType, benefitName, status);
    }
}

