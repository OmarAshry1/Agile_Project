package edu.facilities.model;

public class Parent extends User {
    public Parent(String id, String username, String password) {
        super(id, username, password);
    }

    @Override
    public String getUserType() {
        return "PARENT";
    }
}

