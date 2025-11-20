package edu.facilities.service;

import edu.facilities.model.User;
import edu.facilities.model.Admin;

import java.sql.*;

/**
 * Authentication Service using SQL Server database
 * Handles user authentication and session management
 */
public class AuthService {
    
    private User currentUser;
    
    // Singleton pattern for shared instance across controllers
    private static AuthService instance;
    
    public static AuthService getInstance() {
        if (instance == null) {
            instance = new AuthService();
        }
        return instance;
    }
    
    /**
     * Authenticate user with username and password
     * @param username The username
     * @param password The password
     * @return User object if authentication successful, null otherwise
     */
    public User login(String username, String password) throws SQLException {
        String sql = "SELECT UserID, Username, [Password], UserType FROM Users WHERE Username = ? AND [Password] = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    int userId = rs.getInt("UserID");
                    String userType = rs.getString("UserType");
                    String dbPassword = rs.getString("Password");
                    
                    // Verify password matches
                    if (password.equals(dbPassword)) {
                        User user;
                        if ("ADMIN".equalsIgnoreCase(userType)) {
                            user = new Admin(String.valueOf(userId), username, password);
                        } else {
                            // For other user types, you can create appropriate classes
                            // For now, using Admin as default
                            user = new Admin(String.valueOf(userId), username, password);
                        }
                        
                        this.currentUser = user;
                        return user;
                    }
                }
            }
        }
        return null;
    }
    
    /**
     * Register a new user
     * Note: UserID is auto-increment, so it's not included in INSERT
     * @param username The username
     * @param password The password
     * @param userType The user type (ADMIN, STUDENT, PROFESSOR, STAFF)
     * @return true if registration successful
     */
    public boolean register(String username, String password, String userType) throws SQLException {
        // Check if username already exists
        String checkSql = "SELECT COUNT(*) FROM Users WHERE Username = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
            
            checkStmt.setString(1, username);
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    return false; // Username already exists
                }
            }
        }
        
        // Insert new user (UserID is auto-increment, so not included)
        String insertSql = "INSERT INTO Users (Username, [Password], UserType) VALUES (?, ?, ?)";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
            
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.setString(3, userType);
            
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        }
    }
    
    /**
     * Set the current logged-in user
     * This should be called after successful login
     */
    public void setCurrentUser(User user) {
        this.currentUser = user;
    }
    
    public void logout() {
        currentUser = null;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public String getCurrentUserType() {
        return isLoggedIn() ? currentUser.getUserType() : null;
    }
    
    /**
     * Check if current user is an admin
     */
    public boolean isAdmin() {
        return "ADMIN".equals(getCurrentUserType());
    }
}
