package edu.community.model;

import java.time.LocalDateTime;

/**
 * Model class representing a forum post.
 * US 4.6 - Create Discussion Post
 */
public class ForumPost {
    private int postID;
    private int authorUserID;
    private String authorName;
    private Integer courseID;
    private String courseName;
    private String topic;
    private String title;
    private String content;
    private LocalDateTime createdDate;
    private LocalDateTime lastModifiedDate;
    private boolean isPinned;
    private boolean isLocked;
    private int viewCount;
    private int replyCount;

    public ForumPost() {
    }

    public ForumPost(int postID, int authorUserID, Integer courseID, String topic,
                    String title, String content, LocalDateTime createdDate) {
        this.postID = postID;
        this.authorUserID = authorUserID;
        this.courseID = courseID;
        this.topic = topic;
        this.title = title;
        this.content = content;
        this.createdDate = createdDate;
        this.lastModifiedDate = createdDate;
        this.isPinned = false;
        this.isLocked = false;
        this.viewCount = 0;
        this.replyCount = 0;
    }

    // Getters and Setters
    public int getPostID() {
        return postID;
    }

    public void setPostID(int postID) {
        this.postID = postID;
    }

    public int getAuthorUserID() {
        return authorUserID;
    }

    public void setAuthorUserID(int authorUserID) {
        this.authorUserID = authorUserID;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public Integer getCourseID() {
        return courseID;
    }

    public void setCourseID(Integer courseID) {
        this.courseID = courseID;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public LocalDateTime getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(LocalDateTime lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    public boolean isPinned() {
        return isPinned;
    }

    public void setPinned(boolean pinned) {
        isPinned = pinned;
    }

    public boolean isLocked() {
        return isLocked;
    }

    public void setLocked(boolean locked) {
        isLocked = locked;
    }

    public int getViewCount() {
        return viewCount;
    }

    public void setViewCount(int viewCount) {
        this.viewCount = viewCount;
    }

    public int getReplyCount() {
        return replyCount;
    }

    public void setReplyCount(int replyCount) {
        this.replyCount = replyCount;
    }
}

