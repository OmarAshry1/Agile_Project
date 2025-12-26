package edu.community.model;

import java.time.LocalDateTime;

/**
 * Model class representing a comment on a forum post.
 * US 4.7 - Comment on Post
 */
public class ForumComment {
    private int commentID;
    private int postID;
    private int authorUserID;
    private String authorName;
    private String content;
    private LocalDateTime createdDate;
    private boolean isEdited;
    private LocalDateTime editedDate;

    public ForumComment() {
    }

    public ForumComment(int commentID, int postID, int authorUserID, String content,
                      LocalDateTime createdDate) {
        this.commentID = commentID;
        this.postID = postID;
        this.authorUserID = authorUserID;
        this.content = content;
        this.createdDate = createdDate;
        this.isEdited = false;
    }

    // Getters and Setters
    public int getCommentID() {
        return commentID;
    }

    public void setCommentID(int commentID) {
        this.commentID = commentID;
    }

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

    public boolean isEdited() {
        return isEdited;
    }

    public void setEdited(boolean edited) {
        isEdited = edited;
    }

    public LocalDateTime getEditedDate() {
        return editedDate;
    }

    public void setEditedDate(LocalDateTime editedDate) {
        this.editedDate = editedDate;
    }
}

