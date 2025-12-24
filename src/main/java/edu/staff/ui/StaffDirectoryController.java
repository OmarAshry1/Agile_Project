package edu.staff.ui;

import edu.staff.model.StaffProfile;
import edu.staff.service.StaffProfileService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;

public class StaffDirectoryController implements Initializable {

    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> departmentFilter;
    @FXML
    private FlowPane staffContainer;
    @FXML
    private Label statusLabel;
    @FXML
    private Button searchButton;
    @FXML
    private Button clearButton;

    private StaffProfileService staffService;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        staffService = new StaffProfileService();
        setupDepartmentFilter();
        loadStaffProfiles(null, null);
    }

    private void setupDepartmentFilter() {
        try {
            List<String> departments = staffService.getAllDepartments();
            departments.add(0, "All Departments");
            departmentFilter.setItems(FXCollections.observableArrayList(departments));
            departmentFilter.getSelectionModel().selectFirst();

            departmentFilter.setOnAction(e -> handleSearch());
        } catch (SQLException e) {
            showError("Error loading departments: " + e.getMessage());
        }
    }

    @FXML
    private void handleSearch() {
        String query = searchField.getText().trim();
        String dept = departmentFilter.getSelectionModel().getSelectedItem();
        loadStaffProfiles(query, dept);
    }

    @FXML
    private void handleClear() {
        searchField.clear();
        departmentFilter.getSelectionModel().selectFirst();
        loadStaffProfiles(null, null);
    }

    private void loadStaffProfiles(String query, String department) {
        staffContainer.getChildren().clear();
        statusLabel.setText("Loading...");

        try {
            List<StaffProfile> profiles = staffService.searchStaff(query, department);

            if (profiles.isEmpty()) {
                Label noResults = new Label("No staff members found matching your criteria.");
                noResults.setFont(Font.font("System", 16));
                noResults.setTextFill(Color.DARKGRAY);
                staffContainer.getChildren().add(noResults);
            } else {
                for (StaffProfile profile : profiles) {
                    if (profile.isActive()) {
                        staffContainer.getChildren().add(createStaffCard(profile));
                    }
                }
            }
            statusLabel.setText("Found " + profiles.size() + " staff member(s).");
        } catch (SQLException e) {
            showError("Error loading staff profiles: " + e.getMessage());
            statusLabel.setText("Error loading data.");
        }
    }

    private VBox createStaffCard(StaffProfile profile) {
        VBox card = new VBox(5);
        card.setPadding(new Insets(15));
        card.setPrefWidth(280);
        card.setStyle(
                "-fx-background-color: white; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 0); -fx-background-radius: 5;");

        Label nameLabel = new Label(profile.getName());
        nameLabel.setFont(Font.font("System", FontWeight.BOLD, 18));
        nameLabel.setTextFill(Color.web("#2c3e50"));

        Label roleLabel = new Label(profile.getRole());
        roleLabel.setFont(Font.font("System", FontWeight.NORMAL, 14));
        roleLabel.setTextFill(Color.web("#7f8c8d"));
        roleLabel.setStyle("-fx-font-style: italic;");

        Label deptLabel = new Label(profile.getDepartment());
        deptLabel.setStyle(
                "-fx-background-color: #3498db; -fx-text-fill: white; -fx-padding: 3 8 3 8; -fx-background-radius: 10;");

        Separator sep = new Separator();
        sep.setPadding(new Insets(5, 0, 5, 0));

        VBox contactBox = new VBox(3);
        contactBox.getChildren().add(createIconLabel("📧 " + profile.getEmail()));
        if (profile.getPhone() != null && !profile.getPhone().isEmpty()) {
            contactBox.getChildren().add(createIconLabel("📞 " + profile.getPhone()));
        }
        if (profile.getOfficeLocation() != null && !profile.getOfficeLocation().isEmpty()) {
            contactBox.getChildren().add(createIconLabel("📍 " + profile.getOfficeLocation()));
        }
        if (profile.getOfficeHours() != null && !profile.getOfficeHours().isEmpty()) {
            contactBox.getChildren().add(createIconLabel("🕒 " + profile.getOfficeHours()));
        }

        // Bio tooltip or expander could be added here
        if (profile.getBio() != null && !profile.getBio().isEmpty()) {
            Tooltip bioTooltip = new Tooltip(profile.getBio());
            bioTooltip.setPrefWidth(300);
            bioTooltip.setWrapText(true);
            Tooltip.install(card, bioTooltip);
        }

        card.getChildren().addAll(nameLabel, roleLabel, deptLabel, sep, contactBox);
        return card;
    }

    private Label createIconLabel(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        return label;
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
