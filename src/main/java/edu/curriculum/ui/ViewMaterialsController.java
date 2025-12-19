package edu.curriculum.ui;

import edu.curriculum.model.CourseMaterial;
import edu.curriculum.service.MaterialService;
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
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Controller for View Course Materials
 * US 2.6 - View Course Materials (Student)
 */
public class ViewMaterialsController {

    @FXML private TableView<CourseMaterial> materialsTable;
    @FXML private TableColumn<CourseMaterial, String> courseColumn;
    @FXML private TableColumn<CourseMaterial, String> titleColumn;
    @FXML private TableColumn<CourseMaterial, String> descriptionColumn;
    @FXML private TableColumn<CourseMaterial, String> typeColumn;
    @FXML private TableColumn<CourseMaterial, String> uploadDateColumn;
    @FXML private TableColumn<CourseMaterial, String> fileSizeColumn;

    @FXML private Button downloadButton;
    @FXML private Button openLinkButton;
    @FXML private Button refreshButton;
    @FXML private Button backButton;

    @FXML private Label statusLabel;

    private ObservableList<CourseMaterial> materialsList = FXCollections.observableArrayList();
    private MaterialService materialService = new MaterialService();
    private AuthService authService = AuthService.getInstance();

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML
    public void initialize() {
        if (!checkStudentAccess()) {
            disableAllControls();
            return;
        }

        setupTableColumns();
        loadMaterials();
    }

    private boolean checkStudentAccess() {
        if (!authService.isLoggedIn()) {
            showError("Access Denied", "Please login to view course materials.");
            return false;
        }

        String userType = authService.getCurrentUserType();
        if (!"STUDENT".equals(userType)) {
            showError("Access Denied", "Only students can view course materials.");
            return false;
        }
        return true;
    }

    private void setupTableColumns() {
        courseColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().getCourse().getCode() + " - " +
                        cellData.getValue().getCourse().getName())
        );

        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));

        descriptionColumn.setCellValueFactory(cellData -> {
            String desc = cellData.getValue().getDescription();
            if (desc != null && desc.length() > 50) {
                desc = desc.substring(0, 50) + "...";
            }
            return new javafx.beans.property.SimpleStringProperty(desc != null ? desc : "");
        });

        typeColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().getMaterialType().toString())
        );

        uploadDateColumn.setCellValueFactory(cellData -> {
            if (cellData.getValue().getUploadDate() != null) {
                return new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().getUploadDate().format(DATE_FORMATTER));
            }
            return new javafx.beans.property.SimpleStringProperty("N/A");
        });

        fileSizeColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().getFormattedFileSize())
        );

        // Update button visibility based on selection
        materialsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                boolean isLink = newSelection.isLink();
                downloadButton.setVisible(!isLink);
                downloadButton.setManaged(!isLink);
                openLinkButton.setVisible(isLink);
                openLinkButton.setManaged(isLink);
            } else {
                downloadButton.setVisible(false);
                downloadButton.setManaged(false);
                openLinkButton.setVisible(false);
                openLinkButton.setManaged(false);
            }
        });
    }

    private void loadMaterials() {
        try {
            User student = authService.getCurrentUser();
            if (student == null) {
                showError("Authentication Error", "User session expired. Please login again.");
                return;
            }

            // US 2.6 - Get materials for enrolled courses, organized by upload date
            List<CourseMaterial> materials = materialService.getMaterialsForStudent(student.getId());
            materialsList.clear();
            materialsList.addAll(materials);
            materialsTable.setItems(materialsList);

            statusLabel.setText("Loaded " + materials.size() + " material(s)");
        } catch (SQLException e) {
            showError("Database Error", "Failed to load materials: " + e.getMessage());
        }
    }

    @FXML
    private void handleDownload() {
        CourseMaterial selectedMaterial = materialsTable.getSelectionModel().getSelectedItem();
        if (selectedMaterial == null) {
            showWarning("No Selection", "Please select a material to download.");
            return;
        }

        if (selectedMaterial.isLink()) {
            showWarning("Invalid Action", "This is a link, not a file. Use 'Open Link' instead.");
            return;
        }

        try {
            Path sourcePath = Paths.get(selectedMaterial.getFilePath());
            if (!Files.exists(sourcePath)) {
                showError("File Not Found", "The material file could not be found on the server.");
                return;
            }

            // Open file location
            File file = sourcePath.toFile();
            try {
                // Use ProcessBuilder to open file location
                String os = System.getProperty("os.name").toLowerCase();
                if (os.contains("win")) {
                    new ProcessBuilder("explorer.exe", "/select,", file.getAbsolutePath()).start();
                } else if (os.contains("mac")) {
                    new ProcessBuilder("open", "-R", file.getAbsolutePath()).start();
                } else {
                    // Linux
                    new ProcessBuilder("xdg-open", file.getParent()).start();
                }
                showInfo("File Location", "File location opened");
            } catch (Exception e) {
                showInfo("File Path", "File path: " + selectedMaterial.getFilePath());
            }
        } catch (Exception e) {
            showError("Error", "Failed to access file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleOpenLink() {
        CourseMaterial selectedMaterial = materialsTable.getSelectionModel().getSelectedItem();
        if (selectedMaterial == null) {
            showWarning("No Selection", "Please select a material to open.");
            return;
        }

        if (!selectedMaterial.isLink()) {
            showWarning("Invalid Action", "This is a file, not a link. Use 'Download' instead.");
            return;
        }

        try {
            String url = selectedMaterial.getFilePath();
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "http://" + url;
            }

            try {
                // Use ProcessBuilder to open URL
                String os = System.getProperty("os.name").toLowerCase();
                if (os.contains("win")) {
                    new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", url).start();
                } else if (os.contains("mac")) {
                    new ProcessBuilder("open", url).start();
                } else {
                    // Linux
                    new ProcessBuilder("xdg-open", url).start();
                }
            } catch (Exception e) {
                showInfo("Link", "Link URL: " + url);
            }
        } catch (Exception e) {
            showError("Error", "Failed to open link: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleRefresh() {
        loadMaterials();
    }

    @FXML
    private void handleBack(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/dashboard.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Dashboard");
            stage.show();
        } catch (IOException e) {
            showError("Navigation Error", "Could not return to dashboard: " + e.getMessage());
        }
    }

    private void disableAllControls() {
        if (materialsTable != null) materialsTable.setDisable(true);
        if (downloadButton != null) downloadButton.setDisable(true);
        if (openLinkButton != null) openLinkButton.setDisable(true);
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

