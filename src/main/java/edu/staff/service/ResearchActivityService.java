package edu.staff.service;

import edu.staff.model.ResearchActivity;
import edu.facilities.service.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Service class for managing Research Activities.
 * US 3.9, 3.10
 */
public class ResearchActivityService {

    /**
     * Add a new research activity (Staff only)
     * US 3.9 - Required: Title, Type, Publication Date
     */
    public void addResearchActivity(ResearchActivity activity) throws SQLException {
        String sql = "INSERT INTO ResearchActivities (StaffUserID, Title, Type, PublicationDate, " +
                     "Description, JournalName, ConferenceName, Publisher, DOI, URL) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, activity.getStaffUserID());
            pstmt.setString(2, activity.getTitle());
            pstmt.setString(3, activity.getType());
            pstmt.setDate(4, Date.valueOf(activity.getPublicationDate()));
            
            if (activity.getDescription() != null && !activity.getDescription().isEmpty()) {
                pstmt.setString(5, activity.getDescription());
            } else {
                pstmt.setNull(5, Types.VARCHAR);
            }
            
            if (activity.getJournalName() != null && !activity.getJournalName().isEmpty()) {
                pstmt.setString(6, activity.getJournalName());
            } else {
                pstmt.setNull(6, Types.VARCHAR);
            }
            
            if (activity.getConferenceName() != null && !activity.getConferenceName().isEmpty()) {
                pstmt.setString(7, activity.getConferenceName());
            } else {
                pstmt.setNull(7, Types.VARCHAR);
            }
            
            if (activity.getPublisher() != null && !activity.getPublisher().isEmpty()) {
                pstmt.setString(8, activity.getPublisher());
            } else {
                pstmt.setNull(8, Types.VARCHAR);
            }
            
            if (activity.getDoi() != null && !activity.getDoi().isEmpty()) {
                pstmt.setString(9, activity.getDoi());
            } else {
                pstmt.setNull(9, Types.VARCHAR);
            }
            
            if (activity.getUrl() != null && !activity.getUrl().isEmpty()) {
                pstmt.setString(10, activity.getUrl());
            } else {
                pstmt.setNull(10, Types.VARCHAR);
            }

            pstmt.executeUpdate();
        }
    }

    /**
     * Get all research activities for a specific staff member
     */
    public List<ResearchActivity> getResearchActivitiesByStaffUserID(int staffUserID) throws SQLException {
        List<ResearchActivity> activities = new ArrayList<>();
        String sql = "SELECT ra.*, u.Username as StaffUsername " +
                     "FROM ResearchActivities ra " +
                     "LEFT JOIN Users u ON ra.StaffUserID = u.UserID " +
                     "WHERE ra.StaffUserID = ? " +
                     "ORDER BY ra.PublicationDate DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, staffUserID);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    ResearchActivity activity = mapResultSetToActivity(rs);
                    activity.setStaffName(rs.getString("StaffUsername"));
                    activities.add(activity);
                }
            }
        }
        return activities;
    }

    /**
     * Get all research activities (Admin view)
     * US 3.10 - View research per staff member, filter by department
     */
    public List<ResearchActivity> getAllResearchActivities(String departmentFilter) throws SQLException {
        List<ResearchActivity> activities = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
            "SELECT ra.*, u.Username as StaffUsername, sp.Department " +
            "FROM ResearchActivities ra " +
            "LEFT JOIN Users u ON ra.StaffUserID = u.UserID " +
            "LEFT JOIN StaffProfiles sp ON u.UserID = sp.UserID " +
            "WHERE 1=1"
        );
        
        List<Object> params = new ArrayList<>();
        
        if (departmentFilter != null && !departmentFilter.isEmpty() && !departmentFilter.equals("All Departments")) {
            sql.append(" AND sp.Department = ?");
            params.add(departmentFilter);
        }
        
        sql.append(" ORDER BY ra.PublicationDate DESC");

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    ResearchActivity activity = mapResultSetToActivity(rs);
                    activity.setStaffName(rs.getString("StaffUsername"));
                    activity.setDepartment(rs.getString("Department"));
                    activities.add(activity);
                }
            }
        }
        return activities;
    }

    /**
     * Get all departments for filtering
     */
    public List<String> getAllDepartments() throws SQLException {
        List<String> departments = new ArrayList<>();
        String sql = "SELECT DISTINCT Department FROM StaffProfiles WHERE Department IS NOT NULL ORDER BY Department";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                departments.add(rs.getString("Department"));
            }
        }
        return departments;
    }

    private ResearchActivity mapResultSetToActivity(ResultSet rs) throws SQLException {
        ResearchActivity activity = new ResearchActivity();
        activity.setResearchID(rs.getInt("ResearchID"));
        activity.setStaffUserID(rs.getInt("StaffUserID"));
        activity.setTitle(rs.getString("Title"));
        activity.setType(rs.getString("Type"));
        
        Date pubDate = rs.getDate("PublicationDate");
        if (pubDate != null) {
            activity.setPublicationDate(pubDate.toLocalDate());
        }
        
        activity.setDescription(rs.getString("Description"));
        activity.setJournalName(rs.getString("JournalName"));
        activity.setConferenceName(rs.getString("ConferenceName"));
        activity.setPublisher(rs.getString("Publisher"));
        activity.setDoi(rs.getString("DOI"));
        activity.setUrl(rs.getString("URL"));
        
        Timestamp created = rs.getTimestamp("CreatedDate");
        if (created != null) {
            activity.setCreatedDate(created.toLocalDateTime());
        }
        
        Timestamp updated = rs.getTimestamp("UpdatedDate");
        if (updated != null) {
            activity.setUpdatedDate(updated.toLocalDateTime());
        }
        
        return activity;
    }
}

