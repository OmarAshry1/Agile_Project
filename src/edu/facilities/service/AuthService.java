package edu.facilities.service;

import edu.facilities.model.User;

import java.util.HashMap;
import java.util.Map;

public class AuthService {

    private final Map<String, User> usersByUsername = new HashMap<>();
    private User currentUser;


    public void registerUser(User user) {
        if (user == null) {
            return;
        }
        usersByUsername.put(user.getUsername(), user);
    }


    public boolean login(String username, String password) {
        if (username == null || password == null) {
            return false;
        }

        User user = usersByUsername.get(username);
        if (user == null) {
            return false;
        }

        if (!password.equals(user.getPassword())) {
            return false;
        }

        currentUser = user;
        return true;
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


    public boolean hasRole(String... roles) {
        if (!isLoggedIn() || roles == null) {
            return false;
        }
        String type = currentUser.getUserType();
        for (String role : roles) {
            if (type.equals(role)) {
                return true;
            }
        }
        return false;
    }
}
