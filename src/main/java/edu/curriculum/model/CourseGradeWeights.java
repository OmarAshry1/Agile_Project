package edu.curriculum.model;

/**
 * Model representing grade weight distribution for a course
 * Used to calculate final grades based on weighted percentages
 */
public class CourseGradeWeights {
    private String courseId;
    private double assignmentsWeight;  // Percentage weight for assignments (0-100)
    private double quizzesWeight;      // Percentage weight for quizzes (0-100)
    private double examsWeight;       // Percentage weight for exams (0-100)

    public CourseGradeWeights(String courseId, double assignmentsWeight, double quizzesWeight, double examsWeight) {
        this.courseId = courseId;
        this.assignmentsWeight = assignmentsWeight;
        this.quizzesWeight = quizzesWeight;
        this.examsWeight = examsWeight;
    }

    // --- Getters ---
    public String getCourseId() {
        return courseId;
    }

    public double getAssignmentsWeight() {
        return assignmentsWeight;
    }

    public double getQuizzesWeight() {
        return quizzesWeight;
    }

    public double getExamsWeight() {
        return examsWeight;
    }

    // --- Setters ---
    public void setCourseId(String courseId) {
        this.courseId = courseId;
    }

    public void setAssignmentsWeight(double assignmentsWeight) {
        this.assignmentsWeight = assignmentsWeight;
    }

    public void setQuizzesWeight(double quizzesWeight) {
        this.quizzesWeight = quizzesWeight;
    }

    public void setExamsWeight(double examsWeight) {
        this.examsWeight = examsWeight;
    }

    /**
     * Validate that weights sum to 100%
     * @return true if valid, false otherwise
     */
    public boolean isValid() {
        double total = assignmentsWeight + quizzesWeight + examsWeight;
        return Math.abs(total - 100.0) < 0.01; // Allow small floating point differences
    }

    /**
     * Get total weight percentage
     */
    public double getTotalWeight() {
        return assignmentsWeight + quizzesWeight + examsWeight;
    }

    @Override
    public String toString() {
        return String.format("CourseGradeWeights[courseId=%s, Assignments=%.1f%%, Quizzes=%.1f%%, Exams=%.1f%%]",
                courseId, assignmentsWeight, quizzesWeight, examsWeight);
    }
}

