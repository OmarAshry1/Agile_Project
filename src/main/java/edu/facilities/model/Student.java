package edu.facilities.model;

public class Student extends User {
    public Student(String id , String username , String password) {
        super(id , username , password);
    }

    @Override
    public String getUserType() {
        return "STUDENT";
    }
}
