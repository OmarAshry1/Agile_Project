package edu.facilities.model;

/**
 * Enum for transcript request status
 * US 2.2, 2.3, 2.4 - Transcript Management
 */
public enum TranscriptStatus {
    PENDING,            // Request submitted, waiting for admin processing
    IN_PROGRESS,        // Admin is processing the request
    READY_FOR_PICKUP,   // Transcript generated and ready for pickup
    COMPLETED,          // Transcript picked up/completed
    CANCELLED           // Request cancelled
}


