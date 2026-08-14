package edu.facilities.service;

import edu.facilities.model.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.*;

/** Authentication and session state for Sprint 1. Public registration can create students only. */
public final class AuthService {
    private static final AuthService INSTANCE = new AuthService();
    private User currentUser;
    private AuthService() { }
    public static AuthService getInstance() { return INSTANCE; }

    public User login(String username, String password) throws SQLException {
        if (username == null || username.isBlank() || password == null || password.isBlank()) return null;
        String sql = "select id, username, role from users where lower(username)=lower(?) and password_hash=? and active=true";
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement s = c.prepareStatement(sql)) {
            s.setString(1, username.trim()); s.setString(2, hash(password));
            try (ResultSet r = s.executeQuery()) {
                if (r.next()) { currentUser = create(r.getLong("id"), r.getString("username"), r.getString("role")); return currentUser; }
            }
        }
        currentUser = null; return null;
    }

    public boolean register(String username, String password, String email, String role) throws SQLException {
        if (username == null || username.isBlank() || password == null || password.length() < 8) return false;
        String safeRole = role == null ? "STUDENT" : role.toUpperCase();
        if (!safeRole.equals("STUDENT")) safeRole = "STUDENT";
        String sql = "insert into users(username,email,password_hash,role) values(?,?,?,?)";
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement s = c.prepareStatement(sql)) {
            s.setString(1, username.trim()); s.setString(2, email == null || email.isBlank() ? null : email.trim());
            s.setString(3, hash(password)); s.setString(4, safeRole); s.executeUpdate(); return true;
        } catch (SQLException e) { if ("23505".equals(e.getSQLState())) return false; throw e; }
    }

    private User create(long id, String username, String role) {
        return switch (role.toUpperCase()) {
            case "ADMIN" -> new Admin(String.valueOf(id), username, null);
            case "STAFF" -> new Staff(String.valueOf(id), username, null);
            case "PROFESSOR" -> new Professor(String.valueOf(id), username, null);
            default -> new Student(String.valueOf(id), username, null);
        };
    }
    public User getCurrentUser() { return currentUser; }
    public String getCurrentUserType() { return currentUser == null ? null : currentUser.getUserType(); }
    public boolean isLoggedIn() { return currentUser != null; }
    public boolean hasRole(String... roles) { if (currentUser == null) return false; for (String r : roles) if (r.equalsIgnoreCase(currentUser.getUserType())) return true; return false; }
    public void logout() { currentUser = null; }
    public static String hash(String raw) {
        try { byte[] bytes = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8)); StringBuilder b = new StringBuilder(); for (byte x : bytes) b.append(String.format("%02x", x)); return b.toString(); }
        catch (Exception e) { throw new IllegalStateException(e); }
    }
}
