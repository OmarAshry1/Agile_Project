package edu.facilities.ui;

import edu.facilities.service.AuthService;
import edu.facilities.service.DatabaseConnection;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Controller for Staff Directory
 */
public class StaffDirectoryController {

    @FXML private TableView<StaffMember> staffTable;
    @FXML private TableColumn<StaffMember, String> userIdColumn;
    @FXML private TableColumn<StaffMember, String> usernameColumn;
    @FXML private TableColumn<StaffMember, String> emailColumn;
    @FXML private TableColumn<StaffMember, String> roleColumn;
    @FXML private TableColumn<StaffMember, String> departmentColumn;
    @FXML private TableColumn<StaffMember, String> phoneColumn;
    @FXML private TableColumn<StaffMember, String> officeColumn;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> departmentFilter;
    @FXML private ComboBox<String> roleFilter;

    @FXML private Button backButton;
    @FXML private Label totalStaffLabel;
    @FXML private Label professorsLabel;
    @FXML private Label staffMembersLabel;

    private ObservableList<StaffMember> staffList = FXCollections.observableArrayList();
    private AuthService authService = AuthService.getInstance();

    @FXML
    public void initialize() {
        if (!checkAccess()) {
            disableAllControls();
            return;
        }

        setupTableColumns();
        populateFilters();
        loadStaff();
        updateStatistics();
    }

    private boolean checkAccess() {
        if (!authService.isLoggedIn()) {
            showError("Access Denied", "Please login to access this feature.");
            return false;
        }
        return true;
    }

    private void setupTableColumns() {
        userIdColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().userId)
        );

        usernameColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().username)
        );

        emailColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().email)
        );

        roleColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().role)
        );

        departmentColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().department)
        );

        phoneColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().phone)
        );

        officeColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().office)
        );

        staffTable.setItems(staffList);
    }

    private void populateFilters() {
        roleFilter.getItems().addAll("All Roles", "PROFESSOR", "STAFF", "ADMIN");
        roleFilter.setValue("All Roles");

        departmentFilter.getItems().add("All Departments");
        departmentFilter.setValue("All Departments");
    }

    private void loadStaff() {
        staffList.clear();
        String sql = "SELECT DISTINCT u.UserID, u.Username, u.Email, ut.TypeCode as Role " +
                     "FROM Users u " +
                     "INNER JOIN UserRoles ur ON u.UserID = ur.UserID AND ur.IsPrimary = true " +
                     "INNER JOIN UserTypes ut ON ur.UserTypeID = ut.UserTypeID " +
                     "WHERE ut.TypeCode IN ('STAFF', 'PROFESSOR', 'ADMIN') " +
                     "ORDER BY u.Username";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                StaffMember member = new StaffMember();
                member.userId = rs.getString("UserID");
                member.username = rs.getString("Username");
                member.email = rs.getString("Email");
                member.role = rs.getString("Role");
                member.department = "N/A";
                member.phone = "N/A";
                member.office = "N/A";
                staffList.add(member);
            }
        } catch (SQLException e) {
            showError("Database Error", "Failed to load staff: " + e.getMessage());
        }
    }

    private void updateStatistics() {
        int total = staffList.size();
        long professors = staffList.stream().filter(s -> "PROFESSOR".equals(s.role)).count();
        long staff = staffList.stream().filter(s -> "STAFF".equals(s.role)).count();

        if (totalStaffLabel != null) totalStaffLabel.setText(String.valueOf(total));
        if (professorsLabel != null) professorsLabel.setText(String.valueOf(professors));
        if (staffMembersLabel != null) staffMembersLabel.setText(String.valueOf(staff));
    }

    @FXML
    private void handleSearch() {
        loadStaff();
    }

    @FXML
    private void handleFilterDepartment() {
        loadStaff();
    }

    @FXML
    private void handleFilterRole() {
        loadStaff();
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

    private void disableAllControls() {
        if (staffTable != null) staffTable.setDisable(true);
        if (searchField != null) searchField.setDisable(true);
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Inner class for staff member data
    public static class StaffMember {
        public String userId;
        public String username;
        public String email;
        public String role;
        public String department;
        public String phone;
        public String office;
    }
}

