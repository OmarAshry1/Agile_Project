package edu.community.model;

import java.time.LocalDateTime;

public class Message {
    private int messageID;
    private int senderUserID;
    private int receiverUserID;
    private String senderName;
    private String receiverName;
    private String subject;
    private String messageBody;
    private LocalDateTime sentDate;
    private boolean isRead;
    private Integer parentMessageID;

    public Message(int senderUserID, int receiverUserID, String subject, String messageBody) {
        this.senderUserID = senderUserID;
        this.receiverUserID = receiverUserID;
        this.subject = subject;
        this.messageBody = messageBody;
    }

    public Message() {
    }

    // Getters and Setters
    public int getMessageID() {
        return messageID;
    }

    public void setMessageID(int messageID) {
        this.messageID = messageID;
    }

    public int getSenderUserID() {
        return senderUserID;
    }

    public void setSenderUserID(int senderUserID) {
        this.senderUserID = senderUserID;
    }

    public int getReceiverUserID() {
        return receiverUserID;
    }

    public void setReceiverUserID(int receiverUserID) {
        this.receiverUserID = receiverUserID;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getReceiverName() {
        return receiverName;
    }

    public void setReceiverName(String receiverName) {
        this.receiverName = receiverName;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getMessageBody() {
        return messageBody;
    }

    public void setMessageBody(String messageBody) {
        this.messageBody = messageBody;
    }

    public LocalDateTime getSentDate() {
        return sentDate;
    }

    public void setSentDate(LocalDateTime sentDate) {
        this.sentDate = sentDate;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public Integer getParentMessageID() {
        return parentMessageID;
    }

    public void setParentMessageID(Integer parentMessageID) {
        this.parentMessageID = parentMessageID;
    }
}
