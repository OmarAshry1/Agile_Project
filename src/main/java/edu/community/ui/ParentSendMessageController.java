package edu.community.ui;

import edu.community.service.ParentTeacherMessageService;
import edu.facilities.model.User;
import edu.facilities.service.AuthService;
import javafx.collections.FXCollections;
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
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controller for parents to send messages to teachers.
 * US 4.1 - Send Message to Teacher
 */
public class ParentSendMessageController implements Initializable {

    @FXML private ComboBox<User> studentComboBox;
    @FXML private ComboBox<User> teacherComboBox;
    @FXML private TextField subjectField;
    @FXML private TextArea messageBodyArea;
    @FXML private Button sendButton;
    @FXML private Button cancelButton;

    private ParentTeacherMessageService messageService;
    private AuthService authService;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        messageService = new ParentTeacherMessageService();
        authService = AuthService.getInstance();

        // Access control - only parents
        if (!authService.isLoggedIn()) {
            showAlert(Alert.AlertType.ERROR, "Access Denied", "Please login to send messages.");
            return;
        }

        String userType = authService.getCurrentUserType();
        if (!"PARENT".equals(userType)) {
            showAlert(Alert.AlertType.ERROR, "Access Denied", 
                "Only parents can send messages to teachers. Your user type: " + 
                (userType != null ? userType : "Unknown"));
            return;
        }

        setupStudentComboBox();
        setupTeacherComboBox();
    }

    private void setupStudentComboBox() {
        // Get students linked to this parent
        try {
            int parentUserID = Integer.parseInt(authService.getCurrentUser().getId());
            List<User> students = messageService.getParentStudents(parentUserID);
            
            if (students.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "No Students Linked", 
                    "You don't have any students linked to your account. " +
                    "Please contact the administrator to link your child(ren) to your account.");
                studentComboBox.setItems(FXCollections.observableArrayList());
                studentComboBox.setDisable(true);
                return;
            }
            
            studentComboBox.setItems(FXCollections.observableArrayList(students));
            
            studentComboBox.setCellFactory(param -> new ListCell<User>() {
                @Override
                protected void updateItem(User item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(item.getUsername() + " (ID: " + item.getId() + ")");
                    }
                }
            });
            
            studentComboBox.setOnAction(e -> {
                User selectedStudent = studentComboBox.getValue();
                if (selectedStudent != null) {
                    loadTeachersForStudent(Integer.parseInt(selectedStudent.getId()));
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupTeacherComboBox() {
        teacherComboBox.setCellFactory(param -> new ListCell<User>() {
            @Override
            protected void updateItem(User item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getUsername() + " (ID: " + item.getId() + ")");
                }
            }
        });
    }

    private void loadTeachersForStudent(int studentUserID) {
        try {
            List<User> teachers = messageService.getStudentTeachers(studentUserID);
            teacherComboBox.setItems(FXCollections.observableArrayList(teachers));
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to load teachers: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleSend(ActionEvent event) {
        if (studentComboBox.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Please select your child.");
            return;
        }

        if (teacherComboBox.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Please select a teacher.");
            return;
        }

        if (subjectField.getText() == null || subjectField.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Subject is required.");
            return;
        }

        if (messageBodyArea.getText() == null || messageBodyArea.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Message content is required.");
            return;
        }

        try {
            int parentUserID = Integer.parseInt(authService.getCurrentUser().getId());
            int teacherUserID = Integer.parseInt(teacherComboBox.getValue().getId());
            int studentUserID = Integer.parseInt(studentComboBox.getValue().getId());
            String subject = subjectField.getText().trim();
            String messageBody = messageBodyArea.getText().trim();

            // Verify parent-student relationship before sending
            if (!messageService.verifyParentStudentRelationship(parentUserID, studentUserID)) {
                showAlert(Alert.AlertType.ERROR, "Authorization Error", 
                    "You are not authorized to send messages about this student. " +
                    "Please contact the administrator if you believe this is an error.");
                return;
            }

            boolean success = messageService.sendParentMessage(
                parentUserID, teacherUserID, studentUserID, subject, messageBody
            );

            if (success) {
                showAlert(Alert.AlertType.INFORMATION, "Success", "Message sent successfully!");
                navigateToDashboard(event);
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to send message.");
            }
        } catch (SQLException e) {
            String errorMessage = e.getMessage();
            if (errorMessage != null && errorMessage.contains("not authorized")) {
                showAlert(Alert.AlertType.ERROR, "Authorization Error", errorMessage);
            } else {
                showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to send message: " + errorMessage);
            }
            e.printStackTrace();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to send message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleCancel(ActionEvent event) {
        navigateToDashboard(event);
    }

    private void navigateToDashboard(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/dashboard.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("");
            stage.setMaximized(true);
            stage.show();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Navigation Error", "Could not load dashboard: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

