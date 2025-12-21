package edu.curriculum.service;

import edu.curriculum.model.TranscriptEntry;
import edu.facilities.model.Enrollment;
import edu.facilities.model.EnrollmentStatus;
import edu.facilities.model.User;
import edu.facilities.service.EnrollmentService;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for viewing student transcripts
 * US: As a student, I want to view my transcript so that I can track academic progress
 */
public class TranscriptViewService {

    private EnrollmentService enrollmentService = new EnrollmentService();

    /**
     * Get transcript entries for a student (completed courses with grades)
     * @param student The student user
     * @return List of transcript entries
     * @throws SQLException if database error occurs
     */
    public List<TranscriptEntry> getTranscriptEntries(User student) throws SQLException {
        List<TranscriptEntry> entries = new ArrayList<>();

        // Get all enrollments (including completed ones)
        List<Enrollment> enrollments = enrollmentService.getStudentEnrollments(student, false);

        for (Enrollment enrollment : enrollments) {
            // Only include completed or failed courses with grades
            EnrollmentStatus status = enrollment.getStatus();
            String grade = enrollment.getGrade();

            if ((status == EnrollmentStatus.COMPLETED || status == EnrollmentStatus.FAILED) 
                && grade != null && !grade.isEmpty()) {
                
                TranscriptEntry entry = new TranscriptEntry(
                        enrollment.getCourse().getCode(),
                        enrollment.getCourse().getName(),
                        enrollment.getCourse().getCredits(),
                        grade,
                        enrollment.getCourse().getSemester()
                );
                entries.add(entry);
            }
        }

        // Sort by semester (most recent first)
        entries.sort((a, b) -> {
            // Simple string comparison for semester (e.g., "Fall 2024", "Spring 2024")
            return b.getSemester().compareTo(a.getSemester());
        });

        return entries;
    }

    /**
     * Calculate cumulative GPA for a student
     * @param entries List of transcript entries
     * @return Cumulative GPA (0.0 to 4.0)
     */
    public double calculateGPA(List<TranscriptEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return 0.0;
        }

        double totalGradePoints = 0.0;
        int totalCredits = 0;

        for (TranscriptEntry entry : entries) {
            double gradePoints = entry.getGradePoints();
            int credits = entry.getCredits();
            
            totalGradePoints += (gradePoints * credits);
            totalCredits += credits;
        }

        if (totalCredits == 0) {
            return 0.0;
        }

        return totalGradePoints / totalCredits;
    }

    /**
     * Calculate total completed credit hours
     * @param entries List of transcript entries
     * @return Total credit hours
     */
    public int calculateTotalCredits(List<TranscriptEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return 0;
        }

        return entries.stream()
                .mapToInt(TranscriptEntry::getCredits)
                .sum();
    }

    /**
     * Get student information for transcript header
     */
    public String getStudentName(User student) {
        return student.getUsername();
    }

    /**
     * Get student ID
     */
    public String getStudentId(User student) {
        return student.getId();
    }
}

