package edu.curriculum.model;

/**
 * Model representing a single course entry in a student transcript
 */
public class TranscriptEntry {
    private String courseCode;
    private String courseName;
    private int credits;
    private String finalGrade;
    private String semester;

    public TranscriptEntry(String courseCode, String courseName, int credits, String finalGrade, String semester) {
        this.courseCode = courseCode;
        this.courseName = courseName;
        this.credits = credits;
        this.finalGrade = finalGrade;
        this.semester = semester;
    }

    // --- Getters ---
    public String getCourseCode() {
        return courseCode;
    }

    public String getCourseName() {
        return courseName;
    }

    public int getCredits() {
        return credits;
    }

    public String getFinalGrade() {
        return finalGrade;
    }

    public String getSemester() {
        return semester;
    }

    /**
     * Convert letter grade to GPA points (4.0 scale)
     * A+ = 4.0, A = 4.0, A- = 3.7
     * B+ = 3.3, B = 3.0, B- = 2.7
     * C+ = 2.3, C = 2.0, C- = 1.7
     * D+ = 1.3, D = 1.0, D- = 0.7
     * F = 0.0
     */
    public double getGradePoints() {
        if (finalGrade == null || finalGrade.isEmpty()) {
            return 0.0;
        }

        String grade = finalGrade.toUpperCase().trim();
        switch (grade) {
            case "A+":
            case "A":
                return 4.0;
            case "A-":
                return 3.7;
            case "B+":
                return 3.3;
            case "B":
                return 3.0;
            case "B-":
                return 2.7;
            case "C+":
                return 2.3;
            case "C":
                return 2.0;
            case "C-":
                return 1.7;
            case "D+":
                return 1.3;
            case "D":
                return 1.0;
            case "F":
                return 0.0;
            default:
                return 0.0; // Unknown grade
        }
    }
}

