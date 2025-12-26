package edu.staff.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Model class representing Payroll Information.
 * US 3.14 - View Payroll Information
 */
public class PayrollInformation {
    private int payrollID;
    private int staffUserID;
    private LocalDate payPeriodStart;
    private LocalDate payPeriodEnd;
    private LocalDate payDate;
    private String payFrequency;
    private LocalDate effectiveDate;
    private BigDecimal baseSalary;
    private BigDecimal overtimePay;
    private BigDecimal bonuses;
    private BigDecimal grossPay;
    private BigDecimal taxDeduction;
    private BigDecimal insuranceDeduction;
    private BigDecimal otherDeductions;
    private BigDecimal totalDeductions;
    private BigDecimal netPay;
    private String paymentMethod;
    private String notes;
    private Integer createdByUserID;
    private java.sql.Timestamp createdDate;
    private java.sql.Timestamp updatedDate;

    public PayrollInformation() {
        this.overtimePay = BigDecimal.ZERO;
        this.bonuses = BigDecimal.ZERO;
        this.taxDeduction = BigDecimal.ZERO;
        this.insuranceDeduction = BigDecimal.ZERO;
        this.otherDeductions = BigDecimal.ZERO;
        this.totalDeductions = BigDecimal.ZERO;
        this.paymentMethod = "DIRECT_DEPOSIT";
        this.payFrequency = "MONTHLY";
    }

    public PayrollInformation(int payrollID, int staffUserID, LocalDate payPeriodStart,
                              LocalDate payPeriodEnd, LocalDate payDate, BigDecimal baseSalary,
                              BigDecimal overtimePay, BigDecimal bonuses, BigDecimal grossPay,
                              BigDecimal taxDeduction, BigDecimal insuranceDeduction,
                              BigDecimal otherDeductions, BigDecimal totalDeductions,
                              BigDecimal netPay, String paymentMethod, String notes,
                              java.sql.Timestamp createdDate, java.sql.Timestamp updatedDate) {
        this.payrollID = payrollID;
        this.staffUserID = staffUserID;
        this.payPeriodStart = payPeriodStart;
        this.payPeriodEnd = payPeriodEnd;
        this.payDate = payDate;
        this.baseSalary = baseSalary;
        this.overtimePay = overtimePay != null ? overtimePay : BigDecimal.ZERO;
        this.bonuses = bonuses != null ? bonuses : BigDecimal.ZERO;
        this.grossPay = grossPay;
        this.taxDeduction = taxDeduction != null ? taxDeduction : BigDecimal.ZERO;
        this.insuranceDeduction = insuranceDeduction != null ? insuranceDeduction : BigDecimal.ZERO;
        this.otherDeductions = otherDeductions != null ? otherDeductions : BigDecimal.ZERO;
        this.totalDeductions = totalDeductions != null ? totalDeductions : BigDecimal.ZERO;
        this.netPay = netPay;
        this.paymentMethod = paymentMethod != null ? paymentMethod : "DIRECT_DEPOSIT";
        this.notes = notes;
        this.createdDate = createdDate;
        this.updatedDate = updatedDate;
    }

    // Getters and Setters
    public int getPayrollID() {
        return payrollID;
    }

    public void setPayrollID(int payrollID) {
        this.payrollID = payrollID;
    }

    public int getStaffUserID() {
        return staffUserID;
    }

    public void setStaffUserID(int staffUserID) {
        this.staffUserID = staffUserID;
    }

    public LocalDate getPayPeriodStart() {
        return payPeriodStart;
    }

    public void setPayPeriodStart(LocalDate payPeriodStart) {
        this.payPeriodStart = payPeriodStart;
    }

    public LocalDate getPayPeriodEnd() {
        return payPeriodEnd;
    }

    public void setPayPeriodEnd(LocalDate payPeriodEnd) {
        this.payPeriodEnd = payPeriodEnd;
    }

    public LocalDate getPayDate() {
        return payDate;
    }

    public void setPayDate(LocalDate payDate) {
        this.payDate = payDate;
    }

    public BigDecimal getBaseSalary() {
        return baseSalary;
    }

    public void setBaseSalary(BigDecimal baseSalary) {
        this.baseSalary = baseSalary;
    }

    public BigDecimal getOvertimePay() {
        return overtimePay;
    }

    public void setOvertimePay(BigDecimal overtimePay) {
        this.overtimePay = overtimePay;
    }

    public BigDecimal getBonuses() {
        return bonuses;
    }

    public void setBonuses(BigDecimal bonuses) {
        this.bonuses = bonuses;
    }

    public BigDecimal getGrossPay() {
        return grossPay;
    }

    public void setGrossPay(BigDecimal grossPay) {
        this.grossPay = grossPay;
    }

    public BigDecimal getTaxDeduction() {
        return taxDeduction;
    }

    public void setTaxDeduction(BigDecimal taxDeduction) {
        this.taxDeduction = taxDeduction;
    }

    public BigDecimal getInsuranceDeduction() {
        return insuranceDeduction;
    }

    public void setInsuranceDeduction(BigDecimal insuranceDeduction) {
        this.insuranceDeduction = insuranceDeduction;
    }

    public BigDecimal getOtherDeductions() {
        return otherDeductions;
    }

    public void setOtherDeductions(BigDecimal otherDeductions) {
        this.otherDeductions = otherDeductions;
    }

    public BigDecimal getTotalDeductions() {
        return totalDeductions;
    }

    public void setTotalDeductions(BigDecimal totalDeductions) {
        this.totalDeductions = totalDeductions;
    }

    public BigDecimal getNetPay() {
        return netPay;
    }

    public void setNetPay(BigDecimal netPay) {
        this.netPay = netPay;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
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

    public String getPayFrequency() {
        return payFrequency;
    }

    public void setPayFrequency(String payFrequency) {
        this.payFrequency = payFrequency;
    }

    public LocalDate getEffectiveDate() {
        return effectiveDate;
    }

    public void setEffectiveDate(LocalDate effectiveDate) {
        this.effectiveDate = effectiveDate;
    }

    public Integer getCreatedByUserID() {
        return createdByUserID;
    }

    public void setCreatedByUserID(Integer createdByUserID) {
        this.createdByUserID = createdByUserID;
    }

    @Override
    public String toString() {
        return String.format("PayrollInformation[ID=%d, Period=%s to %s, NetPay=%.2f]",
                payrollID, payPeriodStart, payPeriodEnd, netPay);
    }
}

