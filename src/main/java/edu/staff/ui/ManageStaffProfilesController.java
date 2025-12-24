package edu.staff.ui;

import edu.staff.model.StaffProfile;
import edu.staff.service.StaffProfileService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.util.Callback;

import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class ManageStaffProfilesController implements Initializable {

    @FXML
    private TextField searchField;
    @FXML
    private ListView<StaffProfile> staffListView;
    @FXML
    private Button addNewButton;

    // Form Fields
    @FXML
    private VBox formContainer;
    @FXML
    private TextField nameField;
    @FXML
    private TextField roleField;
    @FXML
    private ComboBox<String> departmentComboBox;
    @FXML
    private TextField emailField;
    @FXML
    private TextField officeLocationField;
    @FXML
    private TextField officeHoursField;
    @FXML
    private TextField phoneField;
    @FXML
    private DatePicker hireDatePicker;
    @FXML
    private TextArea bioArea;
    @FXML
    private CheckBox isActiveCheckBox;
    @FXML
    private Button saveButton;
    @FXML
    private Button deleteButton;

    private StaffProfileService staffService;
    private ObservableList<StaffProfile> staffList;
    private StaffProfile currentProfile; // The profile currently being edited

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        staffService = new StaffProfileService();
        staffList = FXCollections.observableArrayList();
        staffListView.setItems(staffList);

        setupDepartmentComboBox();
        setupListView();
        setupSearch();

        loadStaffList();
        clearForm(); // Start with clear form
    }

    private void setupDepartmentComboBox() {
        try {
            List<String> departments = staffService.getAllDepartments();
            // Ensure we have some defaults if DB is empty
            if (departments.isEmpty()) {
                departments.addAll(List.of("Computer Science", "Mathematics", "Physics", "Biology", "Chemistry",
                        "Engineering", "Administration"));
            }
            departmentComboBox.setItems(FXCollections.observableArrayList(departments));
        } catch (SQLException e) {
            showError("Error loading departments: " + e.getMessage());
        }
    }

    private void setupListView() {
        staffListView.setCellFactory(new Callback<ListView<StaffProfile>, ListCell<StaffProfile>>() {
            @Override
            public ListCell<StaffProfile> call(ListView<StaffProfile> param) {
                return new ListCell<StaffProfile>() {
                    @Override
                    protected void updateItem(StaffProfile item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                        } else {
                            setText(item.getName() + " - " + item.getRole());
                        }
                    }
                };
            }
        });

        staffListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                populateForm(newVal);
            }
        });
    }

    private void setupSearch() {
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filterList(newValue);
        });
    }

    private void loadStaffList() {
        try {
            List<StaffProfile> profiles = staffService.getAllStaff();
            staffList.setAll(profiles);
        } catch (SQLException e) {
            showError("Error loading staff list: " + e.getMessage());
        }
    }

    private void filterList(String query) {
        if (query == null || query.isEmpty()) {
            loadStaffList();
        } else {
            // Simple client-side filter for responsiveness, or could call DB search
            try {
                // Using DB search for consistency
                List<StaffProfile> results = staffService.searchStaff(query, null);
                staffList.setAll(results);
            } catch (SQLException e) {
                showError("Search error: " + e.getMessage());
            }
        }
    }

    private void populateForm(StaffProfile profile) {
        currentProfile = profile;

        nameField.setText(profile.getName());
        roleField.setText(profile.getRole());
        departmentComboBox.setValue(profile.getDepartment());
        emailField.setText(profile.getEmail());
        officeLocationField.setText(profile.getOfficeLocation());
        officeHoursField.setText(profile.getOfficeHours());
        phoneField.setText(profile.getPhone());
        hireDatePicker.setValue(profile.getHireDate());
        bioArea.setText(profile.getBio());
        isActiveCheckBox.setSelected(profile.isActive());

        deleteButton.setDisable(false);
        saveButton.setText("Update Profile");
    }

    @FXML
    private void handleAddNew() {
        staffListView.getSelectionModel().clearSelection();
        clearForm();
    }

    private void clearForm() {
        currentProfile = null;

        nameField.clear();
        roleField.clear();
        departmentComboBox.getSelectionModel().clearSelection();
        emailField.clear();
        officeLocationField.clear();
        officeHoursField.clear();
        phoneField.clear();
        hireDatePicker.setValue(null);
        bioArea.clear();
        isActiveCheckBox.setSelected(true);

        deleteButton.setDisable(true);
        saveButton.setText("Save New Profile");

        nameField.requestFocus();
    }

    @FXML
    private void handleSave() {
        if (!validateInput()) {
            return;
        }

        try {
            boolean isNew = (currentProfile == null);
            StaffProfile profile = isNew ? new StaffProfile() : currentProfile;

            profile.setName(nameField.getText().trim());
            profile.setRole(roleField.getText().trim());
            profile.setDepartment(departmentComboBox.getValue());
            profile.setEmail(emailField.getText().trim());
            profile.setOfficeLocation(officeLocationField.getText().trim());
            profile.setOfficeHours(officeHoursField.getText().trim());
            profile.setPhone(phoneField.getText().trim());
            profile.setHireDate(hireDatePicker.getValue());
            profile.setBio(bioArea.getText().trim());
            profile.setActive(isActiveCheckBox.isSelected());

            if (isNew) {
                staffService.addStaff(profile);
                showInfo("Staff profile added successfully.");
            } else {
                staffService.updateStaff(profile);
                showInfo("Staff profile updated successfully.");
            }

            loadStaffList();
            clearForm();

        } catch (SQLException e) {
            showError("Error saving profile: " + e.getMessage());
        }
    }

    @FXML
    private void handleDelete() {
        if (currentProfile == null)
            return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Delete");
        alert.setHeaderText("Delete Staff Profile");
        alert.setContentText("Are you sure you want to delete " + currentProfile.getName() + "?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                staffService.deleteStaff(currentProfile.getStaffID());
                loadStaffList();
                clearForm();
                showInfo("Profile deleted.");
            } catch (SQLException e) {
                showError("Error deleting profile: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleCancel() {
        clearForm();
    }

    private boolean validateInput() {
        StringBuilder errorMessage = new StringBuilder();

        if (nameField.getText() == null || nameField.getText().trim().isEmpty()) {
            errorMessage.append("Name is required.\n");
        }
        if (roleField.getText() == null || roleField.getText().trim().isEmpty()) {
            errorMessage.append("Role is required.\n");
        }
        if (departmentComboBox.getValue() == null) {
            errorMessage.append("Department is required.\n");
        }
        if (emailField.getText() == null || emailField.getText().trim().isEmpty()) {
            errorMessage.append("Email is required.\n");
        }

        if (errorMessage.length() == 0) {
            return true;
        } else {
            showError("Please correct the following errors:\n" + errorMessage.toString());
            return false;
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
