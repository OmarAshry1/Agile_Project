package edu.curriculum.ui;

import edu.curriculum.model.TranscriptEntry;
import edu.curriculum.service.TranscriptViewService;
import edu.facilities.model.User;
import edu.facilities.service.AuthService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/**
 * Controller for View Transcript Screen (view_transcript.fxml)
 * US: As a student, I want to view my transcript so that I can track academic progress
 */
public class ViewTranscriptController {

    @FXML private TableView<TranscriptEntry> transcriptTable;
    @FXML private TableColumn<TranscriptEntry, String> semesterColumn;
    @FXML private TableColumn<TranscriptEntry, String> codeColumn;
    @FXML private TableColumn<TranscriptEntry, String> nameColumn;
    @FXML private TableColumn<TranscriptEntry, String> creditsColumn;
    @FXML private TableColumn<TranscriptEntry, String> gradeColumn;
    
    @FXML private Label studentInfoLabel;
    @FXML private Label gpaLabel;
    @FXML private Label creditsLabel;
    @FXML private Label coursesLabel;
    @FXML private Button exportPdfButton;
    @FXML private Button refreshButton;
    @FXML private Button backButton;
    @FXML private Label statusLabel;

    private ObservableList<TranscriptEntry> transcriptList = FXCollections.observableArrayList();
    private TranscriptViewService transcriptService = new TranscriptViewService();
    private AuthService authService = AuthService.getInstance();

    @FXML
    public void initialize() {
        // Check if user is logged in
        if (!authService.isLoggedIn()) {
            showError("Access Denied", "Please login to view transcript.");
            disableAllControls();
            return;
        }

        // Check user type - only STUDENT can view transcript
        String userType = authService.getCurrentUserType();
        if (!"STUDENT".equals(userType)) {
            showError("Access Denied", "Only students can view their transcript.");
            disableAllControls();
            return;
        }

        // Setup table columns
        setupTableColumns();

        // Load transcript data
        loadTranscript();

        System.out.println("ViewTranscriptController initialized successfully!");
    }

    /**
     * Setup table columns
     */
    private void setupTableColumns() {
        semesterColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getSemester())
        );

        codeColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getCourseCode())
        );

        nameColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getCourseName())
        );

        creditsColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(String.valueOf(cellData.getValue().getCredits()))
        );

        gradeColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getFinalGrade())
        );
    }

    /**
     * Load transcript data
     */
    private void loadTranscript() {
        try {
            User currentUser = authService.getCurrentUser();
            if (currentUser == null) {
                return;
            }

            // Get transcript entries
            List<TranscriptEntry> entries = transcriptService.getTranscriptEntries(currentUser);
            transcriptList.setAll(entries);
            transcriptTable.setItems(transcriptList);

            // Update student info
            studentInfoLabel.setText("Student: " + transcriptService.getStudentName(currentUser) + 
                                    " (ID: " + transcriptService.getStudentId(currentUser) + ")");

            // Calculate and display statistics
            double gpa = transcriptService.calculateGPA(entries);
            int totalCredits = transcriptService.calculateTotalCredits(entries);
            int courseCount = entries.size();

            gpaLabel.setText(String.format("Cumulative GPA: %.2f", gpa));
            creditsLabel.setText("Completed Credits: " + totalCredits);
            coursesLabel.setText("Completed Courses: " + courseCount);

            showStatus("Loaded " + courseCount + " completed courses", true);
        } catch (SQLException e) {
            showError("Database Error", "Failed to load transcript: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Export transcript as PDF
     */
    @FXML
    private void handleExportPDF() {
        try {
            User currentUser = authService.getCurrentUser();
            if (currentUser == null) {
                return;
            }

            // Show file save dialog
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save Transcript as PDF");
            fileChooser.setInitialFileName("Transcript_" + currentUser.getUsername() + ".pdf");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("PDF Files", "*.pdf")
            );

            Stage stage = (Stage) exportPdfButton.getScene().getWindow();
            File file = fileChooser.showSaveDialog(stage);

            if (file != null) {
                exportToPDF(file);
                showStatus("Transcript exported successfully to: " + file.getName(), true);
            }
        } catch (Exception e) {
            showError("Export Error", "Failed to export transcript: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Export transcript to PDF file using Apache PDFBox
     * Note: Requires PDFBox library to be available (run mvn install first)
     */
    private void exportToPDF(File file) throws Exception {
        User currentUser = authService.getCurrentUser();
        List<TranscriptEntry> entries = transcriptService.getTranscriptEntries(currentUser);
        
        // Try to use PDFBox if available, otherwise fall back to text file
        try {
            // Use reflection to check if PDFBox is available
            Class<?> pdDocumentClass = Class.forName("org.apache.pdfbox.pdmodel.PDDocument");
            Class<?> pdPageClass = Class.forName("org.apache.pdfbox.pdmodel.PDPage");
            Class<?> contentStreamClass = Class.forName("org.apache.pdfbox.pdmodel.PDPageContentStream");
            Class<?> fontClass = Class.forName("org.apache.pdfbox.pdmodel.font.PDType1Font");
            
            // PDFBox is available - use it
            Object document = pdDocumentClass.getDeclaredConstructor().newInstance();
            Object page = pdPageClass.getDeclaredConstructor().newInstance();
            pdDocumentClass.getMethod("addPage", pdPageClass).invoke(document, page);
            
            Object contentStream = contentStreamClass.getDeclaredConstructor(
                    pdDocumentClass, pdPageClass).newInstance(document, page);
            
            float margin = 50;
            float yPosition = 750;
            float lineHeight = 20;
            float fontSize = 12;
            float titleFontSize = 16;
            
            // Get font constants
            Object helveticaBold = fontClass.getField("HELVETICA_BOLD").get(null);
            Object helvetica = fontClass.getField("HELVETICA").get(null);
            
            // Title
            contentStreamClass.getMethod("setFont", fontClass, float.class)
                    .invoke(contentStream, helveticaBold, titleFontSize);
            contentStreamClass.getMethod("beginText").invoke(contentStream);
            contentStreamClass.getMethod("newLineAtOffset", float.class, float.class)
                    .invoke(contentStream, margin, yPosition);
            contentStreamClass.getMethod("showText", String.class)
                    .invoke(contentStream, "ACADEMIC TRANSCRIPT");
            contentStreamClass.getMethod("endText").invoke(contentStream);
            yPosition -= lineHeight * 2;
            
            // Student Information
            contentStreamClass.getMethod("setFont", fontClass, float.class)
                    .invoke(contentStream, helvetica, fontSize);
            contentStreamClass.getMethod("beginText").invoke(contentStream);
            contentStreamClass.getMethod("newLineAtOffset", float.class, float.class)
                    .invoke(contentStream, margin, yPosition);
            contentStreamClass.getMethod("showText", String.class)
                    .invoke(contentStream, "Student: " + transcriptService.getStudentName(currentUser));
            contentStreamClass.getMethod("endText").invoke(contentStream);
            yPosition -= lineHeight;
            
            contentStreamClass.getMethod("beginText").invoke(contentStream);
            contentStreamClass.getMethod("newLineAtOffset", float.class, float.class)
                    .invoke(contentStream, margin, yPosition);
            contentStreamClass.getMethod("showText", String.class)
                    .invoke(contentStream, "Student ID: " + transcriptService.getStudentId(currentUser));
            contentStreamClass.getMethod("endText").invoke(contentStream);
            yPosition -= lineHeight * 2;
            
            // Statistics
            double gpa = transcriptService.calculateGPA(entries);
            int totalCredits = transcriptService.calculateTotalCredits(entries);
            
            contentStreamClass.getMethod("beginText").invoke(contentStream);
            contentStreamClass.getMethod("newLineAtOffset", float.class, float.class)
                    .invoke(contentStream, margin, yPosition);
            contentStreamClass.getMethod("showText", String.class)
                    .invoke(contentStream, "Cumulative GPA: " + String.format("%.2f", gpa));
            contentStreamClass.getMethod("endText").invoke(contentStream);
            yPosition -= lineHeight;
            
            contentStreamClass.getMethod("beginText").invoke(contentStream);
            contentStreamClass.getMethod("newLineAtOffset", float.class, float.class)
                    .invoke(contentStream, margin, yPosition);
            contentStreamClass.getMethod("showText", String.class)
                    .invoke(contentStream, "Total Credits: " + totalCredits);
            contentStreamClass.getMethod("endText").invoke(contentStream);
            yPosition -= lineHeight * 2;
            
            // Table Header
            contentStreamClass.getMethod("setFont", fontClass, float.class)
                    .invoke(contentStream, helveticaBold, fontSize);
            float tableY = yPosition;
            contentStreamClass.getMethod("beginText").invoke(contentStream);
            contentStreamClass.getMethod("newLineAtOffset", float.class, float.class)
                    .invoke(contentStream, margin, tableY);
            contentStreamClass.getMethod("showText", String.class)
                    .invoke(contentStream, "COURSE HISTORY");
            contentStreamClass.getMethod("endText").invoke(contentStream);
            tableY -= lineHeight * 1.5f;
            
            // Column headers
            String[] headers = {"Semester", "Code", "Course Name", "Credits", "Grade"};
            float[] xPositions = {margin, margin + 120, margin + 200, margin + 500, margin + 570};
            
            for (int i = 0; i < headers.length; i++) {
                contentStreamClass.getMethod("beginText").invoke(contentStream);
                contentStreamClass.getMethod("newLineAtOffset", float.class, float.class)
                        .invoke(contentStream, xPositions[i], tableY);
                contentStreamClass.getMethod("showText", String.class)
                        .invoke(contentStream, headers[i]);
                contentStreamClass.getMethod("endText").invoke(contentStream);
            }
            tableY -= lineHeight;
            
            // Draw line under header
            contentStreamClass.getMethod("moveTo", float.class, float.class)
                    .invoke(contentStream, margin, tableY);
            contentStreamClass.getMethod("lineTo", float.class, float.class)
                    .invoke(contentStream, 550, tableY);
            contentStreamClass.getMethod("stroke").invoke(contentStream);
            tableY -= lineHeight * 0.5f;
            
            // Course entries
            contentStreamClass.getMethod("setFont", fontClass, float.class)
                    .invoke(contentStream, helvetica, fontSize);
            for (TranscriptEntry entry : entries) {
                if (tableY < 100) {
                    // Add new page if needed
                    contentStreamClass.getMethod("close").invoke(contentStream);
                    Object newPage = pdPageClass.getDeclaredConstructor().newInstance();
                    pdDocumentClass.getMethod("addPage", pdPageClass).invoke(document, newPage);
                    contentStream = contentStreamClass.getDeclaredConstructor(
                            pdDocumentClass, pdPageClass).newInstance(document, newPage);
                    tableY = 750;
                }
                
                float[] entryXPositions = {margin, margin + 120, margin + 200, margin + 500, margin + 570};
                String[] entryValues = {
                    entry.getSemester(),
                    entry.getCourseCode(),
                    entry.getCourseName().length() > 35 ? entry.getCourseName().substring(0, 32) + "..." : entry.getCourseName(),
                    String.valueOf(entry.getCredits()),
                    entry.getFinalGrade()
                };
                
                for (int i = 0; i < entryValues.length; i++) {
                    contentStreamClass.getMethod("beginText").invoke(contentStream);
                    contentStreamClass.getMethod("newLineAtOffset", float.class, float.class)
                            .invoke(contentStream, entryXPositions[i], tableY);
                    contentStreamClass.getMethod("showText", String.class)
                            .invoke(contentStream, entryValues[i]);
                    contentStreamClass.getMethod("endText").invoke(contentStream);
                }
                
                tableY -= lineHeight;
            }
            
            contentStreamClass.getMethod("close").invoke(contentStream);
            pdDocumentClass.getMethod("save", File.class).invoke(document, file);
            pdDocumentClass.getMethod("close").invoke(document);
            
        } catch (ClassNotFoundException e) {
            // PDFBox not available - export as text file instead
            exportToTextFile(file, currentUser, entries);
            throw new Exception("PDFBox library not available. Exported as text file instead. Please run 'mvn install' to download PDFBox dependency.");
        }
    }
    
    /**
     * Fallback: Export transcript as text file
     */
    private void exportToTextFile(File file, User currentUser, List<TranscriptEntry> entries) throws IOException {
        StringBuilder content = new StringBuilder();
        content.append("ACADEMIC TRANSCRIPT\n");
        content.append("===================\n\n");
        content.append("Student: ").append(transcriptService.getStudentName(currentUser)).append("\n");
        content.append("Student ID: ").append(transcriptService.getStudentId(currentUser)).append("\n\n");
        
        double gpa = transcriptService.calculateGPA(entries);
        int totalCredits = transcriptService.calculateTotalCredits(entries);
        
        content.append("Cumulative GPA: ").append(String.format("%.2f", gpa)).append("\n");
        content.append("Total Credits: ").append(totalCredits).append("\n\n");
        
        content.append("COURSE HISTORY\n");
        content.append("==============\n\n");
        content.append(String.format("%-15s %-12s %-40s %-8s %-10s\n", 
                "Semester", "Code", "Course Name", "Credits", "Grade"));
        content.append("------------------------------------------------------------------------\n");
        
        for (TranscriptEntry entry : entries) {
            content.append(String.format("%-15s %-12s %-40s %-8d %-10s\n",
                    entry.getSemester(),
                    entry.getCourseCode(),
                    entry.getCourseName().length() > 40 ? entry.getCourseName().substring(0, 37) + "..." : entry.getCourseName(),
                    entry.getCredits(),
                    entry.getFinalGrade()));
        }
        
        java.nio.file.Files.write(file.toPath(), content.toString().getBytes());
    }

    @FXML
    private void handleRefresh() {
        loadTranscript();
        showStatus("Transcript refreshed", true);
    }

    @FXML
    private void handleBack(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/dashboard.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));        stage.setMaximized(true);stage.show();
        } catch (IOException e) {
            showError("Navigation Error", "Unable to return to dashboard: " + e.getMessage());
        }
    }

    private void disableAllControls() {
        if (exportPdfButton != null) exportPdfButton.setDisable(true);
        if (refreshButton != null) refreshButton.setDisable(true);
        if (transcriptTable != null) transcriptTable.setDisable(true);
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showStatus(String message, boolean success) {
        if (statusLabel != null) {
            statusLabel.setText(message);
            statusLabel.setVisible(true);
            statusLabel.setStyle(success ? "-fx-text-fill: #4CAF50;" : "-fx-text-fill: #E53935;");
        }
    }
}

