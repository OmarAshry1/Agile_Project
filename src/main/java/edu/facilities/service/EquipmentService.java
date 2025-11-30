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
                    "e.SerialNumber, e.Status, e.Location, e.Notes, e.CreatedDate " +
                    "FROM Equipment e " +
                    "INNER JOIN EquipmentType et ON e.EquipmentTypeID = et.EquipmentTypeID " +
                    "WHERE e.Status = 'AVAILABLE' " +
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
                    "ea.Department, ea.AllocatedByUserID, ea.AllocationDate, " +
                    "ea.ReturnDate, ea.Notes, ea.Status, " +
                    "e.EquipmentTypeID, et.Name as EquipmentTypeName, " +
                    "e.SerialNumber, e.Status as EquipmentStatus, e.Location " +
                    "FROM EquipmentAllocation ea " +
                    "INNER JOIN Equipment e ON ea.EquipmentID = e.EquipmentID " +
                    "INNER JOIN EquipmentType et ON e.EquipmentTypeID = et.EquipmentTypeID " +
                    "WHERE ea.AllocatedToUserID = ? AND ea.Status = 'ACTIVE' " +
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
                    "ea.Department, ea.AllocatedByUserID, ea.AllocationDate, " +
                    "ea.ReturnDate, ea.Notes, ea.Status, " +
                    "e.EquipmentTypeID, et.Name as EquipmentTypeName, " +
                    "e.SerialNumber, e.Status as EquipmentStatus, e.Location " +
                    "FROM EquipmentAllocation ea " +
                    "INNER JOIN Equipment e ON ea.EquipmentID = e.EquipmentID " +
                    "INNER JOIN EquipmentType et ON e.EquipmentTypeID = et.EquipmentTypeID " +
                    "WHERE ea.Department = ? AND ea.Status = 'ACTIVE' " +
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
        String checkSql = "SELECT Status FROM Equipment WHERE EquipmentID = ?";
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
        
        // Insert allocation
        String sql = "INSERT INTO EquipmentAllocation " +
                    "(EquipmentID, AllocatedToUserID, Department, AllocatedByUserID, Notes, Status) " +
                    "VALUES (?, ?, ?, ?, ?, 'ACTIVE')";
        
        // Update equipment status
        String updateSql = "UPDATE Equipment SET Status = 'ALLOCATED' WHERE EquipmentID = ?";
        
        Connection conn = DatabaseConnection.getConnection();
        try {
            conn.setAutoCommit(false);
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                 PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                
                Integer allocatedToUserIdInt = null;
                if (allocatedToUserId != null && !allocatedToUserId.isBlank()) {
                    allocatedToUserIdInt = Integer.parseInt(allocatedToUserId);
                }
                
                pstmt.setInt(1, equipmentIdInt);
                if (allocatedToUserIdInt != null) {
                    pstmt.setInt(2, allocatedToUserIdInt);
                } else {
                    pstmt.setNull(2, Types.INTEGER);
                }
                pstmt.setString(3, department);
                pstmt.setInt(4, allocatedByUserIdInt);
                pstmt.setString(5, notes);
                
                pstmt.executeUpdate();
                
                // Get generated allocation ID
                int allocationId = -1;
                try (ResultSet keys = pstmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        allocationId = keys.getInt(1);
                    }
                }
                
                // Update equipment status
                updateStmt.setInt(1, equipmentIdInt);
                updateStmt.executeUpdate();
                
                conn.commit();
                
                // Return allocation object
                return getAllocationById(String.valueOf(allocationId));
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
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
        
        String sql = "UPDATE EquipmentAllocation SET Status = 'RETURNED', ReturnDate = GETDATE() " +
                    "WHERE AllocationID = ?";
        
        String updateEquipmentSql = "UPDATE Equipment SET Status = 'AVAILABLE' WHERE EquipmentID = ?";
        
        Connection conn = DatabaseConnection.getConnection();
        try {
            conn.setAutoCommit(false);
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql);
                 PreparedStatement updateStmt = conn.prepareStatement(updateEquipmentSql)) {
                
                // Get equipment ID from allocation
                int equipmentIdInt = Integer.parseInt(allocation.getEquipment().getId());
                
                pstmt.setInt(1, allocationIdInt);
                pstmt.executeUpdate();
                
                updateStmt.setInt(1, equipmentIdInt);
                updateStmt.executeUpdate();
                
                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * Get all software licenses
     * @return List of software licenses
     * @throws SQLException if database error occurs
     */
    public List<SoftwareLicense> getAllSoftwareLicenses() throws SQLException {
        List<SoftwareLicense> licenses = new ArrayList<>();
        
        String sql = "SELECT LicenseID, SoftwareName, LicenseKey, Vendor, PurchaseDate, " +
                    "ExpiryDate, Cost, Quantity, UsedQuantity, Status, Notes, " +
                    "CreatedDate, UpdatedDate " +
                    "FROM SoftwareLicenses " +
                    "ORDER BY ExpiryDate ASC, SoftwareName";
        
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
        
        String sql = "SELECT LicenseID, SoftwareName, LicenseKey, Vendor, PurchaseDate, " +
                    "ExpiryDate, Cost, Quantity, UsedQuantity, Status, Notes, " +
                    "CreatedDate, UpdatedDate " +
                    "FROM SoftwareLicenses " +
                    "WHERE Status = 'ACTIVE' " +
                    "AND ExpiryDate IS NOT NULL " +
                    "AND ExpiryDate >= GETDATE() " +
                    "AND ExpiryDate <= DATEADD(day, 30, GETDATE()) " +
                    "ORDER BY ExpiryDate ASC";
        
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
        
        String sql = "INSERT INTO Equipment (EquipmentTypeID, SerialNumber, Status, Location, Notes) " +
                    "VALUES (?, ?, 'AVAILABLE', ?, ?)";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setInt(1, equipmentTypeIdInt);
            pstmt.setString(2, serialNumber);
            pstmt.setString(3, location);
            pstmt.setString(4, notes);
            
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
                    "e.SerialNumber, e.Status, e.Location, e.Notes, e.CreatedDate " +
                    "FROM Equipment e " +
                    "INNER JOIN EquipmentType et ON e.EquipmentTypeID = et.EquipmentTypeID " +
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
        
        String sql = "SELECT ea.AllocationID, ea.EquipmentID, ea.AllocatedToUserID, " +
                    "ea.Department, ea.AllocatedByUserID, ea.AllocationDate, " +
                    "ea.ReturnDate, ea.Notes, ea.Status, " +
                    "e.EquipmentTypeID, et.Name as EquipmentTypeName, " +
                    "e.SerialNumber, e.Status as EquipmentStatus, e.Location " +
                    "FROM EquipmentAllocation ea " +
                    "INNER JOIN Equipment e ON ea.EquipmentID = e.EquipmentID " +
                    "INNER JOIN EquipmentType et ON e.EquipmentTypeID = et.EquipmentTypeID " +
                    "WHERE ea.AllocationID = ?";
        
        Connection conn = DatabaseConnection.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, allocationIdInt);
            
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
        String sql = "SELECT UserID, Username, Email, UserType FROM Users WHERE UserID = ?";
        
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
}

