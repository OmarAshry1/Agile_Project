package edu.facilities.service;

import edu.facilities.model.*;
import edu.facilities.model.Student;
import edu.facilities.model.Professor;
import edu.facilities.model.Staff;
import edu.facilities.model.Admin;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for managing equipment, allocations, and software licenses
 */
public class EquipmentService {

    /**
     * Get all available equipment of a specific type
     * @param equipmentTypeName The name of the equipment type to search for
     * @param department The department to filter by (null for all departments)
     * @return List of available equipment
     * @throws SQLException if database error occurs
     */
    public List<Equipment> getAvailableEquipment(String equipmentTypeName, String department) throws SQLException {
        List<Equipment> equipmentList = new ArrayList<>();
        
        String sql = "SELECT e.EquipmentID, e.EquipmentTypeID, et.Name as EquipmentTypeName, " +
                    "e.SerialNumber, st.StatusCode as Status, e.Location, e.Notes, e.CreatedDate " +
                    "FROM Equipment e " +
                    "INNER JOIN EquipmentType et ON e.EquipmentTypeID = et.EquipmentTypeID " +
                    "INNER JOIN StatusTypes st ON e.StatusTypeID = st.StatusTypeID AND st.EntityType = 'EQUIPMENT' " +
                    "WHERE st.StatusCode = 'AVAILABLE' " +
                    "AND (? IS NULL OR et.Name LIKE ?)";
        
        // If department is specified, only show equipment not allocated to other departments
        if (department != null && !department.isBlank()) {
            sql += " AND e.EquipmentID NOT IN (" +
                  "SELECT EquipmentID FROM EquipmentAllocation " +
                  "WHERE Status = 'ACTIVE' AND Department IS NOT NULL AND Department != ?)";
        }
        
        sql += " ORDER BY et.Name, e.EquipmentID";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, equipmentTypeName != null && !equipmentTypeName.isBlank() ? "%" + equipmentTypeName + "%" : null);
            pstmt.setString(2, equipmentTypeName != null && !equipmentTypeName.isBlank() ? "%" + equipmentTypeName + "%" : null);
            
            if (department != null && !department.isBlank()) {
                pstmt.setString(3, department);
            }
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Equipment equipment = mapResultSetToEquipment(rs);
                    equipmentList.add(equipment);
                }
            }
        }
        
        return equipmentList;
    }

    /**
     * Get equipment allocated to a specific user
     * @param userId The user ID
     * @return List of equipment allocations
     * @throws SQLException if database error occurs
     */
    public List<EquipmentAllocation> getEquipmentByUser(String userId) throws SQLException {
        List<EquipmentAllocation> allocations = new ArrayList<>();
        
        int userIdInt;
        try {
            userIdInt = Integer.parseInt(userId);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid user ID: " + userId);
        }
        
        String sql = "SELECT ea.AllocationID, ea.EquipmentID, ea.AllocatedToUserID, " +
                    "d.Name as Department, ea.AllocatedByUserID, ea.AllocationDate, " +
                    "ea.ReturnDate, ea.Notes, ast.StatusCode as Status, " +
                    "e.EquipmentTypeID, et.Name as EquipmentTypeName, " +
                    "e.SerialNumber, est.StatusCode as EquipmentStatus, e.Location " +
                    "FROM EquipmentUserAllocations ea " +
                    "INNER JOIN Equipment e ON ea.EquipmentID = e.EquipmentID " +
                    "INNER JOIN EquipmentType et ON e.EquipmentTypeID = et.EquipmentTypeID " +
                    "INNER JOIN StatusTypes est ON e.StatusTypeID = est.StatusTypeID AND est.EntityType = 'EQUIPMENT' " +
                    "INNER JOIN StatusTypes ast ON ea.StatusTypeID = ast.StatusTypeID AND ast.EntityType = 'ALLOCATION' " +
                    "LEFT JOIN Departments d ON ea.DepartmentID = d.DepartmentID " +
                    "WHERE ea.AllocatedToUserID = ? AND ast.StatusCode = 'ACTIVE' " +
                    "ORDER BY ea.AllocationDate DESC";
        
        Connection conn = DatabaseConnection.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userIdInt);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    EquipmentAllocation allocation = createAllocationFromResultSet(rs, conn);
                    if (allocation != null) {
                        allocations.add(allocation);
                    }
                }
            }
        }
        
        return allocations;
    }

    /**
     * Get equipment allocated to a specific department
     * @param department The department name
     * @return List of equipment allocations
     * @throws SQLException if database error occurs
     */
    public List<EquipmentAllocation> getEquipmentByDepartment(String department) throws SQLException {
        List<EquipmentAllocation> allocations = new ArrayList<>();
        
        String sql = "SELECT ea.AllocationID, ea.EquipmentID, ea.AllocatedToUserID, " +
                    "d.Name as Department, ea.AllocatedByUserID, ea.AllocationDate, " +
                    "ea.ReturnDate, ea.Notes, ast.StatusCode as Status, " +
                    "e.EquipmentTypeID, et.Name as EquipmentTypeName, " +
                    "e.SerialNumber, est.StatusCode as EquipmentStatus, e.Location " +
                    "FROM EquipmentDepartmentAllocations ea " +
                    "INNER JOIN Equipment e ON ea.EquipmentID = e.EquipmentID " +
                    "INNER JOIN EquipmentType et ON e.EquipmentTypeID = et.EquipmentTypeID " +
                    "INNER JOIN StatusTypes est ON e.StatusTypeID = est.StatusTypeID AND est.EntityType = 'EQUIPMENT' " +
                    "INNER JOIN StatusTypes ast ON ea.StatusTypeID = ast.StatusTypeID AND ast.EntityType = 'ALLOCATION' " +
                    "INNER JOIN Departments d ON ea.DepartmentID = d.DepartmentID " +
                    "WHERE d.Name = ? AND ast.StatusCode = 'ACTIVE' " +
                    "ORDER BY ea.AllocationDate DESC";
        
        Connection conn = DatabaseConnection.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, department);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    EquipmentAllocation allocation = createAllocationFromResultSet(rs, conn);
                    if (allocation != null) {
                        allocations.add(allocation);
                    }
                }
            }
        }
        
        return allocations;
    }

    /**
     * Allocate equipment to a user or department
     * @param equipmentId The equipment ID
     * @param allocatedToUserId The user ID to allocate to (null if allocating to department)
     * @param department The department to allocate to (null if allocating to user)
     * @param allocatedByUserId The admin user ID who is allocating
     * @param notes Optional notes
     * @return EquipmentAllocation object if successful
     * @throws SQLException if database error occurs
     */
    public EquipmentAllocation allocateEquipment(String equipmentId, String allocatedToUserId, 
                                                 String department, String allocatedByUserId, 
                                                 String notes) throws SQLException {
        if (equipmentId == null || equipmentId.isBlank()) {
            throw new IllegalArgumentException("Equipment ID is required");
        }
        
        if ((allocatedToUserId == null || allocatedToUserId.isBlank()) && 
            (department == null || department.isBlank())) {
            throw new IllegalArgumentException("Either user ID or department must be specified");
        }
        
        int equipmentIdInt;
        int allocatedByUserIdInt;
        try {
            equipmentIdInt = Integer.parseInt(equipmentId);
            allocatedByUserIdInt = Integer.parseInt(allocatedByUserId);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid ID format");
        }
        
        // Check if equipment is available
        String checkSql = "SELECT st.StatusCode as Status " +
                          "FROM Equipment e " +
                          "INNER JOIN StatusTypes st ON e.StatusTypeID = st.StatusTypeID AND st.EntityType = 'EQUIPMENT' " +
                          "WHERE e.EquipmentID = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
            
            checkStmt.setInt(1, equipmentIdInt);
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalArgumentException("Equipment not found");
                }
                String status = rs.getString("Status");
                if (!"AVAILABLE".equals(status)) {
                    throw new IllegalArgumentException("Equipment is not available for allocation");
                }
            }
        }
        
        // Get status type IDs
        int activeStatusId = getStatusTypeId("ACTIVE", "ALLOCATION");
        int allocatedStatusId = getStatusTypeId("ALLOCATED", "EQUIPMENT");
        
        // Determine allocation type and insert accordingly
        String sql;
        boolean isDepartmentAllocation = (department != null && !department.isBlank());
        
        if (isDepartmentAllocation) {
            // Department allocation
            sql = "INSERT INTO EquipmentDepartmentAllocations " +
                  "(EquipmentID, DepartmentID, AllocatedByUserID, Notes, StatusTypeID) " +
                  "VALUES (?, ?, ?, ?, ?)";
        } else {
            // User allocation
            sql = "INSERT INTO EquipmentUserAllocations " +
                  "(EquipmentID, AllocatedToUserID, AllocatedByUserID, Notes, StatusTypeID) " +
                  "VALUES (?, ?, ?, ?, ?)";
        }
        
        // Update equipment status
        String updateSql = "UPDATE Equipment SET StatusTypeID = ? WHERE EquipmentID = ?";
        
        Connection conn = DatabaseConnection.getConnection();
        boolean originalAutoCommit = conn.getAutoCommit();
        try {
            conn.setAutoCommit(false);
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                 PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                
                pstmt.setInt(1, equipmentIdInt);
                
                if (isDepartmentAllocation) {
                    int departmentId = getDepartmentIdByName(department);
                    pstmt.setInt(2, departmentId);
                    pstmt.setInt(3, allocatedByUserIdInt);
                    pstmt.setString(4, notes != null ? notes : "");
                    pstmt.setInt(5, activeStatusId);
                } else {
                    Integer allocatedToUserIdInt = Integer.parseInt(allocatedToUserId);
                    pstmt.setInt(2, allocatedToUserIdInt);
                    pstmt.setInt(3, allocatedByUserIdInt);
                    pstmt.setString(4, notes != null ? notes : "");
                    pstmt.setInt(5, activeStatusId);
                }
                
                pstmt.executeUpdate();
                
                // Get generated allocation ID
                int allocationId = -1;
                try (ResultSet keys = pstmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        allocationId = keys.getInt(1);
                    }
                }
                
                // Update equipment status
                updateStmt.setInt(1, allocatedStatusId);
                updateStmt.setInt(2, equipmentIdInt);
                updateStmt.executeUpdate();
                
                conn.commit();
                
                // Return allocation object
                return getAllocationById(String.valueOf(allocationId));
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                try {
                    conn.setAutoCommit(originalAutoCommit);
                } catch (SQLException e) {
                    System.err.println("Error restoring auto-commit: " + e.getMessage());
                }
            }
        } catch (SQLException e) {
            throw e;
        }
    }

    /**
     * Return equipment (mark as returned)
     * @param allocationId The allocation ID
     * @param returnedByUserId The user ID returning the equipment
     * @return true if successful
     * @throws SQLException if database error occurs
     */
    public boolean returnEquipment(String allocationId, String returnedByUserId) throws SQLException {
        if (allocationId == null || allocationId.isBlank()) {
            throw new IllegalArgumentException("Allocation ID is required");
        }
        
        int allocationIdInt;
        try {
            allocationIdInt = Integer.parseInt(allocationId);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid allocation ID format");
        }
        
        // Get allocation to find equipment ID
        EquipmentAllocation allocation = getAllocationById(allocationId);
        if (allocation == null) {
            throw new IllegalArgumentException("Allocation not found");
        }
        
        if (allocation.getStatus() == AllocationStatus.RETURNED) {
            throw new IllegalArgumentException("Equipment has already been returned");
        }
        
        // Get status type IDs
        int returnedStatusId = getStatusTypeId("RETURNED", "ALLOCATION");
        int availableStatusId = getStatusTypeId("AVAILABLE", "EQUIPMENT");
        
        // Determine which table to update (try user allocations first, then department)
        String checkSql = "SELECT 'USER' as AllocationType FROM EquipmentUserAllocations WHERE AllocationID = ? " +
                         "UNION ALL " +
                         "SELECT 'DEPARTMENT' as AllocationType FROM EquipmentDepartmentAllocations WHERE AllocationID = ?";
        
        String allocationType = null;
        try (Connection checkConn = DatabaseConnection.getConnection();
             PreparedStatement checkStmt = checkConn.prepareStatement(checkSql)) {
            checkStmt.setInt(1, allocationIdInt);
            checkStmt.setInt(2, allocationIdInt);
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next()) {
                    allocationType = rs.getString("AllocationType");
                }
            }
        }
        
        if (allocationType == null) {
            throw new IllegalArgumentException("Allocation not found");
        }
        
        String sql = allocationType.equals("USER") ?
                    "UPDATE EquipmentUserAllocations SET StatusTypeID = ?, ReturnDate = CURRENT_TIMESTAMP WHERE AllocationID = ?" :
                    "UPDATE EquipmentDepartmentAllocations SET StatusTypeID = ?, ReturnDate = CURRENT_TIMESTAMP WHERE AllocationID = ?";
        
        String updateEquipmentSql = "UPDATE Equipment SET StatusTypeID = ? WHERE EquipmentID = ?";
        
        Connection conn = DatabaseConnection.getConnection();
        boolean originalAutoCommit = conn.getAutoCommit();
        try {
            conn.setAutoCommit(false);
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql);
                 PreparedStatement updateStmt = conn.prepareStatement(updateEquipmentSql)) {
                
                // Get equipment ID from allocation
                int equipmentIdInt = Integer.parseInt(allocation.getEquipment().getId());
                
                pstmt.setInt(1, returnedStatusId);
                pstmt.setInt(2, allocationIdInt);
                pstmt.executeUpdate();
                
                updateStmt.setInt(1, availableStatusId);
                updateStmt.setInt(2, equipmentIdInt);
                updateStmt.executeUpdate();
                
                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                try {
                    conn.setAutoCommit(originalAutoCommit);
                } catch (SQLException e) {
                    System.err.println("Error restoring auto-commit: " + e.getMessage());
                }
            }
        } catch (SQLException e) {
            // Re-throw SQLException from inner try-catch
            throw e;
        }
    }

    /**
     * Get all software licenses
     * @return List of software licenses
     * @throws SQLException if database error occurs
     */
    public List<SoftwareLicense> getAllSoftwareLicenses() throws SQLException {
        List<SoftwareLicense> licenses = new ArrayList<>();
        
        String sql = "SELECT sl.LicenseID, sl.SoftwareName, sl.LicenseKey, sl.Vendor, sl.PurchaseDate, " +
                    "sl.ExpiryDate, sl.Cost, sl.Quantity, sl.UsedQuantity, st.StatusCode as Status, sl.Notes, " +
                    "sl.CreatedDate, sl.UpdatedDate " +
                    "FROM SoftwareLicenses sl " +
                    "INNER JOIN StatusTypes st ON sl.StatusTypeID = st.StatusTypeID AND st.EntityType = 'LICENSE' " +
                    "ORDER BY sl.ExpiryDate ASC, sl.SoftwareName";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                SoftwareLicense license = mapResultSetToLicense(rs);
                licenses.add(license);
            }
        }
        
        return licenses;
    }

    /**
     * Get total expenses for software licenses
     * @return Total cost of all licenses
     * @throws SQLException if database error occurs
     */
    public Double getTotalLicenseExpenses() throws SQLException {
        String sql = "SELECT SUM(Cost) as TotalCost FROM SoftwareLicenses WHERE Cost IS NOT NULL";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            if (rs.next()) {
                Double total = rs.getDouble("TotalCost");
                return rs.wasNull() ? 0.0 : total;
            }
        }
        
        return 0.0;
    }

    /**
     * Get licenses near expiry (within 30 days)
     * @return List of licenses near expiry
     * @throws SQLException if database error occurs
     */
    public List<SoftwareLicense> getLicensesNearExpiry() throws SQLException {
        List<SoftwareLicense> licenses = new ArrayList<>();
        
        String sql = "SELECT sl.LicenseID, sl.SoftwareName, sl.LicenseKey, sl.Vendor, sl.PurchaseDate, " +
                    "sl.ExpiryDate, sl.Cost, sl.Quantity, sl.UsedQuantity, st.StatusCode as Status, sl.Notes, " +
                    "sl.CreatedDate, sl.UpdatedDate " +
                    "FROM SoftwareLicenses sl " +
                    "INNER JOIN StatusTypes st ON sl.StatusTypeID = st.StatusTypeID AND st.EntityType = 'LICENSE' " +
                    "WHERE st.StatusCode = 'ACTIVE' " +
                    "AND sl.ExpiryDate IS NOT NULL " +
                    "AND sl.ExpiryDate >= CURRENT_DATE " +
                    "AND sl.ExpiryDate <= CURRENT_DATE + INTERVAL '30 days' " +
                    "ORDER BY sl.ExpiryDate ASC";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                SoftwareLicense license = mapResultSetToLicense(rs);
                licenses.add(license);
            }
        }
        
        return licenses;
    }

    /**
     * Add new equipment (Admin only)
     * @param equipmentTypeId The equipment type ID
     * @param serialNumber Optional serial number
     * @param location Optional location
     * @param notes Optional notes
     * @return Equipment object if successful
     * @throws SQLException if database error occurs
     */
    public Equipment addEquipment(String equipmentTypeId, String serialNumber, 
                                 String location, String notes) throws SQLException {
        if (equipmentTypeId == null || equipmentTypeId.isBlank()) {
            throw new IllegalArgumentException("Equipment type ID is required");
        }
        
        int equipmentTypeIdInt;
        try {
            equipmentTypeIdInt = Integer.parseInt(equipmentTypeId);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid equipment type ID format");
        }
        
        // Get available status type ID
        int availableStatusId = getStatusTypeId("AVAILABLE", "EQUIPMENT");
        
        String sql = "INSERT INTO Equipment (EquipmentTypeID, SerialNumber, StatusTypeID, Location, Notes) " +
                    "VALUES (?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setInt(1, equipmentTypeIdInt);
            pstmt.setString(2, serialNumber != null ? serialNumber : "");
            pstmt.setInt(3, availableStatusId);
            pstmt.setString(4, location != null ? location : "");
            pstmt.setString(5, notes != null ? notes : "");
            
            pstmt.executeUpdate();
            
            // Get generated equipment ID
            try (ResultSet keys = pstmt.getGeneratedKeys()) {
                if (keys.next()) {
                    int equipmentId = keys.getInt(1);
                    return getEquipmentById(String.valueOf(equipmentId));
                }
            }
        }
        
        return null;
    }

    /**
     * Add new software license (Admin only)
     * @param softwareName The software name
     * @param licenseKey Optional license key
     * @param vendor Optional vendor name
     * @param purchaseDate Optional purchase date
     * @param expiryDate Optional expiry date (null for perpetual)
     * @param cost Optional cost
     * @param quantity Number of licenses
     * @param notes Optional notes
     * @return SoftwareLicense object if successful
     * @throws SQLException if database error occurs
     */
    public SoftwareLicense addSoftwareLicense(String softwareName, String licenseKey, 
                                             String vendor, LocalDate purchaseDate, 
                                             LocalDate expiryDate, Double cost, 
                                             int quantity, String notes) throws SQLException {
        if (softwareName == null || softwareName.isBlank()) {
            throw new IllegalArgumentException("Software name is required");
        }
        
        if (quantity < 1) {
            throw new IllegalArgumentException("Quantity must be at least 1");
        }
        
        // Get active status type ID
        int activeStatusId = getStatusTypeId("ACTIVE", "LICENSE");
        
        String sql = "INSERT INTO SoftwareLicenses " +
                    "(SoftwareName, LicenseKey, Vendor, PurchaseDate, ExpiryDate, Cost, Quantity, UsedQuantity, StatusTypeID, Notes, CreatedDate, UpdatedDate) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, 0, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setString(1, softwareName);
            pstmt.setString(2, licenseKey != null ? licenseKey : "");
            pstmt.setString(3, vendor != null ? vendor : "");
            if (purchaseDate != null) {
                pstmt.setDate(4, Date.valueOf(purchaseDate));
            } else {
                pstmt.setNull(4, Types.DATE);
            }
            if (expiryDate != null) {
                pstmt.setDate(5, Date.valueOf(expiryDate));
            } else {
                pstmt.setNull(5, Types.DATE);
            }
            if (cost != null) {
                pstmt.setDouble(6, cost);
            } else {
                pstmt.setNull(6, Types.DECIMAL);
            }
            pstmt.setInt(7, quantity);
            pstmt.setInt(8, activeStatusId);
            pstmt.setString(9, notes != null ? notes : "");
            
            pstmt.executeUpdate();
            
            // Get generated license ID
            try (ResultSet keys = pstmt.getGeneratedKeys()) {
                if (keys.next()) {
                    int licenseId = keys.getInt(1);
                    return getSoftwareLicenseById(String.valueOf(licenseId));
                }
            }
        }
        
        return null;
    }

    /**
     * Get software license by ID
     * @param licenseId The license ID
     * @return SoftwareLicense object or null if not found
     * @throws SQLException if database error occurs
     */
    private SoftwareLicense getSoftwareLicenseById(String licenseId) throws SQLException {
        int licenseIdInt = Integer.parseInt(licenseId);
        
        String sql = "SELECT sl.LicenseID, sl.SoftwareName, sl.LicenseKey, sl.Vendor, sl.PurchaseDate, " +
                    "sl.ExpiryDate, sl.Cost, sl.Quantity, sl.UsedQuantity, st.StatusCode as Status, sl.Notes, " +
                    "sl.CreatedDate, sl.UpdatedDate " +
                    "FROM SoftwareLicenses sl " +
                    "INNER JOIN StatusTypes st ON sl.StatusTypeID = st.StatusTypeID AND st.EntityType = 'LICENSE' " +
                    "WHERE sl.LicenseID = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, licenseIdInt);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToLicense(rs);
                }
            }
        }
        
        return null;
    }

    /**
     * Get all equipment types
     * @return List of equipment type names
     * @throws SQLException if database error occurs
     */
    public List<String> getAllEquipmentTypes() throws SQLException {
        List<String> types = new ArrayList<>();
        
        String sql = "SELECT Name FROM EquipmentType ORDER BY Name";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                types.add(rs.getString("Name"));
            }
        }
        
        return types;
    }

    /**
     * Get equipment type ID by name, create if doesn't exist
     * @param equipmentTypeName The equipment type name
     * @return Equipment type ID
     * @throws SQLException if database error occurs
     */
    public int getEquipmentTypeIdByName(String equipmentTypeName) throws SQLException {
        String sql = "SELECT EquipmentTypeID FROM EquipmentType WHERE Name = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, equipmentTypeName);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("EquipmentTypeID");
                }
            }
        }
        
        // If not found, create it
        return createEquipmentType(equipmentTypeName);
    }

    /**
     * Create a new equipment type
     * @param equipmentTypeName The equipment type name
     * @return Equipment type ID
     * @throws SQLException if database error occurs
     */
    public int createEquipmentType(String equipmentTypeName) throws SQLException {
        String sql = "INSERT INTO EquipmentType (Name) VALUES (?)";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setString(1, equipmentTypeName);
            pstmt.executeUpdate();
            
            try (ResultSet keys = pstmt.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        
        return -1;
    }

    // Helper methods

    private Equipment mapResultSetToEquipment(ResultSet rs) throws SQLException {
        String id = String.valueOf(rs.getInt("EquipmentID"));
        String equipmentTypeId = String.valueOf(rs.getInt("EquipmentTypeID"));
        String equipmentTypeName = rs.getString("EquipmentTypeName");
        String serialNumber = rs.getString("SerialNumber");
        String statusStr = rs.getString("Status");
        String location = rs.getString("Location");
        String notes = rs.getString("Notes");
        Timestamp createdDate = rs.getTimestamp("CreatedDate");
        
        EquipmentStatus status = EquipmentStatus.AVAILABLE;
        if (statusStr != null) {
            try {
                status = EquipmentStatus.valueOf(statusStr);
            } catch (IllegalArgumentException e) {
                status = EquipmentStatus.AVAILABLE;
            }
        }
        
        LocalDateTime created = createdDate != null ? createdDate.toLocalDateTime() : LocalDateTime.now();
        
        return new Equipment(id, equipmentTypeId, equipmentTypeName, serialNumber, 
                           status, location, notes, created);
    }

    private Equipment getEquipmentById(String equipmentId) throws SQLException {
        int equipmentIdInt = Integer.parseInt(equipmentId);
        
        String sql = "SELECT e.EquipmentID, e.EquipmentTypeID, et.Name as EquipmentTypeName, " +
                    "e.SerialNumber, st.StatusCode as Status, e.Location, e.Notes, e.CreatedDate " +
                    "FROM Equipment e " +
                    "INNER JOIN EquipmentType et ON e.EquipmentTypeID = et.EquipmentTypeID " +
                    "INNER JOIN StatusTypes st ON e.StatusTypeID = st.StatusTypeID AND st.EntityType = 'EQUIPMENT' " +
                    "WHERE e.EquipmentID = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, equipmentIdInt);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToEquipment(rs);
                }
            }
        }
        
        return null;
    }

    private EquipmentAllocation getAllocationById(String allocationId) throws SQLException {
        int allocationIdInt = Integer.parseInt(allocationId);
        
        // Try to find allocation in user allocations first
        String sql = "SELECT ea.AllocationID, ea.EquipmentID, ea.AllocatedToUserID, " +
                    "NULL as Department, NULL as DepartmentID, ea.AllocatedByUserID, ea.AllocationDate, " +
                    "ea.ReturnDate, ea.Notes, ast.StatusCode as Status, " +
                    "e.EquipmentTypeID, et.Name as EquipmentTypeName, " +
                    "e.SerialNumber, est.StatusCode as EquipmentStatus, e.Location " +
                    "FROM EquipmentUserAllocations ea " +
                    "INNER JOIN Equipment e ON ea.EquipmentID = e.EquipmentID " +
                    "INNER JOIN EquipmentType et ON e.EquipmentTypeID = et.EquipmentTypeID " +
                    "INNER JOIN StatusTypes est ON e.StatusTypeID = est.StatusTypeID AND est.EntityType = 'EQUIPMENT' " +
                    "INNER JOIN StatusTypes ast ON ea.StatusTypeID = ast.StatusTypeID AND ast.EntityType = 'ALLOCATION' " +
                    "WHERE ea.AllocationID = ? " +
                    "UNION ALL " +
                    "SELECT ea.AllocationID, ea.EquipmentID, NULL as AllocatedToUserID, " +
                    "d.Name as Department, ea.DepartmentID, ea.AllocatedByUserID, ea.AllocationDate, " +
                    "ea.ReturnDate, ea.Notes, ast.StatusCode as Status, " +
                    "e.EquipmentTypeID, et.Name as EquipmentTypeName, " +
                    "e.SerialNumber, est.StatusCode as EquipmentStatus, e.Location " +
                    "FROM EquipmentDepartmentAllocations ea " +
                    "INNER JOIN Equipment e ON ea.EquipmentID = e.EquipmentID " +
                    "INNER JOIN EquipmentType et ON e.EquipmentTypeID = et.EquipmentTypeID " +
                    "INNER JOIN StatusTypes est ON e.StatusTypeID = est.StatusTypeID AND est.EntityType = 'EQUIPMENT' " +
                    "INNER JOIN StatusTypes ast ON ea.StatusTypeID = ast.StatusTypeID AND ast.EntityType = 'ALLOCATION' " +
                    "INNER JOIN Departments d ON ea.DepartmentID = d.DepartmentID " +
                    "WHERE ea.AllocationID = ?";
        
        Connection conn = DatabaseConnection.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, allocationIdInt);
            pstmt.setInt(2, allocationIdInt);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return createAllocationFromResultSet(rs, conn);
                }
            }
        }
        
        return null;
    }

    private EquipmentAllocation createAllocationFromResultSet(ResultSet rs, Connection conn) throws SQLException {
        String allocationId = String.valueOf(rs.getInt("AllocationID"));
        String equipmentId = String.valueOf(rs.getInt("EquipmentID"));
        
        // Get equipment
        Equipment equipment = getEquipmentById(equipmentId);
        if (equipment == null) {
            return null;
        }
        
        // Get allocated to user
        User allocatedToUser = null;
        Integer allocatedToUserId = rs.getObject("AllocatedToUserID", Integer.class);
        if (allocatedToUserId != null) {
            allocatedToUser = getUserById(conn, allocatedToUserId);
        }
        
        String department = rs.getString("Department");
        
        // Get allocated by user
        int allocatedByUserId = rs.getInt("AllocatedByUserID");
        User allocatedByUser = getUserById(conn, allocatedByUserId);
        if (allocatedByUser == null) {
            return null;
        }
        
        Timestamp allocationDate = rs.getTimestamp("AllocationDate");
        Timestamp returnDate = rs.getTimestamp("ReturnDate");
        String notes = rs.getString("Notes");
        String statusStr = rs.getString("Status");
        
        AllocationStatus status = AllocationStatus.ACTIVE;
        if ("RETURNED".equals(statusStr)) {
            status = AllocationStatus.RETURNED;
        }
        
        LocalDateTime allocDate = allocationDate != null ? allocationDate.toLocalDateTime() : LocalDateTime.now();
        LocalDateTime retDate = returnDate != null ? returnDate.toLocalDateTime() : null;
        
        return new EquipmentAllocation(allocationId, equipment, allocatedToUser, department, 
                                     allocatedByUser, allocDate, retDate, notes, status);
    }

    private User getUserById(Connection conn, int userId) throws SQLException {
        String sql = "SELECT u.UserID, u.Username, u.Email, ut.TypeCode as UserType " +
                     "FROM Users u " +
                     "INNER JOIN UserRoles ur ON u.UserID = ur.UserID AND ur.IsPrimary = true " +
                     "INNER JOIN UserTypes ut ON ur.UserTypeID = ut.UserTypeID " +
                     "WHERE u.UserID = ?";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String id = String.valueOf(rs.getInt("UserID"));
                    String username = rs.getString("Username");
                    String userType = rs.getString("UserType");
                    
                    return createUser(id, username, userType);
                }
            }
        }
        return null;
    }

    private User createUser(String id, String username, String userType) {
        if (userType == null || userType.isBlank()) {
            return new Student(id, username, null);
        }
        
        switch (userType.toUpperCase()) {
            case "STUDENT":
                return new Student(id, username, null);
            case "PROFESSOR":
                return new Professor(id, username, null);
            case "STAFF":
                return new Staff(id, username, null);
            case "ADMIN":
                return new Admin(id, username, null);
            default:
                return new Student(id, username, null);
        }
    }

    private SoftwareLicense mapResultSetToLicense(ResultSet rs) throws SQLException {
        String id = String.valueOf(rs.getInt("LicenseID"));
        String softwareName = rs.getString("SoftwareName");
        String licenseKey = rs.getString("LicenseKey");
        String vendor = rs.getString("Vendor");
        Date purchaseDate = rs.getDate("PurchaseDate");
        Date expiryDate = rs.getDate("ExpiryDate");
        Double cost = rs.getObject("Cost", Double.class);
        int quantity = rs.getInt("Quantity");
        int usedQuantity = rs.getInt("UsedQuantity");
        String statusStr = rs.getString("Status");
        String notes = rs.getString("Notes");
        Date createdDate = rs.getDate("CreatedDate");
        Date updatedDate = rs.getDate("UpdatedDate");
        
        LicenseStatus status = LicenseStatus.ACTIVE;
        if (statusStr != null) {
            try {
                status = LicenseStatus.valueOf(statusStr);
            } catch (IllegalArgumentException e) {
                status = LicenseStatus.ACTIVE;
            }
        }
        
        LocalDate purchase = purchaseDate != null ? purchaseDate.toLocalDate() : null;
        LocalDate expiry = expiryDate != null ? expiryDate.toLocalDate() : null;
        LocalDate created = createdDate != null ? createdDate.toLocalDate() : LocalDate.now();
        LocalDate updated = updatedDate != null ? updatedDate.toLocalDate() : LocalDate.now();
        
        return new SoftwareLicense(id, softwareName, licenseKey, vendor, purchase, expiry, 
                                  cost, quantity, usedQuantity, status, notes, created, updated);
    }

    /**
     * Get status type ID by status code and entity type
     * @param statusCode The status code (e.g., "ACTIVE", "AVAILABLE")
     * @param entityType The entity type (e.g., "EQUIPMENT", "LICENSE", "ALLOCATION")
     * @return Status type ID
     * @throws SQLException if status not found or database error occurs
     */
    private int getStatusTypeId(String statusCode, String entityType) throws SQLException {
        String sql = "SELECT StatusTypeID FROM StatusTypes WHERE StatusCode = ? AND EntityType = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, statusCode);
            pstmt.setString(2, entityType);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("StatusTypeID");
                }
            }
        }
        
        throw new SQLException("Status type not found: " + statusCode + " for entity: " + entityType);
    }

    /**
     * Get department ID by department name
     * @param departmentName The department name
     * @return Department ID
     * @throws SQLException if department not found or database error occurs
     */
    private int getDepartmentIdByName(String departmentName) throws SQLException {
        String sql = "SELECT DepartmentID FROM Departments WHERE Name = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, departmentName);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("DepartmentID");
                }
            }
        }
        
        throw new SQLException("Department not found: " + departmentName);
    }
}

