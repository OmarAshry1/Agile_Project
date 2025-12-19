package edu.curriculum.model;

import edu.facilities.model.Course;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Model representing a Course Material
 * US 2.5 - Upload Course Materials
 * US 2.6 - View Course Materials
 */
public class CourseMaterial {
    private String id;
    private Course course;
    private String title;
    private String description;
    private MaterialType materialType;
    private String fileName;
    private String filePath; // Path to stored file or URL for links
    private long fileSizeBytes; // File size in bytes
    private LocalDateTime uploadDate;
    private String uploadedByUserId; // Professor who uploaded

    public CourseMaterial() {
    }

    public CourseMaterial(String id, Course course, String title, String description,
                         MaterialType materialType, String fileName, String filePath,
                         long fileSizeBytes, LocalDateTime uploadDate, String uploadedByUserId) {
        this.id = id;
        this.course = course;
        this.title = title;
        this.description = description;
        this.materialType = materialType;
        this.fileName = fileName;
        this.filePath = filePath;
        this.fileSizeBytes = fileSizeBytes;
        this.uploadDate = uploadDate;
        this.uploadedByUserId = uploadedByUserId;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public MaterialType getMaterialType() {
        return materialType;
    }

    public void setMaterialType(MaterialType materialType) {
        this.materialType = materialType;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public void setFileSizeBytes(long fileSizeBytes) {
        this.fileSizeBytes = fileSizeBytes;
    }

    public LocalDateTime getUploadDate() {
        return uploadDate;
    }

    public void setUploadDate(LocalDateTime uploadDate) {
        this.uploadDate = uploadDate;
    }

    public String getUploadedByUserId() {
        return uploadedByUserId;
    }

    public void setUploadedByUserId(String uploadedByUserId) {
        this.uploadedByUserId = uploadedByUserId;
    }

    /**
     * Get formatted file size (e.g., "2.5 MB")
     */
    public String getFormattedFileSize() {
        if (fileSizeBytes < 1024) {
            return fileSizeBytes + " B";
        } else if (fileSizeBytes < 1024 * 1024) {
            return String.format("%.2f KB", fileSizeBytes / 1024.0);
        } else {
            return String.format("%.2f MB", fileSizeBytes / (1024.0 * 1024.0));
        }
    }

    /**
     * Check if material is a link (not a file)
     */
    public boolean isLink() {
        return materialType == MaterialType.LINK;
    }

    @Override
    public String toString() {
        return title + " [" + materialType + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CourseMaterial)) return false;
        CourseMaterial that = (CourseMaterial) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

