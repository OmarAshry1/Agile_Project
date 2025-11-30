package edu.facilities.model;

import java.time.LocalDateTime;

/**
 * Represents a piece of equipment in the system
 */
public class Equipment {
    private final String id;
    private final String equipmentTypeId;
    private final String equipmentTypeName;
    private String serialNumber;
    private EquipmentStatus status;
    private String location;
    private String notes;
    private final LocalDateTime createdDate;

    public Equipment(String id, String equipmentTypeId, String equipmentTypeName, 
                    String serialNumber, EquipmentStatus status, String location, 
                    String notes, LocalDateTime createdDate) {
        this.id = id;
        this.equipmentTypeId = equipmentTypeId;
        this.equipmentTypeName = equipmentTypeName;
        this.serialNumber = serialNumber;
        this.status = status;
        this.location = location;
        this.notes = notes;
        this.createdDate = createdDate;
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getEquipmentTypeId() {
        return equipmentTypeId;
    }

    public String getEquipmentTypeName() {
        return equipmentTypeName;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public EquipmentStatus getStatus() {
        return status;
    }

    public void setStatus(EquipmentStatus status) {
        this.status = status;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    @Override
    public String toString() {
        return equipmentTypeName + " [" + id + "] (" + status + ")";
    }
}

