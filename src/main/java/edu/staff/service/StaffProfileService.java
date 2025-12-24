package edu.staff.service;

import edu.staff.model.StaffProfile;
import edu.facilities.service.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class StaffProfileService {

    public List<StaffProfile> getAllStaff() throws SQLException {
        List<StaffProfile> profiles = new ArrayList<>();
        String sql = "SELECT * FROM StaffProfiles ORDER BY Name";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql);
                ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                profiles.add(mapResultSetToStaffProfile(rs));
            }
        }
        return profiles;
    }

    public List<StaffProfile> searchStaff(String query, String department) throws SQLException {
        List<StaffProfile> profiles = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM StaffProfiles WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (query != null && !query.isEmpty()) {
            sql.append(" AND (Name LIKE ? OR Role LIKE ? OR Email LIKE ?)");
            String searchPattern = "%" + query + "%";
            params.add(searchPattern);
            params.add(searchPattern);
            params.add(searchPattern);
        }

        if (department != null && !department.equals("All Departments") && !department.isEmpty()) {
            sql.append(" AND Department = ?");
            params.add(department);
        }

        sql.append(" ORDER BY Name");

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    profiles.add(mapResultSetToStaffProfile(rs));
                }
            }
        }
        return profiles;
    }

    public void addStaff(StaffProfile profile) throws SQLException {
        String sql = "INSERT INTO StaffProfiles (Name, Role, Department, Email, OfficeHours, OfficeLocation, Phone, HireDate, Bio, IsActive) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, profile.getName());
            pstmt.setString(2, profile.getRole());
            pstmt.setString(3, profile.getDepartment());
            pstmt.setString(4, profile.getEmail());
            pstmt.setString(5, profile.getOfficeHours());
            pstmt.setString(6, profile.getOfficeLocation());
            pstmt.setString(7, profile.getPhone());
            pstmt.setDate(8, Date.valueOf(profile.getHireDate()));
            pstmt.setString(9, profile.getBio());
            pstmt.setBoolean(10, profile.isActive());

            pstmt.executeUpdate();
        }
    }

    public void updateStaff(StaffProfile profile) throws SQLException {
        String sql = "UPDATE StaffProfiles SET Name=?, Role=?, Department=?, Email=?, OfficeHours=?, OfficeLocation=?, Phone=?, HireDate=?, Bio=?, IsActive=? WHERE StaffID=?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, profile.getName());
            pstmt.setString(2, profile.getRole());
            pstmt.setString(3, profile.getDepartment());
            pstmt.setString(4, profile.getEmail());
            pstmt.setString(5, profile.getOfficeHours());
            pstmt.setString(6, profile.getOfficeLocation());
            pstmt.setString(7, profile.getPhone());
            pstmt.setDate(8, Date.valueOf(profile.getHireDate()));
            pstmt.setString(9, profile.getBio());
            pstmt.setBoolean(10, profile.isActive());
            pstmt.setInt(11, profile.getStaffID());

            pstmt.executeUpdate();
        }
    }

    public void deleteStaff(int staffID) throws SQLException {
        String sql = "DELETE FROM StaffProfiles WHERE StaffID=?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, staffID);
            pstmt.executeUpdate();
        }
    }

    public List<String> getAllDepartments() throws SQLException {
        List<String> departments = new ArrayList<>();
        String sql = "SELECT DISTINCT Department FROM StaffProfiles ORDER BY Department";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql);
                ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                departments.add(rs.getString("Department"));
            }
        }
        return departments;
    }

    private StaffProfile mapResultSetToStaffProfile(ResultSet rs) throws SQLException {
        StaffProfile profile = new StaffProfile();
        profile.setStaffID(rs.getInt("StaffID"));
        profile.setUserID(rs.getObject("UserID") != null ? rs.getInt("UserID") : null);
        profile.setName(rs.getString("Name"));
        profile.setRole(rs.getString("Role"));
        profile.setDepartment(rs.getString("Department"));
        profile.setEmail(rs.getString("Email"));
        profile.setOfficeHours(rs.getString("OfficeHours"));
        profile.setOfficeLocation(rs.getString("OfficeLocation"));
        profile.setPhone(rs.getString("Phone"));
        if (rs.getDate("HireDate") != null) {
            profile.setHireDate(rs.getDate("HireDate").toLocalDate());
        }
        profile.setBio(rs.getString("Bio"));
        profile.setActive(rs.getBoolean("IsActive"));
        return profile;
    }
}
