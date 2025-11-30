package edu.facilities.model;

import java.time.LocalDate;

/**
 * Represents a software license in the system
 */
public class SoftwareLicense {
    private final String id;
    private final String softwareName;
    private String licenseKey;
    private String vendor;
    private LocalDate purchaseDate;
    private LocalDate expiryDate;
    private Double cost;
    private int quantity;
    private int usedQuantity;
    private LicenseStatus status;
    private String notes;
    private final LocalDate createdDate;
    private LocalDate updatedDate;

    public SoftwareLicense(String id, String softwareName, String licenseKey, 
                          String vendor, LocalDate purchaseDate, LocalDate expiryDate, 
                          Double cost, int quantity, int usedQuantity, LicenseStatus status, 
                          String notes, LocalDate createdDate, LocalDate updatedDate) {
        this.id = id;
        this.softwareName = softwareName;
        this.licenseKey = licenseKey;
        this.vendor = vendor;
        this.purchaseDate = purchaseDate;
        this.expiryDate = expiryDate;
        this.cost = cost;
        this.quantity = quantity;
        this.usedQuantity = usedQuantity;
        this.status = status;
        this.notes = notes;
        this.createdDate = createdDate;
        this.updatedDate = updatedDate;
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getSoftwareName() {
        return softwareName;
    }

    public String getLicenseKey() {
        return licenseKey;
    }

    public void setLicenseKey(String licenseKey) {
        this.licenseKey = licenseKey;
    }

    public String getVendor() {
        return vendor;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    public LocalDate getPurchaseDate() {
        return purchaseDate;
    }

    public void setPurchaseDate(LocalDate purchaseDate) {
        this.purchaseDate = purchaseDate;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public Double getCost() {
        return cost;
    }

    public void setCost(Double cost) {
        this.cost = cost;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public int getUsedQuantity() {
        return usedQuantity;
    }

    public void setUsedQuantity(int usedQuantity) {
        this.usedQuantity = usedQuantity;
    }

    public int getAvailableQuantity() {
        return quantity - usedQuantity;
    }

    public LicenseStatus getStatus() {
        return status;
    }

    public void setStatus(LicenseStatus status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDate getCreatedDate() {
        return createdDate;
    }

    public LocalDate getUpdatedDate() {
        return updatedDate;
    }

    public void setUpdatedDate(LocalDate updatedDate) {
        this.updatedDate = updatedDate;
    }

    /**
     * Check if license is near expiry (within 30 days)
     */
    public boolean isNearExpiry() {
        if (expiryDate == null) {
            return false; // Perpetual license
        }
        LocalDate today = LocalDate.now();
        LocalDate warningDate = expiryDate.minusDays(30);
        return !today.isAfter(expiryDate) && !today.isBefore(warningDate);
    }

    /**
     * Check if license is expired
     */
    public boolean isExpired() {
        if (expiryDate == null) {
            return false; // Perpetual license
        }
        return LocalDate.now().isAfter(expiryDate);
    }
}

