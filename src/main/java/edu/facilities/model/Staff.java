package edu.facilities.model;

public class Staff extends User {
    public Staff(String id , String username, String password) {
        super(id, username, password);
    }

    @Override
    public String getUserType() {
        return "STAFF";
    }
}
