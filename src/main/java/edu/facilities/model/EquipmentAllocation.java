package edu.facilities.model;

import java.time.LocalDateTime;

/**
 * Represents an equipment allocation to a staff member or department
 */
public class EquipmentAllocation {
    private final String id;
    private final Equipment equipment;
    private final User allocatedToUser;
    private final String department;
    private final User allocatedByUser;
    private final LocalDateTime allocationDate;
    private LocalDateTime returnDate;
    private String notes;
    private AllocationStatus status;

    public EquipmentAllocation(String id, Equipment equipment, User allocatedToUser, 
                              String department, User allocatedByUser, 
                              LocalDateTime allocationDate, LocalDateTime returnDate, 
                              String notes, AllocationStatus status) {
        this.id = id;
        this.equipment = equipment;
        this.allocatedToUser = allocatedToUser;
        this.department = department;
        this.allocatedByUser = allocatedByUser;
        this.allocationDate = allocationDate;
        this.returnDate = returnDate;
        this.notes = notes;
        this.status = status;
    }

    // Getters
    public String getId() {
        return id;
    }

    public Equipment getEquipment() {
        return equipment;
    }

    public User getAllocatedToUser() {
        return allocatedToUser;
    }

    public String getDepartment() {
        return department;
    }

    public User getAllocatedByUser() {
        return allocatedByUser;
    }

    public LocalDateTime getAllocationDate() {
        return allocationDate;
    }

    public LocalDateTime getReturnDate() {
        return returnDate;
    }

    public void setReturnDate(LocalDateTime returnDate) {
        this.returnDate = returnDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public AllocationStatus getStatus() {
        return status;
    }

    public void setStatus(AllocationStatus status) {
        this.status = status;
    }
}

