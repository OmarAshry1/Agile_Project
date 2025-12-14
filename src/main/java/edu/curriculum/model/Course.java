package edu.curriculum.model;

import edu.facilities.model.User;
import java.time.LocalDateTime;
import java.util.Objects;

public class Course {
    private final String id;
    private String code;
    private String name;
    private String description;
    private int credits;
    private String department;
    private String semester;
    private CourseType type;
    private User professor;
    private LocalDateTime createdDate;

    public Course(String id, String code, String name, String description, int credits,
                  String department, String semester, CourseType type, User professor,
                  LocalDateTime createdDate) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.description = description;
        this.credits = credits;
        this.department = department;
        this.semester = semester;
        this.type = type;
        this.professor = professor;
        this.createdDate = createdDate;
    }

    // --- Getters ---
    public String getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public int getCredits() { return credits; }
    public String getDepartment() { return department; }
    public String getSemester() { return semester; }
    public CourseType getType() { return type; }
    public User getProfessor() { return professor; }
    public LocalDateTime getCreatedDate() { return createdDate; }

    // --- Setters ---
    public void setCode(String code) { this.code = code; }
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setCredits(int credits) { this.credits = credits; }
    public void setDepartment(String department) { this.department = department; }
    public void setSemester(String semester) { this.semester = semester; }
    public void setType(CourseType type) { this.type = type; }
    public void setProfessor(User professor) { this.professor = professor; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }

    @Override
    public String toString() {
        return name + " [" + code + "] (" + type +
                ", credits=" + credits +
                ", dept=" + department +
                ", semester=" + semester + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Course)) return false;
        Course course = (Course) o;
        return Objects.equals(id, course.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

