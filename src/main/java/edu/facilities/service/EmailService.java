package edu.facilities.service;

import edu.facilities.model.Booking;
import edu.facilities.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Service for sending email notifications
 * Note: This is a placeholder implementation. In production, integrate with
 * an actual email service (SMTP, SendGrid, AWS SES, etc.)
 */
public class EmailService {
    
    private static EmailService instance;
    
    private EmailService() {
        // Private constructor for singleton
    }
    
    public static EmailService getInstance() {
        if (instance == null) {
            instance = new EmailService();
        }
        return instance;
    }
    
    /**
     * Send booking confirmation email
     * @param booking The booking to confirm
     * @return true if email was sent successfully (or would be sent in production)
     */
    public boolean sendBookingConfirmation(Booking booking) {
        try {
            String userEmail = getUserEmail(booking.getUser());
            
            if (userEmail == null || userEmail.isBlank()) {
                System.out.println("Warning: User " + booking.getUser().getUsername() + 
                                 " does not have an email address. Cannot send confirmation email.");
                return false;
            }
            
            String subject = "Room Booking Confirmation - " + booking.getRoom().getId();
            String body = buildBookingConfirmationEmail(booking);
            
            // TODO: In production, send actual email using SMTP or email service
            // For now, just log the email that would be sent
            System.out.println("========================================");
            System.out.println("EMAIL CONFIRMATION (Would be sent in production)");
            System.out.println("To: " + userEmail);
            System.out.println("Subject: " + subject);
            System.out.println("Body:");
            System.out.println(body);
            System.out.println("========================================");
            
            // In production, this would return the result of actual email sending
            return true;
            
        } catch (Exception e) {
            System.err.println("Error sending booking confirmation email: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Get user email from database
     */
    private String getUserEmail(User user) throws SQLException {
        if (user == null || user.getId() == null) {
            return null;
        }
        
        try {
            int userId = Integer.parseInt(user.getId());
            String sql = "SELECT Email FROM Users WHERE UserID = ?";
            
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                
                pstmt.setInt(1, userId);
                
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getString("Email");
                    }
                }
            }
        } catch (NumberFormatException e) {
            System.err.println("Invalid user ID format: " + user.getId());
        }
        
        return null;
    }
    
    /**
     * Build booking confirmation email body
     */
    private String buildBookingConfirmationEmail(Booking booking) {
        StringBuilder email = new StringBuilder();
        email.append("Dear ").append(booking.getUser().getUsername()).append(",\n\n");
        email.append("Your room booking has been confirmed.\n\n");
        email.append("Booking Details:\n");
        email.append("-----------------\n");
        email.append("Booking ID: ").append(booking.getId()).append("\n");
        email.append("Room: ").append(booking.getRoom().getId()).append(" - ").append(booking.getRoom().getName()).append("\n");
        email.append("Room Type: ").append(booking.getRoom().getType()).append("\n");
        email.append("Location: ").append(booking.getRoom().getLocation()).append("\n");
        email.append("Start Time: ").append(booking.getBookingDate()).append("\n");
        email.append("End Time: ").append(booking.getEndDate()).append("\n");
        
        if (booking.getPurpose() != null && !booking.getPurpose().isBlank()) {
            email.append("Purpose: ").append(booking.getPurpose()).append("\n");
        }
        
        email.append("\nThank you for using the University Facilities Management System.\n");
        email.append("\nIf you need to cancel this booking, please contact the facilities office.\n");
        
        return email.toString();
    }
}

