package edu.staff.ui;

import edu.staff.model.ResearchActivity;
import edu.staff.service.ResearchActivityService;
import edu.facilities.service.AuthService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.ResourceBundle;

/**
 * Controller for adding research activities (Staff only)
 * US 3.9 - Add Research Activity
 */
public class AddResearchActivityController implements Initializable {

    @FXML
    private TextField titleField;
    @FXML
    private ComboBox<String> typeComboBox;
    @FXML
    private DatePicker publicationDatePicker;
    @FXML
    private TextArea descriptionArea;
    @FXML
    private TextField journalNameField;
    @FXML
    private TextField conferenceNameField;
    @FXML
    private TextField publisherField;
    @FXML
    private TextField doiField;
    @FXML
    private TextField urlField;
    @FXML
    private Button saveButton;
    @FXML
    private Button backButton;

    private ResearchActivityService researchService;
    private AuthService authService;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        researchService = new ResearchActivityService();
        authService = AuthService.getInstance();

        // Check staff access
        if (!checkStaffAccess()) {
            return;
        }

        setupTypeComboBox();
    }

    private boolean checkStaffAccess() {
        if (authService == null || !authService.isLoggedIn()) {
            showError("Access Denied", "You must be logged in to access this page.");
            return false;
        }

        String userType = authService.getCurrentUserType();
        if (!"STAFF".equals(userType)) {
            showError("Access Denied", "Only staff members can add research activities.");
            return false;
        }
        return true;
    }

    private void setupTypeComboBox() {
        ObservableList<String> types = FXCollections.observableArrayList(
            Arrays.asList("JOURNAL", "CONFERENCE", "BOOK", "PATENT", "THESIS", "OTHER")
        );
        typeComboBox.setItems(types);
    }

    @FXML
    private void handleSave(ActionEvent event) {
        if (!validateInput()) {
            return;
        }

        try {
            ResearchActivity activity = new ResearchActivity();
            activity.setStaffUserID(Integer.parseInt(authService.getCurrentUser().getId()));
            activity.setTitle(titleField.getText().trim());
            activity.setType(typeComboBox.getValue());
            activity.setPublicationDate(publicationDatePicker.getValue());
            
            if (descriptionArea.getText() != null && !descriptionArea.getText().trim().isEmpty()) {
                activity.setDescription(descriptionArea.getText().trim());
            }
            
            if (journalNameField.getText() != null && !journalNameField.getText().trim().isEmpty()) {
                activity.setJournalName(journalNameField.getText().trim());
            }
            
            if (conferenceNameField.getText() != null && !conferenceNameField.getText().trim().isEmpty()) {
                activity.setConferenceName(conferenceNameField.getText().trim());
            }
            
            if (publisherField.getText() != null && !publisherField.getText().trim().isEmpty()) {
                activity.setPublisher(publisherField.getText().trim());
            }
            
            if (doiField.getText() != null && !doiField.getText().trim().isEmpty()) {
                activity.setDoi(doiField.getText().trim());
            }
            
            if (urlField.getText() != null && !urlField.getText().trim().isEmpty()) {
                activity.setUrl(urlField.getText().trim());
            }

            researchService.addResearchActivity(activity);
            showInfo("Success", "Research activity added successfully.");
            clearForm();
        } catch (SQLException e) {
            showError("Error", "Failed to add research activity: " + e.getMessage());
        } catch (NumberFormatException e) {
            showError("Error", "Invalid user ID.");
        }
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

    private boolean validateInput() {
        StringBuilder errors = new StringBuilder();

        if (titleField.getText() == null || titleField.getText().trim().isEmpty()) {
            errors.append("Title is required.\n");
        }

        if (typeComboBox.getValue() == null || typeComboBox.getValue().isEmpty()) {
            errors.append("Type is required.\n");
        }

        if (publicationDatePicker.getValue() == null) {
            errors.append("Publication date is required.\n");
        }

        if (errors.length() > 0) {
            showError("Validation Error", "Please correct the following errors:\n" + errors.toString());
            return false;
        }

        return true;
    }

    private void clearForm() {
        titleField.clear();
        typeComboBox.getSelectionModel().clearSelection();
        publicationDatePicker.setValue(null);
        descriptionArea.clear();
        journalNameField.clear();
        conferenceNameField.clear();
        publisherField.clear();
        doiField.clear();
        urlField.clear();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
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

