package edu.staff.service;

import edu.staff.model.PayrollInformation;
import edu.facilities.service.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import edu.facilities.model.User;
import java.math.BigDecimal;

/**
 * Service class for managing Payroll Information.
 * US 3.14 - Add/Update Payroll Information (HR Admin)
 * US 3.16 - View Payroll Information (Staff)
 */
public class PayrollService {

    /**
     * Get payroll information for a specific staff member
     * @param staffUserID Staff user ID
     * @return List of payroll records
     * @throws SQLException Database error
     */
    public List<PayrollInformation> getPayrollByStaff(int staffUserID) throws SQLException {
        List<PayrollInformation> payrolls = new ArrayList<>();
        String sql = "SELECT * FROM PayrollInformation WHERE StaffUserID = ? ORDER BY PayDate DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, staffUserID);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    payrolls.add(mapResultSetToPayroll(rs));
                }
            }
        }

        return payrolls;
    }

    /**
     * Get payroll information for a specific period
     * @param staffUserID Staff user ID
     * @param startDate Period start date
     * @param endDate Period end date
     * @return List of payroll records
     * @throws SQLException Database error
     */
    public List<PayrollInformation> getPayrollByPeriod(int staffUserID, Date startDate, Date endDate) throws SQLException {
        List<PayrollInformation> payrolls = new ArrayList<>();
        String sql = "SELECT * FROM PayrollInformation " +
                     "WHERE StaffUserID = ? AND PayPeriodStart >= ? AND PayPeriodEnd <= ? " +
                     "ORDER BY PayDate DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, staffUserID);
            stmt.setDate(2, startDate);
            stmt.setDate(3, endDate);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    payrolls.add(mapResultSetToPayroll(rs));
                }
            }
        }

        return payrolls;
    }

    /**
     * Get a specific payroll record by ID
     * @param payrollID Payroll ID
     * @return Payroll information or null if not found
     * @throws SQLException Database error
     */
    public PayrollInformation getPayrollById(int payrollID) throws SQLException {
        String sql = "SELECT * FROM PayrollInformation WHERE PayrollID = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, payrollID);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToPayroll(rs);
                }
            }
        }

        return null;
    }

    /**
     * Add a new payroll record (HR Admin only)
     * @param payroll Payroll information to add
     * @param createdByUserID HR Admin user ID
     * @throws SQLException Database error
     */
    public void addPayroll(PayrollInformation payroll, int createdByUserID) throws SQLException {
        // Calculate gross pay and net pay
        BigDecimal grossPay = calculateGrossPay(payroll);
        BigDecimal totalDeductions = calculateTotalDeductions(payroll);
        BigDecimal netPay = grossPay.subtract(totalDeductions);
        
        payroll.setGrossPay(grossPay);
        payroll.setTotalDeductions(totalDeductions);
        payroll.setNetPay(netPay);
        
        String sql = "INSERT INTO PayrollInformation (StaffUserID, PayPeriodStart, PayPeriodEnd, PayDate, " +
                     "PayFrequency, EffectiveDate, BaseSalary, OvertimePay, Bonuses, GrossPay, " +
                     "TaxDeduction, InsuranceDeduction, OtherDeductions, TotalDeductions, NetPay, " +
                     "PaymentMethod, Notes, CreatedByUserID, CreatedDate, UpdatedDate) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, payroll.getStaffUserID());
            stmt.setDate(2, Date.valueOf(payroll.getPayPeriodStart()));
            stmt.setDate(3, Date.valueOf(payroll.getPayPeriodEnd()));
            stmt.setDate(4, Date.valueOf(payroll.getPayDate()));
            stmt.setString(5, payroll.getPayFrequency() != null ? payroll.getPayFrequency() : "MONTHLY");
            stmt.setDate(6, Date.valueOf(payroll.getEffectiveDate()));
            stmt.setBigDecimal(7, payroll.getBaseSalary());
            stmt.setBigDecimal(8, payroll.getOvertimePay());
            stmt.setBigDecimal(9, payroll.getBonuses());
            stmt.setBigDecimal(10, grossPay);
            stmt.setBigDecimal(11, payroll.getTaxDeduction());
            stmt.setBigDecimal(12, payroll.getInsuranceDeduction());
            stmt.setBigDecimal(13, payroll.getOtherDeductions());
            stmt.setBigDecimal(14, totalDeductions);
            stmt.setBigDecimal(15, netPay);
            stmt.setString(16, payroll.getPaymentMethod());
            stmt.setString(17, payroll.getNotes());
            stmt.setInt(18, createdByUserID);

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        payroll.setPayrollID(rs.getInt(1));
                    }
                }
            }
        }
    }

    /**
     * Update an existing payroll record (HR Admin only)
     * @param payroll Payroll information to update
     * @param updatedByUserID HR Admin user ID
     * @throws SQLException Database error
     */
    public void updatePayroll(PayrollInformation payroll, int updatedByUserID) throws SQLException {
        // Calculate gross pay and net pay
        BigDecimal grossPay = calculateGrossPay(payroll);
        BigDecimal totalDeductions = calculateTotalDeductions(payroll);
        BigDecimal netPay = grossPay.subtract(totalDeductions);
        
        payroll.setGrossPay(grossPay);
        payroll.setTotalDeductions(totalDeductions);
        payroll.setNetPay(netPay);
        
        String sql = "UPDATE PayrollInformation SET " +
                     "PayPeriodStart = ?, PayPeriodEnd = ?, PayDate = ?, PayFrequency = ?, " +
                     "EffectiveDate = ?, BaseSalary = ?, OvertimePay = ?, Bonuses = ?, " +
                     "GrossPay = ?, TaxDeduction = ?, InsuranceDeduction = ?, OtherDeductions = ?, " +
                     "TotalDeductions = ?, NetPay = ?, PaymentMethod = ?, Notes = ?, " +
                     "UpdatedDate = CURRENT_TIMESTAMP " +
                     "WHERE PayrollID = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, Date.valueOf(payroll.getPayPeriodStart()));
            stmt.setDate(2, Date.valueOf(payroll.getPayPeriodEnd()));
            stmt.setDate(3, Date.valueOf(payroll.getPayDate()));
            stmt.setString(4, payroll.getPayFrequency() != null ? payroll.getPayFrequency() : "MONTHLY");
            stmt.setDate(5, Date.valueOf(payroll.getEffectiveDate()));
            stmt.setBigDecimal(6, payroll.getBaseSalary());
            stmt.setBigDecimal(7, payroll.getOvertimePay());
            stmt.setBigDecimal(8, payroll.getBonuses());
            stmt.setBigDecimal(9, grossPay);
            stmt.setBigDecimal(10, payroll.getTaxDeduction());
            stmt.setBigDecimal(11, payroll.getInsuranceDeduction());
            stmt.setBigDecimal(12, payroll.getOtherDeductions());
            stmt.setBigDecimal(13, totalDeductions);
            stmt.setBigDecimal(14, netPay);
            stmt.setString(15, payroll.getPaymentMethod());
            stmt.setString(16, payroll.getNotes());
            stmt.setInt(17, payroll.getPayrollID());

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new IllegalArgumentException("Payroll record not found");
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
        String sql = "SELECT u.UserID, u.USERNAME, u.Email, ut.TypeCode as UserType " +
                     "FROM Users u " +
                     "INNER JOIN UserRoles ur ON u.UserID = ur.UserID AND ur.IsPrimary = true " +
                     "INNER JOIN UserTypes ut ON ur.UserTypeID = ut.UserTypeID " +
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
     * Calculate gross pay from base salary, overtime, and bonuses
     */
    private BigDecimal calculateGrossPay(PayrollInformation payroll) {
        BigDecimal base = payroll.getBaseSalary() != null ? payroll.getBaseSalary() : BigDecimal.ZERO;
        BigDecimal overtime = payroll.getOvertimePay() != null ? payroll.getOvertimePay() : BigDecimal.ZERO;
        BigDecimal bonuses = payroll.getBonuses() != null ? payroll.getBonuses() : BigDecimal.ZERO;
        return base.add(overtime).add(bonuses);
    }

    /**
     * Calculate total deductions
     */
    private BigDecimal calculateTotalDeductions(PayrollInformation payroll) {
        BigDecimal tax = payroll.getTaxDeduction() != null ? payroll.getTaxDeduction() : BigDecimal.ZERO;
        BigDecimal insurance = payroll.getInsuranceDeduction() != null ? payroll.getInsuranceDeduction() : BigDecimal.ZERO;
        BigDecimal other = payroll.getOtherDeductions() != null ? payroll.getOtherDeductions() : BigDecimal.ZERO;
        return tax.add(insurance).add(other);
    }

    /**
     * Map ResultSet to PayrollInformation object
     */
    private PayrollInformation mapResultSetToPayroll(ResultSet rs) throws SQLException {
        PayrollInformation payroll = new PayrollInformation();
        payroll.setPayrollID(rs.getInt("PayrollID"));
        payroll.setStaffUserID(rs.getInt("StaffUserID"));
        
        Date payPeriodStart = rs.getDate("PayPeriodStart");
        if (payPeriodStart != null) {
            payroll.setPayPeriodStart(payPeriodStart.toLocalDate());
        }
        
        Date payPeriodEnd = rs.getDate("PayPeriodEnd");
        if (payPeriodEnd != null) {
            payroll.setPayPeriodEnd(payPeriodEnd.toLocalDate());
        }
        
        Date payDate = rs.getDate("PayDate");
        if (payDate != null) {
            payroll.setPayDate(payDate.toLocalDate());
        }
        
        payroll.setPayFrequency(rs.getString("PayFrequency"));
        
        Date effectiveDate = rs.getDate("EffectiveDate");
        if (effectiveDate != null) {
            payroll.setEffectiveDate(effectiveDate.toLocalDate());
        }
        
        payroll.setBaseSalary(rs.getBigDecimal("BaseSalary"));
        payroll.setOvertimePay(rs.getBigDecimal("OvertimePay"));
        payroll.setBonuses(rs.getBigDecimal("Bonuses"));
        payroll.setGrossPay(rs.getBigDecimal("GrossPay"));
        payroll.setTaxDeduction(rs.getBigDecimal("TaxDeduction"));
        payroll.setInsuranceDeduction(rs.getBigDecimal("InsuranceDeduction"));
        payroll.setOtherDeductions(rs.getBigDecimal("OtherDeductions"));
        payroll.setTotalDeductions(rs.getBigDecimal("TotalDeductions"));
        payroll.setNetPay(rs.getBigDecimal("NetPay"));
        payroll.setPaymentMethod(rs.getString("PaymentMethod"));
        payroll.setNotes(rs.getString("Notes"));
        
        int createdBy = rs.getInt("CreatedByUserID");
        if (!rs.wasNull()) {
            payroll.setCreatedByUserID(createdBy);
        }
        
        payroll.setCreatedDate(rs.getTimestamp("CreatedDate"));
        payroll.setUpdatedDate(rs.getTimestamp("UpdatedDate"));

        return payroll;
    }
}

