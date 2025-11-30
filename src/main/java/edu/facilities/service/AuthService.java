package edu.facilities.service;

import edu.facilities.model.User;
import edu.facilities.model.Student;
import edu.facilities.model.Staff;
import edu.facilities.model.Professor;
import edu.facilities.model.Admin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Authentication service for user login and registration
 * Uses singleton pattern to maintain session state
 */
public class AuthService {

    private static AuthService instance;
    private User currentUser;
    private String currentUserType;

    private AuthService() {
        // Private constructor for singleton
        currentUser = null;
        currentUserType = null;
    }

    /**
     * Get singleton instance
     */
    public static AuthService getInstance() {
        if (instance == null) {
            instance = new AuthService();
        }
        return instance;
    }

    /**
     * Login user with username and password
     * @param username Username
     * @param password Plain text password
     * @return User object if successful, null otherwise
     * @throws SQLException if database error occurs
     */
    public User login(String username, String password) throws SQLException {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return null;
        }

        String hashedPassword = hashPassword(password);

        String sql = "SELECT UserID, USERNAME, Email, UserType FROM Users WHERE USERNAME = ? AND [Password] = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            stmt.setString(2, hashedPassword);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String userId = String.valueOf(rs.getInt("UserID"));
                    String userType = rs.getString("UserType");
                    
                    // Create appropriate User object based on userType
                    User user = createUser(userId, username, userType);
                    
                    // Store in session
                    this.currentUser = user;
                    this.currentUserType = userType;

                    System.out.println("User logged in: " + username + " (" + userType + ")");
                    return user;
                }
            }
        }

        // Login failed
        this.currentUser = null;
        this.currentUserType = null;
        return null;
    }

    /**
     * Register a new user
     * @param username Username (must be unique)
     * @param password Plain text password
     * @param userType User type (STUDENT, PROFESSOR, STAFF, ADMIN)
     * @return true if registration successful, false if username already exists
     * @throws SQLException if database error occurs
     */
    public boolean register(String username, String password, String userType) throws SQLException {
        return register(username, password, null, userType);
    }

    /**
     * Register a new user with email
     * @param username Username (must be unique)
     * @param password Plain text password
     * @param email Email address (optional)
     * @param userType User type (STUDENT, PROFESSOR, STAFF, ADMIN)
     * @return true if registration successful, false if username already exists
     * @throws SQLException if database error occurs
     */
    public boolean register(String username, String password, String email, String userType) throws SQLException {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return false;
        }

        if (userType == null || userType.isBlank()) {
            userType = "STUDENT"; // Default
        }

        // Check if username already exists
        String checkSql = "SELECT COUNT(*) as count FROM Users WHERE USERNAME = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {

            checkStmt.setString(1, username);
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next() && rs.getInt("count") > 0) {
                    // Username already exists
                    return false;
                }
            }
        }

        // Insert new user
        String hashedPassword = hashPassword(password);
        String insertSql = "INSERT INTO Users (USERNAME, [Password], Email, UserType) VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(insertSql)) {

            stmt.setString(1, username);
            stmt.setString(2, hashedPassword);
            stmt.setString(3, email != null && !email.isBlank() ? email : null);
            stmt.setString(4, userType);

            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("User registered: " + username + " (" + userType + ")");
                return true;
            }
        }

        return false;
    }

    /**
     * Get currently logged in user
     * @return User object or null if not logged in
     */
    public User getCurrentUser() {
        return currentUser;
    }

    /**
     * Get current user type
     * @return User type string (ADMIN, STAFF, PROFESSOR, STUDENT) or null if not logged in
     */
    public String getCurrentUserType() {
        return currentUserType;
    }

    /**
     * Check if user is logged in
     * @return true if user is logged in, false otherwise
     */
    public boolean isLoggedIn() {
        return currentUser != null;
    }

    /**
     * Logout current user
     */
    public void logout() {
        this.currentUser = null;
        this.currentUserType = null;
        System.out.println("User logged out");
    }
    
    /**
     * Create appropriate User instance based on userType
     * @param id User ID
     * @param username Username
     * @param userType User type (STUDENT, PROFESSOR, STAFF, ADMIN)
     * @return User instance of appropriate type
     */
    private User createUser(String id, String username, String userType) {
        if (userType == null || userType.isBlank()) {
            return new Student(id, username, null); // Default to Student
        }
        
        // Create appropriate concrete User instance based on userType
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
                // Default to Student if userType is unknown
                return new Student(id, username, null);
        }
    }
    
    /**
     * Hash password using SHA-256
     * Note: In production, use bcrypt or Argon2 for better security
     * @param password Plain text password
     * @return Hashed password
     */
    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = md.digest(password.getBytes());

            // Convert bytes to hexadecimal string
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            // Fallback to plain text if hashing fails (not recommended for production)
            System.err.println("Warning: Password hashing failed, using plain text");
            return password;
        }
    }
}