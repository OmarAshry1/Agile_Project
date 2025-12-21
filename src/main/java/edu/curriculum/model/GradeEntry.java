package edu.curriculum.model;

/**
 * Model representing a grade entry for display in grade tables
 * Used to display assignment, quiz, and exam grades in a unified format
 */
public class GradeEntry {
    private String type; // "Assignment", "Quiz", or "Exam"
    private String title;
    private Integer pointsEarned;
    private Integer totalPoints;
    private Double percentage;
    private String feedback;

    /**
     * Constructor for GradeEntry
     * @param type The type of grade entry (Assignment, Quiz, or Exam)
     * @param title The title of the assignment/quiz/exam
     * @param pointsEarned Points earned by the student (can be null if not graded)
     * @param totalPoints Total points possible
     * @param feedback Feedback provided (can be null or empty)
     */
    public GradeEntry(String type, String title, Integer pointsEarned, Integer totalPoints, String feedback) {
        this.type = type;
        this.title = title;
        this.pointsEarned = pointsEarned;
        this.totalPoints = totalPoints;
        this.feedback = feedback;
        
        // Calculate percentage if we have both points earned and total points
        if (pointsEarned != null && totalPoints != null && totalPoints > 0) {
            this.percentage = (pointsEarned.doubleValue() / totalPoints) * 100.0;
        } else {
            this.percentage = null;
        }
    }

    // --- Getters ---
    public String getType() { 
        return type; 
    }
    
    public String getTitle() { 
        return title; 
    }
    
    public Integer getPointsEarned() { 
        return pointsEarned; 
    }
    
    public Integer getTotalPoints() { 
        return totalPoints; 
    }
    
    public Double getPercentage() { 
        return percentage; 
    }
    
    public String getFeedback() { 
        return feedback; 
    }

    // --- Setters (if needed for future modifications) ---
    public void setType(String type) {
        this.type = type;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public void setPointsEarned(Integer pointsEarned) {
        this.pointsEarned = pointsEarned;
        // Recalculate percentage when points earned changes
        if (pointsEarned != null && totalPoints != null && totalPoints > 0) {
            this.percentage = (pointsEarned.doubleValue() / totalPoints) * 100.0;
        } else {
            this.percentage = null;
        }
    }
    
    public void setTotalPoints(Integer totalPoints) {
        this.totalPoints = totalPoints;
        // Recalculate percentage when total points changes
        if (pointsEarned != null && totalPoints != null && totalPoints > 0) {
            this.percentage = (pointsEarned.doubleValue() / totalPoints) * 100.0;
        } else {
            this.percentage = null;
        }
    }
    
    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }

    @Override
    public String toString() {
        return String.format("GradeEntry[type=%s, title=%s, points=%d/%d, percentage=%.1f%%]",
                type, title, 
                pointsEarned != null ? pointsEarned : 0,
                totalPoints != null ? totalPoints : 0,
                percentage != null ? percentage : 0.0);
    }
}

