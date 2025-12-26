package edu.community.service;

import edu.community.model.ForumComment;
import edu.community.model.ForumPost;
import edu.facilities.service.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for forum posts and comments.
 * US 4.6 - Create Discussion Post
 * US 4.7 - Comment on Post
 */
public class ForumService {

    /**
     * Create a new forum post (US 4.6)
     */
    public int createPost(ForumPost post) throws SQLException {
        String sql = "INSERT INTO ForumPosts (AuthorUserID, CourseID, Topic, Title, Content, " +
                     "CreatedDate, LastModifiedDate, IsPinned, IsLocked, ViewCount, ReplyCount) " +
                     "VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, ?, ?, 0, 0) " +
                     "RETURNING PostID";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, post.getAuthorUserID());
            
            if (post.getCourseID() != null) {
                pstmt.setInt(2, post.getCourseID());
            } else {
                pstmt.setNull(2, Types.INTEGER);
            }
            
            pstmt.setString(3, post.getTopic());
            pstmt.setString(4, post.getTitle());
            pstmt.setString(5, post.getContent());
            pstmt.setBoolean(6, post.isPinned());
            pstmt.setBoolean(7, post.isLocked());
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("PostID");
                }
            }
        }
        
        throw new SQLException("Failed to create forum post");
    }

    /**
     * Get all forum posts
     */
    public List<ForumPost> getAllPosts(Integer courseID, String topic) throws SQLException {
        StringBuilder sql = new StringBuilder(
            "SELECT p.*, u.USERNAME as AuthorName, c.CourseName " +
            "FROM ForumPosts p " +
            "LEFT JOIN Users u ON p.AuthorUserID = u.UserID " +
            "LEFT JOIN Courses c ON p.CourseID = c.CourseID " +
            "WHERE 1=1"
        );
        
        List<Object> params = new ArrayList<>();
        
        if (courseID != null) {
            sql.append(" AND p.CourseID = ?");
            params.add(courseID);
        }
        
        if (topic != null && !topic.isEmpty()) {
            sql.append(" AND p.Topic = ?");
            params.add(topic);
        }
        
        sql.append(" ORDER BY p.IsPinned DESC, p.CreatedDate DESC");
        
        List<ForumPost> posts = new ArrayList<>();
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            
            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    posts.add(mapResultSetToPost(rs));
                }
            }
        }
        
        return posts;
    }

    /**
     * Get a single post by ID
     */
    public ForumPost getPostById(int postID) throws SQLException {
        String sql = "SELECT p.*, u.USERNAME as AuthorName, c.CourseName " +
                    "FROM ForumPosts p " +
                    "LEFT JOIN Users u ON p.AuthorUserID = u.UserID " +
                    "LEFT JOIN Courses c ON p.CourseID = c.CourseID " +
                    "WHERE p.PostID = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, postID);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    ForumPost post = mapResultSetToPost(rs);
                    // Increment view count
                    incrementViewCount(postID);
                    return post;
                }
            }
        }
        
        return null;
    }

    /**
     * Add a comment to a post (US 4.7)
     */
    public int addComment(ForumComment comment) throws SQLException {
        String sql = "INSERT INTO ForumComments (PostID, AuthorUserID, Content, CreatedDate, IsEdited) " +
                     "VALUES (?, ?, ?, CURRENT_TIMESTAMP, FALSE) RETURNING CommentID";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, comment.getPostID());
            pstmt.setInt(2, comment.getAuthorUserID());
            pstmt.setString(3, comment.getContent());
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    int commentID = rs.getInt("CommentID");
                    // Update reply count
                    updateReplyCount(comment.getPostID());
                    return commentID;
                }
            }
        }
        
        throw new SQLException("Failed to add comment");
    }

    /**
     * Get all comments for a post
     */
    public List<ForumComment> getPostComments(int postID) throws SQLException {
        String sql = "SELECT c.*, u.USERNAME as AuthorName " +
                    "FROM ForumComments c " +
                    "LEFT JOIN Users u ON c.AuthorUserID = u.UserID " +
                    "WHERE c.PostID = ? " +
                    "ORDER BY c.CreatedDate ASC";
        
        List<ForumComment> comments = new ArrayList<>();
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, postID);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    comments.add(mapResultSetToComment(rs));
                }
            }
        }
        
        return comments;
    }

    /**
     * Get distinct topics
     */
    public List<String> getDistinctTopics() throws SQLException {
        String sql = "SELECT DISTINCT Topic FROM ForumPosts WHERE Topic IS NOT NULL ORDER BY Topic";
        
        List<String> topics = new ArrayList<>();
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                topics.add(rs.getString("Topic"));
            }
        }
        
        return topics;
    }

    private void incrementViewCount(int postID) throws SQLException {
        String sql = "UPDATE ForumPosts SET ViewCount = ViewCount + 1 WHERE PostID = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, postID);
            pstmt.executeUpdate();
        }
    }

    private void updateReplyCount(int postID) throws SQLException {
        String sql = "UPDATE ForumPosts SET ReplyCount = " +
                    "(SELECT COUNT(*) FROM ForumComments WHERE PostID = ?) " +
                    "WHERE PostID = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, postID);
            pstmt.setInt(2, postID);
            pstmt.executeUpdate();
        }
    }

    private ForumPost mapResultSetToPost(ResultSet rs) throws SQLException {
        ForumPost post = new ForumPost();
        post.setPostID(rs.getInt("PostID"));
        post.setAuthorUserID(rs.getInt("AuthorUserID"));
        post.setAuthorName(rs.getString("AuthorName"));
        
        int courseID = rs.getInt("CourseID");
        if (!rs.wasNull()) {
            post.setCourseID(courseID);
        }
        
        post.setCourseName(rs.getString("CourseName"));
        post.setTopic(rs.getString("Topic"));
        post.setTitle(rs.getString("Title"));
        post.setContent(rs.getString("Content"));
        
        Timestamp createdDate = rs.getTimestamp("CreatedDate");
        if (createdDate != null) {
            post.setCreatedDate(createdDate.toLocalDateTime());
        }
        
        Timestamp lastModifiedDate = rs.getTimestamp("LastModifiedDate");
        if (lastModifiedDate != null) {
            post.setLastModifiedDate(lastModifiedDate.toLocalDateTime());
        }
        
        post.setPinned(rs.getBoolean("IsPinned"));
        post.setLocked(rs.getBoolean("IsLocked"));
        post.setViewCount(rs.getInt("ViewCount"));
        post.setReplyCount(rs.getInt("ReplyCount"));
        
        return post;
    }

    private ForumComment mapResultSetToComment(ResultSet rs) throws SQLException {
        ForumComment comment = new ForumComment();
        comment.setCommentID(rs.getInt("CommentID"));
        comment.setPostID(rs.getInt("PostID"));
        comment.setAuthorUserID(rs.getInt("AuthorUserID"));
        comment.setAuthorName(rs.getString("AuthorName"));
        comment.setContent(rs.getString("Content"));
        
        Timestamp createdDate = rs.getTimestamp("CreatedDate");
        if (createdDate != null) {
            comment.setCreatedDate(createdDate.toLocalDateTime());
        }
        
        comment.setEdited(rs.getBoolean("IsEdited"));
        
        Timestamp editedDate = rs.getTimestamp("EditedDate");
        if (editedDate != null) {
            comment.setEditedDate(editedDate.toLocalDateTime());
        }
        
        return comment;
    }
}

