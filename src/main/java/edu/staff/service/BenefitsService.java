package edu.staff.service;

import edu.staff.model.BenefitsInformation;
import edu.facilities.service.DatabaseConnection;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import edu.facilities.model.User;

/**
 * Service class for managing Benefits Information.
 * US 3.15 - Add/Update Benefits Information (HR Admin)
 * US 3.17 - View Benefits Information (Staff)
 */
public class BenefitsService {

    /**
     * Get benefits information for a specific staff member
     * @param staffUserID Staff user ID
     * @return List of benefits
     * @throws SQLException Database error
     */
    public List<BenefitsInformation> getBenefitsByStaff(int staffUserID) throws SQLException {
        List<BenefitsInformation> benefits = new ArrayList<>();
        String sql = "SELECT * FROM BenefitsInformation WHERE StaffUserID = ? ORDER BY StartDate DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, staffUserID);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    benefits.add(mapResultSetToBenefits(rs));
                }
            }
        }

        return benefits;
    }

    /**
     * Get active benefits for a specific staff member
     * @param staffUserID Staff user ID
     * @return List of active benefits
     * @throws SQLException Database error
     */
    public List<BenefitsInformation> getActiveBenefitsByStaff(int staffUserID) throws SQLException {
        List<BenefitsInformation> benefits = new ArrayList<>();
        String sql = "SELECT * FROM BenefitsInformation " +
                     "WHERE StaffUserID = ? AND Status = 'ACTIVE' " +
                     "ORDER BY BenefitType, StartDate DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, staffUserID);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    benefits.add(mapResultSetToBenefits(rs));
                }
            }
        }

        return benefits;
    }

    /**
     * Get benefits by type for a specific staff member
     * @param staffUserID Staff user ID
     * @param benefitType Benefit type filter
     * @return List of benefits
     * @throws SQLException Database error
     */
    public List<BenefitsInformation> getBenefitsByType(int staffUserID, String benefitType) throws SQLException {
        List<BenefitsInformation> benefits = new ArrayList<>();
        String sql = "SELECT * FROM BenefitsInformation " +
                     "WHERE StaffUserID = ? AND BenefitType = ? " +
                     "ORDER BY StartDate DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, staffUserID);
            stmt.setString(2, benefitType);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    benefits.add(mapResultSetToBenefits(rs));
                }
            }
        }

        return benefits;
    }

    /**
     * Get a specific benefit by ID
     * @param benefitID Benefit ID
     * @return Benefits information or null if not found
     * @throws SQLException Database error
     */
    public BenefitsInformation getBenefitById(int benefitID) throws SQLException {
        String sql = "SELECT * FROM BenefitsInformation WHERE BenefitID = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, benefitID);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToBenefits(rs);
                }
            }
        }

        return null;
    }

    /**
     * Get distinct benefit types for a staff member
     * @param staffUserID Staff user ID
     * @return List of distinct benefit types
     * @throws SQLException Database error
     */
    public List<String> getDistinctBenefitTypes(int staffUserID) throws SQLException {
        List<String> types = new ArrayList<>();
        String sql = "SELECT DISTINCT BenefitType FROM BenefitsInformation " +
                     "WHERE StaffUserID = ? ORDER BY BenefitType";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, staffUserID);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    types.add(rs.getString("BenefitType"));
                }
            }
        }

        return types;
    }

    /**
     * Add a new benefit (HR Admin only)
     * @param benefit Benefits information to add
     * @param updatedByUserID HR Admin user ID
     * @throws SQLException Database error
     */
    public void addBenefit(BenefitsInformation benefit, int updatedByUserID) throws SQLException {
        String sql = "INSERT INTO BenefitsInformation (StaffUserID, BenefitType, BenefitName, " +
                     "CoverageAmount, CoverageDetails, StartDate, EndDate, Status, Provider, " +
                     "PolicyNumber, Notes, UpdatedByUserID, CreatedDate, UpdatedDate) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, benefit.getStaffUserID());
            stmt.setString(2, benefit.getBenefitType());
            stmt.setString(3, benefit.getBenefitName());
            stmt.setBigDecimal(4, benefit.getCoverageAmount());
            stmt.setString(5, benefit.getCoverageDetails());
            stmt.setDate(6, Date.valueOf(benefit.getStartDate()));
            
            if (benefit.getEndDate() != null) {
                stmt.setDate(7, Date.valueOf(benefit.getEndDate()));
            } else {
                stmt.setNull(7, Types.DATE);
            }
            
            stmt.setString(8, benefit.getStatus() != null ? benefit.getStatus() : "ACTIVE");
            stmt.setString(9, benefit.getProvider());
            stmt.setString(10, benefit.getPolicyNumber());
            stmt.setString(11, benefit.getNotes());
            stmt.setInt(12, updatedByUserID);

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        benefit.setBenefitID(rs.getInt(1));
                    }
                }
            }
        }
    }

    /**
     * Update an existing benefit (HR Admin only)
     * @param benefit Benefits information to update
     * @param updatedByUserID HR Admin user ID
     * @throws SQLException Database error
     */
    public void updateBenefit(BenefitsInformation benefit, int updatedByUserID) throws SQLException {
        String sql = "UPDATE BenefitsInformation SET " +
                     "BenefitType = ?, BenefitName = ?, CoverageAmount = ?, CoverageDetails = ?, " +
                     "StartDate = ?, EndDate = ?, Status = ?, Provider = ?, PolicyNumber = ?, " +
                     "Notes = ?, UpdatedByUserID = ?, UpdatedDate = CURRENT_TIMESTAMP " +
                     "WHERE BenefitID = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, benefit.getBenefitType());
            stmt.setString(2, benefit.getBenefitName());
            stmt.setBigDecimal(3, benefit.getCoverageAmount());
            stmt.setString(4, benefit.getCoverageDetails());
            stmt.setDate(5, Date.valueOf(benefit.getStartDate()));
            
            if (benefit.getEndDate() != null) {
                stmt.setDate(6, Date.valueOf(benefit.getEndDate()));
            } else {
                stmt.setNull(6, Types.DATE);
            }
            
            stmt.setString(7, benefit.getStatus());
            stmt.setString(8, benefit.getProvider());
            stmt.setString(9, benefit.getPolicyNumber());
            stmt.setString(10, benefit.getNotes());
            stmt.setInt(11, updatedByUserID);
            stmt.setInt(12, benefit.getBenefitID());

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new IllegalArgumentException("Benefit record not found");
            }
        }
    }

    /**
     * Delete a benefit (HR Admin only)
     * @param benefitID Benefit ID to delete
     * @throws SQLException Database error
     */
    public void deleteBenefit(int benefitID) throws SQLException {
        String sql = "DELETE FROM BenefitsInformation WHERE BenefitID = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, benefitID);

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new IllegalArgumentException("Benefit record not found");
            }
        }
    }

    /**
     * Get all staff members (for HR Admin to select)
     * @return List of staff users
     * @throws SQLException Database error
     */
    public List<User> getAllStaff() throws SQLException {
        List<User> staffList = new ArrayList<>();
        String sql = "SELECT u.UserID, u.USERNAME, u.Email, u.UserType " +
                     "FROM Users u " +
                     "INNER JOIN Staff s ON u.UserID = s.UserID " +
                     "ORDER BY u.USERNAME";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                String userId = String.valueOf(rs.getInt("UserID"));
                String username = rs.getString("USERNAME");
                String email = rs.getString("Email");
                User user = new edu.facilities.model.Staff(userId, username, email);
                staffList.add(user);
            }
        }

        return staffList;
    }

    /**
     * Map ResultSet to BenefitsInformation object
     */
    private BenefitsInformation mapResultSetToBenefits(ResultSet rs) throws SQLException {
        BenefitsInformation benefit = new BenefitsInformation();
        benefit.setBenefitID(rs.getInt("BenefitID"));
        benefit.setStaffUserID(rs.getInt("StaffUserID"));
        benefit.setBenefitType(rs.getString("BenefitType"));
        benefit.setBenefitName(rs.getString("BenefitName"));
        
        BigDecimal coverageAmount = rs.getBigDecimal("CoverageAmount");
        if (!rs.wasNull()) {
            benefit.setCoverageAmount(coverageAmount);
        }
        
        benefit.setCoverageDetails(rs.getString("CoverageDetails"));
        
        Date startDate = rs.getDate("StartDate");
        if (startDate != null) {
            benefit.setStartDate(startDate.toLocalDate());
        }
        
        Date endDate = rs.getDate("EndDate");
        if (endDate != null) {
            benefit.setEndDate(endDate.toLocalDate());
        }
        
        benefit.setStatus(rs.getString("Status"));
        benefit.setProvider(rs.getString("Provider"));
        benefit.setPolicyNumber(rs.getString("PolicyNumber"));
        benefit.setNotes(rs.getString("Notes"));
        benefit.setCreatedDate(rs.getTimestamp("CreatedDate"));
        benefit.setUpdatedDate(rs.getTimestamp("UpdatedDate"));
        
        int updatedBy = rs.getInt("UpdatedByUserID");
        if (!rs.wasNull()) {
            benefit.setUpdatedByUserID(updatedBy);
        }

        return benefit;
    }
}

