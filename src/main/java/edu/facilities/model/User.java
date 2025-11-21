package edu.facilities.model;

public abstract class User {
    private final String id;
    private final String username;
    private String password;
    
    protected User(String id, String username, String password) {
        this.id = id;
        this.username = username;
        this.password = password;
    }

    public String getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public void setPassword(String newpassword) {
        this.password = newpassword;
    }
    
    public abstract String getUserType();

    public String getPassword() {
        return password;
    }
    
    public boolean checkPassword(String rawPassword) {
        return password != null && password.equals(rawPassword);
    }
}

