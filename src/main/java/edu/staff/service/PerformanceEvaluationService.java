package edu.staff.service;

import edu.staff.model.PerformanceEvaluation;
import edu.facilities.service.DatabaseConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service class for managing Performance Evaluations.
 * US 3.7, 3.8
 */
public class PerformanceEvaluationService {

    /**
     * Record a new performance evaluation (Admin only)
     * US 3.7 - Evaluation period and score required
     */
    public void recordEvaluation(PerformanceEvaluation evaluation) throws SQLException {
        String sql = "INSERT INTO PerformanceEvaluations (StaffUserID, EvaluationPeriod, Score, " +
                     "EvaluatedByUserID, EvaluationDate, Notes) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, evaluation.getStaffUserID());
            pstmt.setString(2, evaluation.getEvaluationPeriod());
            pstmt.setDouble(3, evaluation.getScore());
            
            if (evaluation.getEvaluatedByUserID() != null) {
                pstmt.setInt(4, evaluation.getEvaluatedByUserID());
            } else {
                pstmt.setNull(4, Types.INTEGER);
            }
            
            if (evaluation.getEvaluationDate() != null) {
                pstmt.setTimestamp(5, Timestamp.valueOf(evaluation.getEvaluationDate()));
            } else {
                pstmt.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
            }
            
            if (evaluation.getNotes() != null && !evaluation.getNotes().isEmpty()) {
                pstmt.setString(6, evaluation.getNotes());
            } else {
                pstmt.setNull(6, Types.VARCHAR);
            }

            pstmt.executeUpdate();
        }
    }

    /**
     * Get all evaluations for a specific staff member
     * US 3.8 - Staff sees only their own evaluations, sorted by date
     */
    public List<PerformanceEvaluation> getEvaluationsByStaffUserID(int staffUserID) throws SQLException {
        List<PerformanceEvaluation> evaluations = new ArrayList<>();
        String sql = "SELECT pe.*, u.Username as StaffUsername " +
                     "FROM PerformanceEvaluations pe " +
                     "LEFT JOIN Users u ON pe.StaffUserID = u.UserID " +
                     "WHERE pe.StaffUserID = ? " +
                     "ORDER BY pe.EvaluationDate DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, staffUserID);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    PerformanceEvaluation eval = mapResultSetToEvaluation(rs);
                    eval.setStaffName(rs.getString("StaffUsername"));
                    evaluations.add(eval);
                }
            }
        }
        return evaluations;
    }

    /**
     * Get all evaluations (Admin view)
     */
    public List<PerformanceEvaluation> getAllEvaluations() throws SQLException {
        List<PerformanceEvaluation> evaluations = new ArrayList<>();
        String sql = "SELECT pe.*, " +
                     "u1.Username as StaffUsername, " +
                     "u2.Username as EvaluatedByUsername " +
                     "FROM PerformanceEvaluations pe " +
                     "LEFT JOIN Users u1 ON pe.StaffUserID = u1.UserID " +
                     "LEFT JOIN Users u2 ON pe.EvaluatedByUserID = u2.UserID " +
                     "ORDER BY pe.EvaluationDate DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                PerformanceEvaluation eval = mapResultSetToEvaluation(rs);
                eval.setStaffName(rs.getString("StaffUsername"));
                eval.setEvaluatedByName(rs.getString("EvaluatedByUsername"));
                evaluations.add(eval);
            }
        }
        return evaluations;
    }

    /**
     * Get all staff user IDs for dropdown selection
     */
    public List<Integer> getAllStaffUserIDs() throws SQLException {
        List<Integer> staffIDs = new ArrayList<>();
        String sql = "SELECT DISTINCT UserID FROM Users WHERE UserType = 'STAFF' ORDER BY UserID";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                staffIDs.add(rs.getInt("UserID"));
            }
        }
        return staffIDs;
    }

    /**
     * Get staff name by user ID
     */
    public String getStaffNameByUserID(int userID) throws SQLException {
        String sql = "SELECT Username FROM Users WHERE UserID = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userID);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("Username");
                }
            }
        }
        return "Unknown";
    }

    private PerformanceEvaluation mapResultSetToEvaluation(ResultSet rs) throws SQLException {
        PerformanceEvaluation eval = new PerformanceEvaluation();
        eval.setEvaluationID(rs.getInt("EvaluationID"));
        eval.setStaffUserID(rs.getInt("StaffUserID"));
        eval.setEvaluationPeriod(rs.getString("EvaluationPeriod"));
        eval.setScore(rs.getDouble("Score"));
        
        Integer evaluatedBy = rs.getObject("EvaluatedByUserID") != null ? 
                              rs.getInt("EvaluatedByUserID") : null;
        eval.setEvaluatedByUserID(evaluatedBy);
        
        Timestamp evalDate = rs.getTimestamp("EvaluationDate");
        if (evalDate != null) {
            eval.setEvaluationDate(evalDate.toLocalDateTime());
        }
        
        eval.setNotes(rs.getString("Notes"));
        
        Timestamp created = rs.getTimestamp("CreatedDate");
        if (created != null) {
            eval.setCreatedDate(created.toLocalDateTime());
        }
        
        return eval;
    }
}

