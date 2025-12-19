package edu.curriculum.service;

import edu.curriculum.model.CourseMaterial;
import edu.curriculum.model.MaterialType;
import edu.facilities.model.Course;
import edu.facilities.service.DatabaseConnection;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for managing course materials
 * US 2.5 - Upload Course Materials
 * US 2.6 - View Course Materials
 */
public class MaterialService {

    private static final long MAX_FILE_SIZE_BYTES = 50 * 1024 * 1024; // 50 MB
    private static final String MATERIALS_DIR = "course_materials"; // Directory to store uploaded files

    /**
     * Upload a course material
     * US 2.5 - Supports PDF, DOCX, PPTX, Links (max 50MB)
     */
    public CourseMaterial uploadMaterial(String courseId, String title, String description,
                                        MaterialType materialType, File file, String linkUrl,
                                        String uploadedByUserId) throws SQLException, IllegalArgumentException, IOException {
        if (courseId == null || courseId.isBlank() || title == null || title.isBlank()) {
            throw new IllegalArgumentException("Course ID and Title are required");
        }

        if (materialType == null) {
            throw new IllegalArgumentException("Material type is required");
        }

        // Validate based on material type
        String filePath = null;
        long fileSize = 0;
        String fileName = null;

        if (materialType == MaterialType.LINK) {
            if (linkUrl == null || linkUrl.isBlank()) {
                throw new IllegalArgumentException("Link URL is required for LINK material type");
            }
            filePath = linkUrl;
            fileName = linkUrl;
        } else {
            if (file == null || !file.exists()) {
                throw new IllegalArgumentException("File is required for non-LINK material types");
            }

            // Validate file extension
            String fileExtension = getFileExtension(file.getName()).toLowerCase();
            if (!isValidFileType(fileExtension, materialType)) {
                throw new IllegalArgumentException(
                    "Invalid file type. Expected: " + getExpectedExtensions(materialType));
            }

            // Validate file size (max 50MB)
            fileSize = file.length();
            if (fileSize > MAX_FILE_SIZE_BYTES) {
                throw new IllegalArgumentException(
                    "File size exceeds maximum limit of 50 MB. Current size: " + 
                    formatFileSize(fileSize));
            }

            // Create materials directory if it doesn't exist
            Path materialsPath = Paths.get(MATERIALS_DIR);
            if (!Files.exists(materialsPath)) {
                Files.createDirectories(materialsPath);
            }

            // Generate unique filename
            String uniqueFileName = System.currentTimeMillis() + "_" + file.getName();
            Path targetPath = materialsPath.resolve(uniqueFileName);

            // Copy file to materials directory
            Files.copy(file.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            filePath = targetPath.toString();
            fileName = file.getName();
        }

        // Insert into database
        Connection conn = DatabaseConnection.getConnection();
        boolean originalAutoCommit = conn.getAutoCommit();

        try {
            conn.setAutoCommit(false);

            String sql = "INSERT INTO CourseMaterials (CourseID, Title, Description, MaterialType, " +
                        "FileName, FilePath, FileSizeBytes, UploadDate, UploadedByUserID) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, GETDATE(), ?)";

            int materialId;
            try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setInt(1, Integer.parseInt(courseId));
                pstmt.setString(2, title);
                pstmt.setString(3, description);
                pstmt.setString(4, materialType.toString());
                pstmt.setString(5, fileName);
                pstmt.setString(6, filePath);
                pstmt.setLong(7, fileSize);
                pstmt.setInt(8, Integer.parseInt(uploadedByUserId));

                int rowsAffected = pstmt.executeUpdate();
                if (rowsAffected > 0) {
                    try (ResultSet keys = pstmt.getGeneratedKeys()) {
                        if (keys.next()) {
                            materialId = keys.getInt(1);
                        } else {
                            conn.rollback();
                            throw new SQLException("Failed to get generated material ID");
                        }
                    }
                } else {
                    conn.rollback();
                    throw new SQLException("Failed to insert material");
                }
            }

            conn.commit();
            return getMaterialById(String.valueOf(materialId));

        } catch (SQLException e) {
            conn.rollback();
            // Clean up uploaded file if database insert failed
            if (filePath != null && !materialType.equals(MaterialType.LINK)) {
                try {
                    Files.deleteIfExists(Paths.get(filePath));
                } catch (Exception ex) {
                    System.err.println("Failed to delete uploaded file: " + ex.getMessage());
                }
            }
            throw e;
        } finally {
            conn.setAutoCommit(originalAutoCommit);
        }
    }

    /**
     * Get all materials for a course
     * US 2.6 - Organized by upload date
     */
    public List<CourseMaterial> getMaterialsByCourse(String courseId) throws SQLException {
        if (courseId == null || courseId.isBlank()) {
            return new ArrayList<>();
        }

        int courseIdInt;
        try {
            courseIdInt = Integer.parseInt(courseId);
        } catch (NumberFormatException e) {
            return new ArrayList<>();
        }

        List<CourseMaterial> materials = new ArrayList<>();
        String sql = "SELECT MaterialID, CourseID, Title, Description, MaterialType, " +
                    "FileName, FilePath, FileSizeBytes, UploadDate, UploadedByUserID " +
                    "FROM CourseMaterials " +
                    "WHERE CourseID = ? " +
                    "ORDER BY UploadDate DESC"; // US 2.6 - Organized by upload date

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, courseIdInt);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    CourseMaterial material = mapResultSetToMaterial(rs, conn);
                    if (material != null) {
                        materials.add(material);
                    }
                }
            }
        }

        return materials;
    }

    /**
     * Get materials for courses a student is enrolled in
     * US 2.6 - View Course Materials (Student)
     */
    public List<CourseMaterial> getMaterialsForStudent(String studentId) throws SQLException {
        if (studentId == null || studentId.isBlank()) {
            return new ArrayList<>();
        }

        int studentIdInt;
        try {
            studentIdInt = Integer.parseInt(studentId);
        } catch (NumberFormatException e) {
            return new ArrayList<>();
        }

        List<CourseMaterial> materials = new ArrayList<>();
        String sql = "SELECT DISTINCT cm.MaterialID, cm.CourseID, cm.Title, cm.Description, " +
                    "cm.MaterialType, cm.FileName, cm.FilePath, cm.FileSizeBytes, " +
                    "cm.UploadDate, cm.UploadedByUserID " +
                    "FROM CourseMaterials cm " +
                    "INNER JOIN Enrollments e ON cm.CourseID = e.CourseID " +
                    "WHERE e.StudentUserID = ? AND e.Status = 'ENROLLED' " +
                    "ORDER BY cm.UploadDate DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, studentIdInt);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    CourseMaterial material = mapResultSetToMaterial(rs, conn);
                    if (material != null) {
                        materials.add(material);
                    }
                }
            }
        }

        return materials;
    }

    /**
     * Get material by ID
     */
    public CourseMaterial getMaterialById(String materialId) throws SQLException {
        if (materialId == null || materialId.isBlank()) {
            return null;
        }

        int materialIdInt;
        try {
            materialIdInt = Integer.parseInt(materialId);
        } catch (NumberFormatException e) {
            return null;
        }

        String sql = "SELECT MaterialID, CourseID, Title, Description, MaterialType, " +
                    "FileName, FilePath, FileSizeBytes, UploadDate, UploadedByUserID " +
                    "FROM CourseMaterials WHERE MaterialID = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, materialIdInt);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToMaterial(rs, conn);
                }
            }
        }

        return null;
    }

    /**
     * Delete a material
     */
    public boolean deleteMaterial(String materialId) throws SQLException {
        if (materialId == null || materialId.isBlank()) {
            return false;
        }

        int materialIdInt;
        try {
            materialIdInt = Integer.parseInt(materialId);
        } catch (NumberFormatException e) {
            return false;
        }

        // Get material to delete associated file
        CourseMaterial material = getMaterialById(materialId);
        if (material != null && !material.isLink()) {
            try {
                Files.deleteIfExists(Paths.get(material.getFilePath()));
            } catch (Exception e) {
                System.err.println("Failed to delete file: " + e.getMessage());
            }
        }

        String sql = "DELETE FROM CourseMaterials WHERE MaterialID = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, materialIdInt);
            return pstmt.executeUpdate() > 0;
        }
    }

    // Helper methods

    private CourseMaterial mapResultSetToMaterial(ResultSet rs, Connection conn) throws SQLException {
        String materialId = String.valueOf(rs.getInt("MaterialID"));
        int courseIdInt = rs.getInt("CourseID");
        String title = rs.getString("Title");
        String description = rs.getString("Description");
        String materialTypeStr = rs.getString("MaterialType");
        String fileName = rs.getString("FileName");
        String filePath = rs.getString("FilePath");
        long fileSizeBytes = rs.getLong("FileSizeBytes");
        Timestamp uploadDateTs = rs.getTimestamp("UploadDate");
        int uploadedByUserId = rs.getInt("UploadedByUserID");

        // Get course - use facilities.CourseService to match facilities.model.Course
        edu.facilities.service.CourseService courseService = new edu.facilities.service.CourseService();
        Course course = courseService.getCourseById(String.valueOf(courseIdInt));
        if (course == null) {
            return null;
        }

        MaterialType materialType = MaterialType.valueOf(materialTypeStr);

        LocalDateTime uploadDate = null;
        if (uploadDateTs != null) {
            uploadDate = uploadDateTs.toLocalDateTime();
        }

        return new CourseMaterial(materialId, course, title, description, materialType,
                                 fileName, filePath, fileSizeBytes, uploadDate,
                                 String.valueOf(uploadedByUserId));
    }

    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(lastDot + 1) : "";
    }

    private boolean isValidFileType(String extension, MaterialType materialType) {
        switch (materialType) {
            case LECTURE:
            case READING:
                return extension.equals("pdf") || extension.equals("docx") || extension.equals("pptx");
            case VIDEO:
                return extension.equals("mp4") || extension.equals("avi") || extension.equals("mov");
            case LINK:
                return true; // Links don't have file extensions
            default:
                return false;
        }
    }

    private String getExpectedExtensions(MaterialType materialType) {
        switch (materialType) {
            case LECTURE:
            case READING:
                return "PDF, DOCX, PPTX";
            case VIDEO:
                return "MP4, AVI, MOV";
            case LINK:
                return "URL";
            default:
                return "Unknown";
        }
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else {
            return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        }
    }
}

