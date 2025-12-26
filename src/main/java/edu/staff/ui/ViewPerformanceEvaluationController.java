package edu.staff.ui;

import edu.staff.model.PerformanceEvaluation;
import edu.staff.service.PerformanceEvaluationService;
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
 * Controller for viewing performance evaluations (Staff only)
 * US 3.8 - View Performance Evaluation
 */
public class ViewPerformanceEvaluationController implements Initializable {

    @FXML
    private TableView<PerformanceEvaluation> evaluationsTable;
    @FXML
    private TableColumn<PerformanceEvaluation, String> periodColumn;
    @FXML
    private TableColumn<PerformanceEvaluation, Double> scoreColumn;
    @FXML
    private TableColumn<PerformanceEvaluation, String> dateColumn;
    @FXML
    private TableColumn<PerformanceEvaluation, String> notesColumn;
    @FXML
    private Label staffNameLabel;
    @FXML
    private Button backButton;

    private PerformanceEvaluationService evaluationService;
    private AuthService authService;
    private ObservableList<PerformanceEvaluation> evaluationsList;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        evaluationService = new PerformanceEvaluationService();
        authService = AuthService.getInstance();

        // Check staff access
        if (!checkStaffAccess()) {
            return;
        }

        setupTable();
        loadEvaluations();
    }

    private boolean checkStaffAccess() {
        if (authService == null || !authService.isLoggedIn()) {
            showError("Access Denied", "You must be logged in to access this page.");
            return false;
        }

        String userType = authService.getCurrentUserType();
        if (!"STAFF".equals(userType)) {
            showError("Access Denied", "Only staff members can view their performance evaluations.");
            return false;
        }
        return true;
    }

    private void setupTable() {
        // Configure columns
        periodColumn.setCellValueFactory(new PropertyValueFactory<>("evaluationPeriod"));
        scoreColumn.setCellValueFactory(new PropertyValueFactory<>("score"));
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("evaluationDate"));
        notesColumn.setCellValueFactory(new PropertyValueFactory<>("notes"));

        // Format date column
        dateColumn.setCellFactory(column -> new TableCell<PerformanceEvaluation, String>() {
            private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    PerformanceEvaluation eval = getTableView().getItems().get(getIndex());
                    if (eval.getEvaluationDate() != null) {
                        setText(eval.getEvaluationDate().format(formatter));
                    } else {
                        setText("");
                    }
                }
            }
        });

        // Format score column
        scoreColumn.setCellFactory(column -> new TableCell<PerformanceEvaluation, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("%.2f", item));
                }
            }
        });

        // Make table read-only
        evaluationsTable.setEditable(false);
    }

    private void loadEvaluations() {
        try {
            int staffUserID = Integer.parseInt(authService.getCurrentUser().getId());
            List<PerformanceEvaluation> evaluations = evaluationService.getEvaluationsByStaffUserID(staffUserID);
            
            evaluationsList = FXCollections.observableArrayList(evaluations);
            evaluationsTable.setItems(evaluationsList);

            // Update staff name label
            if (!evaluations.isEmpty() && evaluations.get(0).getStaffName() != null) {
                staffNameLabel.setText("Evaluations for: " + evaluations.get(0).getStaffName());
            } else {
                staffNameLabel.setText("My Performance Evaluations");
            }
        } catch (SQLException e) {
            showError("Error", "Failed to load evaluations: " + e.getMessage());
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

