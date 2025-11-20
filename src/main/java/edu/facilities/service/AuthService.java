package edu.facilities.service;

import edu.facilities.model.User;

/**
 * Authentication Service
 * Simplified version for GUI - full implementation with database is in src/edu/facilities/service/AuthService
 * This version maintains current user state for session management
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
