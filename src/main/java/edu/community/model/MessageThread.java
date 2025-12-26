package edu.community.model;

import java.time.LocalDateTime;

/**
 * Model class representing a message thread for parent-teacher conversations.
 * US 4.1, 4.2, 4.3 - Parent-Teacher Communication
 */
public class MessageThread {
    private int threadID;
    private int parentUserID;
    private int teacherUserID;
    private int studentUserID;
    private String parentName;
    private String teacherName;
    private String studentName;
    private String subject;
    private LocalDateTime createdDate;
    private LocalDateTime lastMessageDate;
    private int messageCount;

    public MessageThread() {
    }

    public MessageThread(int threadID, int parentUserID, int teacherUserID, int studentUserID,
                        String subject, LocalDateTime createdDate, LocalDateTime lastMessageDate) {
        this.threadID = threadID;
        this.parentUserID = parentUserID;
        this.teacherUserID = teacherUserID;
        this.studentUserID = studentUserID;
        this.subject = subject;
        this.createdDate = createdDate;
        this.lastMessageDate = lastMessageDate;
    }

    // Getters and Setters
    public int getThreadID() {
        return threadID;
    }

    public void setThreadID(int threadID) {
        this.threadID = threadID;
    }

    public int getParentUserID() {
        return parentUserID;
    }

    public void setParentUserID(int parentUserID) {
        this.parentUserID = parentUserID;
    }

    public int getTeacherUserID() {
        return teacherUserID;
    }

    public void setTeacherUserID(int teacherUserID) {
        this.teacherUserID = teacherUserID;
    }

    public int getStudentUserID() {
        return studentUserID;
    }

    public void setStudentUserID(int studentUserID) {
        this.studentUserID = studentUserID;
    }

    public String getParentName() {
        return parentName;
    }

    public void setParentName(String parentName) {
        this.parentName = parentName;
    }

    public String getTeacherName() {
        return teacherName;
    }

    public void setTeacherName(String teacherName) {
        this.teacherName = teacherName;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public LocalDateTime getLastMessageDate() {
        return lastMessageDate;
    }

    public void setLastMessageDate(LocalDateTime lastMessageDate) {
        this.lastMessageDate = lastMessageDate;
    }

    public int getMessageCount() {
        return messageCount;
    }

    public void setMessageCount(int messageCount) {
        this.messageCount = messageCount;
    }
}

