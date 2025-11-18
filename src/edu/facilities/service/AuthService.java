package edu.facilities.service;

import edu.facilities.data.Database;
import edu.facilities.model.Admin;
import edu.facilities.model.Professor;
import edu.facilities.model.Staff;
import edu.facilities.model.Student;
import edu.facilities.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class AuthService {

    private User currentUser;

    public boolean login(String username, String password) {
        if (username == null || password == null) {
            return false;
        }

        String sql = "SELECT UserID, USERNAME, [Password], UserType " +
                "FROM Users WHERE USERNAME = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);

            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return false;
                }

                int userId       = rs.getInt("UserID");
                String dbUsername = rs.getString("USERNAME");
                String dbPassword = rs.getString("Password");
                String userType   = rs.getString("UserType");

                if (!dbPassword.equals(password)) {
                    return false; // wrong password
                }

                String id = String.valueOf(userId);
                currentUser = createUserFromType(id, dbUsername, dbPassword, userType);
                return true;
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error during login", e);
        }
    }


    public User register(String username, String password, String userType) {
        // Validate inputs
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        if (userType == null || userType.trim().isEmpty()) {
            throw new IllegalArgumentException("User type cannot be null or empty");
        }

        // Normalize userType to uppercase to match database constraint
        String normalizedUserType = userType.toUpperCase().trim();

        // Validate userType
        if (!isValidUserType(normalizedUserType)) {
            throw new IllegalArgumentException("Invalid user type: " + userType +
                    ". Must be one of: STUDENT, PROFESSOR, STAFF, ADMIN");
        }

        // Check if username already exists
        if (usernameExists(username)) {
            throw new RuntimeException("Username already exists: " + username);
        }

        // Insert new user into database
        String sql = "INSERT INTO Users (USERNAME, [Password], UserType) VALUES (?, ?, ?)";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, username.trim());
            stmt.setString(2, password);
            stmt.setString(3, normalizedUserType);

            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected == 0) {
                throw new RuntimeException("Failed to create user: no rows affected");
            }

            // Get the generated UserID
            int userId = -1;
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    userId = keys.getInt(1);
                } else {
                    throw new RuntimeException("Failed to retrieve generated user ID");
                }
            }

            // Create and return User object
            String id = String.valueOf(userId);
            User newUser = createUserFromType(id, username.trim(), password, normalizedUserType);

            return newUser;

        } catch (SQLException e) {
            // Handle unique constraint violation specifically
            if (e.getErrorCode() == 2627 || e.getErrorCode() == 2601) { // SQL Server unique constraint violation
                throw new RuntimeException("Username already exists: " + username, e);
            }
            throw new RuntimeException("Error during registration: " + e.getMessage(), e);
        }
    }


    private boolean usernameExists(String username) {
        String sql = "SELECT COUNT(*) FROM Users WHERE USERNAME = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error checking username existence", e);
        }

        return false;
    }


    private boolean isValidUserType(String userType) {
        return "STUDENT".equals(userType) ||
                "PROFESSOR".equals(userType) ||
                "STAFF".equals(userType) ||
                "ADMIN".equals(userType);
    }


    private User createUserFromType(String id,
                                    String username,
                                    String password,
                                    String userType) {
        switch (userType) {
            case "STUDENT":
                return new Student(id, username, password);
            case "PROFESSOR":
                return new Professor(id, username, password);
            case "STAFF":
                return new Staff(id, username, password);
            case "ADMIN":
                return new Admin(id, username, password);
            default:
                throw new IllegalArgumentException("Unknown user type: " + userType);
        }
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
}
