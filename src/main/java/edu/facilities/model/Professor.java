package edu.facilities.model;

public class Professor extends User {
    public Professor(String id , String username , String password) {
        super(id,username,password);
    }

    @Override
    public String getUserType() {
        return "PROFESSOR";
    }
}
