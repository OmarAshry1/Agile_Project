package edu.facilities.model;

public class Admin extends User {
    public Admin(String id, String username , String password) {
        super(id, username, password);
    }

    @Override
    public String getUserType() {
    return "ADMIN";
    }
}
