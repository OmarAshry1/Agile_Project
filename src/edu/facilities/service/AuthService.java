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
                    return false; // username not found
                }

                int userId       = rs.getInt("UserID");
                String dbUsername = rs.getString("USERNAME");
                String dbPassword = rs.getString("Password");   // [Password] alias
                String userType   = rs.getString("UserType");   // 'STUDENT', 'ADMIN', ...

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
