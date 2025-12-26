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
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controller for reviewing research records (Admin only)
 * US 3.10 - Review Research Records
 */
public class ReviewResearchRecordsController implements Initializable {

    @FXML
    private TableView<ResearchActivity> researchTable;
    @FXML
    private TableColumn<ResearchActivity, String> titleColumn;
    @FXML
    private TableColumn<ResearchActivity, String> typeColumn;
    @FXML
    private TableColumn<ResearchActivity, String> staffNameColumn;
    @FXML
    private TableColumn<ResearchActivity, String> departmentColumn;
    @FXML
    private TableColumn<ResearchActivity, String> publicationDateColumn;
    @FXML
    private ComboBox<String> departmentFilterComboBox;
    @FXML
    private Button filterButton;
    @FXML
    private Button backButton;

    private ResearchActivityService researchService;
    private AuthService authService;
    private ObservableList<ResearchActivity> researchList;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        researchService = new ResearchActivityService();
        authService = AuthService.getInstance();

        // Check admin access
        if (!checkAdminAccess()) {
            return;
        }

        setupTable();
        loadDepartments();
        loadResearchActivities(null);
    }

    private boolean checkAdminAccess() {
        if (authService == null || !authService.isLoggedIn()) {
            showError("Access Denied", "You must be logged in to access this page.");
            return false;
        }

        String userType = authService.getCurrentUserType();
        if (!"ADMIN".equals(userType)) {
            showError("Access Denied", "Only administrators can review research records.");
            return false;
        }
        return true;
    }

    private void setupTable() {
        // Configure columns
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        staffNameColumn.setCellValueFactory(new PropertyValueFactory<>("staffName"));
        departmentColumn.setCellValueFactory(new PropertyValueFactory<>("department"));
        publicationDateColumn.setCellValueFactory(new PropertyValueFactory<>("publicationDate"));

        // Format publication date column
        publicationDateColumn.setCellFactory(column -> new TableCell<ResearchActivity, String>() {
            private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    ResearchActivity activity = getTableView().getItems().get(getIndex());
                    if (activity.getPublicationDate() != null) {
                        setText(activity.getPublicationDate().format(formatter));
                    } else {
                        setText("");
                    }
                }
            }
        });

        // Make table read-only
        researchTable.setEditable(false);
    }

    private void loadDepartments() {
        try {
            List<String> departments = researchService.getAllDepartments();
            ObservableList<String> deptList = FXCollections.observableArrayList();
            deptList.add("All Departments");
            deptList.addAll(departments);
            departmentFilterComboBox.setItems(deptList);
            departmentFilterComboBox.setValue("All Departments");
        } catch (SQLException e) {
            showError("Error", "Failed to load departments: " + e.getMessage());
        }
    }

    @FXML
    private void handleFilter(ActionEvent event) {
        String selectedDepartment = departmentFilterComboBox.getValue();
        loadResearchActivities(selectedDepartment);
    }

    private void loadResearchActivities(String departmentFilter) {
        try {
            List<ResearchActivity> activities = researchService.getAllResearchActivities(departmentFilter);
            researchList = FXCollections.observableArrayList(activities);
            researchTable.setItems(researchList);
        } catch (SQLException e) {
            showError("Error", "Failed to load research activities: " + e.getMessage());
        }
    }

    @FXML
    private void handleBack(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/dashboard.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Dashboard");
            stage.setMaximized(true);
            stage.show();
        } catch (IOException e) {
            showError("Navigation Error", "Could not return to dashboard: " + e.getMessage());
        }
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

